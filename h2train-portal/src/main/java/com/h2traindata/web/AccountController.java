package com.h2traindata.web;

import com.h2traindata.application.exception.AuthenticationRequiredException;
import com.h2traindata.application.exception.AccountManagedExternallyException;
import com.h2traindata.application.exception.DuplicateUserAccountException;
import com.h2traindata.application.exception.EmailAlreadyInUseException;
import com.h2traindata.application.exception.EmailConfirmationMismatchException;
import com.h2traindata.application.exception.EmailUnchangedException;
import com.h2traindata.application.exception.ExpiredPasswordResetTokenException;
import com.h2traindata.application.exception.InvalidCredentialsException;
import com.h2traindata.application.exception.InvalidCurrentPasswordException;
import com.h2traindata.application.exception.InvalidPasswordResetTokenException;
import com.h2traindata.application.exception.PasswordConfirmationMismatchException;
import com.h2traindata.application.exception.PasswordUnchangedException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.usecase.AuthenticateUserAccountUseCase;
import com.h2traindata.application.usecase.ChangeUserEmailUseCase;
import com.h2traindata.application.usecase.ChangeUserPasswordUseCase;
import com.h2traindata.application.usecase.GetUserAccountUseCase;
import com.h2traindata.application.usecase.RegisterUserAccountUseCase;
import com.h2traindata.application.usecase.RequestPasswordResetUseCase;
import com.h2traindata.application.usecase.ResetPasswordWithTokenUseCase;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.web.auth.AuthenticatedUserContext;
import com.h2traindata.web.auth.RememberMeService;
import com.h2traindata.web.dto.AccountResponse;
import com.h2traindata.web.dto.ChangeEmailRequest;
import com.h2traindata.web.dto.ChangePasswordRequest;
import com.h2traindata.web.portal.AuthPageRenderer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class AccountController {

    private final RegisterUserAccountUseCase registerUserAccountUseCase;
    private final AuthenticateUserAccountUseCase authenticateUserAccountUseCase;
    private final ChangeUserEmailUseCase changeUserEmailUseCase;
    private final ChangeUserPasswordUseCase changeUserPasswordUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordWithTokenUseCase resetPasswordWithTokenUseCase;
    private final GetUserAccountUseCase getUserAccountUseCase;
    private final ConnectionRepository connectionRepository;
    private final AuthenticatedUserContext authenticatedUserContext;
    private final RememberMeService rememberMeService;
    private final AuthPageRenderer authPageRenderer;
    private final GoogleLoginAvailability googleLoginAvailability;

    public AccountController(RegisterUserAccountUseCase registerUserAccountUseCase,
                             AuthenticateUserAccountUseCase authenticateUserAccountUseCase,
                             ChangeUserEmailUseCase changeUserEmailUseCase,
                             ChangeUserPasswordUseCase changeUserPasswordUseCase,
                             RequestPasswordResetUseCase requestPasswordResetUseCase,
                             ResetPasswordWithTokenUseCase resetPasswordWithTokenUseCase,
                             GetUserAccountUseCase getUserAccountUseCase,
                             ConnectionRepository connectionRepository,
                             AuthenticatedUserContext authenticatedUserContext,
                             RememberMeService rememberMeService,
                             AuthPageRenderer authPageRenderer,
                             GoogleLoginAvailability googleLoginAvailability) {
        this.registerUserAccountUseCase = registerUserAccountUseCase;
        this.authenticateUserAccountUseCase = authenticateUserAccountUseCase;
        this.changeUserEmailUseCase = changeUserEmailUseCase;
        this.changeUserPasswordUseCase = changeUserPasswordUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordWithTokenUseCase = resetPasswordWithTokenUseCase;
        this.getUserAccountUseCase = getUserAccountUseCase;
        this.connectionRepository = connectionRepository;
        this.authenticatedUserContext = authenticatedUserContext;
        this.rememberMeService = rememberMeService;
        this.authPageRenderer = authPageRenderer;
        this.googleLoginAvailability = googleLoginAvailability;
    }

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "status", required = false) String status) {
        return authPageRenderer.renderLogin(googleLoginAvailability.isAvailable(), error, status);
    }

    @GetMapping(value = "/register", produces = MediaType.TEXT_HTML_VALUE)
    public String registerPage(@RequestParam(value = "error", required = false) String error) {
        return authPageRenderer.renderRegister(googleLoginAvailability.isAvailable(), error);
    }

    @GetMapping(value = "/forgot-password", produces = MediaType.TEXT_HTML_VALUE)
    public String forgotPasswordPage(@RequestParam(value = "error", required = false) String error,
                                     @RequestParam(value = "status", required = false) String status) {
        return authPageRenderer.renderForgotPassword(error, status);
    }

    @GetMapping(value = "/account/password/reset", produces = MediaType.TEXT_HTML_VALUE)
    public String resetPasswordPage(@RequestParam String token,
                                    @RequestParam(value = "error", required = false) String error) {
        return authPageRenderer.renderPasswordReset(token, error);
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

    @PostMapping(value = "/account/email", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> changeEmail(@RequestParam String newEmail,
                                            @RequestParam String confirmNewEmail,
                                            @RequestParam String currentPassword,
                                            HttpServletRequest request) {
        String userId;
        try {
            userId = authenticatedUserContext.requireUserId(request);
        } catch (AuthenticationRequiredException exception) {
            return redirect("/login?error=session_required");
        }

        ChangeEmailRequest changeEmailRequest = new ChangeEmailRequest(newEmail, confirmNewEmail, currentPassword);
        try {
            changeUserEmailUseCase.execute(
                    userId,
                    changeEmailRequest.newEmail(),
                    changeEmailRequest.confirmNewEmail(),
                    changeEmailRequest.currentPassword()
            );
            return redirect("/?accountStatus=email_changed");
        } catch (EmailConfirmationMismatchException exception) {
            return redirect("/?accountError=email_mismatch");
        } catch (EmailUnchangedException exception) {
            return redirect("/?accountError=email_unchanged");
        } catch (EmailAlreadyInUseException exception) {
            return redirect("/?accountError=email_in_use");
        } catch (InvalidCurrentPasswordException exception) {
            return redirect("/?accountError=invalid_current_password");
        } catch (AccountManagedExternallyException exception) {
            return redirect("/?accountError=external_account_managed");
        } catch (IllegalArgumentException exception) {
            return redirect("/?accountError=invalid_email");
        }
    }

    @PostMapping(value = "/account/password", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> changePassword(@RequestParam String currentPassword,
                                               @RequestParam String newPassword,
                                               @RequestParam String confirmNewPassword,
                                               HttpServletRequest request) {
        String userId;
        try {
            userId = authenticatedUserContext.requireUserId(request);
        } catch (AuthenticationRequiredException exception) {
            return redirect("/login?error=session_required");
        }

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest(
                currentPassword,
                newPassword,
                confirmNewPassword
        );
        try {
            changeUserPasswordUseCase.execute(
                    userId,
                    changePasswordRequest.currentPassword(),
                    changePasswordRequest.newPassword(),
                    changePasswordRequest.confirmNewPassword()
            );
            return redirect("/?accountStatus=password_changed");
        } catch (PasswordConfirmationMismatchException exception) {
            return redirect("/?accountError=password_mismatch");
        } catch (PasswordUnchangedException exception) {
            return redirect("/?accountError=password_unchanged");
        } catch (InvalidCurrentPasswordException exception) {
            return redirect("/?accountError=invalid_current_password");
        } catch (AccountManagedExternallyException exception) {
            return redirect("/?accountError=external_account_managed");
        } catch (IllegalArgumentException exception) {
            return redirect("/?accountError=invalid_password");
        }
    }

    @PostMapping(value = "/account/password/reset/request", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> requestPasswordReset(HttpServletRequest request) {
        String userId;
        try {
            userId = authenticatedUserContext.requireUserId(request);
        } catch (AuthenticationRequiredException exception) {
            return redirect("/login?error=session_required");
        }

        try {
            requestPasswordResetUseCase.execute(userId, resetPasswordUrl(request));
            return redirect("/?accountStatus=password_reset_requested");
        } catch (AccountManagedExternallyException exception) {
            return redirect("/?accountError=external_account_managed");
        }
    }

    @PostMapping(value = "/account/password/forgot", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> forgotPassword(@RequestParam(value = "email", required = false) String email,
                                               HttpServletRequest request) {
        requestPasswordResetUseCase.executeForEmail(email, resetPasswordUrl(request));
        return redirect("/forgot-password?status=requested");
    }

    @PostMapping(value = "/account/password/reset", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> resetPassword(@RequestParam String token,
                                              @RequestParam String newPassword,
                                              @RequestParam String confirmNewPassword) {
        try {
            resetPasswordWithTokenUseCase.execute(token, newPassword, confirmNewPassword);
            return redirect("/login?status=password_reset");
        } catch (PasswordConfirmationMismatchException exception) {
            return redirect(resetErrorLocation(token, "password_mismatch"));
        } catch (PasswordUnchangedException exception) {
            return redirect(resetErrorLocation(token, "password_unchanged"));
        } catch (ExpiredPasswordResetTokenException exception) {
            return redirect(resetErrorLocation(token, "reset_token_expired"));
        } catch (InvalidPasswordResetTokenException exception) {
            return redirect(resetErrorLocation(token, "reset_token_invalid"));
        } catch (AccountManagedExternallyException exception) {
            return redirect(resetErrorLocation(token, "external_account_managed"));
        } catch (IllegalArgumentException exception) {
            return redirect(resetErrorLocation(token, "invalid_password"));
        }
    }

    @PostMapping(value = "/account/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> login(@RequestParam String login,
                                      @RequestParam String password,
                                      @RequestParam(value = "rememberMe", required = false) String rememberMe,
                                      HttpServletRequest request,
                                      HttpServletResponse response,
                                      HttpSession session) {
        try {
            InternalUserAccount userAccount = authenticateUserAccountUseCase.execute(login, password);
            authenticatedUserContext.login(session, userAccount);
            if (rememberMeRequested(rememberMe)) {
                rememberMeService.remember(request, response, userAccount);
            } else {
                rememberMeService.clear(request, response);
            }
            return redirect("/");
        } catch (InvalidCredentialsException exception) {
            return redirect("/login?error=invalid_credentials");
        }
    }

    @PostMapping("/account/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        rememberMeService.clear(request, response);
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

    private String resetPasswordUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/account/password/reset")
                .replaceQuery(null)
                .build()
                .toUriString();
    }

    private String resetErrorLocation(String token, String error) {
        return ServletUriComponentsBuilder.fromPath("/account/password/reset")
                .queryParam("token", token)
                .queryParam("error", error)
                .build()
                .toUriString();
    }

    private boolean rememberMeRequested(String rememberMe) {
        return rememberMe != null && (
                "true".equalsIgnoreCase(rememberMe)
                        || "on".equalsIgnoreCase(rememberMe)
                        || "1".equals(rememberMe)
        );
    }
}
