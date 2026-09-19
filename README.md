# core-network

Retrofit/OkHttp networking layer for Sentinel Bank, built for a bank that doesn't have a real
backend yet. It ships as a genuine, wired Android networking stack — auth header injection, a
body/token-redacting logger, sane timeouts, and a certificate-pinning seam — while a built-in
**mock mode** lets the entire app run standalone, demoable end-to-end, with zero real server.

Part of the [Sentinel Bank](https://github.com/manojmourya2505) portfolio.

## Install

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/manojmourya2505/sentinel-core-network")
            credentials {
                username = providers.gradleProperty("gpr.user").getOrElse(System.getenv("GITHUB_ACTOR") ?: "")
                password = providers.gradleProperty("gpr.token").getOrElse(System.getenv("GITHUB_TOKEN") ?: "")
            }
        }
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.sentinelbank:core-network:1.0.1")
}
```

## What's inside

- `ApiClientFactory.create(...)` — builds a configured `Retrofit` + `OkHttpClient`: auth header
  injection, redacted request logging, 15s connect/read/write timeouts, and a `kotlinx.serialization`
  JSON converter.
- **Mock mode** (`mockMode = true`, the default) — installs `MockInterceptor` as the outermost
  interceptor so every request is answered from a local fixture registry, never touching the
  network. `DefaultBankingFixtures` seeds a couple of realistic starter fixtures (`GET /accounts`,
  `POST /transfer`); apps typically register their own richer set for their real API surface.
- **Certificate pinning seam** — pass `certificatePins` once a real backend and certificate exist;
  until then the pinner is simply not installed. No code changes needed later, just data.
- `safeApiCall { ... }` — wraps a suspend network call, mapping `IOException` / Retrofit
  `HttpException` / unexpected exceptions into `core-common`'s `Result.Error(AppError.Network(...))`,
  and successful results into `Result.Success`.

## Design notes

- The redacted logger (`RedactedLoggingInterceptor`) never logs request/response bodies or the
  literal `Authorization` header value — only `Authorization: <redacted>` when present — so it's
  safe to leave enabled everywhere, including against production-shaped fixture data.
- `MockInterceptor` never falls through to the real network on an unmatched fixture; it returns a
  synthetic 404 instead, so mock mode stays a hermetic sandbox even for unregistered endpoints.

## Build locally

```bash
./gradlew build
./gradlew publishToMavenLocal   # consume via mavenLocal() while iterating
```
