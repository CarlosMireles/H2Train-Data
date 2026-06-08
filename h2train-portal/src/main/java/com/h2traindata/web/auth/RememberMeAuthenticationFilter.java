package com.h2traindata.web.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RememberMeAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticatedUserContext authenticatedUserContext;
    private final RememberMeService rememberMeService;

    public RememberMeAuthenticationFilter(AuthenticatedUserContext authenticatedUserContext,
                                          RememberMeService rememberMeService) {
        this.authenticatedUserContext = authenticatedUserContext;
        this.rememberMeService = rememberMeService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (authenticatedUserContext.currentUserId(request).isEmpty()) {
            rememberMeService.restoreUserId(request, response)
                    .ifPresent(userId -> authenticatedUserContext.loginUserId(request.getSession(true), userId));
        }
        filterChain.doFilter(request, response);
    }
}
