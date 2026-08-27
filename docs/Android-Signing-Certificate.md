# Android Signing Certificate Issue: Debug Builds on CI

## Symptom

When sideloading a new build of an Android app over an already-installed version, the installation fails with one of these errors:

- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
- The phone shows: "conflicts with an existing package"

Uninstalling the app first and then installing works, but this happens on every new build from CI.

## Root Cause

On every CI runner, Gradle generates a fresh debug keystore because the keystore does not persist between runs. This means every build from CI is signed with a different certificate, even though the source code is identical.

Android refuses to update an installed app if the signing certificate changes. This is a security feature—a changed certificate is how Android detects and blocks impostor apps trying to replace legitimate ones. The mechanism is working correctly. The build process is what needs to be fixed.

This does not occur in local development because a developer's machine keeps the same keystore for years.

## Proper Fix (Secure)

Use GitHub Actions secrets to store the keystore and decode it during the build. This way, every CI build signs with the same certificate without committing sensitive files to the repository.

### Step 1: Generate a debug keystore locally

```bash
keytool -genkeypair -v -keystore debug.keystore \
  -storepass android -keypass android -alias androiddebugkey \
  -keyalg RSA -keysize 2048 -validity 10950 \
  -dname "CN=Android Debug,O=Android,C=US"
```

### Step 2: Encode it as base64

```bash
base64 -i debug.keystore > debug.keystore.b64
```

### Step 3: Add it as a GitHub repository secret

1. Go to your repository **Settings** → **Secrets and variables** → **Actions**
2. Create a new secret named `DEBUG_KEYSTORE_B64`
3. Paste the base64-encoded content
4. Delete the `debug.keystore` file and `debug.keystore.b64` from your machine

### Step 4: Update your GitHub Actions workflow

In your build workflow, decode the keystore before building:

```yaml
- name: Decode debug keystore
  run: |
    echo "${{ secrets.DEBUG_KEYSTORE_B64 }}" | base64 -d > app/debug.keystore
```

Add this step before the Gradle build step.

### Step 5: Configure Gradle to use the keystore

In `app/build.gradle.kts`, set up the debug signing config:

```kotlin
android {
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
}
```

### Step 6: Add to .gitignore

Ensure the keystore is never committed:

```
*.keystore
*.keystore.b64
```

## Why This Approach

- **Secure:** The keystore file is never in source control.
- **Works across all CI runs:** Every build uses the same signing certificate.
- **No secrets exposure:** The keystore is base64-encoded and stored as a GitHub secret, not visible in logs.
- **Minimal setup:** One-time configuration, then it works for all future builds.

## To Verify the Fix Works

Compare the signing certificates of two consecutive builds:

```bash
apksigner verify --print-certs build1.apk | grep "SHA-256 digest"
apksigner verify --print-certs build2.apk | grep "SHA-256 digest"
```

Both should show the same SHA-256 digest. If they match, the signing certificate is consistent across builds and the installation issue is resolved.
