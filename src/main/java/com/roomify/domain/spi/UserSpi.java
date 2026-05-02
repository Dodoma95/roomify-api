package com.roomify.domain.spi;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;

import com.roomify.domain.models.UserSearchFilter;
import com.roomify.infrastucture.models.user.User;

public interface UserSpi {

    void insertUser(@NonNull User user);

    boolean alreadyExists(@NonNull String email);

    Optional<User> findUserByEmail(@NonNull String email);

    Optional<User> findUserById(@NonNull Long id);

    void addRoleToUser(@NonNull Long userId, @NonNull Long roleId);

    @NonNull Page<User> searchUsers(@NonNull UserSearchFilter filter);

}
