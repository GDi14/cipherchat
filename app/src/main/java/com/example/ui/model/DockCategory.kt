package com.example.ui.model

import androidx.compose.ui.graphics.vector.ImageVector

/** One named stop in the fluid dock. The resting marker is its first letter. */
data class DockCategory(
    val id: String,
    val shortLabel: String,
    val fullTitle: String,
    val icon: ImageVector? = null
)
