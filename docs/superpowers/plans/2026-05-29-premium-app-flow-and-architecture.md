# Premium App Flow And Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn GemmaControl from a working engineering POC into a cleaner, more production-shaped Android app with a smooth Home -> Voice/Inbox -> Action -> Success loop, while preserving the strict FunctionGemma safety boundary and local-only verification rules.

**Architecture:** Keep FunctionGemma as a proposal engine only. Kotlin owns parsing, safety routing, confirmation UI, local repository writes, Android intents, and active-notification replies. UI work should introduce a top-level Material 3 app shell, focused screen state models, bottom sheets for short actions, and smaller Compose files instead of adding more complexity to existing large screens.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Navigation 3, Room, DataStore, WorkManager, LiteRT-LM, local JVM tests, `assembleDebug`, `lintDebug`.

---

## Source Inputs

- Local app root: `C:\Users\Admin\Desktop\gemma_control`
- Reference app: `C:\Users\Admin\Desktop\gallery`
- Requested UX direction: clean home, one clear hero action, detail/action screen, fast action, satisfying success feedback, easy return.
- `ui-ux-pro-max` design-system output for this app: flat/minimal mobile app, teal/blue/neutral base, single accent CTA, fast transitions, no clutter, explicit empty/loading/success states.
- `ui-ux-pro-max` Jetpack Compose guidance: event-based navigation, stable Lazy keys, immutable state, state hoisting.
- Google AI Edge Gallery patterns:
  - `C:\Users\Admin\Desktop\gallery\Android\src\app\src\main\java\com\google\ai\edge\gallery\customtasks\mobileactions\MobileActionsTask.kt`
  - `C:\Users\Admin\Desktop\gallery\Android\src\app\src\main\java\com\google\ai\edge\gallery\customtasks\mobileactions\MobileActionsTools.kt`
  - `C:\Users\Admin\Desktop\gallery\Android\src\app\src\main\java\com\google\ai\edge\gallery\customtasks\mobileactions\Actions.kt`
  - `C:\Users\Admin\Desktop\gallery\Android\src\app\src\main\java\com\google\ai\edge\gallery\customtasks\mobileactions\MobileActionsViewModel.kt`
- Official docs to keep checked during execution:
  - Android Material 3 Compose: `https://developer.android.com/develop/ui/compose/designsystems/material3`
  - Compose Navigation: `https://developer.android.com/develop/ui/compose/navigation`
  - Material 3 bottom sheets: `https://developer.android.com/develop/ui/compose/components/bottom-sheets`
  - Android slow rendering/jank: `https://developer.android.com/topic/performance/vitals/render`
  - Android core app quality: `https://developer.android.com/docs/quality-guidelines/core-app-quality`

## Current State Snapshot

- Branch: `codex/functiongemma-tool-routing-hardening`
- Open PR: `#29 [codex] Harden FunctionGemma tool routing`
- Current dirty file before this plan: `GemmaControl/app/src/main/java/com/example/gemmacontrol/data/repository/StoredInboxRepository.kt`
- Current untracked directory: `.codex/skills/ui-ux-pro-max/...`
- Do not stage `.codex/skills/ui-ux-pro-max` unless explicitly asked to vendor that skill into the repo.
- Do not stage raw logs, APKs, AABs, model binaries, screenshots with private data, credentials, `.litertlm`, or `.tflite`.

## Product Direction

Use this app flow:

```text
Launch / setup gate
  -> Home dashboard
  -> One hero action: Speak to GemmaControl
  -> Voice proposal or Inbox detail
  -> Bottom sheet confirmation
  -> Local execution / Android intent / active reply executor
  -> Success feedback
  -> Return to updated Home
```

Top-level destinations:

```text
Home | Voice | Inbox | Settings
```

Do not make `About`, `Privacy`, or `Terms` top-level tabs. Put those inside Settings if they are added later.

Design rules:

- Material 3 first, dynamic color supported, app-owned fallback palette.
- 60 percent background, 30 percent surface/cards, 10 percent accent.
- One main CTA per screen.
- Bottom sheets for quick confirmations and action choices.
- Dialogs only for irreversible/high-risk confirmations when a sheet is not enough.
- No emoji icons in production UI. Use `Icons.Default` / `Icons.Outlined` from Material icons.
- No long splash, no decorative random animation, no blank loading.
- Keep motion small: bottom sheet motion, success check, pressed states, streaming/loading states.
- Respect current safety boundary: no silent reply sends, no silent deletion, no silent capture changes.

---

## Planned File Structure

Create focused UI files:

- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/AppShell.kt`
  - Owns top-level `Scaffold`, `NavigationBar`, and tab selection.
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/AppDestination.kt`
  - Typed top-level destination metadata: route key, label, icon.
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardScreen.kt`
  - Production home dashboard with hero voice CTA, capture status, latest/actionable summaries.
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardViewModel.kt`
  - Aggregates capture permission/status, recent local messages, actionable items, model readiness.
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardModels.kt`
  - Immutable UI state models for dashboard cards.
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceActionSheets.kt`
  - Bottom sheets for reply/tool confirmations and success feedback.
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/InboxActionSheets.kt`
  - Bottom sheets for reply composer, delete confirmation, filters.
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/ActionableInboxSection.kt`
  - Reusable actionable inbox card/list section.
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/theme/GemmaControlDesignTokens.kt`
  - App fallback colors, dimensions, motion constants, text size guidance.

Modify existing files:

- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/Navigation.kt`
  - Replace top-bar-only navigation with app shell after setup.
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/NavigationKeys.kt`
  - Add shell/tab keys if needed; keep setup as a separate gate.
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/MainScreen.kt`
  - Shrink into debug/recent-event content or merge relevant content into `HomeDashboardScreen`.
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceAssistantScreen.kt`
  - Move confirmation/success cards into `VoiceActionSheets.kt`; keep voice mic surface focused.
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/StoredInboxScreen.kt`
  - Move dialogs into bottom sheets; add filter/actionable UI.
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/SettingsScreen.kt`
  - Split model card and permission cards; remove emoji icons.
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/SetupScreen.kt`
  - Align setup colors and icons with the app design system.
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/theme/Theme.kt`
  - Replace default purple fallback palette with GemmaControl fallback palette.
- Modify: `docs/ARCHITECTURE.md`
  - Update app shell and UX architecture once implemented.
- Modify: `docs/IMPLEMENTATION_STATUS.md`
  - Track completed UX/product hardening slices.

Tests to create/modify:

- Create: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/HomeDashboardViewModelTest.kt`
- Create: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/AppDestinationTest.kt`
- Modify: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/FunctionGemmaVoiceProposalHandlerTest.kt`
- Modify: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/VoiceAssistantTest.kt`
- Modify: `GemmaControl/app/src/test/java/com/example/gemmacontrol/data/NotificationPersistenceCoordinatorTest.kt`
- Modify: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ServiceLocatorTest.kt` only if new dependencies are exposed.

---

## Task 0: Finish Current Repository Decryption Refactor

**Why:** The worktree already has a behavior-preserving decryption helper refactor. Finish it before beginning UI work so the branch stays clean and each later slice is isolated.

**Files:**
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/data/repository/StoredInboxRepository.kt`
- Test: `GemmaControl/app/src/test/java/com/example/gemmacontrol/data/NotificationPersistenceCoordinatorTest.kt`

- [ ] **Step 1: Inspect current diff**

Run:

```powershell
git diff -- GemmaControl/app/src/main/java/com/example/gemmacontrol/data/repository/StoredInboxRepository.kt
```

Expected: only helper extraction around `decryptConversationTitle`, `decryptMessageEntity`, `decryptOptionalPayload`, `decryptPayloadOrFallback`, and `DECRYPTION_FAILED`.

- [ ] **Step 2: Run targeted repository tests**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.data.NotificationPersistenceCoordinatorTest" --no-daemon --console=plain --warning-mode=summary
```

Expected: exit code `0`.

- [ ] **Step 3: Run repository quality scan**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control
python C:\Users\Admin\.agents\skills\code-reviewer\scripts\code_quality_checker.py GemmaControl\app\src\main\java\com\example\gemmacontrol\data\repository --language kotlin --json
```

Expected: no `decrypt*` helper reported as a long/high-complexity function. Existing `persistCanonicalEvent` debt may remain for a later repository-specific slice.

- [ ] **Step 4: Stage only the repository file**

Run:

```powershell
git add GemmaControl/app/src/main/java/com/example/gemmacontrol/data/repository/StoredInboxRepository.kt
git diff --cached --name-only
```

Expected staged file:

```text
GemmaControl/app/src/main/java/com/example/gemmacontrol/data/repository/StoredInboxRepository.kt
```

- [ ] **Step 5: Commit**

Run:

```powershell
git commit -m "refactor: share stored inbox decryption helpers"
```

Expected: commit created. Do not stage `.codex/`.

---

## Task 1: Introduce Top-Level App Shell With Bottom Navigation

**Why:** Current navigation is hidden in top-bar icons. A production-feeling Android app with four top-level areas should use a bottom navigation shell: Home, Voice, Inbox, Settings.

**Files:**
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/AppDestination.kt`
- Create: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/AppDestinationTest.kt`
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/AppShell.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/Navigation.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/NavigationKeys.kt`

- [ ] **Step 1: Write destination test**

Create `AppDestinationTest.kt`:

```kotlin
package com.example.gemmacontrol.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationTest {
    @Test
    fun topLevelDestinationsUseExpectedOrder() {
        val labels = AppDestination.topLevel.map { it.label }

        assertEquals(listOf("Home", "Voice", "Inbox", "Settings"), labels)
    }

    @Test
    fun topLevelDestinationRoutesAreUnique() {
        val routes = AppDestination.topLevel.map { it.route }

        assertEquals(routes.toSet().size, routes.size)
        assertTrue(routes.all { it.isNotBlank() })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ui.main.AppDestinationTest" --no-daemon --console=plain --warning-mode=summary
```

Expected: compile failure because `AppDestination` does not exist.

- [ ] **Step 3: Add destination model**

Create `AppDestination.kt`:

```kotlin
package com.example.gemmacontrol.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Home("home", "Home", Icons.Default.Home),
    Voice("voice", "Voice", Icons.Default.Mic),
    Inbox("inbox", "Inbox", Icons.Default.Inbox),
    Settings("settings", "Settings", Icons.Default.Settings);

    companion object {
        val topLevel: List<AppDestination> = listOf(Home, Voice, Inbox, Settings)
    }
}
```

