package com.mahbub.lantern.data.ai

/**
 * One place to change the cloud model. If your Firebase project has no Gemini
 * API free tier, enable billing in the console or point this at a model the
 * project supports. The on-device path does not use this model at all.
 */
internal object FirebaseAiConfig {
    const val CLOUD_MODEL_NAME = "gemini-3.8-flash"
}
