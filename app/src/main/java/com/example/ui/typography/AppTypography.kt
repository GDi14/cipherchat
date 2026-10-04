package com.example.ui.typography

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.example.R

@OptIn(ExperimentalTextApi::class)
enum class ChatFontOption(
    val id: String,
    val displayName: String,
    val styleDescription: String,
    val resId: Int
) {
    ROBOTO_FLEX(
        id = "roboto_flex",
        displayName = "Roboto Flex",
        styleDescription = "Modern Flexible Grotesque",
        resId = R.font.roboto_flex
    ),
    FRAUNCES(
        id = "fraunces",
        displayName = "Fraunces",
        styleDescription = "Expressive Editorial Serif",
        resId = R.font.fraunces
    ),
    NEWSREADER(
        id = "newsreader",
        displayName = "Newsreader",
        styleDescription = "Literary Italic Serif",
        resId = R.font.newsreader
    ),
    SYNE(
        id = "syne",
        displayName = "Syne",
        styleDescription = "Bold Geometric Display",
        resId = R.font.syne
    );

    fun getFontFamily(
        weight: Int = 400,
        width: Float = 100f,
        slant: Float = 0f
    ): FontFamily {
        return try {
            FontFamily(
                Font(
                    resId = resId,
                    weight = FontWeight(weight.coerceIn(100, 900)),
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(weight.coerceIn(100, 900)),
                        FontVariation.width(width.coerceIn(25f, 151f)),
                        FontVariation.slant(slant.coerceIn(-10f, 0f))
                    )
                )
            )
        } catch (e: Exception) {
            FontFamily(Font(resId))
        }
    }

    fun getEditorialDisplayFont(): FontFamily {
        return try {
            FontFamily(
                Font(
                    resId = resId,
                    weight = FontWeight(900),
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(900),
                        FontVariation.width(135f),
                        FontVariation.slant(-10f)
                    )
                )
            )
        } catch (e: Exception) {
            FontFamily(Font(resId, FontWeight.Bold))
        }
    }

    companion object {
        fun fromId(id: String?): ChatFontOption {
            return entries.firstOrNull { it.id == id } ?: ROBOTO_FLEX
        }
    }
}
