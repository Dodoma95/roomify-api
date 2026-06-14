package com.roomify.domain.service.user;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.roomify.domain.api.UserApi;
import com.roomify.domain.models.RoleActionEnum;
import com.roomify.domain.models.RoleEnum;
import com.roomify.domain.models.UserSearchFilter;
import com.roomify.domain.service.auth.AuthService;
import com.roomify.domain.service.user.mapper.UserMapper;
import com.roomify.domain.spi.RoleSpi;
import com.roomify.domain.spi.UserSpi;
import com.roomify.infrastucture.models.user.Role;
import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.in.PageInfoInput;
import com.roomify.presentation.models.in.UpdateMeRequest;
import com.roomify.presentation.models.in.UpdateUserRoleRequest;
import com.roomify.presentation.models.in.UserFilterInput;
import com.roomify.presentation.models.out.PageInfo;
import com.roomify.presentation.models.out.UserAdminResponse;
import com.roomify.presentation.models.out.UserPage;
import com.roomify.presentation.models.out.UserResponse;
import com.roomify.shared.exception.user.RoleAlreadyAssignedException;
import com.roomify.shared.exception.user.RoleNotAssignedException;
import com.roomify.shared.exception.user.RoleNotFoundException;
import com.roomify.shared.exception.user.UserActionForbiddenException;
import com.roomify.shared.exception.user.UserNotFoundException;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService implements UserApi {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserSpi userSpi;
    private final RoleSpi roleSpi;
    private final AuthService authService;
    private final UserMapper userMapper;

    public UserService(UserSpi userSpi, RoleSpi roleSpi, AuthService authService, UserMapper userMapper) {
        this.userSpi = userSpi;
        this.roleSpi = roleSpi;
        this.authService = authService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public void deleteUserById(@NonNull Long id, @NonNull User currentUser) throws UserNotFoundException, UserActionForbiddenException {
        User user = getUser(id);
        if (id.equals(currentUser.getId())) {
            throw UserActionForbiddenException.builder()
                    .message("You cannot delete yourself via this endpoint")
                    .build();
        }
        if (user.getRolesEnum().contains(RoleEnum.SUPER_ADMIN)) {
            throw UserActionForbiddenException.builder()
                    .message("You cannot delete user with this role")
                    .build();
        }

        user.setDeletedAt(Instant.now());
        user.setDeletedBy(currentUser.getId());
        log.warn("User with id %s has been marked as deleted by user with id %s".formatted(id, currentUser.getId()));
    }

    @Override
    @Transactional
    public void deleteMe(@NonNull User currentUser) throws UserNotFoundException {
        Long id = currentUser.getId();
        User user = getUser(id);
        user.setDeletedAt(Instant.now());
        user.setDeletedBy(currentUser.getId());
        log.warn("User with id %s has been marked as deleted".formatted(id));
    }

    @Override
    @Transactional
    public UserResponse updateMe(@NonNull User currentUser, @Nullable UpdateMeRequest request) throws UserNotFoundException {
        Long id = currentUser.getId();
        User user = getUser(id);

        var requestO = Optional.ofNullable(request);
        requestO.map(UpdateMeRequest::firstName).ifPresent(user::setFirstName);
        requestO.map(UpdateMeRequest::lastName).ifPresent(user::setLastName);

        requestO.map(UpdateMeRequest::email)
                .filter(requestedEmail -> !requestedEmail.equals(user.getEmail()))
                .ifPresent(requestedEmail -> {
                            user.setEmail(requestedEmail);
                            user.setEmailVerified(false);
                            authService.processVerificationEmail(user);
                        }
                );

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull UserPage searchUsers(@Nullable UserFilterInput filter, @NonNull PageInfoInput pagination) {
        int page = Objects.requireNonNullElse(pagination.getPage(), 0);
        int pageSize = Objects.requireNonNullElse(pagination.getPageSize(), DEFAULT_PAGE_SIZE);
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);

        UserFilterInput f = Objects.requireNonNullElse(filter, new UserFilterInput());

        UserSearchFilter searchFilter = UserSearchFilter.builder()
                .firstNameContains(f.getFirstNameContains())
                .lastNameContains(f.getLastNameContains())
                .emailContains(f.getEmailContains())
                .role(f.getRole())
                .deleted(f.getDeleted())
                .enabled(f.getEnabled())
                .emailVerified(f.getEmailVerified())
                .page(page)
                .pageSize(pageSize)
                .build();

        Page<User> result = userSpi.searchUsers(searchFilter);
        PageInfo pageInfo = new PageInfo(
                page,
                pageSize,
                (int) result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
        return new UserPage(userMapper.toAdminResponseList(result.getContent()), pageInfo);
    }

    @Override
    @Transactional
    @RateLimiter(name = "creationalRateLimiter")
    public @NonNull UserAdminResponse updateUserRole(@NonNull Long id, @NonNull UpdateUserRoleRequest request, @NonNull User currentUser)
            throws UserNotFoundException, RoleNotFoundException, UserActionForbiddenException, RoleAlreadyAssignedException,
            RoleNotAssignedException {
        User target = getUser(id);
        checkAuthorization(currentUser, target, request.role());
        Role role = roleSpi.findByName(request.role())
                .orElseThrow(() -> RoleNotFoundException.builder()
                        .message("Role %s not found".formatted(request.role()))
                        .build());

        if (RoleActionEnum.ADD.equals(request.action())) {
            addRole(role, request, target);
        } else {
            removeRole(role, request, target);
        }

        log.warn("Role %s %S for user %s by user %s".formatted(request.role(), request.action(), id, currentUser.getId()));
        return userMapper.toAdminResponse(userSpi.findUserById(id)
                .orElseThrow(() -> new IllegalStateException("User %s disappeared after role update".formatted(id))));
    }

    private void removeRole(@NonNull Role role, @NonNull UpdateUserRoleRequest request, @NonNull User target) throws RoleNotAssignedException {
        Set<RoleEnum> targetRoles = target.getRolesEnum();
        if (!targetRoles.contains(request.role())) {
            throw RoleNotAssignedException.builder()
                    .message("User does not have role %s".formatted(request.role()))
                    .build();
        }
        userSpi.removeRoleFromUser(target.getId(), role.getId());
    }

    private void addRole(@NonNull Role role, @NonNull UpdateUserRoleRequest request, @NonNull User target) throws RoleAlreadyAssignedException {
        Set<RoleEnum> targetRoles = target.getRolesEnum();
        if (targetRoles.contains(request.role())) {
            throw RoleAlreadyAssignedException.builder()
                    .message("User already has role %s".formatted(request.role()))
                    .build();
        }
        userSpi.addRoleToUser(target.getId(), role.getId());
    }

    private @NonNull User getUser(@NonNull Long id) throws UserNotFoundException {
        return userSpi.findUserById(id)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User with id %s not found".formatted(id))
                        .build());
    }

    private static void checkAuthorization(@NonNull User currentUser, @NonNull User target, @NonNull RoleEnum requestedRole)
            throws UserActionForbiddenException {
        boolean isSuperAdmin = currentUser.getRolesEnum().contains(RoleEnum.SUPER_ADMIN);

        if (isSuperAdmin && RoleEnum.SUPER_ADMIN.equals(requestedRole)  && currentUser.getId().equals(target.getId())) {
            throw UserActionForbiddenException.builder()
                    .message("You cannot modify your own SUPER_ADMIN role")
                    .build();
        }

        if (!isSuperAdmin) {
            Set<RoleEnum> targetRoles = target.getRolesEnum();
            if (targetRoles.contains(RoleEnum.ADMIN) || targetRoles.contains(RoleEnum.SUPER_ADMIN)) {
                throw UserActionForbiddenException.builder()
                        .message("You cannot modify a user with ADMIN or SUPER_ADMIN role")
                        .build();
            }
            if (RoleEnum.ADMIN.equals(requestedRole)  || RoleEnum.SUPER_ADMIN.equals(requestedRole)) {
                throw UserActionForbiddenException.builder()
                        .message("You cannot assign or remove the role %s".formatted(requestedRole))
                        .build();
            }
        }
    }
}
