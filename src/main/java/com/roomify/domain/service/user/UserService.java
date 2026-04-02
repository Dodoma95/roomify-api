package com.roomify.domain.service.user;

import java.time.Instant;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.roomify.domain.api.UserApi;
import com.roomify.domain.models.RoleEnum;
import com.roomify.domain.spi.UserSpi;
import com.roomify.infrastucture.models.user.User;
import com.roomify.shared.exception.user.UserActionForbiddenException;
import com.roomify.shared.exception.user.UserNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService implements UserApi {

    private final UserSpi userSpi;

    public UserService(UserSpi userSpi) {
        this.userSpi = userSpi;
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
}
