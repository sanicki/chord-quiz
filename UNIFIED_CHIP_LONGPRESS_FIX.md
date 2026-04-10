# Unified FilterChip with Long Press Support

## Problem
The original implementation of the UnifiedFilterChip component was created to ensure uniform height and spacing across all chips in the application. However, this implementation removed the long press functionality that was present in the original custom chip implementations (StrumChip and LibraryFilterChip).

## Solution
The UnifiedFilterChip component was updated to maintain the Material3 FilterChip's visual consistency while adding support for long press gestures through the use of `combinedClickable`.

## Changes Made

### 1. Updated UnifiedFilterChip Component
**File**: `app/src/main/java/com/chordquiz/app/ui/components/chip/UnifiedFilterChip.kt`

The component now accepts an optional `onLongClick` parameter:
```kotlin
@Composable
fun UnifiedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = modifier
            .height(40.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
            )
    )
}
```

### 2. Updated StrumPracticeScreen Usage
**File**: `app/src/main/java/com/chordquiz/app/ui/screen/strumpractice/StrumPracticeScreen.kt`

The saved pattern chips now include the `onLongClick` parameter:
```kotlin
UnifiedFilterChip(
    selected = false,
    onClick = { viewModel.loadPattern(pattern) },
    onLongClick = { viewModel.requestDeletePattern(pattern) },
    label = { Text(pattern.toName()) }
)
```

## Benefits

1. **Visual Consistency**: All chips maintain the same height (40.dp) and padding (12.dp horizontal, 8.dp vertical)
2. **Functional Consistency**: Long press functionality is preserved for chips that require it
3. **Backward Compatibility**: Existing code that doesn't use `onLongClick` continues to work unchanged
4. **Material3 Compliance**: Uses standard Material3 FilterChip component for consistent styling

## Testing

The implementation was verified to:
- Maintain uniform height and spacing (40.dp height, 12.dp horizontal padding, 8.dp vertical padding)
- Support both regular click and long press events
- Preserve all existing functionality