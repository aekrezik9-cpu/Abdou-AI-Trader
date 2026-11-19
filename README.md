# Abdou AI Trader

An Android chart-image analysis assistant. The app can request Android's screen-capture permission, capture the visible screen after a short delay, or let the user select a saved chart image. It then sends that image to the OpenAI Responses API for Arabic analysis.

## Current implementation
- Arabic Android interface.
- User-approved MediaProjection screen capture (one capture per permission session).
- Image picker for saved chart screenshots.
- Vision analysis through the OpenAI API using `gpt-4.1-mini`.
- Requests a cautious CALL / PUT / WAIT assessment, visible technical context, reasons, and uncertainty.
- Manual trading only. No automatic orders, no Quotex login, no private Quotex API, and no guaranteed returns.
- API key is entered at runtime and is not saved by the app.

## Requirements for AI analysis
An OpenAI API key with API billing enabled is required. A ChatGPT subscription does not include API credits. Create/manage API keys only through the official OpenAI platform. Never share the key in GitHub, screenshots, chats, or source code.

## Build
The GitHub Actions workflow in `.github/workflows/android.yml` builds a debug APK on pushes to `main` and manual workflow dispatch. Download the APK from the successful workflow's Artifacts section.

## Screen capture use
1. Open the app and tap **التقاط الشاشة الآن**.
2. Approve Android's screen-capture prompt.
3. Immediately switch to the chart app; capture starts after five seconds.
4. Return to Abdou AI Trader and analyze the captured image.

Android may show a capture indicator. The user must grant permission each time.

## Limitations and safety
- This is an initial implementation, not a validated trading system.
- The model can misread candles, indicators, chart scales, and short timeframes.
- One-to-two-minute binary/options trades are highly risky. Use demo mode and verify the chart manually.
- No trade should be placed solely from the model output.
- Do not enter an API key on a device you do not trust.
