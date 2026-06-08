package com.h2traindata.web.auth;

import com.h2traindata.application.port.out.RememberMeTokenRepository;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.PasswordResetTokenService;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.RememberMeToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RememberMeService {

    public static final String COOKIE_NAME = "H2TRAIN_REMEMBER_ME";

    private static final Duration TOKEN_TTL = Duration.ofDays(30);

    private final RememberMeTokenRepository rememberMeTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenService tokenService;
    private final Clock clock;

    @Autowired
    public RememberMeService(RememberMeTokenRepository rememberMeTokenRepository,
                             UserAccountRepository userAccountRepository,
                             PasswordResetTokenService tokenService) {
        this(rememberMeTokenRepository, userAccountRepository, tokenService, Clock.systemUTC());
    }

    RememberMeService(RememberMeTokenRepository rememberMeTokenRepository,
                      UserAccountRepository userAccountRepository,
                      PasswordResetTokenService tokenService,
                      Clock clock) {
        this.rememberMeTokenRepository = rememberMeTokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    public void remember(HttpServletRequest request, HttpServletResponse response, InternalUserAccount userAccount) {
        String rawToken = tokenService.generateToken();
        Instant now = Instant.now(clock);
        rememberMeTokenRepository.save(new RememberMeToken(
                tokenService.hashToken(rawToken),
                userAccount.id(),
                now.plus(TOKEN_TTL),
                now,
                now
        ));
        response.addCookie(cookie(rawToken, (int) TOKEN_TTL.toSeconds(), request.isSecure()));
    }

    public Optional<String> restoreUserId(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> rawToken = rememberMeCookieValue(request);
        if (rawToken.isEmpty()) {
            return Optional.empty();
        }

        String tokenHash;
        try {
            tokenHash = tokenService.hashToken(rawToken.get());
        } catch (IllegalArgumentException exception) {
            clear(response, request.isSecure());
            return Optional.empty();
        }

        Optional<RememberMeToken> storedToken = rememberMeTokenRepository.findByTokenHash(tokenHash);
        if (storedToken.isEmpty()) {
            clear(response, request.isSecure());
            return Optional.empty();
        }

        RememberMeToken rememberMeToken = storedToken.get();
        Instant now = Instant.now(clock);
        if (rememberMeToken.isExpired(now)) {
            rememberMeTokenRepository.deleteByTokenHash(tokenHash);
            clear(response, request.isSecure());
            return Optional.empty();
        }

        Optional<InternalUserAccount> userAccount = userAccountRepository.findById(rememberMeToken.userId());
        if (userAccount.isEmpty()) {
            rememberMeTokenRepository.deleteByTokenHash(tokenHash);
            clear(response, request.isSecure());
            return Optional.empty();
        }

        rememberMeTokenRepository.save(rememberMeToken.markUsed(now));
        return Optional.of(userAccount.get().id());
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        rememberMeCookieValue(request).ifPresent(this::deleteStoredToken);
        clear(response, request.isSecure());
    }

    private void deleteStoredToken(String rawToken) {
        try {
            rememberMeTokenRepository.deleteByTokenHash(tokenService.hashToken(rawToken));
        } catch (IllegalArgumentException exception) {
            // Invalid client-side token values still need the browser cookie cleared.
        }
    }

    private Optional<String> rememberMeCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private void clear(HttpServletResponse response, boolean secure) {
        response.addCookie(cookie("", 0, secure));
    }

    private Cookie cookie(String value, int maxAge, boolean secure) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
}
