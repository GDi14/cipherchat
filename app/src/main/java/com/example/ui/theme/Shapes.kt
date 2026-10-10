package com.example.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Material 3 Expressive specialized bubble and pill shapes
val PillShape = RoundedCornerShape(percent = 50)

val ExpressiveOutgoingBubbleShape = RoundedCornerShape(
    topStart = CornerSize(22.dp),
    topEnd = CornerSize(22.dp),
    bottomStart = CornerSize(22.dp),
    bottomEnd = CornerSize(6.dp)
)

val ExpressiveIncomingBubbleShape = RoundedCornerShape(
    topStart = CornerSize(22.dp),
    topEnd = CornerSize(22.dp),
    bottomStart = CornerSize(6.dp),
    bottomEnd = CornerSize(22.dp)
)

val ExpressiveCardShape = RoundedCornerShape(28.dp)
val ExpressiveButtonShape = RoundedCornerShape(24.dp)
val ExpressiveInputShape = RoundedCornerShape(20.dp)
val ExpressiveSheetShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
val ExpressiveTopSurfaceShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val ExpressiveBottomSurfaceShape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
