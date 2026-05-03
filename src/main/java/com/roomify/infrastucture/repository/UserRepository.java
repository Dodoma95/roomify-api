package com.roomify.infrastucture.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.roomify.infrastucture.models.user.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    boolean existsUserByEmail(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO roomify.user_roles (user_id, role_id) VALUES (:userId, :roleId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void addRoleToUser(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM roomify.user_roles WHERE user_id = :userId AND role_id = :roleId", nativeQuery = true)
    void removeRoleFromUser(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
