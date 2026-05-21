package com.h2traindata.web.portal;

import org.springframework.stereotype.Component;

@Component
public class AuthPageRenderer {

    public String renderLogin(boolean googleEnabled, String error) {
        return render(
                "Sign in",
                "Access your internal H2Train account",
                """
                        <form class="auth-form" method="post" action="/account/login">
                            <label>
                                <span>Email or username</span>
                                <input name="login" autocomplete="username" required>
                            </label>
                            <label>
                                <span>Password</span>
                                <input name="password" type="password" autocomplete="current-password" required>
                            </label>
                            <button class="primary-action" type="submit">Sign in</button>
                        </form>
                        """,
                "Need an account?",
                "/register",
                "Create account",
                googleEnabled,
                error
        );
    }

    public String renderRegister(boolean googleEnabled, String error) {
        return render(
                "Create account",
                "Register your internal H2Train account",
                """
                        <form class="auth-form" method="post" action="/account/register">
                            <label>
                                <span>Username</span>
                                <input name="username" autocomplete="username" required>
                            </label>
                            <label>
                                <span>Email</span>
                                <input name="email" type="email" autocomplete="email" required>
                            </label>
                            <label>
                                <span>Password</span>
                                <input name="password" type="password" autocomplete="new-password" minlength="8" required>
                            </label>
                            <label>
                                <span>Confirm password</span>
                                <input name="confirmPassword" type="password" autocomplete="new-password" minlength="8" required>
                            </label>
                            <button class="primary-action" type="submit">Create account</button>
                        </form>
                        """,
                "Already registered?",
                "/login",
                "Sign in",
                googleEnabled,
                error
        );
    }

    private String render(String title,
                          String subtitle,
                          String formMarkup,
                          String alternateLabel,
                          String alternateHref,
                          String alternateText,
                          boolean googleEnabled,
                          String error) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s - H2Train Data</title>
                    <link rel="stylesheet" href="/portal.css">
                </head>
                <body>
                    <main class="auth-shell">
                        <section class="auth-hero">
                            <div class="brand-lockup">
                                <img class="brand-logo" src="/h2train-logo.png" alt="H2Train logo">
                                <div class="eyebrow">Internal Access</div>
                            </div>
                            <h1>H2Train Data</h1>
                            <p class="hero-copy">
                                Use one internal account to connect Strava and Fitbit, keep provider consent together,
                                and manage automatic sync from a single workspace.
                            </p>
                            <div class="auth-highlights">
                                <div>
                                    <strong>Account identity</strong>
                                    <span>Username, email, and encrypted password storage.</span>
                                </div>
                                <div>
                                    <strong>Provider linking</strong>
                                    <span>Every connection stays attached to your internal user ID.</span>
                                </div>
                            </div>
                        </section>
                        <section class="auth-card">
                            <div class="provider-tag">Account Portal</div>
                            <h1>%s</h1>
                            <p>%s</p>
                            %s
                            %s
                            %s
                            <p class="auth-alternate">%s <a href="%s">%s</a></p>
                        </section>
                    </main>
                </body>
                </html>
                """.formatted(
                escape(title),
                escape(title),
                escape(subtitle),
                renderError(error),
                formMarkup,
                renderGoogleAction(googleEnabled),
                escape(alternateLabel),
                alternateHref,
                escape(alternateText)
        );
    }

    private String renderGoogleAction(boolean googleEnabled) {
        if (!googleEnabled) {
            return "";
        }
        return """
                <div class="auth-divider"><span>or</span></div>
                <a class="google-action" href="/auth/google/login">Continue with Google</a>
                """;
    }

    private String renderError(String error) {
        if (error == null || error.isBlank()) {
            return "";
        }
        return "<p class=\"auth-error\">" + escape(errorMessage(error)) + "</p>";
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
            default -> "The request could not be completed.";
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
