package com.h2traindata.web.auth;

import com.h2traindata.domain.InternalUserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;

public interface AuthenticatedUserContext {

    String USER_ID_ATTRIBUTE = "h2train.userId";

    void login(HttpSession session, InternalUserAccount userAccount);

    void loginUserId(HttpSession session, String userId);

    Optional<String> currentUserId(HttpServletRequest request);

    String requireUserId(HttpServletRequest request);

    void logout(HttpServletRequest request);
}
