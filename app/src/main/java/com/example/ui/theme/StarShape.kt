package com.example.ui.theme

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.CornerRounding
import kotlin.math.abs

class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    private val matrix = Matrix()
    private val androidPath = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        androidPath.rewind()
        
        polygon.cubics.forEachIndexed { index, cubic ->
            if (index == 0) {
                androidPath.moveTo(cubic.anchor0X, cubic.anchor0Y)
            }
            androidPath.cubicTo(
                cubic.control0X, cubic.control0Y,
                cubic.control1X, cubic.control1Y,
                cubic.anchor1X, cubic.anchor1Y
            )
        }
        androidPath.close()

        val bounds = RectF()
        androidPath.computeBounds(bounds, true)
        val scaleX = size.width / bounds.width()
        val scaleY = size.height / bounds.height()
        val scale = minOf(scaleX, scaleY)
        
        matrix.reset()
        matrix.postTranslate(-bounds.left, -bounds.top)
        matrix.postScale(scale, scale)
        
        androidPath.transform(matrix)
        return Outline.Generic(androidPath.asComposePath())
    }
}

val ExpressiveStarShape = PolygonShape(
    RoundedPolygon.star(
        numVerticesPerRadius = 8,
        innerRadius = 0.75f,
        rounding = CornerRounding(radius = 0.2f)
    )
)

val ExpressiveCloverShape = PolygonShape(
    RoundedPolygon.star(
        numVerticesPerRadius = 4,
        innerRadius = 0.6f,
        rounding = CornerRounding(radius = 0.4f)
    )
)

val ExpressiveCookieShape = PolygonShape(
    RoundedPolygon.star(
        numVerticesPerRadius = 10,
        innerRadius = 0.85f,
        rounding = CornerRounding(radius = 0.2f)
    )
)

val ExpressiveHexStarShape = PolygonShape(
    RoundedPolygon.star(
        numVerticesPerRadius = 6,
        innerRadius = 0.7f,
        rounding = CornerRounding(radius = 0.3f)
    )
)

val ExpressiveShapesList = listOf(
    ExpressiveStarShape,
    ExpressiveCloverShape,
    ExpressiveCookieShape,
    ExpressiveHexStarShape
)

fun getRandomExpressiveShape(seed: String): Shape {
    val index = abs(seed.hashCode()) % ExpressiveShapesList.size
    return ExpressiveShapesList[index]
}
