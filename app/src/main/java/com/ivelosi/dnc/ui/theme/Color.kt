package com.ivelosi.dnc.ui.theme

import androidx.compose.ui.graphics.Color

// Primary (brand red – brighter)
val primaryLight = Color(0xFFFF1717)          // Vivid Red (status bar in light mode)
val primaryDark = Color(0xFFFF1717)           // Bright Coral Red (sent bubble in dark mode)

// Primary containers
val primaryContainerLight = Color(0xFFD50000) // Deep Pure Red (buttons / emphasis)
val primaryContainerDark = Color(0xFFFF3D00)  // Fiery Orange-Red (text/buttons in dark mode)

val onPrimaryContainerLight = Color(0xFFFFFFFF) // White text/icons on red
val onPrimaryContainerDark = Color(0xFF000000)  // Black text/icons on lighter reds

// Secondary (grays for balance)
val secondaryLight = Color(0xFF616161)        // Medium Gray (light mode accents)
val secondaryDark = Color(0xFF9E9E9E)         // Light Gray (dark mode accents)
val onSecondaryLight = Color(0xFFFFFFFF)      // White text on gray
val onSecondaryDark = Color(0xFF000000)       // Black text on gray

// Backgrounds
val backgroundLight = Color(0xFFFFFFFF)       // White (light mode background)
val backgroundDark = Color(0xFF000000)        // Pure Black (dark mode background)

val surfaceLight = Color(0xFFF5F5F5)          // Light gray surface (headers, cards)
val surfaceDark = Color(0xFF1A1A1A)           // Near-black surface (headers, cards)

val secondarySurfaceLight = Color(0xFFEEEEEE) // Light gray chat bubble (received)
val secondarySurfaceDark = Color(0xFF2C2C2C)  // Dark gray chat bubble (received)

// Surface Text Colors
val onSurface20Light = Color(0xFF212121)      // Dark gray text (light mode)
val onSurface40Light = Color(0xFF616161)      // Medium gray text
val onSurface60Light = Color(0xFF9E9E9E)      // Disabled / placeholder text

val onSurface20Dark = Color(0xFFFFFFFF)       // White text (primary text on dark)
val onSurface40Dark = Color(0xFFBDBDBD)       // Gray for hints
val onSurface60Dark = Color(0xFF757575)       // Muted gray for disabled/secondary text
