package com.example.demo.auth;

import java.util.EnumSet;
import java.util.Set;

public enum UserRole {
    SELLER,
    BUYER,
    ADMIN,
    /**
     * Legacy value kept for backwards compatibility with older database records.
     * All LANDLORD accounts are treated as SELLER roles.
     */
    @Deprecated
    LANDLORD;

    private static final EnumSet<UserRole> SELLER_ROLES = EnumSet.of(SELLER, LANDLORD);

    public boolean isSellerRole() {
        return SELLER_ROLES.contains(this);
    }

    public static Set<UserRole> sellerRoles() {
        return EnumSet.copyOf(SELLER_ROLES);
    }
}
