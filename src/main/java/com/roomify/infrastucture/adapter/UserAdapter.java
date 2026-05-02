package com.roomify.infrastucture.adapter;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.roomify.domain.models.UserSearchFilter;
import com.roomify.domain.spi.UserSpi;
import com.roomify.infrastucture.models.user.User;
import com.roomify.infrastucture.repository.UserRepository;
import com.roomify.infrastucture.repository.specification.UserSpecifications;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.nonNull;

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
    public @NonNull Page<User> searchUsers(@NonNull UserSearchFilter filter) {
        log.debug("Searching users page={}, size={}", filter.getPage(), filter.getPageSize());

        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        if (nonNull(filter.getFirstNameContains()) && !filter.getFirstNameContains().isBlank()) {
            spec = spec.and(UserSpecifications.firstNameContains(filter.getFirstNameContains()));
        }
        if (nonNull(filter.getLastNameContains()) && !filter.getLastNameContains().isBlank()) {
            spec = spec.and(UserSpecifications.lastNameContains(filter.getLastNameContains()));
        }
        if (nonNull(filter.getEmailContains()) && !filter.getEmailContains().isBlank()) {
            spec = spec.and(UserSpecifications.emailContains(filter.getEmailContains()));
        }
        if (nonNull(filter.getRole())) {
            spec = spec.and(UserSpecifications.hasRole(filter.getRole()));
        }
        if (nonNull(filter.getDeleted())) {
            spec = spec.and(UserSpecifications.isDeleted(filter.getDeleted()));
        }
        if (nonNull(filter.getEnabled())) {
            spec = spec.and(UserSpecifications.isEnabled(filter.getEnabled()));
        }
        if (nonNull(filter.getEmailVerified())) {
            spec = spec.and(UserSpecifications.isEmailVerified(filter.getEmailVerified()));
        }

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getPageSize());
        return userRepository.findAll(spec, pageable);
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
