# Vocabuildary Android

## Build

The repo includes the Gradle wrapper, so you do not need a system Gradle install.
For a local debug APK, run:

```sh
./scripts/build-debug.sh
```

The script installs a local JDK 21 and Android SDK command-line tools under
`.gradle/`, then runs `./gradlew assembleDebug`.

The debug build defaults to:

```text
VOCABUILDARY_API_BASE_URL=http://10.0.2.2:8000/api/vocabuildary/
VOCABUILDARY_AUTH_MODE=local
```

That works with the local Docker backend from an Android emulator. For a physical
device, point the app at your machine's LAN address:

```sh
VOCABUILDARY_API_BASE_URL=http://192.168.1.25:8000/api/vocabuildary/ ./scripts/build-debug.sh
```

Release builds default to gateway-token auth:

```text
VOCABUILDARY_AUTH_MODE=gateway-token
VOCABUILDARY_API_BASE_URL=https://api-get-away.krishnarajthadesar.in/api/vocabuildary/
VOCABUILDARY_MOBILE_AUTH_PATH=mobile/auth/start
VOCABUILDARY_MOBILE_REDIRECT_URI=com.kptgames.vocabuildary://auth
```

The app opens the API gateway in a browser. Once the gateway authenticates the
user and injects the usual trusted identity headers, Vocabuildary redirects back
to the app with a mobile bearer token. The APK does not contain SSO provider,
issuer, or client-id configuration.
