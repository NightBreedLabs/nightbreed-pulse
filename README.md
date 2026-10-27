# NightBreed Pulse

<p align="center">
  <b>Advanced Local Network Diagnostics, Smart Device Management & Gemini AI Assistant</b><br>
  <i>Built for the Google Pixel 11 Pro • Styled with Tokyo Night Dark Theme</i>
</p>

---

## 🌟 Overview

**NightBreed Pulse** is an intelligent, high-performance Android utility engineered by **NightBreedLabs**. Designed specifically to take advantage of modern Android hardware while prioritizing system resource hygiene, Pulse allows technicians, developers, and power users to map local networks, diagnose Smart TVs, inspect Windows PCs, wake sleeping devices, and leverage multimodal Google Gemini AI for instant root-cause analysis.

---

## ✨ Features

### 🔍 1. Subnet Discovery & Port Profiler
* **High-Concurrency Scanning**: Probes up to 254 local subnet hosts in seconds using structured Kotlin coroutines (`Dispatchers.IO`).
* **Automated Device Classification**:
  * **Gateways & Routers**: Discovers local routing hardware and web management interfaces (ports `80`, `443`, `8080`, `8443`).
  * **Vizio SmartCast TVs**: Detects REST API on port `7345` and pairing service on port `9000`.
  * **Roku TVs & Streamers**: Detects External Control Protocol (ECP) on port `8060`.
  * **Android TV / Fire TV**: Detects open ADB ports (`5555`) and Google Cast endpoints (`8008`, `8009`).
  * **Windows Workstations & Servers**: Identifies SMB file sharing (`445`) and Remote Desktop (`3389`).
* **Live Latency & Port Badges**: Displays millisecond round-trip response and responsive port chips.

### ⚡ 2. Automated Diagnostic Suite
* **Gateway & Routing Latency**: Benchmarks local packet round-trips to detect Wi-Fi interference.
* **DNS Resolution Speed**: Tests lookup latencies against local gateways, Google DNS (`8.8.8.8`), and Cloudflare (`1.1.1.1`).
* **Public WAN IP & Route Leak Check**: Queries external public egress IP to verify VPN or ISP routing status.
* **Termux Bridge Validator**: Verifies whether the local Termux SSH daemon (`127.0.0.1:8022`) is active.
* **Wake-on-LAN (WOL)**: Transmits 102-byte UDP Magic Packets to wake suspended PCs on the local broadcast domain.
* **One-Tap "Run All"**: Executes all diagnostic routines sequentially.

### 📺 3. Smart TV Hardware Controller
* **Vizio SmartCast Inspector**:
  * Queries real-time power state, active HDMI inputs (`HDMI 1`, `HDMI 2`, `SmartCast`), and system details.
  * Handles local self-signed SSL certificates automatically.
* **Roku TV Remote & App Explorer**:
  * Displays friendly name, model number, and Wi-Fi MAC address.
  * Lists all installed streaming apps.
  * On-screen remote control buttons (`Home`, `Power`, `HDMI 1`, `HDMI 2`).

### 🧠 4. Gemini AI Diagnostics Assistant
* **Context-Aware Network Intelligence**: Injects live subnet topology, discovered devices, and open ports into the diagnostic context.
* **Pre-Engineered Troubleshooting Chips**:
  * *"Analyze discovered network devices"*
  * *"Diagnose TV HDMI / ARC issue"*
  * *"Windows SMB file sharing not visible"*
  * *"Why is gateway latency high?"*
* **Secure API Key Management**: Store and update your Google Gemini API key securely.

### 🍃 5. Zero-Lag Resource Hygiene
* **Auto-Closing Sockets**: Every network probe strictly closes TCP/UDP sockets immediately in `use { ... }` blocks.
* **No Persistent Polling**: When tests complete, background worker coroutines stop and release CPU resources immediately.
* **Proactive Memory Reclamation**: Calls proactive memory reclamation hints after heavy 254-host scans to keep your phone fluid.

---

## 🎨 Theme: Tokyo Night Dark

Designed to match the Google Pixel 11 Pro aesthetic with the authentic **Tokyo Night** palette:
* **Background**: `#1A1B26` (Deep Night Storm)
* **Surface Cards**: `#24283B` / `#1F2335`
* **Tokyo Blue / Cyan**: `#7AA2F7`
* **Tokyo Violet**: `#BB9AF7`
* **Emerald Success**: `#9ECE6A`
* **Amber Warning**: `#E0AF68`
* **Coral Error**: `#F7768E`

---

## 🛠️ Tech Stack & Architecture

* **Language**: Kotlin 2.3+
* **UI Toolkit**: Jetpack Compose with Material 3 / Material You
* **Build System**: Gradle 9.1.0 (Kotlin DSL)
* **Target SDK**: Android 36 (Android 15/16)
* **Architecture**: Clean MVVM with Kotlin StateFlow & Coroutines
* **Networking**: Native non-blocking Java/Kotlin sockets (`java.net.*`, `javax.net.ssl.*`)

---

## 📦 Building from Source

```bash
# Clone the repository
git clone https://github.com/NightBreedLabs/nightbreed-pulse.git
cd nightbreed-pulse

# Build the debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).  
Copyright (c) 2026 NightBreedLabs.
