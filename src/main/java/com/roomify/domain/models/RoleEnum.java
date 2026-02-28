package com.roomify.domain.models;

public enum RoleEnum {
    USER,
    ADMIN,
    SUPER_ADMIN;

    public String asAuthority() {
        return "ROLE_" + this.name();
    }
}
