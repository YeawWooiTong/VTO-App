# VTO App UI System

## Handling System Insets

The VTO app uses a consistent approach to handle system insets (status bar, navigation bar) across all screens:

### For XML-based fragments:

1. All fragments should extend `BaseFragment` which handles insets automatically.
   ```kotlin
   class MyFragment : BaseFragment() {
       // Fragment implementation
   }
   ```

2. The `BaseFragment` applies proper padding to account for system insets.

### For Compose-based screens:

1. Use the `ComposeScreen` wrapper for all top-level Compose screens:
   ```kotlin
   ComposeView(requireContext()).apply {
       setContent {
           VTOAppTheme {
               ComposeScreen {
                   // Your screen content here
               }
           }
       }
   }
   ```

2. The `ComposeScreen` automatically applies proper padding for status bar and navigation bar.

3. If you need custom inset handling, you can access insets through `LocalInsets.current`:
   ```kotlin
   val insets = LocalInsets.current
   // Use insets.statusBarHeight, insets.navigationBarHeight
   ```

## Edge-to-Edge Design

The app uses an edge-to-edge design with:

1. Transparent status bar (`android:statusBarColor="@android:color/transparent"`)
2. Proper inset handling to ensure content doesn't overlap with system UI
3. Light/dark context-aware system bars through `android:windowLightStatusBar`

## Theme Implementation

The app has both light and dark themes with:

1. `themes.xml` for light mode
2. `values-night/themes.xml` for dark mode
3. Dynamic colors support on Android 12+ (API 31+)

## Adding New Screens

When adding new screens:

1. Make fragments extend `BaseFragment`
2. For Compose screens, wrap with `ComposeScreen`
3. For DialogFragments, manually add inset handling code

## Important Components:

- `BaseFragment`: Base class for proper inset handling in XML-based fragments
- `ComposeScreen`: Compose wrapper for proper inset handling
- `InsetsProvider`: Composition local provider for insets
- `LocalInsets`: CompositionLocal for accessing inset values 