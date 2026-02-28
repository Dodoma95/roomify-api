package com.roomify.domain.spi;

import java.util.Optional;

import org.jspecify.annotations.NonNull;

import com.roomify.infrastucture.models.user.User;

public interface UserSpi {

    void insertUser(@NonNull User user);

    boolean alreadyExists(@NonNull String email);

    Optional<User> findUserByEmail(@NonNull String email);

}
