package com.sfhotels.common.security;

public final class Roles {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    public static final String INTERNAL = "INTERNAL";

    public static final String HAS_USER_OR_ADMIN = "hasAnyRole('USER', 'ADMIN')";
    public static final String HAS_ADMIN = "hasRole('ADMIN')";
    public static final String HAS_INTERNAL = "hasRole('INTERNAL')";
    public static final String HAS_ANY_ROLE = "hasAnyRole('USER', 'ADMIN', 'INTERNAL')";

    private Roles() {
    }
}
