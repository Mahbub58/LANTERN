#!/usr/bin/env bash
#
# One-time Firebase setup for the debug build.
#
# It registers the App Check debug token currently printed by the app and
# deploys firestore.rules. It needs a Firebase login:
#
#   npx --yes firebase-tools@15.37.0 login
#   ./scripts/firebase_setup.sh
#
# Anonymous Auth and Gemini API billing are project settings that still need
# the Firebase console (links are printed at the end).
set -euo pipefail

PROJECT="${FIREBASE_PROJECT:-brief-11186}"
APP_ID="${FIREBASE_APP_ID:-1:780322729351:android:cf86bad376836edccc7722}"
FIREBASE=(npx --yes firebase-tools@15.37.0)

if ! "${FIREBASE[@]}" login:list 2>/dev/null | grep -qi "logged in"; then
  echo "Not logged in to Firebase. Run:"
  echo "  npx --yes firebase-tools@15.37.0 login"
  exit 1
fi

APP_PACKAGE="${APP_PACKAGE:-com.mahbub.lantern}"
TOKEN="${1:-}"
if [ -z "$TOKEN" ]; then
  TOKEN="$(adb logcat -d -s DebugAppCheckProvider 2>/dev/null \
    | grep -oE '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}' \
    | tail -1 || true)"
fi
if [ -z "$TOKEN" ]; then
  PREFS="$(adb shell run-as "$APP_PACKAGE" ls shared_prefs 2>/dev/null \
    | grep -m1 'appcheck.debug.store' || true)"
  if [ -n "$PREFS" ]; then
    TOKEN="$(adb shell run-as "$APP_PACKAGE" cat "shared_prefs/$PREFS" 2>/dev/null \
      | grep -oE '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}' \
      | tail -1 || true)"
  fi
fi

if [ -z "$TOKEN" ]; then
  echo "No App Check debug token found in logcat."
  echo "Launch the debug app once, then run this script again."
  exit 1
fi

echo "Registering App Check debug token: $TOKEN"
"${FIREBASE[@]}" appcheck:debugtokens:create "$TOKEN" \
  --project "$PROJECT" --app "$APP_ID" || {
    echo "Token registration failed. You can add it manually:"
    echo "  https://console.firebase.google.com/project/$PROJECT/appcheck/apps"
  }

echo "Deploying Firestore rules..."
"${FIREBASE[@]}" deploy --only firestore:rules --project "$PROJECT"

cat <<LINKS

Still required in the Firebase console:
  1. Anonymous Auth:
     https://console.firebase.google.com/project/$PROJECT/authentication/providers
  2. App Check enforcement for AI Logic and Firestore:
     https://console.firebase.google.com/project/$PROJECT/appcheck/apis
  3. Gemini API billing (cloud answers only):
     https://aistudio.google.com/billing
  4. Nano 4 device enrollment (on-device answers):
     https://developers.google.com/ml-kit/genai/aicore-dev-preview

Then relaunch the app. Reports queued locally are retried every 30 seconds.
LINKS
