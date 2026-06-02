# OneAPI Android Native Rebuild Checklist

## Architecture

- [x] No WebView in RecyclerView rows.
- [x] No `ScrollView` for message lists.
- [x] No full-screen loading overlay for conversation tabs.
- [x] Composer is persistent and updated in place.
- [x] Codex and Claude state are isolated.
- [x] Network and JSON parsing do not run on main thread.
- [x] Session selection renders from cache immediately.
- [ ] Polling cannot overwrite a newly selected session.

## Chat

- [x] Default app entry opens Chat.
- [x] Chat recent sessions switch assistant automatically.
- [ ] Attachments still work.
- [ ] Model/assistant/thinking/context settings still work.
- [x] Send works through `ChatController`.
- [ ] Cancel works.

## Image

- [x] Image prompt send works through `ImageController`.
- [ ] Image attachment works.
- [ ] Size/quality settings UI works.
- [ ] Result preview/save/share works.

## Codex / Claude

- [ ] Device binding works.
- [x] Local session list loads from cache immediately.
- [ ] Codex session switch loop does not freeze.
- [ ] Claude session switch loop does not freeze.
- [ ] Codex/Claude tab switch loop does not freeze.
- [x] Permission toggle does not rebuild page.
- [x] Skill selections display immediately.
- [x] Skill selections do not leak across clients.
- [x] Job send includes correct session id and extension refs.

## Markdown

- [x] Heading renders natively.
- [x] Bullet/number list renders natively.
- [x] Code block renders natively.
- [x] Table renders as table.
- [x] Table scrolls horizontally inside bubble.
- [x] Table cell max width is <= 80% bubble width.
- [x] Thinking boundary is correct for Chinese, English, and XML-style tags.

## Drawer / Account

- [ ] Announcements sync.
- [ ] Version update check/download/install.
- [x] Service status API controller exists.
- [x] Wallet and consumption API controller exists.
- [x] Subscription list and purchase API controller exists.
- [x] Settings persist server/token/device id.

## Verification

- [x] `testReleaseUnitTest` passes.
- [x] `assembleRelease` passes.
- [ ] APK installs on connected Android device.
- [ ] Logcat has no crash/ANR during smoke test.
- [ ] Screenshots captured for chat, Codex, Claude, keyboard-open scroll dock.
