package com.roomify.domain.service.user;

import java.time.Instant;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.roomify.domain.api.UserApi;
import com.roomify.domain.models.RoleEnum;
import com.roomify.domain.service.auth.AuthService;
import com.roomify.domain.spi.UserSpi;
import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.in.UpdateMeRequest;
import com.roomify.presentation.models.out.UserResponse;
import com.roomify.shared.exception.user.UserActionForbiddenException;
import com.roomify.shared.exception.user.UserNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService implements UserApi {

    private final UserSpi userSpi;
    private final AuthService authService;

    public UserService(UserSpi userSpi, AuthService authService) {
        this.userSpi = userSpi;
        this.authService = authService;
    }

    @Override
    @Transactional
    public void deleteUserById(@NonNull Long id, @NonNull User currentUser) throws UserNotFoundException, UserActionForbiddenException {
        User user = userSpi.findUserById(id)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User with id %s not found".formatted(id))
                        .build());

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
        User user = userSpi.findUserById(id)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User with id %s not found".formatted(id))
                        .build());
        user.setDeletedAt(Instant.now());
        user.setDeletedBy(currentUser.getId());
        log.warn("User with id %s has been marked as deleted".formatted(id));
    }

    @Override
    @Transactional
    public UserResponse updateMe(@NonNull User currentUser, @Nullable UpdateMeRequest request) throws UserNotFoundException {
        Long id = currentUser.getId();
        User user = userSpi.findUserById(id)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User with id %s not found".formatted(id))
                        .build());

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
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()
        );
    }
}
