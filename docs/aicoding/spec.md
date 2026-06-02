# OneAPI Android Native Rebuild Spec

## Goal

Build a new native Android client in `D:\WorkSpace\NewAPI\OneAPI_Android` that preserves the working OneAPI mobile features while replacing the unstable chat/Codex/Claude rendering stack with a predictable native architecture.

The primary defect boundary is explicit:

- Codex/Claude tab switching must not freeze, blank, or permanently lose the composer.
- Codex/Claude recent-session switching must not freeze and must render the selected session immediately.
- Permission mode, model, reasoning, Skill/Plugin/Command, and project/session changes must be local state updates, not full Activity rebuilds.
- Chat and image conversations must keep working and must be selectable by recent session.
- Existing normal functions must remain available: Chat, image generation/edit prompt flow, Codex/Claude desktop sync, device binding, version update, announcement sync, service status, usage/wallet query, subscription/package purchase.

## Non Goals

- Do not copy the old monolithic `MainActivity.java` structure.
- Do not put message lists in `ScrollView`.
- Do not render RecyclerView rows with WebView.
- Do not use Compose in this rebuild; use stable Android Views, RecyclerView, Room, and Java for maximum compatibility with the existing local toolchain.

## Product Surface

### Top Navigation

- Default entry: Chat.
- Primary tabs: Chat, Image, Codex, Claude.
- Drawer/menu entries: AI Chat/assistants, subscriptions, service status, wallet/usage, settings/device/update.
- Tab selection changes only active screen state; it must not cancel background polling or destroy shared repositories.

### Chat

- Local conversations grouped by assistant.
- Recent-session list shows sessions by assistant group.
- Selecting a recent session updates the current assistant to that session group automatically.
- Message list uses paged Room-backed data and RecyclerView.
- Composer supports attachments, assistant selection, model, thinking, context window, and send/cancel.

### Image

- Local conversations grouped by image assistant.
- Recent-session list works like Chat.
- Composer supports one input image where required, assistant, size, quality, and send/cancel.
- Generated image result supports preview, save/download, share/copy actions.

### Codex / Claude

- Sessions are fetched from `/api/mobile/desktop-sessions`.
- Device binding comes from `/api/mobile/desktop-devices` and `/api/mobile/desktop-bindings`.
- Selected session is stored per client: `codex` and `claude`.
- Selected project name/path is stored per client.
- Selected extension refs are stored per client. Codex selections never bleed into Claude and vice versa.
- Permission mode is global unless server contract requires per-client later, but changing it updates only the toolbar icon and job request payload.
- Recent-session dialog is built from cached sessions immediately, then refreshed in the background.
- Selecting a session renders that selected session from the in-memory/cache object immediately and updates cache asynchronously.
- Polling merges updates into the active session only if the selected session id still matches.
- Message list must not contain WebView. Markdown is rendered with native TextViews, code blocks, quote blocks, and horizontally scrollable native tables.

### Settings / Server Functions

- Login/server config and stored token/app id are read from SharedPreferences.
- Version update uses `/api/download/packages`.
- Announcements use `/api/status`.
- Service status uses `/api/service-status`.
- Usage/wallet uses `/api/user/self`, `/api/user/topup/self`, `/api/log/self`, `/api/subscription/self`.
- Subscription plans and purchase use existing server endpoints currently used by Android rebuild.

## Stability Requirements

- No full-screen loading overlay for conversation transitions.
- Never remove composer before the replacement composer is already ready.
- Conversation screen state is represented by immutable `ScreenState`.
- RecyclerView adapter uses stable item ids and DiffUtil/ListAdapter.
- Long histories are loaded by pages:
  - Initial render: latest 30 display items.
  - Load earlier: prepend 30 items.
  - Active polling: append/update only changed tail items.
- All network and JSON parsing runs off the main thread.
- Main thread is used only for state dispatch and view binding.
- Any render error emits an inline error row and keeps composer visible.

## Data Model

- `ConversationSession`
  - `sessionId`, `kind`, `client`, `groupName`, `title`, `projectName`, `projectPath`, `updatedAt`, `preview`
- `ConversationMessage`
  - `messageId`, `sessionId`, `role`, `contentType`, `text`, `timestamp`, `sortIndex`, `rawJson`
- `DesktopLog`
  - represented as message-list display items, not separate UI containers.
- `ComposerState`
  - per mode fields for selected assistant/model/reasoning/context/image settings.
  - per desktop client selected session/project/extensions.

## Acceptance Criteria

- Repeatedly switch `codex -> claude -> codex -> claude` at least 30 times: no white screen, no lost composer, no ANR, no freeze.
- In Codex and Claude, switch recent sessions at least 20 times each: selected session renders within 500 ms from cache and composer remains visible.
- Toggle permission mode 30 times on Codex and Claude: no list rebuild, no freeze, toolbar icon updates.
- Select multiple skills in Claude: selected tags display immediately in the composer and do not appear in Codex.
- Markdown table renders as a table, supports local horizontal scroll, and each cell max width is at most 80% of bubble width.
- Thinking block ends at its actual boundary and does not absorb final answer content.
- Open keyboard: right scroll controls recenter in the visible chat area above the keyboard.
- Build release APK successfully and install on connected device.
