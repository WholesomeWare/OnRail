# AI Article Summaries Implementation Plan

Add AI-powered article summaries to the MÁVINFORM section, using Gemini 2.5 Flash Lite. Summaries will be available only for signed-in users and will be cached in Firebase Realtime Database to save tokens.

## User Review Required

> [!IMPORTANT]
> **Gemini API Key**: I will set up the `Gemini` class to look for an API key in the Firebase Realtime Database config (`config/GEMINI_API_KEY`). Please ensure this key is set in your Firebase console.

> [!NOTE]
> **Model Selection**: The plan uses `gemini-1.5-flash-latest` as a default if `gemini-2.5-flash-lite` is not yet available in the stable SDK version, but it is configured to use the requested model name.

## Proposed Changes

### Dependencies

#### [MODIFY] [libs.versions.toml](file:///D:/Coding/Android/OnRail/gradle/libs.versions.toml)
- Add `generativeai` version and library definition.

#### [MODIFY] [build.gradle.kts](file:///D:/Coding/Android/OnRail/app/build.gradle.kts)
- Add `google-generativeai` implementation dependency.

---

### Data Models & Sources

#### [MODIFY] [MIArticle.kt](file:///D:/Coding/Android/OnRail/app/src/main/java/com/csakitheone/onrail/data/model/MIArticle.kt)
- Add `aiSummary` field to the `MIArticle` data class.

#### [MODIFY] [RTDB.kt](file:///D:/Coding/Android/OnRail/app/src/main/java/com/csakitheone/onrail/data/sources/RTDB.kt)
- Add `getArticleSummary` and `saveArticleSummary` methods.
- Add `CONFIG_KEY_GEMINI_API_KEY` constant.

#### [NEW] [Gemini.kt](file:///D:/Coding/Android/OnRail/app/src/main/java/com/csakitheone/onrail/data/Gemini.kt)
- Implement a singleton to initialize the Gemini model and generate summaries.
- It will fetch the API key from `RTDB`.

---

### UI Integration

#### [MODIFY] [MIArticleDisplay.kt](file:///D:/Coding/Android/OnRail/app/src/main/java/com/csakitheone/onrail/ui/components/MIArticleDisplay.kt)
- Update the article dialog to show the AI summary for signed-in users.
- Implement the logic to fetch from RTDB first, then generate via Gemini if missing.
- Add a loading state for the summary generation.

## Verification Plan

### Automated Tests
- N/A (Manual verification on device/emulator is preferred for UI and Network features).

### Manual Verification
1. Sign in with a Google account.
2. Open a MÁVINFORM article.
3. Verify that an AI summary appears (one sentence).
4. Verify that opening the same article again (or by another signed-in user) loads the summary instantly without calling the AI again (can be checked via logs).
5. Sign out and verify that the AI summary is NOT shown.
