package com.roomify.infrastucture.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.roomify.domain.models.RoleEnum;
import com.roomify.infrastucture.models.user.Role;
import com.roomify.infrastucture.models.user.User;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleEnum name);
}
