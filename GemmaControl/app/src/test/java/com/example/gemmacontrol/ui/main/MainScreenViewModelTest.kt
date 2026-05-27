package com.example.gemmacontrol.ui.main

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun uiState_initiallyLoading() = runTest {
    val viewModel = MainScreenViewModel()
    assertEquals(MainScreenUiState.Loading, viewModel.uiState.value)
  }
}

