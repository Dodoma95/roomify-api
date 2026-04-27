package com.roomify.presentation.models.out;

import java.time.LocalDate;

import org.jspecify.annotations.NonNull;

public record AvailableSlot(@NonNull LocalDate from, @NonNull LocalDate to) {}
