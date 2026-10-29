package com.example.myapplication.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CarWashIcons {
    val Person = Icons.Default.Person
    val LocationPin = Icons.Outlined.LocationOn
    val ElectricBolt = Icons.Default.ElectricBolt
    val Search = Icons.Default.Search
    val FilterTune = Icons.Default.Tune
    val NavigationArrow = Icons.Outlined.NearMe
    val ConfirmationNumber = Icons.Outlined.ConfirmationNumber
    val AccountCircle = Icons.Outlined.AccountCircle

    /**
     * Custom Car Wash Home icon matching the design (shelter with top sprays and car shape underneath)
     */
    val CarWashHome: ImageVector
        get() {
            if (_carWashHome != null) return _carWashHome!!
            _carWashHome = ImageVector.Builder(
                name = "CarWashHome",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                // Top spray dots / lines
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round
                ) {
                    moveTo(7f, 2f); lineTo(7f, 3.5f)
                    moveTo(12f, 1.5f); lineTo(12f, 3f)
                    moveTo(17f, 2f); lineTo(17f, 3.5f)
                }
                // Roof bar structure
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(3f, 5.5f)
                    lineTo(21f, 5.5f)
                    moveTo(5f, 5.5f)
                    lineTo(5f, 7.5f)
                    moveTo(19f, 5.5f)
                    lineTo(19f, 7.5f)
                }
                // Car roof and body outline
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(6f, 13f)
                    lineTo(8f, 9.5f)
                    lineTo(16f, 9.5f)
                    lineTo(18f, 13f)
                    lineTo(20f, 13f)
                    curveTo(20.5f, 13f, 21f, 13.5f, 21f, 14f)
                    lineTo(21f, 17f)
                    curveTo(21f, 17.5f, 20.5f, 18f, 20f, 18f)
                    lineTo(4f, 18f)
                    curveTo(3.5f, 18f, 3f, 17.5f, 3f, 17f)
                    lineTo(3f, 14f)
                    curveTo(3f, 13.5f, 3.5f, 13f, 4f, 13f)
                    close()
                }
                // Car wheels
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round
                ) {
                    moveTo(6.5f, 18f); lineTo(6.5f, 19.5f)
                    moveTo(17.5f, 18f); lineTo(17.5f, 19.5f)
                }
            }.build()
            return _carWashHome!!
        }

    private var _carWashHome: ImageVector? = null

    /**
     * Custom Car Icon for "High Pressure Jet" badge
     */
    val HighPressureCar: ImageVector
        get() {
            if (_highPressureCar != null) return _highPressureCar!!
            _highPressureCar = ImageVector.Builder(
                name = "HighPressureCar",
                defaultWidth = 18.dp,
                defaultHeight = 18.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = SolidColor(Color.White),
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(18.92f, 6.01f)
                    curveTo(18.72f, 5.42f, 18.16f, 5f, 17.5f, 5f)
                    horizontalLineTo(6.5f)
                    curveTo(5.84f, 5f, 5.28f, 5.42f, 5.08f, 6.01f)
                    lineTo(3f, 12f)
                    verticalLineTo(20f)
                    curveTo(3f, 20.55f, 3.45f, 21f, 4f, 21f)
                    horizontalLineTo(5f)
                    curveTo(5.55f, 21f, 6f, 20.55f, 6f, 20f)
                    verticalLineTo(19f)
                    horizontalLineTo(18f)
                    verticalLineTo(20f)
                    curveTo(18f, 20.55f, 18.45f, 21f, 19f, 21f)
                    horizontalLineTo(20f)
                    curveTo(20.55f, 21f, 21f, 20.55f, 21f, 20f)
                    verticalLineTo(12f)
                    lineTo(18.92f, 6.01f)
                    close()
                    moveTo(6.85f, 7f)
                    horizontalLineTo(17.14f)
                    lineTo(18.22f, 10.12f)
                    horizontalLineTo(5.78f)
                    lineTo(6.85f, 7f)
                    close()
                    moveTo(19f, 17f)
                    horizontalLineTo(5f)
                    verticalLineTo(12f)
                    horizontalLineTo(19f)
                    verticalLineTo(17f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(7.5f, 14.5f)
                    curveTo(8.33f, 14.5f, 9f, 13.83f, 9f, 13f)
                    curveTo(9f, 12.17f, 8.33f, 11.5f, 7.5f, 11.5f)
                    curveTo(6.67f, 11.5f, 6f, 12.17f, 6f, 13f)
                    curveTo(6f, 13.83f, 6.67f, 14.5f, 7.5f, 14.5f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(16.5f, 14.5f)
                    curveTo(17.33f, 14.5f, 18f, 13.83f, 18f, 13f)
                    curveTo(18f, 12.17f, 17.33f, 11.5f, 16.5f, 11.5f)
                    curveTo(15.67f, 11.5f, 15f, 12.17f, 15f, 13f)
                    curveTo(15f, 13.83f, 15.67f, 14.5f, 16.5f, 14.5f)
                    close()
                }
            }.build()
            return _highPressureCar!!
        }

    private var _highPressureCar: ImageVector? = null

    /**
     * Custom Soap / Foam Spray icon for "Super Foam" badge
     */
    val SuperFoamIcon: ImageVector
        get() {
            if (_superFoamIcon != null) return _superFoamIcon!!
            _superFoamIcon = ImageVector.Builder(
                name = "SuperFoamIcon",
                defaultWidth = 18.dp,
                defaultHeight = 18.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12f, 3f)
                    curveTo(8f, 3f, 5f, 6f, 5f, 10f)
                    curveTo(3.5f, 11.5f, 3.5f, 14.5f, 5f, 16f)
                    curveTo(6.5f, 17.5f, 9f, 18f, 12f, 18f)
                    curveTo(15f, 18f, 17.5f, 17.5f, 19f, 16f)
                    curveTo(20.5f, 14.5f, 20.5f, 11.5f, 19f, 10f)
                    curveTo(19f, 6f, 16f, 3f, 12f, 3f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round
                ) {
                    moveTo(9f, 12f); lineTo(15f, 12f)
                }
            }.build()
            return _superFoamIcon!!
        }

    private var _superFoamIcon: ImageVector? = null
}
