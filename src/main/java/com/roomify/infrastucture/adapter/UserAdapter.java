package com.roomify.infrastucture.adapter;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import com.roomify.domain.spi.UserSpi;
import com.roomify.infrastucture.models.user.User;
import com.roomify.infrastucture.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserAdapter implements UserSpi {

    private final UserRepository userRepository;

    public UserAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void insertUser(@NonNull User user) {
        log.debug("Inserting user with email: {}", user.getEmail());
        userRepository.save(user);
    }

    @Override
    public void addRoleToUser(@NonNull Long userId, @NonNull Long roleId) {
        log.debug("Adding role {} to user {}", roleId, userId);
        userRepository.addRoleToUser(userId, roleId);
    }

    @Override
    public boolean alreadyExists(@NonNull String email) {
        log.debug("Checking if user with email {} already exists", email);
        return userRepository.existsUserByEmail(email);
    }

    @Override
    public Optional<User> findUserByEmail(@NonNull String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findUserById(@NonNull Long id) {
        log.debug("Finding user by id: {}", id);
        return userRepository.findByIdAndDeletedAtIsNull(id);
    }
}
