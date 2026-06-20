package com.example.sharescreen.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Radius values
val RadiusSm = 8.dp
val RadiusMd = 12.dp
val RadiusLg = 16.dp
val RadiusXl = 24.dp
val RadiusPill = 999.dp

// Material3 Shapes
val ShareScreenShapes = Shapes(
    extraSmall = RoundedCornerShape(RadiusSm),
    small = RoundedCornerShape(RadiusSm),
    medium = RoundedCornerShape(RadiusMd),
    large = RoundedCornerShape(RadiusLg),
    extraLarge = RoundedCornerShape(RadiusXl)
)
