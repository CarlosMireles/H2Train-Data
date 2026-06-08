package com.h2traindata.web.portal;

import org.springframework.stereotype.Component;

@Component
public class AuthPageRenderer {

    public String renderLogin(boolean googleEnabled, String error, String status) {
        return renderAuthPage(
                "Sign in",
                "H2Train Data sign in",
                "Welcome back",
                "Sign in to access your H2Train Data account",
                icon("lock"),
                """
                        <form class="auth-form auth-login-form" method="post" action="/account/login">
                            <label class="auth-field">
                                <span class="auth-field-label">Email or username</span>
                                <span class="auth-input">
                                    <span class="auth-input-icon" aria-hidden="true">__ICON_USER__</span>
                                    <input name="login" autocomplete="username" placeholder="Enter your email or username" required>
                                </span>
                            </label>
                            <label class="auth-field">
                                <span class="auth-field-label">Password</span>
                                <span class="auth-input">
                                    <span class="auth-input-icon" aria-hidden="true">__ICON_LOCK_SMALL__</span>
                                    <input id="login-password" name="password" type="password" autocomplete="current-password" placeholder="Enter your password" required>
                                    <button class="auth-password-toggle" type="button" data-action="toggle-password" data-target="login-password" aria-label="Show password">__ICON_EYE__</button>
                                </span>
                            </label>
                            <div class="auth-form-meta">
                                <label class="auth-checkbox" title="Keep this browser signed in for 30 days.">
                                    <input name="rememberMe" type="checkbox" value="true">
                                    <span>Remember me</span>
                                </label>
                                <a class="auth-meta-link" href="/forgot-password">Forgot password?</a>
                            </div>
                            <button class="primary-action auth-button auth-button-primary" type="submit">__ICON_LOCK_BUTTON__ Sign in</button>
                        </form>
                        """,
                "Need an account?",
                "/register",
                "Create account",
                googleEnabled,
                error,
                status
        );
    }

    public String renderLogin(boolean googleEnabled, String error) {
        return renderLogin(googleEnabled, error, null);
    }

    public String renderRegister(boolean googleEnabled, String error) {
        return renderAuthPage(
                "Create account",
                "H2Train Data registration",
                "Create your account",
                "Register your internal H2Train Data account",
                icon("user-plus"),
                """
                        <form class="auth-form auth-login-form" method="post" action="/account/register">
                            <label class="auth-field">
                                <span class="auth-field-label">Username</span>
                                <span class="auth-input">
                                    <span class="auth-input-icon" aria-hidden="true">__ICON_USER__</span>
                                    <input name="username" autocomplete="username" placeholder="Choose a username" required>
                                </span>
                            </label>
                            <label class="auth-field">
                                <span class="auth-field-label">Email</span>
                                <span class="auth-input">
                                    <span class="auth-input-icon" aria-hidden="true">__ICON_MAIL__</span>
                                    <input name="email" type="email" autocomplete="email" placeholder="Enter your email" required>
                                </span>
                            </label>
                            <label class="auth-field">
                                <span class="auth-field-label">Password</span>
                                <span class="auth-input">
                                    <span class="auth-input-icon" aria-hidden="true">__ICON_LOCK_SMALL__</span>
                                    <input id="register-password" name="password" type="password" autocomplete="new-password" minlength="8" placeholder="Create a password" required>
                                    <button class="auth-password-toggle" type="button" data-action="toggle-password" data-target="register-password" aria-label="Show password">__ICON_EYE__</button>
                                </span>
                            </label>
                            <label class="auth-field">
                                <span class="auth-field-label">Confirm password</span>
                                <span class="auth-input">
                                    <span class="auth-input-icon" aria-hidden="true">__ICON_LOCK_SMALL__</span>
                                    <input id="register-confirm-password" name="confirmPassword" type="password" autocomplete="new-password" minlength="8" placeholder="Confirm your password" required>
                                    <button class="auth-password-toggle" type="button" data-action="toggle-password" data-target="register-confirm-password" aria-label="Show password">__ICON_EYE__</button>
                                </span>
                            </label>
                            <button class="primary-action auth-button auth-button-primary" type="submit">__ICON_USER_PLUS__ Create account</button>
                        </form>
                        """,
                "Already registered?",
                "/login",
                "Sign in",
                googleEnabled,
                error,
                null
        );
    }

