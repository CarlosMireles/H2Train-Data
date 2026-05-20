package com.h2traindata.web.auth;

import com.h2traindata.application.exception.AuthenticationRequiredException;
import com.h2traindata.domain.InternalUserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedSession {

    public static final String USER_ID_ATTRIBUTE = "h2train.userId";

    public void login(HttpSession session, InternalUserAccount userAccount) {
        session.setAttribute(USER_ID_ATTRIBUTE, userAccount.id());
    }

    public Optional<String> currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        Object userId = session.getAttribute(USER_ID_ATTRIBUTE);
        return userId instanceof String value && !value.isBlank()
                ? Optional.of(value)
                : Optional.empty();
    }

    public String requireUserId(HttpServletRequest request) {
        return currentUserId(request).orElseThrow(AuthenticationRequiredException::new);
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