- [ ] **Step 4: Run destination test**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ui.main.AppDestinationTest" --no-daemon --console=plain --warning-mode=summary
```

Expected: PASS.

- [ ] **Step 5: Add app shell**

Create `AppShell.kt`:

```kotlin
package com.example.gemmacontrol.ui.main

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun AppShell(
    modifier: Modifier = Modifier,
) {
    var selectedDestination by remember { mutableStateOf(AppDestination.Home) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                AppDestination.topLevel.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedDestination == destination,
                        onClick = { selectedDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedDestination) {
            AppDestination.Home -> HomeDashboardScreen(contentPadding = innerPadding)
            AppDestination.Voice -> VoiceAssistantScreen(onBack = { selectedDestination = AppDestination.Home })
            AppDestination.Inbox -> StoredInboxScreen(onBack = { selectedDestination = AppDestination.Home })
            AppDestination.Settings -> SettingsScreen(onBack = { selectedDestination = AppDestination.Home })
        }
    }
}
```

This compiles only after Task 2 creates `HomeDashboardScreen`.

- [ ] **Step 6: Wire shell from setup completion**

Modify `Navigation.kt` so `Main` renders `AppShell()` instead of `MainScreen(...)`. Keep `SetupScreen` as the launch gate.

Expected replacement:

```kotlin
entry<Main> {
    AppShell()
}
```

Remove unused imports for `StoredInbox`, `VoiceAssistant`, `SettingsScreen`, and `MainScreen` after the shell owns tab switching.

- [ ] **Step 7: Run build after Task 2 exists**

Run after Task 2:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: exit code `0`.

- [ ] **Step 8: Commit**

Run:

```powershell
git add GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/AppDestination.kt GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/AppDestinationTest.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/AppShell.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/Navigation.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/NavigationKeys.kt
git commit -m "feat(ui): add top-level app shell"
```

---

## Task 2: Build Home Dashboard With One Hero Action

**Why:** Home should not be a debug event feed first. It should clearly show status, one primary action, recent/actionable context, and where to go next.

**Files:**
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardModels.kt`
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardViewModel.kt`
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardScreen.kt`
- Create: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/HomeDashboardViewModelTest.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ServiceLocator.kt` only if dependencies need a cleaner boundary.

- [ ] **Step 1: Write dashboard model test**

Create `HomeDashboardViewModelTest.kt`:

```kotlin
package com.example.gemmacontrol.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDashboardViewModelTest {
    @Test
    fun dashboardSummaryChoosesSpeakAsPrimaryAction() {
        val state = HomeDashboardUiState.Ready(
            captureEnabled = true,
            notificationAccessGranted = true,
            storageEnabled = true,
            modelReady = true,
            recentMessageCount = 3,
            actionableItemCount = 2,
            latestConversationName = "Mom"
        )

        assertEquals("Speak", state.primaryActionLabel)
        assertTrue(state.statusLine.contains("3"))
        assertTrue(state.statusLine.contains("2"))
        assertFalse(state.requiresSetupAttention)
    }

    @Test
    fun dashboardSummaryFlagsSetupAttentionWhenCaptureIsNotReady() {
        val state = HomeDashboardUiState.Ready(
            captureEnabled = false,
            notificationAccessGranted = true,
            storageEnabled = false,
            modelReady = false,
            recentMessageCount = 0,
            actionableItemCount = 0,
            latestConversationName = null
        )

        assertTrue(state.requiresSetupAttention)
        assertEquals("Review setup", state.secondaryActionLabel)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ui.main.HomeDashboardViewModelTest" --no-daemon --console=plain --warning-mode=summary
```

Expected: compile failure because `HomeDashboardUiState` does not exist.

- [ ] **Step 3: Add dashboard UI models**

Create `HomeDashboardModels.kt`:

```kotlin
package com.example.gemmacontrol.ui.main

sealed interface HomeDashboardUiState {
    data object Loading : HomeDashboardUiState

    data class Ready(
        val captureEnabled: Boolean,
        val notificationAccessGranted: Boolean,
        val storageEnabled: Boolean,
        val modelReady: Boolean,
        val recentMessageCount: Int,
        val actionableItemCount: Int,
        val latestConversationName: String?
    ) : HomeDashboardUiState {
        val primaryActionLabel: String = "Speak"
        val secondaryActionLabel: String = if (requiresSetupAttention) "Review setup" else "Open inbox"
        val requiresSetupAttention: Boolean = !captureEnabled || !notificationAccessGranted
        val statusLine: String =
            "$recentMessageCount recent message${if (recentMessageCount == 1) "" else "s"} · " +
                "$actionableItemCount actionable item${if (actionableItemCount == 1) "" else "s"}"
    }
}
```

- [ ] **Step 4: Run dashboard model test**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ui.main.HomeDashboardViewModelTest" --no-daemon --console=plain --warning-mode=summary
```

Expected: PASS.

- [ ] **Step 5: Add dashboard screen**

Create `HomeDashboardScreen.kt`:

```kotlin
package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeDashboardScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    state: HomeDashboardUiState = HomeDashboardUiState.Ready(
        captureEnabled = true,
        notificationAccessGranted = true,
        storageEnabled = false,
        modelReady = false,
        recentMessageCount = 0,
        actionableItemCount = 0,
        latestConversationName = null
    ),
) {
    when (state) {
        HomeDashboardUiState.Loading -> {
            Column(
                modifier = modifier.fillMaxSize().padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is HomeDashboardUiState.Ready -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "GemmaControl",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = state.statusLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Text(
                            text = "Ask GemmaControl about recent WhatsApp notifications",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                            Text(state.primaryActionLabel)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.latestConversationName?.let { "Continue from $it" }
                        ?: "New messages and follow-ups will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

The initial screen can be static in this task to unblock the shell. The next task wires real state and navigation callbacks.

- [ ] **Step 6: Wire Home hero action to Voice tab**

Update `HomeDashboardScreen` signature:

```kotlin
fun HomeDashboardScreen(
    contentPadding: PaddingValues,
    onSpeak: () -> Unit,
    onOpenInbox: () -> Unit,
    onReviewSetup: () -> Unit,
    modifier: Modifier = Modifier,
    state: HomeDashboardUiState = HomeDashboardUiState.Loading,
)
```

Update primary button:

```kotlin
Button(onClick = onSpeak, modifier = Modifier.fillMaxWidth()) {
    Text(state.primaryActionLabel)
}
```

Update `AppShell.kt`:

```kotlin
AppDestination.Home -> HomeDashboardScreen(
    contentPadding = innerPadding,
    onSpeak = { selectedDestination = AppDestination.Voice },
    onOpenInbox = { selectedDestination = AppDestination.Inbox },
    onReviewSetup = { selectedDestination = AppDestination.Settings }
)
```

- [ ] **Step 7: Run local unit tests and assemble**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: exit code `0`.

- [ ] **Step 8: Commit**

Run:

```powershell
git add GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardModels.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardViewModel.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardScreen.kt GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/HomeDashboardViewModelTest.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/AppShell.kt
git commit -m "feat(ui): add home dashboard hero action"
```

---

## Task 3: Add Real Dashboard Aggregation

**Why:** Home must show useful state, not static marketing copy. It should summarize capture readiness, local storage, FunctionGemma model readiness, recent messages, and actionable items.

**Files:**
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardViewModel.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardScreen.kt`
- Test: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/HomeDashboardViewModelTest.kt`

- [ ] **Step 1: Extend tests for state aggregation**

Add tests that construct `HomeDashboardUiState.Ready` from fake values:

```kotlin
@Test
fun dashboardSummaryUsesLatestConversationWhenAvailable() {
    val state = HomeDashboardUiState.Ready(
        captureEnabled = true,
        notificationAccessGranted = true,
        storageEnabled = true,
        modelReady = false,
        recentMessageCount = 5,
        actionableItemCount = 1,
        latestConversationName = "Aunt May"
    )

    assertEquals("Aunt May", state.latestConversationName)
    assertEquals("Open inbox", state.secondaryActionLabel)
}
```

- [ ] **Step 2: Add viewmodel dependencies**

Implement `HomeDashboardViewModel` with dependencies passed in constructor:

```kotlin
class HomeDashboardViewModel(
    private val repository: LocalWhatsAppDataRepository,
    private val preferencesRepository: CapturePreferencesRepository,
    private val notificationAccessChecker: () -> Boolean,
    private val modelReadyChecker: suspend () -> Boolean,
) : ViewModel()
```

Use `StateFlow<HomeDashboardUiState>`, `viewModelScope.launch`, and existing repository calls:

```kotlin
val recent = repository.listRecentMessages(conversationName = null, limit = 5, sinceMinutes = null)
val actionable = repository.getActionableInbox(status = "PENDING", priority = null, limit = 10)
```

- [ ] **Step 3: Keep UI state immutable**

Do not expose mutable collections from `HomeDashboardUiState`. Convert repository lists to counts and display DTOs before exposing them.

- [ ] **Step 4: Wire viewmodel from screen**

Use `viewModel { HomeDashboardViewModel(...) }` only after the required dependencies can be obtained through `ServiceLocator`.

- [ ] **Step 5: Run tests**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ui.main.HomeDashboardViewModelTest" --no-daemon --console=plain --warning-mode=summary
```

Expected: PASS.

- [ ] **Step 6: Run full local gate**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: exit code `0`.

- [ ] **Step 7: Commit**

Run:

```powershell
git add GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardViewModel.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/HomeDashboardScreen.kt GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/HomeDashboardViewModelTest.kt
git commit -m "feat(ui): summarize app state on home"
```

---

## Task 4: Replace Voice Confirmation Cards With Bottom Sheets

**Why:** Voice currently shows confirmation cards inline below the mic. For short actions, Material 3 bottom sheets feel more Android-native and keep the mic surface focused.

**Files:**
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceActionSheets.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceAssistantScreen.kt`
- Modify: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/FunctionGemmaVoiceProposalHandlerTest.kt`

- [ ] **Step 1: Preserve existing state tests**

Run before edits:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ui.main.FunctionGemmaVoiceProposalHandlerTest" --tests "com.example.gemmacontrol.ui.main.VoiceAssistantTest" --no-daemon --console=plain --warning-mode=summary
```

Expected: PASS.

- [ ] **Step 2: Extract voice sheet composables**

Move these composables from `VoiceAssistantScreen.kt` to `VoiceActionSheets.kt`:

```text
ReadLatestConfirmationCard
VoiceReplyConfirmationCard
LocalToolConfirmationCard
LocalToolSucceededCard
VoiceFailureCard
LanguagePackMissingCard
SystemRecognitionConsentCard
TwoButtonRow
```

Rename them to sheet-specific names:

```text
ReadLatestConfirmationSheetContent
VoiceReplyConfirmationSheetContent
LocalToolConfirmationSheetContent
LocalToolSucceededSheetContent
VoiceFailureSheetContent
LanguagePackMissingSheetContent
SystemRecognitionConsentSheetContent
```

- [ ] **Step 3: Use `ModalBottomSheet` for action states**

In `VoiceAssistantScreen.kt`, render the mic/header as the stable main surface. Render a `ModalBottomSheet` when state is:

```kotlin
VoiceAssistantState.CommandReady
VoiceAssistantState.ConfirmationRequired
VoiceAssistantState.LocalToolConfirmationRequired
VoiceAssistantState.LocalToolSucceeded
VoiceAssistantState.Failure
VoiceAssistantState.LanguagePackMissingError
VoiceAssistantState.ConfirmSystemRecognitionConsent
```

Use:

```kotlin
val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
ModalBottomSheet(
    onDismissRequest = onCancel,
    sheetState = sheetState
) {
    VoiceActionSheetContent(...)
}
```

- [ ] **Step 4: Keep streaming inline**

Do not put `VoiceAssistantState.Streaming` in a sheet. Keep FunctionGemma streaming visible near the mic so the user sees live progress and can stop response.

- [ ] **Step 5: Run compile/tests**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: exit code `0`.

- [ ] **Step 6: Commit**

Run:

```powershell
git add GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceActionSheets.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceAssistantScreen.kt GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/FunctionGemmaVoiceProposalHandlerTest.kt
git commit -m "feat(voice): move confirmations to bottom sheets"
```

---

## Task 5: Add Actionable Inbox UI

**Why:** Backend support for actionable inbox exists, but there is no first-class UI for it. This is one of the highest-value product loops: message -> follow-up/priority/reminder -> completion -> success.

**Files:**
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/ActionableInboxSection.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/StoredInboxViewModel.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/StoredInboxScreen.kt`
- Modify: `GemmaControl/app/src/test/java/com/example/gemmacontrol/data/NotificationPersistenceCoordinatorTest.kt`

- [ ] **Step 1: Add repository-backed viewmodel state**

Add immutable state to `StoredInboxViewModel`:

```kotlin
data class ActionableInboxUiItem(
    val id: String,
    val messageEventId: String,
    val title: String,
    val conversationName: String,
    val priority: String,
    val status: String,
    val dueAt: String?,
    val text: String?
)
```

Expose:

```kotlin
val actionableItems: StateFlow<List<ActionableInboxUiItem>>
```

- [ ] **Step 2: Add tests for actionable state mapping**

Use existing fake DAO setup in `NotificationPersistenceCoordinatorTest` to verify:

```kotlin
val pending = repository.getActionableInbox(status = "PENDING", priority = "HIGH", limit = 10)
assertEquals(setOf("FOLLOW_UP", "PRIORITY_MESSAGE"), pending.map { it.type }.toSet())
```

This already exists; extend only if the viewmodel mapping has a separate pure mapper.

- [ ] **Step 3: Add ActionableInboxSection**

Create `ActionableInboxSection.kt` with:

```kotlin
@Composable
fun ActionableInboxSection(
    items: List<ActionableInboxUiItem>,
    onComplete: (String) -> Unit,
    onOpenMessage: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

UI rules:

- Empty state: "No pending actions" plus a short line explaining that follow-ups and high-priority messages appear here.
- Item row: title, conversation, priority/status chip, due date if present, one primary small action.
- Use stable keys in Lazy lists.

- [ ] **Step 4: Add section to Inbox**

Place `ActionableInboxSection` above encrypted message list in `StoredInboxScreen.kt`.

- [ ] **Step 5: Add completion success feedback**

When `onComplete` succeeds, show snackbar:

```text
Follow-up completed.
```

When it fails:

```text
Follow-up could not be completed.
```

- [ ] **Step 6: Run local gate**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: exit code `0`.

- [ ] **Step 7: Commit**

Run:

```powershell
git add GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/ActionableInboxSection.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/StoredInboxViewModel.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/StoredInboxScreen.kt GemmaControl/app/src/test/java/com/example/gemmacontrol/data/NotificationPersistenceCoordinatorTest.kt
git commit -m "feat(inbox): surface actionable items"
```

---

## Task 6: Convert Inbox Dialogs To Bottom Sheets

**Why:** Stored Inbox currently has many AlertDialogs. Reply compose, send review, storage opt-in, delete confirmation, and filters should become predictable sheets or focused high-risk confirmations.

**Files:**
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/InboxActionSheets.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/StoredInboxScreen.kt`
- Modify: `GemmaControl/app/src/test/java/com/example/gemmacontrol/notifications/ReplyActionTest.kt` only if send behavior changes.

- [ ] **Step 1: Extract sheet content**

Move dialog content into named sheet composables:

```text
StorageConsentSheetContent
DeleteLocalDataSheetContent
ReplyComposerSheetContent
ReplySendReviewSheetContent
```

- [ ] **Step 2: Keep destructive confirmation visually distinct**

For delete all:

```kotlin
ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
```

Text must include:

```text
This deletes only local GemmaControl data. It does not delete WhatsApp chats.
```

- [ ] **Step 3: Replace magic numbers with constants**

In `StoredInboxScreen.kt`, replace:

```kotlin
1000
120.dp
```

with:

```kotlin
private const val MAX_REPLY_CHARS = 1000
private val ReplyEditorHeight = 120.dp
```

- [ ] **Step 4: Run local gate**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: exit code `0`.

- [ ] **Step 5: Commit**

Run:

```powershell
git add GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/InboxActionSheets.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/StoredInboxScreen.kt
git commit -m "feat(inbox): use bottom sheets for quick actions"
```

---

## Task 7: Create App Design Tokens And Remove Screen-Local Palettes

**Why:** Setup, Main, Inbox, and Theme each define separate colors. Production UI needs one visual language.

**Files:**
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/theme/GemmaControlDesignTokens.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/theme/Color.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/theme/Theme.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/MainScreen.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/StoredInboxScreen.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/SetupScreen.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/SettingsScreen.kt`

- [ ] **Step 1: Add fallback palette**

Create `GemmaControlDesignTokens.kt`:

```kotlin
package com.example.gemmacontrol.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object GemmaControlColors {
    val Primary = Color(0xFF0D9488)
    val Secondary = Color(0xFF2563EB)
    val Action = Color(0xFFF97316)
    val Background = Color(0xFFF8FAFC)
    val Text = Color(0xFF1E293B)
}

object GemmaControlSpacing {
    val Tiny = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val XLarge = 32.dp
}

object GemmaControlShape {
    val CardRadius = 16.dp
    val ButtonRadius = 16.dp
    val SheetRadius = 24.dp
}
```

- [ ] **Step 2: Update MaterialTheme fallback**

In `Theme.kt`, use:

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = GemmaControlColors.Primary,
    secondary = GemmaControlColors.Secondary,
    tertiary = GemmaControlColors.Action,
    background = GemmaControlColors.Background,
    onBackground = GemmaControlColors.Text,
    onSurface = GemmaControlColors.Text
)
```

Keep dynamic color enabled on Android 12+.

- [ ] **Step 3: Remove emoji icons from Settings**

Replace `emoji: String` in `PermissionLinkCard` with `icon: ImageVector`.

Example:

```kotlin
PermissionLinkCard(
    icon = Icons.Default.Notifications,
    title = "Notification Listener Access",
    ...
)
```

- [ ] **Step 4: Remove negative letter spacing**

In `MainScreen.kt`, remove:

```kotlin
letterSpacing = (-0.3).sp
```

Use default letter spacing.

- [ ] **Step 5: Run compile/lint**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: exit code `0`.

- [ ] **Step 6: Commit**

Run:

```powershell
git add GemmaControl/app/src/main/java/com/example/gemmacontrol/theme/GemmaControlDesignTokens.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/theme/Color.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/theme/Theme.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/MainScreen.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/StoredInboxScreen.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/SetupScreen.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/SettingsScreen.kt
git commit -m "style(ui): align screens to app design tokens"
```

---

## Task 8: Add Gallery-Style Function Call Details To Confirmations

**Why:** Gallery shows function call details separately from model response. GemmaControl should show tool name, parameters, and safety level before confirmation so users understand what the model proposed.

**Files:**
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ai/tools/ToolCallDisplayModels.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/FunctionGemmaVoiceProposalHandler.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceModels.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceActionSheets.kt`
- Modify: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/FunctionGemmaVoiceProposalHandlerTest.kt`

- [ ] **Step 1: Add display model**

Create `ToolCallDisplayModels.kt`:

```kotlin
package com.example.gemmacontrol.ai.tools

data class ToolCallDisplayDetails(
    val toolName: String,
    val safetyLevel: ToolSafetyLevel,
    val parameters: List<ToolCallDisplayParameter>
)

data class ToolCallDisplayParameter(
    val name: String,
    val value: String
)

fun ToolProposal.toDisplayDetails(): ToolCallDisplayDetails {
    return ToolCallDisplayDetails(
        toolName = name.value,
        safetyLevel = definition.safetyLevel,
        parameters = arguments.map { (name, value) ->
            ToolCallDisplayParameter(name = name, value = value.displayValue())
        }
    )
}

private fun ToolArgument.displayValue(): String {
    return when (this) {
        is ToolArgument.StringValue -> value
        is ToolArgument.IntegerValue -> value.toString()
        is ToolArgument.BooleanValue -> value.toString()
    }
}
```

- [ ] **Step 2: Add handler test**

In `FunctionGemmaVoiceProposalHandlerTest.kt`, add a test that a local write proposal carries display details into `PendingLocalToolAction`.

Expected assertions:

```kotlin
assertEquals("create_follow_up_from_message", action.toolCallDetails.toolName)
assertEquals(ToolSafetyLevel.LocalWrite, action.toolCallDetails.safetyLevel)
assertTrue(action.toolCallDetails.parameters.any { it.name == "message_event_id" })
```

- [ ] **Step 3: Extend `PendingLocalToolAction`**

In `VoiceModels.kt`, add:

```kotlin
val toolCallDetails: ToolCallDisplayDetails
```

to `PendingLocalToolAction`.

- [ ] **Step 4: Show details in sheet**

In `VoiceActionSheets.kt`, render:

```text
Tool: create_follow_up_from_message
Safety: LocalWrite
message_event_id: ...
follow_up_title: ...
```

For `message_text`, truncate display to 160 characters but keep execution payload unchanged.

- [ ] **Step 5: Run targeted tests**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ui.main.FunctionGemmaVoiceProposalHandlerTest" --tests "com.example.gemmacontrol.ai.tools.ToolSafetyRouterTest" --no-daemon --console=plain --warning-mode=summary
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add GemmaControl/app/src/main/java/com/example/gemmacontrol/ai/tools/ToolCallDisplayModels.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/FunctionGemmaVoiceProposalHandler.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceModels.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/VoiceActionSheets.kt GemmaControl/app/src/test/java/com/example/gemmacontrol/ui/main/FunctionGemmaVoiceProposalHandlerTest.kt
git commit -m "feat(ai): show function call details before confirmation"
```

---

## Task 9: Make Model Download Production-Shaped

**Why:** The current Settings model card asks for raw HTTPS URL and SHA-256. That is useful for development but not premium. Production users need a clear installed/missing/downloading card and safe advanced controls.

**Files:**
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/FunctionGemmaModelCard.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/SettingsScreen.kt`
- Modify: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ai/model/FunctionGemmaModelCatalogTest.kt`
- Modify: `GemmaControl/app/src/test/java/com/example/gemmacontrol/ai/model/ModelDownloadUiStateMapperTest.kt`

- [ ] **Step 1: Preserve catalog tests**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ai.model.FunctionGemmaModelCatalogTest" --tests "com.example.gemmacontrol.ai.model.ModelDownloadUiStateMapperTest" --no-daemon --console=plain --warning-mode=summary
```

Expected: PASS.

- [ ] **Step 2: Extract model card**

Move `FunctionGemmaModelDownloadCard`, `ModelDownloadProgressBlock`, `formatBytes`, and `formatDuration` to `FunctionGemmaModelCard.kt`.

- [ ] **Step 3: Add advanced/manual section**

Default view:

```text
FunctionGemma Mobile Actions
Status: Installed / Missing / Downloading / Failed
[Download verified model] [Cancel]
```

Advanced section collapsed by default:

```text
Manual model URL
SHA-256
```

Keep manual URL validation strict:

```kotlin
require(url.startsWith("https://")) { "Model URL must use HTTPS." }
require(sha256.matches(Regex("^[a-fA-F0-9]{64}$"))) { "SHA-256 must be 64 hex characters." }
```

- [ ] **Step 4: Do not add or commit model binaries**

Before staging:

```powershell
git ls-files --others --exclude-standard | rg "(?i)(\.litertlm$|\.tflite$|\.apk$|\.aab$|\.log$|credential|secret|screenshot)"
```

Expected: no matches. Exit code `1` from `rg` is acceptable and means no matches.

- [ ] **Step 5: Run full local gate**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: exit code `0`.

- [ ] **Step 6: Commit**

Run:

```powershell
git add GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/FunctionGemmaModelCard.kt GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/SettingsScreen.kt GemmaControl/app/src/test/java/com/example/gemmacontrol/ai/model/FunctionGemmaModelCatalogTest.kt GemmaControl/app/src/test/java/com/example/gemmacontrol/ai/model/ModelDownloadUiStateMapperTest.kt
git commit -m "feat(settings): polish FunctionGemma model card"
```

---

## Task 10: Reduce Large Compose Files Without Behavior Changes

**Why:** Code-reviewer flags `MainScreen.kt`, `StoredInboxScreen.kt`, `SettingsScreen.kt`, `SetupScreen.kt`, and `VoiceAssistantScreen.kt` as large/complex. Split only along UI responsibility boundaries, with tests and builds after each slice.

**Files:**
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/MainScreen.kt`
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/MainEventCards.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/StoredInboxScreen.kt`
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/StoredMessageRow.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/SettingsScreen.kt`
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/SettingsPermissionCards.kt`
- Modify: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/SetupScreen.kt`
- Create: `GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main/SetupStepCards.kt`

- [ ] **Step 1: Move Main event-card components**

Move from `MainScreen.kt` to `MainEventCards.kt`:

```text
NotificationEventCard
Badge
Chip
rememberFormatter
```

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: PASS.

- [ ] **Step 2: Move StoredMessageRow**

Move `StoredMessageRow` from `StoredInboxScreen.kt` to `StoredMessageRow.kt`.

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: PASS.

- [ ] **Step 3: Move Settings permission cards**

Move from `SettingsScreen.kt` to `SettingsPermissionCards.kt`:

```text
SectionHeader
VoiceInputModeCard
PermissionLinkCard
```

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: PASS.

- [ ] **Step 4: Move Setup step cards**

Move from `SetupScreen.kt` to `SetupStepCards.kt`:

```text
ProgressBadge
SetupStepCard
MiuiAutostartCard
```

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: PASS.

- [ ] **Step 5: Run quality scan**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control
python C:\Users\Admin\.agents\skills\code-reviewer\scripts\code_quality_checker.py GemmaControl\app\src\main\java\com\example\gemmacontrol\ui\main --language kotlin --json
```

Expected: fewer long-function findings in UI files. Some complex screen roots may remain until deeper state extraction.

- [ ] **Step 6: Commit**

Run:

```powershell
git add GemmaControl/app/src/main/java/com/example/gemmacontrol/ui/main
git commit -m "refactor(ui): split large Compose surfaces"
```

---

## Task 11: Update Docs And PR

**Why:** Architecture docs currently emphasize tool routing. After UX hardening, docs should reflect app shell, bottom sheets, actionable inbox UI, and model card direction.

**Files:**
- Modify: `docs/ARCHITECTURE.md`
- Modify: `docs/AI_TOOL_ROUTING.md`
- Modify: `docs/IMPLEMENTATION_STATUS.md`
- Modify: PR #29 body.

- [ ] **Step 1: Update architecture docs**

Add sections:

```text
Top-Level App Shell
Home Dashboard
Voice Confirmation Sheets
Actionable Inbox UI
FunctionGemma Model Card
```

- [ ] **Step 2: Update implementation status**

Mark completed slices with exact commit hashes after each commit.

- [ ] **Step 3: Run artifact scans**

Run:

```powershell
git diff --cached --name-only | rg "(?i)(\.apk$|\.aab$|\.log$|\.litertlm$|\.tflite$|credential|secret|screenshot)"
git ls-files --others --exclude-standard | rg "(?i)(\.apk$|\.aab$|\.log$|\.litertlm$|\.tflite$|credential|secret|screenshot)"
```

Expected: no matches. `rg` exit code `1` is acceptable.

- [ ] **Step 4: Run full local gate**

Run:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon --console=plain --warning-mode=summary
```

Expected: exit code `0`.

- [ ] **Step 5: Commit docs**

Run:

```powershell
git add docs/ARCHITECTURE.md docs/AI_TOOL_ROUTING.md docs/IMPLEMENTATION_STATUS.md
git commit -m "docs: document premium app flow plan"
```

- [ ] **Step 6: Push and update PR**

Run:

```powershell
git push
gh pr edit 29 --body-file <updated-pr-body-file>
gh pr checks 29
```

Expected: PR checks pass or are pending only for external CI. Do not mark physical-device validation complete.

---

## Deferred Until Physical Device

Do not claim these complete during local-only work:

- Real WhatsApp notification capture on the phone.
- Real active `RemoteInput` reply send.
- Real WhatsApp share/click-to-chat launch.
- Real FunctionGemma `.litertlm` runtime quality and latency.
- Real microphone/on-device speech recognition behavior.
- Real jank/performance trace from a device.

Record them in `docs/DEVICE_VALIDATION.md` after physical testing.

---

## Verification Commands

Use compact terminal output. Do not paste full logs into chat.

Targeted tests:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ui.main.AppDestinationTest" --no-daemon --console=plain --warning-mode=summary
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ui.main.HomeDashboardViewModelTest" --no-daemon --console=plain --warning-mode=summary
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.ui.main.FunctionGemmaVoiceProposalHandlerTest" --no-daemon --console=plain --warning-mode=summary
.\gradlew.bat testDebugUnitTest --tests "com.example.gemmacontrol.data.NotificationPersistenceCoordinatorTest" --no-daemon --console=plain --warning-mode=summary
```

Full local gate:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon --console=plain --warning-mode=summary
```

Quality scan:

```powershell
cd C:\Users\Admin\Desktop\gemma_control
python C:\Users\Admin\.agents\skills\code-reviewer\scripts\code_quality_checker.py GemmaControl\app\src\main\java --language kotlin --json
```

Artifact scan:

```powershell
git diff --cached --name-only | rg "(?i)(\.apk$|\.aab$|\.log$|\.litertlm$|\.tflite$|credential|secret|screenshot)"
git ls-files --others --exclude-standard | rg "(?i)(\.apk$|\.aab$|\.log$|\.litertlm$|\.tflite$|credential|secret|screenshot)"
```

---

## Execution Order

1. Finish current repository refactor.
2. Add app shell and Home destination.
3. Add real dashboard aggregation.
4. Move voice confirmations to bottom sheets.
5. Add Actionable Inbox UI.
6. Move inbox dialogs to bottom sheets.
7. Add design tokens and remove local palettes/emojis.
8. Add function-call details to confirmation UI.
9. Polish FunctionGemma model card.
10. Split large Compose files.
11. Update docs and PR.

This order keeps risk controlled: first clean the dirty worktree, then create the navigation foundation, then add product value, then refactor large files after the new behavior is covered.

## Self-Review

- Spec coverage: covers requested app flow, Material 3 direction, bottom navigation, bottom sheets, success/empty/loading states, FunctionGemma safety, Gallery function-call display pattern, code quality, docs, tests, local-only verification, and artifact safety.
- Placeholder scan: no banned placeholder phrasing or intentionally undefined task remains.
- Type consistency: planned names use current package `com.example.gemmacontrol` and existing classes such as `VoiceAssistantScreen`, `StoredInboxScreen`, `WhatsAppLocalToolExecutor`, `ToolProposal`, and `LocalWhatsAppDataRepository`.
- Scope control: physical device behavior is explicitly deferred and must not be claimed during local Gradle-only work.
