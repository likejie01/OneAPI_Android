# OneAPI Android Native Rebuild Tasks

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to execute this task list in order. Each task should build and test before moving forward.

## Task 1: Bootstrap Native Android Project

**Files**

- Create `settings.gradle`
- Create `build.gradle`
- Create `gradle.properties`
- Create `app/build.gradle`
- Create `app/src/main/AndroidManifest.xml`
- Create `app/src/main/res/values/styles.xml`

**Steps**

- [ ] Create Android Gradle app using `com.android.application`.
- [ ] Set namespace/applicationId to `center.oneapi.mobile`.
- [ ] Set minSdk 26, target/compile SDK 35.
- [ ] Add dependencies:
  - `androidx.recyclerview:recyclerview:1.3.2`
  - `androidx.room:room-runtime:2.6.1`
  - `androidx.room:room-compiler:2.6.1`
  - `junit:junit:4.13.2`
- [ ] Build with:
  - `D:\Sofeware\OneAPI-Mobile-Toolchains\gradle-8.10.2\bin\gradle.bat testReleaseUnitTest`
  - Expected: `BUILD SUCCESSFUL`

## Task 2: Copy Assets and Theme

**Files**

- Copy drawable assets from `D:\WorkSpace\NewAPI\OneAPI_Android_Rebuild\app\src\main\res`
- Create `app/src/main/java/center/oneapi/mobile/ui/UiKit.java`

**Steps**

- [ ] Copy nav/tool/drawer icons.
- [ ] Implement colors, dp helper, rounded backgrounds, text helpers.
- [ ] Keep visual style close to current app but do not copy monolithic screen code.

## Task 3: Preferences and API Client

**Files**

- Create `app/src/main/java/center/oneapi/mobile/core/AppPrefs.java`
- Create `app/src/main/java/center/oneapi/mobile/core/ApiClient.java`
- Create `app/src/test/java/center/oneapi/mobile/core/ApiClientTest.java`

**Steps**

- [ ] Implement server/token/app id/bound device persistence.
- [ ] Implement `get`, `post`, `delete` JSON calls with background execution only.
- [ ] Unit test URL joining and error parsing.

## Task 4: Room Conversation Storage

**Files**

- Create `data/ConversationSessionEntity.java`
- Create `data/ConversationMessageEntity.java`
- Create `data/ConversationDao.java`
- Create `data/OneApiDatabase.java`
- Create `data/ConversationRepository.java`
- Create `app/src/test/java/center/oneapi/mobile/data/ConversationRepositoryTest.java`

**Steps**

- [ ] Define session/message entities.
- [ ] Add DAO query for latest page and earlier page.
- [ ] Add repository selection functions.
- [ ] Test assistant group auto-switch.
- [ ] Test Codex/Claude selected session isolation.

## Task 5: Native Markdown Renderer

**Files**

- Create `ui/markdown/MarkdownBlock.java`
- Create `ui/markdown/MarkdownParser.java`
- Create `ui/markdown/MarkdownViews.java`
- Create `app/src/test/java/center/oneapi/mobile/ui/markdown/MarkdownParserTest.java`

**Steps**

- [ ] Parse thinking blocks from `思考过程`, `Thinking:`, `Thinking：`, and `<thinking>`.
- [ ] Parse markdown tables with header/separator/body rows.
- [ ] Render table using `HorizontalScrollView` and native rows.
- [ ] Enforce max cell width at 80% of bubble width.
- [ ] Test table parsing and thinking boundary.

## Task 6: Conversation RecyclerView

**Files**

- Create `ui/conversation/ConversationDisplayItem.java`
- Create `ui/conversation/ConversationAdapter.java`
- Create `ui/conversation/ConversationFragment.java`
- Create `ui/conversation/ScrollDock.java`

**Steps**

- [ ] Implement `ListAdapter` with DiffUtil.
- [ ] Implement stable item ids.
- [ ] Implement load-earlier row.
- [ ] Implement four scroll buttons: top, current bubble top, current bubble bottom, bottom.
- [ ] Recenter scroll dock when keyboard changes.

## Task 7: Composer and Toolbars

**Files**

- Create `ui/composer/ComposerState.java`
- Create `ui/composer/ComposerView.java`
- Create `ui/composer/FlowTagLayout.java`

**Steps**

- [ ] Composer never gets removed during state updates.
- [ ] Project/assistant tag and selected skills live in the same wrapping tag layout.
- [ ] Permission toggle updates icon only.
- [ ] Skill selection is per desktop client.

## Task 8: Chat and Image Flows

**Files**

- Create `features/chat/ChatController.java`
- Create `features/image/ImageController.java`

**Steps**

- [ ] Port chat send API payload.
- [ ] Port image send API payload.
- [ ] Persist user message before network request.
- [ ] Replace progress row with final assistant response.

## Task 9: Codex/Claude Desktop Flows

**Files**

- Create `features/desktop/DesktopRepository.java`
- Create `features/desktop/DesktopController.java`
- Create `features/desktop/DesktopSessionMapper.java`
- Create tests under `app/src/test/java/center/oneapi/mobile/features/desktop`

**Steps**

- [ ] Fetch and cache desktop sessions.
- [ ] Render selected session immediately from cache.
- [ ] Poll only active client/session.
- [ ] Send desktop job with device id, session id, model, reasoning, permission, extension refs.
- [ ] Stress-test tab and session reducers.

## Task 10: Drawer Pages

**Files**

- Create `features/settings/SettingsController.java`
- Create `features/billing/BillingController.java`
- Create `features/status/StatusController.java`

**Steps**

- [ ] Implement device binding.
- [ ] Implement version update.
- [ ] Implement announcements.
- [ ] Implement service status.
- [ ] Implement wallet/usage.
- [ ] Implement subscriptions and purchase.

## Task 11: Main Shell

**Files**

- Create `MainActivity.java`
- Create `navigation/AppSection.java`
- Create `navigation/AppRouter.java`

**Steps**

- [ ] Implement top nav.
- [ ] Implement drawer.
- [ ] Keep a single conversation fragment for main conversation sections.
- [ ] Section changes dispatch state; they do not rebuild Activity.

## Task 12: Device Verification

**Steps**

- [ ] Install release APK on connected device.
- [ ] Run Codex/Claude tab switch loop manually and with adb where possible.
- [ ] Run recent-session switching loop.
- [ ] Toggle permission loop.
- [ ] Open keyboard and capture screenshot for scroll dock placement.
- [ ] Capture logcat and verify no `FATAL EXCEPTION`, `ANR`, or `Input dispatching timed out`.
