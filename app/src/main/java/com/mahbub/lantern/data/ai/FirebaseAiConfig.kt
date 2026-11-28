package com.mahbub.lantern.data.ai

/**
 * One place to change the cloud model.
 *
 * The free tier allows only 20 requests/day for `gemini-3.8-flash`, which is
 * easy to exhaust while testing. `gemini-3.5-flash-lite` is also a supported
 * Firebase AI Logic model with structured output and a 500/day free tier, so it
 * is the default for the demo. Switch back to `gemini-3.8-flash` when billing
 * is enabled. The on-device path does not use this model at all.
 */
internal object FirebaseAiConfig {
    const val CLOUD_MODEL_NAME = "gemini-3.5-flash-lite"
}
