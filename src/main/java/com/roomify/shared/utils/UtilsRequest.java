package com.roomify.shared.utils;

import java.util.Locale;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class UtilsRequest {

    public static String normalizeEmail(@NonNull String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }

}
