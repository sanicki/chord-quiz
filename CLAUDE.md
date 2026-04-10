# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application for learning musical chords and notes. It supports multiple instruments including guitar, bass, ukulele, and banjo. The app features chord recognition using audio input and provides interactive chord diagrams along with practice quizzes.

## Architecture

The application follows a clean architecture pattern with clear separation of concerns:

1. **Data Layer**: Handles data persistence and retrieval from Room database
2. **Domain Layer**: Contains business logic and use cases
3. **Data Repository Layer**: Abstracts data access with concrete implementations
4. **Presentation Layer**: Composed of UI screens and components using Jetpack Compose

## Key Components

### Audio Processing
The app uses a sophisticated chord recognition system that:
- Analyzes audio input using frequency detection
- Implements pitch detection and chroma vector matching
- Uses a windowed consistency check to reduce false positives
- Provides configurable difficulty levels for recognition

### Instruments and Chords
- Supports multiple instruments (guitar, bass, ukulele, banjo)
- Each instrument has defined open string notes and tuning
- Chord definitions include multiple fingerings (voicings)
- Chord types define note intervals and naming

### UI Components
- Interactive chord diagrams with visual fingering
- Music staff and note display
- Quiz screens for playing and drawing chords
- Tuner functionality
- Practice setup and result screens

## Build and Development

### Build System
- Uses Android Gradle Plugin (AGP)
- Kotlin Multiplatform for shared logic
- Jetpack Compose for UI
- Hilt for dependency injection
- Room for local database

### GitHub Actions Builds
- This project is built exclusively via GitHub Actions workflows
- Build and test processes are configured in `.github/workflows/build.yml`
- All builds and tests execute within the GitHub Actions environment

### Testing Strategy
- Unit tests for domain logic and use cases
- Instrumentation tests for UI components
- Database tests for Room persistence layer
- Integration tests for audio processing components

## Key Files and Folders

### Core Application Files
- `app/src/main/java/com/chordquiz/app/ChordQuizApplication.kt` - Main application class
- `app/src/main/java/com/chordquiz/app/ui/MainActivity.kt` - Main activity with navigation
- `app/src/main/java/com/chordquiz/app/ui/navigation/NavGraph.kt` - Navigation setup

### Audio Processing
- `app/src/main/java/com/chordquiz/app/audio/ChordRecognizer.kt` - Main chord recognition logic
- `app/src/main/java/com/chordquiz/app/audio/PitchDetector.kt` - Pitch detection
- `app/src/main/java/com/chordquiz/app/audio/FftAnalyzer.kt` - FFT analysis

### Data Layer
- `app/src/main/java/com/chordquiz/app/data/db/` - Database entities and DAOs
- `app/src/main/java/com/chordquiz/app/data/repository/` - Repository implementations

### Domain Logic
- `app/src/main/java/com/chordquiz/app/domain/` - Use cases and business logic
- `app/src/main/java/com/chordquiz/app/domain/model/Difficulty.kt` - Difficulty levels

### UI Components
- `app/src/main/java/com/chordquiz/app/ui/components/chord/` - Chord diagram components
- `app/src/main/java/com/chordquiz/app/ui/screen/` - Screen implementations

## Key Features

1. **Chord Recognition**: Real-time audio chord recognition using sophisticated algorithms
2. **Multiple Instruments**: Support for guitar, bass, ukulele, and banjo
3. **Interactive Chord Diagrams**: Visual fingering diagrams with interactive elements
4. **Quiz Modes**: Both draw and play quiz modes for learning
5. **Tuner**: Built-in tuner functionality
6. **Practice Management**: Settings for quiz difficulty, question count, etc.
7. **Results Tracking**: Performance tracking and results display

## Testing Strategy

The application uses a combination of unit tests and instrumentation tests:
- Unit tests for domain logic and use cases
- Instrumentation tests for UI components
- Database tests for Room persistence layer
- Integration tests for audio processing components

## Dependencies

Key libraries include:
- AndroidX Compose for UI
- Hilt for dependency injection
- Room for local database
- Kotlin Coroutines for async operations
- JUnit for testing

## Development Guidelines

1. Follow the existing code style and patterns
2. Maintain clear separation of concerns between layers
3. Ensure audio processing performance is optimized
4. Test with multiple instruments and chord types
5. Validate audio recognition accuracy across different conditions
6. All changes must be compatible with GitHub Actions CI environment
7. Create feature branches for all changes (never commit directly to main)
8. Submit all changes via Pull Requests for review