package com.roomify.domain.models;

import org.jspecify.annotations.Nullable;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserSearchFilter {
    @Nullable String firstNameContains;
    @Nullable String lastNameContains;
    @Nullable String emailContains;
    @Nullable RoleEnum role;
    @Nullable Boolean deleted;
    @Nullable Boolean enabled;
    @Nullable Boolean emailVerified;
    int page;
    int pageSize;
}
