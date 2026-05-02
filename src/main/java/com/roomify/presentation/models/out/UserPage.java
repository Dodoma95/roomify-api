package com.roomify.presentation.models.out;

import java.util.List;

import org.jspecify.annotations.NonNull;

public record UserPage(
        @NonNull List<UserAdminResponse> results,
        @NonNull PageInfo pageInfo
) {}
