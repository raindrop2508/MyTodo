# MyPotato - Pomodoro TODO App

**Language / 语言:** [中文](README.md) | **English**

> A local-first Android Pomodoro TODO app that combines Eisenhower Matrix priority management with the Pomodoro Technique.

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat\&logo=kotlin)
![Material3](https://img.shields.io/badge/Material%20Design-3-757575?style=flat\&logo=materialdesign)
![Room](https://img.shields.io/badge/Room-2.6.1-3DDC84?style=flat\&logo=android)
![minSdk](https://img.shields.io/badge/minSdk-29-3DDC84?style=flat\&logo=android)
![Version](https://img.shields.io/badge/version-1.0.0-blue?style=flat)
![License](https://img.shields.io/badge/license-GPL--2.1-blue?style=flat)

***

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Development Environment](#development-environment)
- [Build & Run](#build--run)
- [Changelog](#changelog)
- [License](#license)

***

## Overview

MyPotato is a **local-first** native Android TODO app that deeply integrates the Eisenhower Matrix with the Pomodoro Technique, helping users focus on what truly matters.

### Core Principles

- **Local-first**: All data is stored in a local Room database; no network required
- **Single source of truth**: The Repository pattern keeps data consistent across screens
- **Reactive UI**: Flow + StateFlow drive automatic UI updates
- **Material Design 3**: Follows the latest Material You design guidelines

***

## Features

### ✅ Shipped in V1.0

#### Task Management

- **Create tasks**: Quick create from both Today and Tasks entry points
- **Eisenhower Matrix**: Combine urgency and importance (Urgent & Important, Important, Urgent, Other)
- **Task types**: One-time tasks / long-running tasks (with Pomodoro support)
- **Edit tasks**: Title, description, notes, type, category, and priority are all editable
- **Complete tasks**: Mark complete / incomplete with instant sync
- **Delete tasks**: Confirmation dialog with cascading deletion of sub-steps

#### Step Management

- **Sub-step CRUD**: Add, edit, and delete steps under a task
- **Step ordering**: Drag-and-drop reordering (edit mode)
- **Completion state**: Independent completion flags and completion timestamps
- **Progress display**: Task detail shows completion progress (x/y)

#### Category Management

- **Dynamic categories**: Category chips are generated from the Repository with no hardcoding
- **Color labels**: Categories use color tags for quick recognition
- **Default categories**: Built-in seed categories are initialized on first launch

#### Pomodoro Timer

- **Countdown**: Default 25-minute focus session; customizable
- **State machine**: Full IDLE / RUNNING / PAUSED / COMPLETED lifecycle
- **Pause / Resume**: Pause duration is excluded from focus time
- **Manual stop**: End the current Pomodoro early
- **Short / long breaks**: Full work–break cycle
- **Session persistence**: Pomodoro sessions are stored in Room and survive restarts
- **Sound alerts**: Completion sound with on/off toggle

#### Data Persistence

- **Room database**: Four business tables (Task / TaskStep / Category / PomodoroSession)
- **Repository pattern**: Unified data access that hides underlying storage details
- **Reactive data flow**: Flow / StateFlow drive automatic UI refresh

#### Settings

- **Custom Pomodoro parameters**: Work duration, short break, long break, long-break interval
- **Sound toggle**: Pomodoro completion sound on/off
- **Version info**: Displays the current app version

### 🚧 Planned (Later Releases)

- Statistics screen (time distribution, completion rate, category analysis)
- Full dark mode support
- Multi-language support (Simplified Chinese / English)
- Data import/export (JSON / CSV)
- Task reminders and notifications
- Home screen widgets

***

## Architecture

### High-Level Architecture

Follows Google’s recommended **single Activity + multiple Fragments + MVVM** architecture:

```
┌─────────────────────────────────────────────┐
│            UI Layer (Fragment/Activity)     │
│  Today / Tasks / Detail / Edit / Settings /  │
│  Pomodoro                                   │
└──────────────────┬──────────────────────────┘
                   │ depends on
┌──────────────────▼──────────────────────────┐
│             ViewModel Layer                  │
│  Holds state + exposes StateFlow + logic     │
└──────────────────┬──────────────────────────┘
                   │ depends on
┌──────────────────▼──────────────────────────┐
│           Repository Layer (interfaces)      │
│  Unified data access; hides data sources     │
└──────────────────┬──────────────────────────┘
                   │ implemented by
┌──────────────────▼──────────────────────────┐
│         Room Data Layer                      │
│  Entity / DAO / Database / Mapper            │
└─────────────────────────────────────────────┘
```

### Tech Stack

| Area | Choice | Version |
| ---- | ------ | ------- |
| Language | Kotlin | 2.0.21 |
| UI | ViewBinding + XML + Material3 | 1.13.0 |
| Navigation | Jetpack Navigation Component | 2.9.5 |
| Architecture | MVVM + Repository | - |
| Persistence | Room | 2.6.1 |
| Async | Kotlin Coroutines + Flow/StateFlow | - |
| Lifecycle | AndroidX Lifecycle | 2.8.7 |
| Build | Gradle + Version Catalog | AGP 8.12.3 |
| Min SDK | API 29 (Android 10) | - |
| Target/Compile SDK | API 36 | - |

### Core Design Patterns

1. **Repository pattern**: Unified data access; seamless Fake → Room switch
2. **MVVM**: Stateless Views driven by ViewModels
3. **State machine**: Clear, controllable Pomodoro session transitions
4. **Singleton**: Room database with double-checked locking lazy init
5. **Adapter pattern**: RecyclerView Adapter + ViewBinding

***

## Project Structure

```
MyPotato/
├── app/                                    # Main app module
│   └── src/main/
│       ├── java/com/gordon/mypotato/
│       │   ├── MainActivity.kt             # Main Activity (nav host)
│       │   ├── MyPotatoApp.kt              # Application class
│       │   ├── data/                       # Data layer
│       │   │   ├── AppDatabase.kt          # Room database
│       │   │   ├── dao/                    # DAO interfaces
│       │   │   ├── entity/                 # Room entities
│       │   │   ├── mapper/                 # Entity ↔ Domain mappers
│       │   │   ├── repository/             # Repository implementations
│       │   │   └── initializer/            # Database initialization
│       │   ├── domain/                     # Domain models (pure Kotlin)
│       │   ├── ui/                         # UI layer
│       │   │   ├── today/                  # Today screen
│       │   │   ├── tasks/                  # Task list / detail / edit
│       │   │   ├── pomodoro/               # Pomodoro screen
│       │   │   ├── settings/               # Settings screen
│       │   │   ├── statistics/             # Statistics (entry not exposed yet)
│       │   │   └── common/                 # Shared UI components
│       │   └── viewmodel/                  # ViewModel layer
│       └── res/                            # Resources
│           ├── layout/                     # Layouts
│           ├── menu/                       # Menus
│           ├── navigation/                 # Navigation graph
│           ├── drawable/                   # Vectors and selectors
│           ├── values/                     # Strings, colors, dims, themes
│           └── values-night/               # Dark theme (to be filled in)
├── docs/                                   # Project docs
│   ├── plan/                               # Stage plans
│   └── stageB/ / stageC/ / stageD/         # Stage summaries and designs
├── gradle/
│   └── libs.versions.toml                  # Version Catalog
├── build.gradle                            # Root build script
├── settings.gradle                         # Project settings
├── README.md                               # Chinese README
└── README.en.md                            # This file
```

***

## Development Environment

### Required Tools

- **Android Studio**: Latest stable release (Hedgehog or newer recommended)
- **JDK**: 11 (required by AGP 8.x)
- **Android SDK**:
  - compileSdk = 36
  - minSdk = 29
  - targetSdk = 36

### Recommended Setup

- Physical device or emulator running Android 10 (API 29) or higher
- At least 8GB RAM (16GB recommended)
- Enable ADB debugging for physical devices

***

## Build & Run

### 1. Clone the Repository

```bash
git clone <repository-url>
cd MyPotato
```

### 2. Open the Project

1. Launch Android Studio
2. Choose **Open an Existing Project**
3. Select the `MyPotato` project root
4. Wait for Gradle sync to finish (first open may take a few minutes to download dependencies)

### 3. Configure a Device

- **Emulator**: Create an Android 10+ AVD via AVD Manager
- **Physical device**: Connect a phone and enable Developer Options + USB debugging

### 4. Run the App

1. Select the target device in the toolbar
2. Click **Run** (▶️) or press `Shift + F10`
3. Wait for the app to install and launch

***

## Changelog

### v1.0.0 (Current)

**Release date**: 2026-07

**Core features**:

- Full task CRUD (create / edit / complete / delete)
- Eisenhower Matrix priority categories (urgency / importance)
- One-time and long-running task types
- Sub-step management (add / edit / delete / complete)
- Full Pomodoro features (countdown / pause / resume / manual stop)
- Pomodoro session persistence (Room)
- Category management (dynamic chips)
- Settings (custom Pomodoro parameters, sound toggle)
- Room persistence so data survives app restarts

**Architecture highlights**:

- Single Activity + multiple Fragments + MVVM
- Repository pattern + Room implementation
- Reactive Flow/StateFlow data pipeline
- Material Design 3 theme system

***

## License

This project is licensed under the **GNU General Public License v2.1 (GPL-2.1)**.

See the [LICENSE](LICENSE) file in the project root for details.

***

> Made with Gordon Mark using Kotlin & Material Design 3
