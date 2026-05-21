package com.h2traindata.web;

import com.h2traindata.application.exception.DuplicateUserAccountException;
import com.h2traindata.application.exception.InvalidCredentialsException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.usecase.AuthenticateUserAccountUseCase;
import com.h2traindata.application.usecase.GetUserAccountUseCase;
import com.h2traindata.application.usecase.RegisterUserAccountUseCase;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.web.auth.AuthenticatedUserContext;
import com.h2traindata.web.dto.AccountResponse;
import com.h2traindata.web.portal.AuthPageRenderer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    private final RegisterUserAccountUseCase registerUserAccountUseCase;
    private final AuthenticateUserAccountUseCase authenticateUserAccountUseCase;
    private final GetUserAccountUseCase getUserAccountUseCase;
    private final ConnectionRepository connectionRepository;
    private final AuthenticatedUserContext authenticatedUserContext;
    private final AuthPageRenderer authPageRenderer;
    private final GoogleLoginAvailability googleLoginAvailability;

    public AccountController(RegisterUserAccountUseCase registerUserAccountUseCase,
                             AuthenticateUserAccountUseCase authenticateUserAccountUseCase,
                             GetUserAccountUseCase getUserAccountUseCase,
                             ConnectionRepository connectionRepository,
                             AuthenticatedUserContext authenticatedUserContext,
                             AuthPageRenderer authPageRenderer,
                             GoogleLoginAvailability googleLoginAvailability) {
        this.registerUserAccountUseCase = registerUserAccountUseCase;
        this.authenticateUserAccountUseCase = authenticateUserAccountUseCase;
        this.getUserAccountUseCase = getUserAccountUseCase;
        this.connectionRepository = connectionRepository;
        this.authenticatedUserContext = authenticatedUserContext;
        this.authPageRenderer = authPageRenderer;
        this.googleLoginAvailability = googleLoginAvailability;
    }

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public String loginPage(@RequestParam(value = "error", required = false) String error) {
        return authPageRenderer.renderLogin(googleLoginAvailability.isAvailable(), error);
    }

    @GetMapping(value = "/register", produces = MediaType.TEXT_HTML_VALUE)
    public String registerPage(@RequestParam(value = "error", required = false) String error) {
        return authPageRenderer.renderRegister(googleLoginAvailability.isAvailable(), error);
    }

    @PostMapping(value = "/account/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> register(@RequestParam String username,
                                         @RequestParam String email,
                                         @RequestParam String password,
                                         @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                         HttpSession session) {
        if (!password.equals(confirmPassword)) {
            return redirect("/register?error=password_mismatch");
        }

        try {
            InternalUserAccount userAccount = registerUserAccountUseCase.execute(username, email, password);
            authenticatedUserContext.login(session, userAccount);
            return redirect("/");
        } catch (DuplicateUserAccountException exception) {
            return redirect("/register?error=account_exists");
        } catch (IllegalArgumentException exception) {
            return redirect("/register?error=invalid_registration");
        }
    }

    @PostMapping(value = "/account/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> login(@RequestParam String login,
                                      @RequestParam String password,
                                      HttpSession session) {
        try {
            InternalUserAccount userAccount = authenticateUserAccountUseCase.execute(login, password);
            authenticatedUserContext.login(session, userAccount);
            return redirect("/");
        } catch (InvalidCredentialsException exception) {
            return redirect("/login?error=invalid_credentials");
        }
    }

    @PostMapping("/account/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authenticatedUserContext.logout(request);
        return redirect("/login");
    }

    @GetMapping(value = "/account/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse me(HttpServletRequest request) {
        String userId = authenticatedUserContext.requireUserId(request);
        InternalUserAccount userAccount = getUserAccountUseCase.execute(userId);
        return toResponse(userAccount);
    }

    private AccountResponse toResponse(InternalUserAccount userAccount) {
        Set<String> providers = new LinkedHashSet<>(userAccount.providerIds());
        providers.addAll(connectionRepository.findByUserId(userAccount.id()).stream()
                .map(connection -> connection.providerId())
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        return new AccountResponse(
                userAccount.id(),
                userAccount.email(),
                userAccount.username(),
                providers
        );
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(location))
                .build();
    }
}
