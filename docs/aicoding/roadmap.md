# OneAPI Android Native Rebuild Roadmap

## Architecture Choice

Use native Android Views with Java 17, RecyclerView, Room, and AndroidX lifecycle-free state containers. This is the lowest-risk choice for the current workspace because the local Gradle/JDK/Android SDK toolchain already builds Java Android projects successfully, and it avoids introducing Compose/Kotlin plugin version risk.

The rebuild separates UI into narrow controllers:

- `MainActivity`: shell, navigation, drawer, lifecycle.
- `ConversationFragment`: stable RecyclerView + composer host.
- `ConversationViewModel`: screen state reducer and background jobs.
- `ConversationRepository`: Room and SharedPreferences bridge for local chat/image and cached desktop sessions.
- `DesktopRepository`: device binding, session polling, desktop jobs, extensions.
- `BillingRepository`: wallet, usage, subscriptions.
- `MarkdownRenderer`: native markdown display items, no WebView in lists.

## Phases

### Phase 0: Project Bootstrap

- Create Gradle Android app in `OneAPI_Android`.
- Reuse existing package `center.oneapi.mobile` unless server/device binding depends on app id only.
- Copy visual assets from `OneAPI_Android_Rebuild/app/src/main/res`.
- Add Java 17, AndroidX RecyclerView, Room, lifecycle-independent executors, JUnit.

### Phase 1: Core Infrastructure

- Implement `ApiClient` with token/server preference support and clear error model.
- Implement `AppPrefs` for stored settings.
- Implement Room schema for sessions/messages.
- Implement single-threaded state dispatcher for UI state.

### Phase 2: Stable Conversation Engine

- Implement `ConversationDisplayItem`.
- Implement `ConversationAdapter extends ListAdapter`.
- Implement native markdown rows:
  - paragraph
  - heading
  - bullet/numbered list
  - quote
  - code block
  - table in `HorizontalScrollView`
  - thinking block
- Implement paging: latest page, load earlier, append tail.

### Phase 3: Local Chat/Image

- Port existing chat request and image request behavior.
- Persist messages to Room first, then sync SharedPreferences compatibility only if needed.
- Recent-session selection automatically updates current assistant group.

### Phase 4: Codex/Claude Desktop Sync

- Implement desktop session cache and per-client selected state.
- Implement recent-session dialog from cache with background refresh.
- Implement polling loop scoped by selected client/session.
- Implement job send with selected model, reasoning, permission, and per-client extension refs.
- Implement permission mode toolbar update without screen rebuild.

### Phase 5: Settings, Billing, Status

- Implement drawer pages:
  - settings/device binding/version update/announcements
  - service status
  - wallet/usage
  - subscriptions and purchase

### Phase 6: Verification and Packaging

- Unit tests for reducers, markdown parser, session selection, extension isolation.
- Device smoke tests:
  - tab switching loop
  - recent-session switching loop
  - permission toggle loop
  - keyboard/scroll dock layout
- Build release APK and install on device.

## Risk Controls

- No WebView in message rows.
- No UI work inside repository callbacks except dispatching immutable state.
- No shared mutable selected session for Codex/Claude.
- No Activity-level `removeAllViews()` for tab switches.
- No global loading overlay for conversation screens.
