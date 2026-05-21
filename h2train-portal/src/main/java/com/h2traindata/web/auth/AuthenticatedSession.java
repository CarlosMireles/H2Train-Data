package com.h2traindata.web.auth;

import com.h2traindata.application.exception.AuthenticationRequiredException;
import com.h2traindata.domain.InternalUserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedSession implements AuthenticatedUserContext {

    public static final String USER_ID_ATTRIBUTE = AuthenticatedUserContext.USER_ID_ATTRIBUTE;

    @Override
    public void login(HttpSession session, InternalUserAccount userAccount) {
        loginUserId(session, userAccount.id());
    }

    @Override
    public void loginUserId(HttpSession session, String userId) {
        session.setAttribute(USER_ID_ATTRIBUTE, userId);
    }

    @Override
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

    @Override
    public String requireUserId(HttpServletRequest request) {
        return currentUserId(request).orElseThrow(AuthenticationRequiredException::new);
    }

    @Override
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
