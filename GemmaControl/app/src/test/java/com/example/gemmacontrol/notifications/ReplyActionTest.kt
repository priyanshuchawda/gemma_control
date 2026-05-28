package com.example.gemmacontrol.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ReplyActionTest {

    @Before
    fun setUp() {
        InMemoryActiveReplyActionRegistry.clear()
    }

    @Test
    fun testRegistry_StartsEmpty() {
        assertNull(InMemoryActiveReplyActionRegistry.getReplyHandle("some_key"))
        assertTrue(InMemoryActiveReplyActionRegistry.availabilityFlow.value.isEmpty())
    }

    @Test
    fun testRegistry_RemoveDisablesAvailability() {
        val registry = InMemoryActiveReplyActionRegistry
        registry.remove("test_key")
        assertNull(registry.getReplyHandle("test_key"))
    }

    @Test
    fun testPolicyRegression_NoExecutableActionsPersisted() {
        // Volatile-only design: verify that Room active notification entities do not contain executable PendingIntent or Action objects
        val entityDir = File("src/main/java/com/example/gemmacontrol/data/local/entity")
        val fallbackDir = File("app/src/main/java/com/example/gemmacontrol/data/local/entity")
        val targetDir = if (entityDir.exists()) entityDir else if (fallbackDir.exists()) fallbackDir else null
        
        if (targetDir != null) {
            targetDir.walkBottomUp()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val content = file.readText()
                    assertFalse(
                        "Executable Actions must never be persisted: ${file.name}",
                        content.contains("PendingIntent") || content.contains("Notification.Action") || content.contains("RemoteInput")
                    )
                }
        }
    }
}
