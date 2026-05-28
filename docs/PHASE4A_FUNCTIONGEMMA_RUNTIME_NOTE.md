# Phase 4A Implementation Note: FunctionGemma On-Device Runtime Blocker

This document outlines the research, official integration requirements, and current blocking status for running FunctionGemma locally on Android.

---

## 1. Official Source & Model Card
- **Model Family:** Gemma 3 (270M Parameter fine-tuned for function calling).
- **Official Model Page:** Hugging Face `google/functiongemma-270m-it` or via `kagglehub.model_download("google/functiongemma/transformers/functiongemma-270m-it")`.
- **Specialized Structure:** Utilizes standard PyTorch/Hugging Face `safetensors` format with specific control tokens for function calling: `<start_function_call>`, `<end_function_call>`.

---

## 2. Android Runtime Path
- **Official Runtime:** **MediaPipe LLM Inference API** (`com.google.mediapipe:tasks-genai`) or **LiteRT-LM** (Google AI Edge runtime).
- **Required Model Format:** `.task` bundle or `.litertlm` format.
- **Conversion Workflow:** Raw Hugging Face PyTorch/Safetensors weights must be converted using the `mediapipe.tasks.genai.converter` python utility to output a `.task` bundle.
- **Gradle Dependency (LiteRT-LM / MediaPipe):**
  ```gradle
  implementation 'com.google.mediapipe:tasks-genai:0.10.35'
  ```

---

## 3. Storage & Execution Design
- **Storage Strategy:** Pushed once to persistent, private, app-specific external storage via ADB:
  ```powershell
  adb push "functiongemma-270m-it.task" "/sdcard/Android/data/com.example.gemmacontrol/files/models/functiongemma-270m-it.task"
  ```
- **Reusability Rule:** Persistent check is performed before any build or test cycle. No automated uninstall (`adb uninstall`) is permitted to avoid deleting the cached model file.
- **RAM/Storage Requirements:** Quantized FunctionGemma-270m task bundle is exceptionally light (~100MB-200MB file size, requiring <300MB RAM), suitable for running on the connected Redmi 13 5G (6GB RAM).

---

## 4. Current Blockers (NOT IMPLEMENTED)
- **Missing Model Artifact:** No pre-converted `.task` or `.litertlm` file is physically present in the workspace or locally downloaded to provision the device.
- **No Active Runtime Dependency:** `com.google.mediapipe:tasks-genai` is not defined in Version Catalog/Gradle configurations, and no model converter pipeline exists in this local context.
- **Decision:** As a real, local, on-device FunctionGemma inference cannot be executed without these components, the application truthfully returns `ModelNotInstalled` status to the UI. The "Suggest Reply" dialog displays a friendly model status card indicating that one-time model provisioning is required, rather than using a mock/placeholder adapter.
