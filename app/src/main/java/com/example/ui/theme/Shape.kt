package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppDimens.RadiusSmall),
    small = RoundedCornerShape(AppDimens.RadiusSmall),
    medium = RoundedCornerShape(AppDimens.RadiusMedium),
    large = RoundedCornerShape(AppDimens.RadiusCard),
    extraLarge = RoundedCornerShape(AppDimens.RadiusDialog)
)
