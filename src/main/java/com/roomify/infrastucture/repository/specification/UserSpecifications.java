package com.roomify.infrastucture.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import com.roomify.domain.models.RoleEnum;
import com.roomify.infrastucture.models.user.Role;
import com.roomify.infrastucture.models.user.User;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class UserSpecifications {

    private UserSpecifications() {}

    public static Specification<User> firstNameContains(String value) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("firstName")), "%" + value.strip().toLowerCase() + "%");
    }

    public static Specification<User> lastNameContains(String value) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("lastName")), "%" + value.strip().toLowerCase() + "%");
    }

    public static Specification<User> emailContains(String value) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("email")), "%" + value.strip().toLowerCase() + "%");
    }

    /**
     * Utilise une sous-requête corrélée pour éviter les doublons liés au @ManyToMany
     * sur la table user_roles lors du comptage de pagination.
     */
    public static Specification<User> hasRole(RoleEnum role) {
        return (root, query, cb) -> {
            Subquery<Long> sq = query.subquery(Long.class);
            Root<User> sqUser = sq.from(User.class);
            Join<User, Role> sqRoles = sqUser.join("roles");
            sq.select(sqUser.get("id"))
                    .where(cb.and(
                            cb.equal(sqUser.get("id"), root.get("id")),
                            cb.equal(sqRoles.get("name"), role)
                    ));
            return cb.exists(sq);
        };
    }

    public static Specification<User> isDeleted(boolean deleted) {
        return (root, query, cb) -> deleted
                ? cb.isNotNull(root.get("deletedAt"))
                : cb.isNull(root.get("deletedAt"));
    }

    public static Specification<User> isEnabled(boolean enabled) {
        return (root, query, cb) -> cb.equal(root.get("enabled"), enabled);
    }

    public static Specification<User> isEmailVerified(boolean emailVerified) {
        return (root, query, cb) -> cb.equal(root.get("emailVerified"), emailVerified);
    }
}