    public String renderForgotPassword(String error, String status) {
        return renderAuthPage(
                "Forgot password",
                "H2Train Data password recovery",
                "Recover password",
                "Enter the email linked to your internal H2Train account",
                icon("lock"),
                """
                        <form class="auth-form auth-login-form" method="post" action="/account/password/forgot">
                            <label class="auth-field">
                                <span class="auth-field-label">Account email</span>
                                <span class="auth-input">
                                    <span class="auth-input-icon" aria-hidden="true">__ICON_MAIL__</span>
                                    <input name="email" type="email" autocomplete="email" placeholder="Enter your account email" required>
                                </span>
                            </label>
                            <button class="primary-action auth-button auth-button-primary" type="submit">__ICON_LOCK_BUTTON__ Send recovery link</button>
                        </form>
                        """,
                "Remembered it?",
                "/login",
                "Sign in",
                false,
                error,
                status
        );
    }

    public String renderPasswordReset(String token, String error) {
        return renderAuthPage(
                "Reset password",
                "H2Train Data password reset",
                "Reset password",
                "Set a new internal H2Train password",
                icon("lock"),
                """
                        <form class="auth-form auth-login-form" method="post" action="/account/password/reset">
                            <input name="token" type="hidden" value="%s">
                            <label class="auth-field">
                                <span class="auth-field-label">New password</span>
                                <span class="auth-input">
                                    <span class="auth-input-icon" aria-hidden="true">__ICON_LOCK_SMALL__</span>
                                    <input id="reset-password" name="newPassword" type="password" autocomplete="new-password" minlength="8" placeholder="Enter a new password" required>
                                    <button class="auth-password-toggle" type="button" data-action="toggle-password" data-target="reset-password" aria-label="Show password">__ICON_EYE__</button>
                                </span>
                            </label>
                            <label class="auth-field">
                                <span class="auth-field-label">Confirm new password</span>
                                <span class="auth-input">
                                    <span class="auth-input-icon" aria-hidden="true">__ICON_LOCK_SMALL__</span>
                                    <input id="reset-confirm-password" name="confirmNewPassword" type="password" autocomplete="new-password" minlength="8" placeholder="Confirm the new password" required>
                                    <button class="auth-password-toggle" type="button" data-action="toggle-password" data-target="reset-confirm-password" aria-label="Show password">__ICON_EYE__</button>
                                </span>
                            </label>
                            <button class="primary-action auth-button auth-button-primary" type="submit">__ICON_LOCK_BUTTON__ Update password</button>
                        </form>
                        """.formatted(escape(token)),
                "Remembered it?",
                "/login",
                "Sign in",
                false,
                error,
                null
        );
    }

