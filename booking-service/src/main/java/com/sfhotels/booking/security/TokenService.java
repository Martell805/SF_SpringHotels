package com.sfhotels.booking.security;

import com.sfhotels.booking.dto.TokenResponse;
import com.sfhotels.booking.entity.User;

public interface TokenService {

    TokenResponse issueUserToken(User user);

    String issueServiceToken();
}
