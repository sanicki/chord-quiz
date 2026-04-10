# Unified Chip Component Implementation Plan

## Overview
This plan outlines the implementation of a single unified FilterChip component to replace all existing chip implementations throughout the chord-quiz app, ensuring uniform height, spacing, and visual consistency.

## Problem Statement
Issue #132 identifies that chips throughout the app lack uniformity in height and spacing, with different implementations using inconsistent dimensions, padding, and visual styling.

## Solution Approach
Replace all existing chip implementations with a single unified FilterChip component that leverages Material3's built-in FilterChip with consistent styling.

## Implementation Steps

### 1. Create Unified Chip Component
Create a new composable `UnifiedFilterChip` that standardizes all chip styling:

```kotlin
@Composable
fun UnifiedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    // Standardized Material3 FilterChip with consistent dimensions and styling
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = modifier
            .height(40.dp) // Uniform height
            .padding(horizontal = 12.dp, vertical = 8.dp) // Uniform padding
    )
}
```

### 2. Update StrumPracticeScreen.kt
Replace custom `StrumChip` implementation with `UnifiedFilterChip`:

- Replace `StrumChip` composable (lines 327-357) with `UnifiedFilterChip`
- Update references in lines 156-169 to use the new unified component
- Ensure all existing functionality (selection, click handling, etc.) is preserved

### 3. Update ChordLibraryScreen.kt
Replace custom `LibraryFilterChip` implementation with `UnifiedFilterChip`:

- Replace `LibraryFilterChip` composable (lines 369-414) with `UnifiedFilterChip`
- Update references in lines 172-196 to use the new unified component
- Ensure all existing functionality is preserved

### 4. Update SettingsScreen.kt
Ensure consistent usage of Material3 FilterChip with standardized styling:

- Review current FilterChip usage (lines 19, 204) and ensure they're using consistent styling
- Update any styling that may deviate from the unified standard

### 5. Update InstrumentSelectionScreen.kt
Ensure consistent usage of Material3 FilterChip:

- Review FilterChip usage (lines 98, 103, 109, 116) and ensure consistent styling
- Update any styling that may deviate from the unified standard

### 6. Update FlowRow Spacing
Standardize horizontal spacing between chips in all FlowRow components:

```kotlin
FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp), // Uniform spacing
    verticalArrangement = Arrangement.spacedBy(4.dp),    // Uniform vertical spacing
    modifier = Modifier.fillMaxWidth()
) {
    // Chips here
}
```

## Implementation Details

### Height Standardization
- All chips will use a consistent height of 40.dp
- Vertical padding will be 8.dp (top and bottom)
- Horizontal padding will be 12.dp (left and right)

### Spacing Standardization
- Horizontal spacing between chips: 8.dp
- Vertical spacing between chip rows: 4.dp
- Padding around chip containers: 8.dp (for visual consistency)

### Color Consistency
- Selected state: Primary container with primary text
- Unselected state: Surface variant with on surface variant text
- Border: 1.dp stroke width
- Ripple effect: Standard Material3 ripple

### Backward Compatibility
- All existing API calls and functionality will be preserved
- Component will be fully backward compatible with existing code

## Testing Plan
1. Verify all chip components maintain their existing functionality
2. Ensure all chips now have consistent height (40.dp)
3. Verify consistent padding (12.dp horizontal, 8.dp vertical)
4. Confirm uniform spacing between chips (8.dp horizontal, 4.dp vertical)
5. Test in all screens: Settings, Strum Practice, Chord Library, and Instrument Selection
6. Verify visual consistency across different chip labels and content

## Dependencies
- AndroidX Compose Material3
- Material3 FilterChip component

## Risks and Mitigations
- Risk: Breaking existing functionality
  - Mitigation: Preserve all existing APIs and functionality
- Risk: Visual changes may impact UX
  - Mitigation: Use conservative standardization that maintains usability