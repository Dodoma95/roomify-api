package com.roomify.infrastructure.adapter;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import com.roomify.domain.models.RoleEnum;
import com.roomify.domain.spi.RoleSpi;
import com.roomify.infrastructure.models.user.Role;
import com.roomify.infrastructure.repository.RoleRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RoleAdapter implements RoleSpi {

    private final RoleRepository roleRepository;

    public RoleAdapter(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Optional<Role> findByName(@NonNull RoleEnum roleName) {
        log.debug("Finding role by name: {}", roleName);
        return roleRepository.findByName(roleName);
    }
}
