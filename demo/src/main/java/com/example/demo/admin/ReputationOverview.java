package com.example.demo.admin;

import java.util.List;

public record ReputationOverview(
        List<UserAccountView> sellers,
        List<UserAccountView> buyers,
        long blacklistedCount
) {
}