    private String renderAuthPage(String title,
                                  String ariaLabel,
                                  String cardTitle,
                                  String cardSubtitle,
                                  String cardIcon,
                                  String formMarkup,
                                  String alternateLabel,
                                  String alternateHref,
                                  String alternateText,
                                  boolean googleEnabled,
                                  String error,
                                  String status) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s - H2Train Data</title>
                    <link rel="stylesheet" href="/portal.css">
                </head>
                <body class="auth-page-body">
                    <main class="auth-layout" aria-label="%s">
                        <section class="auth-brand-panel" aria-label="H2Train Data">
                            <div class="auth-brand-content">
                                <div class="auth-brand-logo-wrap">
                                    <img class="auth-brand-logo" src="/h2train-logo.png" alt="H2Train logo">
                                </div>
                                <div class="auth-brand-copy-block">
                                    <h1>H2Train Data</h1>
                                    <p class="auth-brand-subtitle">Data Ingestion Portal</p>
                                    <span class="auth-brand-rule" aria-hidden="true"></span>
                                    <p class="auth-brand-copy">
                                        Connect your sports data. Keep your consent visible. Ingest with control. Build longitudinal insight.
                                    </p>
                                </div>
                                <div class="auth-data-visual" aria-hidden="true">
                                    <div class="auth-data-card">
                                        <div class="auth-data-lines">
                                            <span></span>
                                            <span></span>
                                            <span></span>
                                        </div>
                                        <svg class="auth-chart" viewBox="0 0 320 150" aria-hidden="true">
                                            <path class="auth-chart-line muted" d="M12 118 C48 70 72 128 105 82 S158 96 190 54 S250 76 308 32"></path>
                                            <path class="auth-chart-line" d="M12 132 C42 110 61 94 83 102 S116 56 138 76 S177 128 204 84 S243 86 308 24"></path>
                                            <circle cx="83" cy="102" r="5"></circle>
                                            <circle cx="138" cy="76" r="5"></circle>
                                            <circle cx="204" cy="84" r="5"></circle>
                                            <circle cx="308" cy="24" r="5"></circle>
                                        </svg>
                                        <div class="auth-mini-bars">
                                            <span style="height: 34%%"></span>
                                            <span style="height: 48%%"></span>
                                            <span style="height: 66%%"></span>
                                            <span style="height: 54%%"></span>
                                            <span style="height: 84%%"></span>
                                        </div>
                                        <div class="auth-donut"><span></span></div>
                                    </div>
                                </div>
                            </div>
                            <div class="auth-system-status">
                                <span class="status-dot" aria-hidden="true"></span>
                                <span>All systems operational</span>
                            </div>
                        </section>

