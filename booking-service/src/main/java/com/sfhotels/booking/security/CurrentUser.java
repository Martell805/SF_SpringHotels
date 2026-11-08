package com.sfhotels.booking.security;

import com.sfhotels.common.security.Roles;
import com.sfhotels.common.security.TokenClaims;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

public record CurrentUser(Long id, String username, boolean admin) {

    public static CurrentUser from(Jwt jwt) {
        Number id = jwt.getClaim(TokenClaims.USER_ID);
        List<String> roles = jwt.getClaimAsStringList(TokenClaims.ROLES);
        boolean admin = roles != null && roles.contains(Roles.ADMIN);
        return new CurrentUser(id == null ? null : id.longValue(), jwt.getSubject(), admin);
    }

    public boolean canAccess(Long ownerId) {
        return admin || ownerId.equals(id);
    }
}
