package com.roomify.domain.spi;

import java.util.Optional;

import org.jspecify.annotations.NonNull;

import com.roomify.domain.models.RoleEnum;
import com.roomify.infrastucture.models.user.Role;

public interface RoleSpi {

    Optional<Role> findByName(@NonNull RoleEnum roleName);

}
