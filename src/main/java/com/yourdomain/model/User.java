package com.yourdomain.model;

public record User(int userId, String name, String username,
                   String passwordHash, String role, String status) {

    public static final String ROLE_CUSTOMER = "Customer";
    public static final String ROLE_STAFF = "Cafe Staff";
    public static final String ROLE_ADMIN = "Admin";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_INACTIVE = "Inactive";
}
