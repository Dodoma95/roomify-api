package com.roomify.presentation.models.out;

import java.util.List;

import org.jspecify.annotations.NonNull;

public record BookingPage(
        @NonNull List<BookingGraphQLResponse> results,
        @NonNull PageInfo pageInfo
) {}
