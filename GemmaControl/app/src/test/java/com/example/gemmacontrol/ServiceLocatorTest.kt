package com.example.gemmacontrol

import org.junit.Assert.assertSame
import org.junit.Test

class ServiceLocatorTest {

    @Test
    fun returnsSingletonGemmaModelManager() {
        val first = ServiceLocator.getGemmaModelManager()
        val second = ServiceLocator.getGemmaModelManager()

        assertSame(first, second)
    }
}
