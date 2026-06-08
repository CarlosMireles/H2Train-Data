package com.h2traindata.web;

import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.port.out.ProviderCatalog;
import com.h2traindata.application.usecase.GetUserAccountUseCase;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.web.auth.AuthenticatedUserContext;
import com.h2traindata.infrastructure.email.PasswordResetMailProperties;
import com.h2traindata.web.mapper.SyncSettingsMapper;
import com.h2traindata.web.portal.PortalPageRenderer;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortalController {

    private final ProviderCatalog providerCatalog;
    private final GetUserAccountUseCase getUserAccountUseCase;
    private final ConnectionRepository connectionRepository;
    private final AuthenticatedUserContext authenticatedUserContext;
    private final SyncSettingsMapper syncSettingsMapper;
    private final PortalPageRenderer portalPageRenderer;
    private final PasswordResetMailProperties passwordResetMailProperties;

    public PortalController(ProviderCatalog providerCatalog,
                            GetUserAccountUseCase getUserAccountUseCase,
                            ConnectionRepository connectionRepository,
                            AuthenticatedUserContext authenticatedUserContext,
                            SyncSettingsMapper syncSettingsMapper,
                            PortalPageRenderer portalPageRenderer,
                            PasswordResetMailProperties passwordResetMailProperties) {
        this.providerCatalog = providerCatalog;
        this.getUserAccountUseCase = getUserAccountUseCase;
        this.connectionRepository = connectionRepository;
        this.authenticatedUserContext = authenticatedUserContext;
        this.syncSettingsMapper = syncSettingsMapper;
        this.portalPageRenderer = portalPageRenderer;
        this.passwordResetMailProperties = passwordResetMailProperties;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> home(HttpServletRequest request,
                                       @RequestParam(value = "providerError", required = false) String providerError,
                                       @RequestParam(value = "provider", required = false) String provider,
                                       @RequestParam(value = "athleteId", required = false) String athleteId,
                                       @RequestParam(value = "accountStatus", required = false) String accountStatus,
                                       @RequestParam(value = "accountError", required = false) String accountError) {
        return authenticatedUserContext.currentUserId(request)
                .map(userId -> {
                    InternalUserAccount userAccount = getUserAccountUseCase.execute(userId);
                    PortalPageRenderer.PortalAlert alert = providerAlert(providerError, provider, athleteId);
                    if (alert == null) {
                        alert = accountAlert(accountStatus, accountError);
                    }
                    String page = portalPageRenderer.render(
                            providerCatalog.registeredProviderIds(),
                            userAccount,
                            connectionRepository.findByUserId(userId).stream()
                                    .map(syncSettingsMapper::toResponse)
                                    .toList(),
                            alert
                    );
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_HTML)
                            .body(page);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/login"))
                        .build());
    }

    private PortalPageRenderer.PortalAlert providerAlert(String providerError, String provider, String athleteId) {
        if (!"already_linked".equals(providerError)) {
            return null;
        }

        String displayProvider = provider == null || provider.isBlank()
                ? "provider"
                : provider.substring(0, 1).toUpperCase() + provider.substring(1).toLowerCase();
        String athleteDetail = athleteId == null || athleteId.isBlank()
                ? ""
                : "Technical detail: Provider account '%s/%s' is already linked to another H2Train user."
                        .formatted(provider == null || provider.isBlank() ? "provider" : provider.toLowerCase(), athleteId);
        String actionHref = provider == null || provider.isBlank()
                ? null
                : "/auth/" + provider.replaceAll("[^A-Za-z0-9._-]", "") + "/login";

        return new PortalPageRenderer.PortalAlert(
                "error",
                displayProvider + " account already linked",
                "This " + displayProvider + " account is already connected to another H2Train user. "
                        + "Sign out of " + displayProvider + " in this browser, use a private window, or choose a different "
                        + displayProvider + " account before trying again.",
                athleteDetail,
                actionHref,
                "Try " + displayProvider + " again"
        );
    }

    private PortalPageRenderer.PortalAlert accountAlert(String accountStatus, String accountError) {
        if ("email_changed".equals(accountStatus)) {
            return new PortalPageRenderer.PortalAlert(
                    "success",
                    "Email updated",
                    "Your internal H2Train account email was changed.",
                    null,
                    null,
                    null
            );
        }
        if ("password_changed".equals(accountStatus)) {
            return new PortalPageRenderer.PortalAlert(
                    "success",
                    "Password updated",
                    "Your internal H2Train account password was changed.",
                    "Existing sessions are not globally invalidated yet because the portal uses local HttpSession state.",
                    null,
                    null
            );
        }
        if ("password_reset_requested".equals(accountStatus)) {
            return new PortalPageRenderer.PortalAlert(
                    "success",
                    "Recovery email requested",
                    passwordResetMailProperties.isEnabled()
                            ? "A password recovery link was sent to the email associated with your internal H2Train account."
                            : "A password recovery link was generated for the email associated with your internal H2Train account.",
                    passwordResetMailProperties.isEnabled()
                            ? null
                            : "Email delivery is disabled, so development mode wrote the recovery link to the application log.",
                    null,
                    null
            );
        }
        if (accountError == null || accountError.isBlank()) {
            return null;
        }
        return new PortalPageRenderer.PortalAlert(
                "error",
                "Account update failed",
                accountErrorMessage(accountError),
                null,
                null,
                null
        );
    }

    private String accountErrorMessage(String accountError) {
        return switch (accountError) {
            case "email_mismatch" -> "The new email and confirmation email must match.";
            case "email_unchanged" -> "The new email must be different from your current email.";
            case "email_in_use" -> "That email is already registered by another H2Train account.";
            case "invalid_email" -> "Enter a valid email address.";
            case "invalid_current_password" -> "The current password is not valid.";
            case "password_mismatch" -> "The new password and confirmation password must match.";
            case "password_unchanged" -> "The new password must be different from your current password.";
            case "invalid_password" -> "Use a password with at least 8 characters.";
            case "external_account_managed" -> "This account is managed through Google. Use Google to manage your email and password.";
            default -> "The account change could not be completed.";
        };
    }
}
