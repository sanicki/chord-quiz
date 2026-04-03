# ChordQuiz - Agent Guidelines

## Project Overview
ChordQuiz is an Android Kotlin application built with Jetpack Compose for learning chord diagrams and recognizing chords via audio.

## Build & Test Commands

### Gradle Tasks
```bash
./gradlew assembleDebug           # Build debug APK
./gradlew assembleRelease         # Build release APK
./gradlew lint                    # Run lint checks
./gradlew test                    # Run all unit tests
./gradlew connectedAndroidTest    # Run instrumentation tests
```

### Running a Single Test
```bash
# Run specific test class
./gradlew test --tests "com.chordquiz.app.domain.EvaluateDrawAnswerUseCaseTest"

# Run test by name pattern
./gradlew test --tests "*.EvaluateDrawAnswerUseCaseTest.correct Am fingering"

# Run all tests in a package
./gradlew test --tests "com.chordquiz.app.*.*Test"

# Run with debug logging
./gradlew test --tests "com.chordquiz.app.domain.EvaluateDrawAnswerUseCaseTest" --info
```

### Test Framework
- **Unit tests**: JUnit 4 with Truth assertions
- **Instrumentation tests**: AndroidX Test with Espresso and Compose Test
- **Mocking**: Hilt for dependency injection testing
- **Coroutines testing**: `kotlinx-coroutines-test`

## Code Style Guidelines

### Imports
- Group imports by Kotlin standard library → androidx → kotlin → other dependencies
- Use alphabetical ordering within each group
- No wildcard imports (except `org.junit.*`)
- Example:
  ```kotlin
  import androidx.compose.foundation.layout.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.*
  import com.chordquiz.app.data.model.*
  import kotlinx.coroutines.flow.*
  import org.junit.*
  ```

### Naming Conventions
- **Classes/Interfaces**: PascalCase (`PracticeSetupViewModel`, `ChordDefinition`)
- **Functions/Methods**: camelCase (`setMode`, `incrementQuestionCount`)
- **Properties**: camelCase for fields, snake_case avoided (`uiState`, not `ui_state`)
- **Constants**: UPPER_SNAKE_CASE (`val DEFAULT_QUESTION_COUNT = 10`)
- **Package names**: lowercase with dots (`com.chordquiz.app.ui.screen.setup`)

### Kotlin Style
- Use data classes for model/data transfer objects (`data class ChordDefinition(...)`)
- Prefer immutability with `val` over `var`
- Use scope functions judiciously (`let`, `run`, `apply`, `also`)
- Extension functions for utility operations in relevant files
- Coroutines for async operations with `StateFlow`/`LiveData` for UI state
- Dependency injection via Hilt (`@Inject` constructors, `@HiltAndroidApp`)

### Compose Patterns
- Split UI into composables: screen-level → feature components → shared components
- ViewModel for state management with `MutableStateFlow` → `StateFlow`
- Use `Modifier` parameters for composables accepting modifiers as last param
- `@Preview` annotations for all composables
- Separate theme files for colors, typography, components

### Error Handling
- Domain errors via exception classes or Result types
- Use sealed interfaces for error states when appropriate
- Log errors with Timber (or Android Logger)
- User-facing errors displayed via SnackBars or dialogs
- Network/error boundaries in ViewModel layer

### Type Safety
- Leverage sealed classes for types like `ChordType`, `QuizMode`, `Instrument`
- Type-safe builders for complex data structures
- `when` expressions exhaustiveness checking over `if-else`
- Generic types with bounded constraints where needed

### File Organization
```
app/src/main/java/com/chordquiz/app/
  ├── ChordQuizApplication.kt
  ├── audio/              # Audio processing (FFT, pitch detection)
  ├── data/               # Data layer
  │   ├── db/             # Room database entities, DAOs
  │   ├── model/          # Domain data models
  │   ├── preferences/    # DataStore preferences
  │   ├── repository/     # Repository implementations
  │   └── seed/           # Chord library seed data
  ├── di/                 # Hilt dependency injection modules
  ├── domain/             # Use cases (business logic)
  └── ui/                 # UI layer
      ├── components/     # Reusable composables
      ├── navigation/     # Navigation graph, routes
      └── screen/         # Screen composables & ViewModels
          ├── {screenName}/
          │   ├── {ScreenName}Screen.kt
          │   └── {ScreenName}ViewModel.kt
```

### Testing Guidelines
- Unit tests for ViewModels, UseCases, Repository implementations
- Test UI state changes and business logic separately
- Use `runTest` coroutines test scope
- Mock external dependencies; use real implementations where simple
- Instrumentation tests for navigation and complex UI interactions

### Git Commit Messages
- Conventional commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`
- Scope in parentheses: `feat(ui): add practice setup screen`
- Imperative mood, present tense

### Hilt DI Patterns
- `@HiltAndroidApp` on Application
- `@HiltViewModel` on ViewModels
- `@Module @InstallIn(ViewModelScope::class)` for ViewModels
- `@Module @InstallIn(ApplicationScope::class)` for app-wide deps
- Separate modules for Database, Repository, UseCase dependencies
