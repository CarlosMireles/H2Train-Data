package com.stravatft.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortalController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Strava Data Portal</title>
                    <style>
                        :root {
                            color-scheme: light;
                            --bg: #f6f1e8;
                            --card: #fffdfa;
                            --text: #1f2933;
                            --accent: #fc4c02;
                            --muted: #52606d;
                        }
                        body {
                            margin: 0;
                            min-height: 100vh;
                            display: grid;
                            place-items: center;
                            background:
                                radial-gradient(circle at top, rgba(252, 76, 2, 0.18), transparent 28%),
                                linear-gradient(135deg, var(--bg), #ebe4d7);
                            font-family: "Segoe UI", sans-serif;
                            color: var(--text);
                        }
                        main {
                            max-width: 720px;
                            margin: 24px;
                            padding: 40px;
                            border-radius: 24px;
                            background: var(--card);
                            box-shadow: 0 20px 60px rgba(31, 41, 51, 0.12);
                        }
                        h1 {
                            margin-top: 0;
                            font-size: 3rem;
                            line-height: 1;
                        }
                        p {
                            font-size: 1.05rem;
                            line-height: 1.6;
                            color: var(--muted);
                        }
                        a {
                            display: inline-block;
                            margin-top: 16px;
                            padding: 14px 22px;
                            border-radius: 999px;
                            background: var(--accent);
                            color: white;
                            text-decoration: none;
                            font-weight: 700;
                        }
                    </style>
                </head>
                <body>
                    <main>
                        <h1>Connect your Strava account</h1>
                        <p>
                            This portal starts the OAuth flow, exchanges the Strava authorization code for tokens,
                            fetches the athlete's recent activities, and hands them to the configured datalake writer.
                        </p>
                        <a href="/auth/strava/login">Login with Strava</a>
                    </main>
                </body>
                </html>
                """;
    }
}