                        <section class="auth-login-panel">
                            <div class="auth-card auth-login-card">
                                <div class="auth-security-icon" aria-hidden="true">%s</div>
                                <div class="auth-card-heading">
                                    <h1>%s</h1>
                                    <p>%s</p>
                                </div>
                                %s
                                %s
                                %s
                                %s
                                <p class="auth-alternate">%s <a href="%s">%s</a></p>
                            </div>
                            <footer class="auth-footer">
                                <span>&copy; 2026 H2Train Data. All rights reserved.</span>
                                <span class="auth-footer-separator" aria-hidden="true"></span>
                                <span class="auth-footer-link" aria-disabled="true">Privacy Policy</span>
                                <span class="auth-footer-separator" aria-hidden="true"></span>
                                <span class="auth-footer-link" aria-disabled="true">Terms of Service</span>
                            </footer>
                        </section>
                    </main>
                    <script src="/portal.js" defer></script>
                </body>
                </html>
                """.formatted(
                escape(title),
                escape(ariaLabel),
                cardIcon,
                escape(cardTitle),
                escape(cardSubtitle),
                renderStatus(status),
                renderError(error),
                renderForm(formMarkup),
                renderGoogleAction(googleEnabled),
                escape(alternateLabel),
                escape(alternateHref),
                escape(alternateText)
        );
    }

    private String renderForm(String formMarkup) {
        return formMarkup
                .replace("__ICON_USER_PLUS__", icon("user-plus"))
                .replace("__ICON_USER__", icon("user"))
                .replace("__ICON_MAIL__", icon("mail"))
                .replace("__ICON_LOCK_SMALL__", icon("lock-small"))
                .replace("__ICON_LOCK_BUTTON__", icon("lock-button"))
                .replace("__ICON_EYE__", icon("eye"));
    }

    private String renderGoogleAction(boolean googleEnabled) {
        if (!googleEnabled) {
            return "";
        }
        return """
                <div class="auth-divider"><span>OR</span></div>
                <a class="google-action auth-google-action" href="/auth/google/login"><span class="google-mark" aria-hidden="true">%s</span>Continue with Google</a>
                """.formatted(googleIcon());
    }

    private String renderError(String error) {
        if (error == null || error.isBlank()) {
            return "";
        }
        return "<p class=\"auth-error\">" + escape(errorMessage(error)) + "</p>";
    }

    private String renderStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        String statusMessage = statusMessage(status);
        return statusMessage.isBlank()
                ? ""
                : "<p class=\"auth-status\">" + escape(statusMessage) + "</p>";
    }

    private String errorMessage(String error) {
        return switch (error) {
            case "invalid_credentials" -> "The credentials are not valid.";
            case "account_exists" -> "An account already exists with that email or username.";
            case "invalid_registration" -> "Use a valid email and a password with at least 8 characters.";
            case "password_mismatch" -> "Passwords must match.";
            case "google_unavailable" -> "Google sign-in is not configured.";
            case "google_failed" -> "Google sign-in could not be completed.";
            case "google_state" -> "Google sign-in state was invalid. Start again.";
            case "session_required" -> "Sign in before connecting providers.";
            case "provider_state" -> "Provider authorization expired or was opened from another session. Start the connection again.";
            case "reset_token_invalid" -> "This password reset link is not valid. Request a new recovery email.";
            case "reset_token_expired" -> "This password reset link has expired. Request a new recovery email.";
            case "password_unchanged" -> "The new password must be different from your current password.";
            case "invalid_password" -> "Use a password with at least 8 characters.";
            case "external_account_managed" -> "This account is managed through Google. Use Google to manage the password.";
            case "delivery_failed" -> "The recovery email could not be sent. Try again later.";
            default -> "The request could not be completed.";
        };
    }

    private String statusMessage(String status) {
        return switch (status) {
            case "password_reset" -> "Your password was updated. Sign in with the new password.";
            case "requested" -> "If a local H2Train account exists for that email, a recovery link will be sent.";
            default -> "";
        };
    }

    private String googleIcon() {
        return """
                <svg class="google-logo" viewBox="0 0 48 48" aria-hidden="true">
                    <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"></path>
                    <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"></path>
                    <path fill="#FBBC05" d="M10.53 28.59A14.45 14.45 0 0 1 9.75 24c0-1.59.28-3.14.78-4.59l-7.98-6.19A23.95 23.95 0 0 0 0 24c0 3.86.92 7.5 2.56 10.78l7.97-6.19z"></path>
                    <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"></path>
                    <path fill="none" d="M0 0h48v48H0z"></path>
                </svg>
                """;
    }

    private String icon(String name) {
        return switch (name) {
            case "user" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="12" cy="8" r="4"></circle>
                        <path d="M4 21a8 8 0 0 1 16 0"></path>
                    </svg>
                    """;
            case "mail" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <rect x="3" y="5" width="18" height="14" rx="2"></rect>
                        <path d="m4 7 8 6 8-6"></path>
                    </svg>
                    """;
            case "lock", "lock-small" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <rect x="5" y="10" width="14" height="10" rx="2"></rect>
                        <path d="M8 10V7a4 4 0 0 1 8 0v3"></path>
                        <path d="M12 14v2"></path>
                    </svg>
                    """;
            case "lock-button" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <rect x="6" y="10" width="12" height="10" rx="2"></rect>
                        <path d="M9 10V7a3 3 0 0 1 6 0v3"></path>
                    </svg>
                    """;
            case "user-plus" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="9" cy="8" r="4"></circle>
                        <path d="M2 21a7 7 0 0 1 14 0"></path>
                        <path d="M19 8v6"></path>
                        <path d="M16 11h6"></path>
                    </svg>
                    """;
            case "eye" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                    </svg>
                    """;
            default -> "";
        };
    }

    private String escape(String value) {
        return value == null
                ? ""
                : value.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;");
    }
}
