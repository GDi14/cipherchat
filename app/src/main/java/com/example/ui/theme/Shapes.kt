package com.example.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
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

val ExpressiveCardShape = RoundedCornerShape(24.dp)
val ExpressiveButtonShape = RoundedCornerShape(20.dp)
val ExpressiveInputShape = RoundedCornerShape(22.dp)
val ExpressiveSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
