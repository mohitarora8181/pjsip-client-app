# 📞 Local VoIP Client (Android) 

> A modern Android client built with **Kotlin and Jetpack Compose** for making local **VoIP (Voice over IP) calls**. It connects to a **local Asterisk server** running on a **Raspberry Pi** via **SIP (Session Initiation Protocol)** connections.


## ✨ Key Features

* **SIP Connectivity:** Secure connection and registration with a local Asterisk SIP server.
* **VoIP Calling:** Initiate outbound voice calls to other registered SIP clients.
* **Incoming Call Management:** Functionality to seamlessly pick up and reject incoming calls.
* **In-Call Controls:** Comprehensive set of features during an active call:
    * **Mute/Unmute** the microphone.
    * **Hold/Resume** the call.
    * **Speaker Toggle (On/Off)** for hands-free operation.
* **Modern UI:** Intuitive and responsive user interface built with Jetpack Compose.

## 🛠️ Tech Stack & Dependencies

This application leverages a modern Android development stack and specialized VoIP libraries:

### Client (Android App)
* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (For a declarative and modern UI)
* **VoIP SDK:** [Ajvoip SDK] (Used for handling SIP registration and call management)

### Backend (Server)
* **Server Platform:** [Raspberry Pi (e.g., Raspberry Pi 4 Model B)]
* **VoIP Server Software:** [Asterisk] (Used as the main PBX/SIP server)
* **Network:** Local Area Network (LAN) for communication between the Android app and the Raspberry Pi.

---

## 🚀 Installation & Local Setup

To run this application, you need to set up both the backend server and the Android client.

### 1. Server Prerequisites (Raspberry Pi/Asterisk)

* Ensure you have an **Asterisk server** fully configured on your Raspberry Pi.
* The Asterisk configuration must define **SIP peers/accounts** that the Android app will use for registration (e.g., in `pjsip.conf` or equivalent PJSIP configurations).
* The Raspberry Pi must be accessible on the **same local network** as the Android device/emulator.
