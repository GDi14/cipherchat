package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// ==============================================================================
// Material 3 Expressive Typography with Google Sans Flex
// Utilizes Variable Font Axes: Weight (wght), Width (wdth), Slant (slnt), Grade (GRAD)
// ==============================================================================

// Display: Expressive, wider width (110f), high grade (+50), bold weight (800)
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexDisplay = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800),
            FontVariation.width(110f),
            FontVariation.slant(0f),
            FontVariation.grade(50)
        )
    )
)

// Headline: Impactful, normal width (100f), medium grade (+25), semi-bold (700)
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexHeadline = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(100f),
            FontVariation.slant(0f),
            FontVariation.grade(25)
        )
    )
)

// Title: Clean, crisp, medium/bold weight (600), grade (+10)
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexTitle = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.width(100f),
            FontVariation.slant(0f),
            FontVariation.grade(10)
        )
    )
)

// Body: Highly legible, standard width (100f), regular weight (400), neutral grade (0)
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexBody = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f),
            FontVariation.slant(0f),
            FontVariation.grade(0)
        )
    )
)

// Label: Compact, slightly condensed width (95f), bold weight (600), tight letter spacing
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexLabel = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.width(95f),
            FontVariation.slant(0f),
            FontVariation.grade(0)
        )
    )
)

// Slanted/Emphasis Variant for security badges, verification tags, or active states
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexSlanted = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(650),
            FontVariation.width(105f),
            FontVariation.slant(-6f),
            FontVariation.grade(20)
        )
    )
)

val ExpressiveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansFlexDisplay,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = GoogleSansFlexDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = GoogleSansFlexDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSansFlexHeadline,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansFlexHeadline,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = GoogleSansFlexHeadline,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansFlexTitle,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansFlexTitle,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSansFlexTitle,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansFlexLabel,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansFlexLabel,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSansFlexLabel,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

object EditorialTypography {
    val MonospacedNumeral = TextStyle.Default
}

annotation class ExperimentalMaterial3ExpressiveApi

