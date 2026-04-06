package com.h2traindata.web;

import com.h2traindata.application.service.ProviderRegistry;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortalController {

    private final ProviderRegistry providerRegistry;

    public PortalController(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        String providerCards = providerRegistry.registeredProviderIds().stream()
                .sorted()
                .map(this::providerCard)
                .reduce("", String::concat);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>H2Train Data</title>
                    <style>
                        :root {
                            color-scheme: light;
                            --bg: #eef5fb;
                            --panel: rgba(255, 255, 255, 0.72);
                            --card: rgba(255, 255, 255, 0.92);
                            --text: #16324b;
                            --muted: #4f6679;
                            --accent: #2d7db5;
                            --accent-bright: #73d2f6;
                            --accent-deep: #113b69;
                            --accent-soft: rgba(45, 125, 181, 0.12);
                            --line: rgba(17, 59, 105, 0.12);
                            --shadow: 0 28px 72px rgba(19, 61, 104, 0.12);
                        }
                        body {
                            margin: 0;
                            position: relative;
                            overflow-x: hidden;
                            background-color: #f7fbfd;
                            background-image:
                                linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(238, 245, 251, 0.96) 55%, #e7f0f7 100%),
                                radial-gradient(circle at 12% 16%, rgba(115, 210, 246, 0.24), transparent 18%),
                                radial-gradient(circle at 88% 10%, rgba(45, 125, 181, 0.15), transparent 16%),
                                radial-gradient(circle at 82% 76%, rgba(17, 59, 105, 0.11), transparent 20%),
                                repeating-linear-gradient(90deg, rgba(17, 59, 105, 0.045) 0 1px, transparent 1px 124px),
                                repeating-linear-gradient(180deg, rgba(17, 59, 105, 0.03) 0 1px, transparent 1px 124px);
                            font-family: "Aptos", "Trebuchet MS", sans-serif;
                            color: var(--text);
                            min-height: 100vh;
                        }
                        body::before,
                        body::after {
                            content: "";
                            position: fixed;
                            pointer-events: none;
                            z-index: 0;
                        }
                        body::before {
                            top: -120px;
                            right: -110px;
                            width: 420px;
                            height: 420px;
                            border-radius: 46% 54% 58% 42% / 42% 45% 55% 58%;
                            background: radial-gradient(circle at 30% 30%, rgba(115, 210, 246, 0.34), rgba(45, 125, 181, 0.08) 58%, transparent 72%);
                            filter: blur(8px);
                        }
                        body::after {
                            left: -140px;
                            bottom: 8vh;
                            width: 460px;
                            height: 460px;
                            border-radius: 50%;
                            background: radial-gradient(circle, rgba(17, 59, 105, 0.10), transparent 68%);
                        }
                        .shell {
                            max-width: 1120px;
                            margin: 0 auto;
                            padding: 48px 24px 72px;
                            position: relative;
                            z-index: 1;
                        }
                        .hero {
                            overflow: hidden;
                            position: relative;
                            padding: 52px 52px 56px;
                            border: 1px solid rgba(255, 255, 255, 0.68);
                            border-radius: 32px;
                            background:
                                linear-gradient(135deg, rgba(255, 255, 255, 0.90), rgba(247, 251, 253, 0.78)),
                                var(--panel);
                            box-shadow: var(--shadow);
                            backdrop-filter: blur(18px);
                        }
                        .hero::before {
                            content: "";
                            position: absolute;
                            inset: 0;
                            background:
                                linear-gradient(125deg, rgba(115, 210, 246, 0.16), transparent 36%),
                                radial-gradient(circle at 78% 24%, rgba(45, 125, 181, 0.14), transparent 20%);
                            pointer-events: none;
                        }
                        .hero::after {
                            content: "";
                            position: absolute;
                            inset: auto -120px -110px auto;
                            width: 320px;
                            height: 320px;
                            border-radius: 50%;
                            background: radial-gradient(circle, rgba(17, 59, 105, 0.18), transparent 68%);
                            pointer-events: none;
                        }
                        .brand-lockup {
                            display: flex;
                            align-items: center;
                            gap: 18px;
                            margin-bottom: 22px;
                        }
                        .brand-logo {
                            width: min(260px, 62vw);
                            height: auto;
                            display: block;
                            padding: 14px 18px;
                            border-radius: 24px;
                            background: linear-gradient(135deg, rgba(12, 34, 58, 0.96), rgba(17, 59, 105, 0.94));
                            border: 1px solid rgba(115, 210, 246, 0.22);
                            box-shadow: 0 18px 34px rgba(12, 34, 58, 0.18);
                        }
                        .eyebrow {
                            display: inline-flex;
                            align-items: center;
                            gap: 10px;
                            padding: 10px 16px;
                            border-radius: 999px;
                            background: rgba(255, 255, 255, 0.72);
                            color: var(--accent-deep);
                            font-size: 0.82rem;
                            font-weight: 800;
                            letter-spacing: 0.14em;
                            text-transform: uppercase;
                            border: 1px solid rgba(17, 59, 105, 0.08);
                        }
                        h1 {
                            margin-top: 0;
                            margin-bottom: 16px;
                            max-width: 10ch;
                            font-size: clamp(2.8rem, 8vw, 5.4rem);
                            line-height: 0.96;
                            letter-spacing: -0.05em;
                            color: var(--accent-deep);
                        }
                        .hero-copy {
                            max-width: 700px;
                            font-size: 1.1rem;
                            line-height: 1.7;
                            color: var(--muted);
                        }
                        .grid-header {
                            display: flex;
                            justify-content: space-between;
                            gap: 18px;
                            align-items: end;
                            margin: 38px 0 20px;
                        }
                        .grid-header h2 {
                            margin: 0;
                            font-size: 1.6rem;
                            letter-spacing: -0.03em;
                        }
                        .grid-header p {
                            margin: 0;
                            max-width: 520px;
                            line-height: 1.6;
                            color: var(--muted);
                        }
                        .providers {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
                            gap: 18px;
                        }
                        .provider-card {
                            --provider-primary: var(--accent);
                            --provider-deep: var(--accent-deep);
                            --provider-soft: var(--accent-soft);
                            display: flex;
                            flex-direction: column;
                            gap: 20px;
                            padding: 24px;
                            border-radius: 26px;
                            border: 1px solid color-mix(in srgb, var(--provider-primary) 22%, white);
                            background:
                                linear-gradient(180deg, rgba(255, 255, 255, 0.96), color-mix(in srgb, var(--provider-soft) 26%, white));
                            box-shadow: 0 18px 32px color-mix(in srgb, var(--provider-primary) 14%, transparent);
                            backdrop-filter: blur(10px);
                        }
                        .provider-top {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            gap: 16px;
                        }
                        .provider-head {
                            display: flex;
                            align-items: center;
                            gap: 16px;
                        }
                        .provider-logo {
                            display: grid;
                            place-items: center;
                            width: 68px;
                            height: 68px;
                            border-radius: 20px;
                            background: linear-gradient(180deg, color-mix(in srgb, var(--provider-primary) 82%, white), var(--provider-primary));
                            box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.3);
                        }
                        .provider-meta h3 {
                            margin: 0 0 4px;
                            font-size: 1.35rem;
                        }
                        .provider-meta span {
                            color: var(--muted);
                            font-size: 0.95rem;
                        }
                        .provider-card p {
                            margin: 0;
                            font-size: 0.94rem;
                            line-height: 1.55;
                            color: var(--muted);
                        }
                        .provider-card a {
                            width: fit-content;
                            display: inline-flex;
                            align-items: center;
                            gap: 10px;
                            padding: 14px 18px;
                            border-radius: 999px;
                            background: linear-gradient(180deg, var(--provider-primary), var(--provider-deep));
                            color: #ffffff;
                            text-decoration: none;
                            font-weight: 800;
                            letter-spacing: 0.01em;
                            box-shadow: 0 14px 24px color-mix(in srgb, var(--provider-primary) 24%, transparent);
                        }
                        .provider-tag {
                            display: inline-flex;
                            width: fit-content;
                            padding: 7px 11px;
                            border-radius: 999px;
                            background: var(--provider-soft);
                            color: var(--provider-deep);
                            font-size: 0.78rem;
                            font-weight: 700;
                            text-transform: uppercase;
                            letter-spacing: 0.08em;
                        }
                        svg {
                            display: block;
                        }
                        @media (max-width: 720px) {
                            .shell {
                                padding: 20px 16px 40px;
                            }
                            .hero {
                                padding: 32px 24px;
                                border-radius: 24px;
                            }
                            .brand-lockup {
                                flex-direction: column;
                                align-items: start;
                                gap: 14px;
                            }
                            .grid-header {
                                flex-direction: column;
                                align-items: start;
                            }
                            .provider-top {
                                flex-direction: column;
                                align-items: start;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="shell">
                        <section class="hero">
                            <div class="brand-lockup">
                                <img class="brand-logo" src="/h2train-logo.png" alt="H2Train logo">
                                <div class="eyebrow">Data Ingestion Portal</div>
                            </div>
                            <h1>H2Train Data</h1>
                            <p class="hero-copy">
                                Sync your sports providers to centralize information in a single data flow.
                                The synchronized data will be used to analyze it later and draw conclusions about
                                performance, habits, and training progression.
                            </p>
                        </section>

                        <section>
                            <div class="grid-header">
                                <h2>Available Providers</h2>
                            </div>
                            <div class="providers">
                                __PROVIDER_CARDS__
                            </div>
                        </section>
                    </div>
                </body>
                </html>
                """.replace("__PROVIDER_CARDS__", providerCards);
    }

    private String providerCard(String providerId) {
        return """
                <article class="provider-card" style="%s">
                    <div class="provider-tag">Provider Sync</div>
                    <div class="provider-top">
                        <div class="provider-head">
                            <div class="provider-logo">
                                %s
                            </div>
                            <div class="provider-meta">
                                <h3>%s</h3>
                                <span>OAuth connection and event ingestion</span>
                            </div>
                        </div>
                        <a href="/auth/%s/login">Connect</a>
                    </div>
                    <p>%s</p>
                </article>
                """.formatted(
                providerTheme(providerId),
                providerLogo(providerId),
                displayName(providerId),
                providerId,
                providerDescription(providerId)
        );
    }

    private String displayName(String providerId) {
        return switch (providerId.toLowerCase(Locale.ROOT)) {
            case "fitbit" -> "Fitbit";
            case "strava" -> "Strava";
            default -> Character.toUpperCase(providerId.charAt(0)) + providerId.substring(1);
        };
    }

    private String providerTheme(String providerId) {
        return switch (providerId.toLowerCase(Locale.ROOT)) {
            case "fitbit" -> "--provider-primary:#00b0b9;--provider-deep:#007e86;--provider-soft:rgba(0,176,185,0.14);";
            case "strava" -> "--provider-primary:#fc4c02;--provider-deep:#c53c02;--provider-soft:rgba(252,76,2,0.14);";
            default -> "--provider-primary:#0b57d0;--provider-deep:#08307b;--provider-soft:rgba(11,87,208,0.12);";
        };
    }

    private String providerDescription(String providerId) {
        return switch (providerId.toLowerCase(Locale.ROOT)) {
            case "fitbit" -> "Sync Fitbit daily activity data and prepare it for downstream analysis in H2Train.";
            case "strava" -> "Import activities and prepare them for later analysis inside the H2Train ecosystem.";
            default -> "Sync provider events and send them to the analytics pipeline.";
        };
    }

    private String providerLogo(String providerId) {
        return switch (providerId.toLowerCase(Locale.ROOT)) {
            case "fitbit" -> """
                    <svg width="34" height="34" viewBox="0 0 34 34" aria-label="Fitbit logo" role="img">
                        <circle cx="9" cy="8" r="2" fill="#ffffff"/>
                        <circle cx="17" cy="8" r="2" fill="#ffffff"/>
                        <circle cx="25" cy="8" r="2" fill="#ffffff"/>
                        <circle cx="9" cy="17" r="2" fill="#ffffff"/>
                        <circle cx="17" cy="17" r="2.4" fill="#ffffff"/>
                        <circle cx="25" cy="17" r="2" fill="#ffffff"/>
                        <circle cx="9" cy="26" r="2" fill="#ffffff"/>
                        <circle cx="17" cy="26" r="2" fill="#ffffff"/>
                    </svg>
                    """;
            case "strava" -> """
                    <svg width="32" height="32" viewBox="0 0 32 32" aria-label="Strava logo" role="img">
                        <path fill="#ffffff" d="M12 3 19.2 17H14.8L12 11.5 9.2 17H4.8z"/>
                        <path fill="#ffffff" d="M21 14.5 27.2 26H23.4L21 21.4 18.6 26H14.8z"/>
                    </svg>
                    """;
            default -> """
                    <svg width="32" height="32" viewBox="0 0 32 32" aria-hidden="true">
                        <circle cx="16" cy="16" r="10" fill="#ffffff"/>
                    </svg>
                    """;
        };
    }
}
