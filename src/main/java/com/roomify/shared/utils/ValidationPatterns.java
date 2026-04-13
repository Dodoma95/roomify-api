package com.roomify.shared.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ValidationPatterns {

    public static final String NAME = "^[\\p{L}]+([ '-][\\p{L}]+)*$";
    @SuppressWarnings("java:S2068") // false positive: this is a regex, not a credential
    public static final String PASSWORD = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).*$";

}
