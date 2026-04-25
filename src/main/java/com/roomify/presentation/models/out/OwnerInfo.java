package com.roomify.presentation.models.out;

import org.jspecify.annotations.NonNull;

public record OwnerInfo(
        @NonNull Long id,
        @NonNull String firstName,
        @NonNull String lastName,
        @NonNull String email
) {}
