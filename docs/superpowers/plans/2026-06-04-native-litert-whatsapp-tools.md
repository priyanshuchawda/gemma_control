# Native LiteRT WhatsApp Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace fragile raw JSON FunctionGemma tool proposals with native LiteRT-LM typed WhatsApp tool callbacks.

**Architecture:** Keep deterministic local voice commands for obvious actions. For ambiguous commands, initialize LiteRT-LM conversations with `WhatsAppTools`, collect the typed callback action, map it through existing safety/proposal models, and preserve explicit user confirmation for reply actions.

**Tech Stack:** Android Kotlin, LiteRT-LM `@Tool` / `tool(...)`, Jetpack ViewModel, JUnit, Gradle.

---

### Task 1: Preserve Local Read Command Shortcut

**Files:**
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceModels.kt`
- Test: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/VoiceAssistantTest.kt`

- [ ] **Step 1: Write the failing test**

Add this assertion to `testParserRecognisesReadCommands`:

```kotlin
val cmd6 = VoiceCommandParser.parse("show my latest WhatsApp messages")
assertEquals(VoiceCommand.ReadLatestMessages, cmd6)
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ui.main.VoiceAssistantTest.testParserRecognisesReadCommands"
```

Expected: FAIL because the phrase currently returns `VoiceCommand.Unsupported`.

- [ ] **Step 3: Write minimal implementation**

Add `"show my latest whatsapp messages"` to `readPhrases`.

- [ ] **Step 4: Run test to verify it passes**

Run the same focused Gradle command.

Expected: PASS.

### Task 2: Map Native Tool Actions To Existing Proposal Flow

**Files:**
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceCommandToolProposalMapper.kt`
- Test: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/VoiceCommandToolProposalMapperTest.kt`

- [ ] **Step 1: Write failing tests**

Add tests that call a new mapper function with:

```kotlin
WhatsAppToolAction.ReadLatestNotifications(limit = 3)
WhatsAppToolAction.ReplyToLatestNotification("I am in a meeting")
WhatsAppToolAction.GetNotificationFrom(senderName = "Mom", limit = 3)
```

Expected results should map to valid existing proposals for `list_recent_whatsapp_messages`, `send_reply_to_active_whatsapp_notification` only after a real active key is supplied by the voice layer, and sender-filtered local lookup.

- [ ] **Step 2: Run focused tests**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ui.main.VoiceCommandToolProposalMapperTest"
```

Expected: FAIL because the mapper has no native action mapping function.

- [ ] **Step 3: Implement action mapping**

Add a mapper function that translates safe native read actions to existing proposal decisions. Keep reply actions routed through active notification lookup in `VoiceAssistantViewModel` because the native tool only contains reply text.

- [ ] **Step 4: Verify focused tests pass**

Run the same focused test command.

Expected: PASS.

### Task 3: Register LiteRT-LM Native WhatsApp Tools

**Files:**
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ai/runtime/GemmaEngine.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ai/runtime/GemmaModelManager.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ai/runtime/LiteRtGemmaEngine.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceAssistantViewModel.kt`
- Test: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ai/runtime/GemmaModelManagerTest.kt`

- [ ] **Step 1: Write failing engine-manager test**

Add a test proving `generateToolProposal` forwards a native tool callback collector to the engine.

- [ ] **Step 2: Run focused test**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ai.runtime.GemmaModelManagerTest"
```

Expected: FAIL because the engine API does not yet accept native callbacks.

- [ ] **Step 3: Update engine API and LiteRT implementation**

Pass `tool(WhatsAppTools(onFunctionCalled = ...))` into `ConversationConfig.tools`, following Gallery's `MobileActionsTask.kt` pattern. Stop relying on raw text JSON for native tool actions.

- [ ] **Step 4: Update voice ViewModel**

Use native actions from FunctionGemma results to produce existing `VoiceAssistantState` values. Continue to require manual confirmation for replies.

- [ ] **Step 5: Verify focused tests pass**

Run the focused manager and mapper tests.

Expected: PASS.

### Task 4: Full Verification And PR

**Files:**
- Commit only Android source/test files and this plan.

- [ ] **Step 1: Run full verification**

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest lintDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Install on phone**

```powershell
.\gradlew.bat installDebug
```

Expected: Installed on 1 device.

- [ ] **Step 3: Create PR**

Push branch `feat/87-native-litert-tools` and open a PR linked to issue #87.
