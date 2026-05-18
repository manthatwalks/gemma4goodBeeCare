package com.beecareanywhere.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beecareanywhere.R
import com.beecareanywhere.ui.theme.CoralPastel
import com.beecareanywhere.ui.theme.InkDark
import com.beecareanywhere.ui.theme.SagePastel
import com.beecareanywhere.ui.theme.SurfaceWhite

/**
 * Watercolor bee brand mark — right-facing by default.
 * Uses mix-blend-mode:multiply equivalent (just multiply ContentScale on cream bg).
 */
@Composable
fun BeeImage(
    modifier: Modifier = Modifier,
    facingLeft: Boolean = false,
) {
    val res = if (facingLeft) R.drawable.bee_left else R.drawable.bee_right
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        androidx.compose.foundation.Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize(),
        )
    }
}

/**
 * Maasai shield mascot drawn on Canvas. Matches the SVG in bee.jsx exactly.
 * viewBox 0 0 100 140, rendered at [width] × [width]*1.35.
 */
@Composable
fun MaasaiShield(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    inkColor: Color = InkDark,
    redColor: Color = CoralPastel,
    greenColor: Color = SagePastel,
) {
    Canvas(modifier = modifier
        .then(
            with(androidx.compose.ui.platform.LocalDensity.current) {
                Modifier
                    .then(Modifier) // no-op placeholder, size set by caller
            }
        )
    ) {
        val sw = (this.size.width * 0.04f).coerceAtLeast(2f)
        fun x(v: Float) = v * this.size.width / 100f
        fun y(v: Float) = v * this.size.height / 140f

        // Crossed spears
        drawLine(inkColor, Offset(x(22f), y(14f)), Offset(x(78f), y(132f)), sw, StrokeCap.Round)
        drawLine(inkColor, Offset(x(78f), y(14f)), Offset(x(22f), y(132f)), sw, StrokeCap.Round)

        // Left spear tip  "M 22 14 q -4 -10 0 -12 q 4 2 0 12 Z"
        val leftTip = Path().apply {
            moveTo(x(22f), y(14f))
            quadraticTo(x(18f), y(4f), x(22f), y(2f))
            quadraticTo(x(26f), y(4f), x(22f), y(14f))
            close()
        }
        drawPath(leftTip, Color.White)
        drawPath(leftTip, inkColor, style = Stroke(sw))

        // Right spear tip "M 78 14 q 4 -10 0 -12 q -4 2 0 12 Z"
        val rightTip = Path().apply {
            moveTo(x(78f), y(14f))
            quadraticTo(x(82f), y(4f), x(78f), y(2f))
            quadraticTo(x(74f), y(4f), x(78f), y(14f))
            close()
        }
        drawPath(rightTip, Color.White)
        drawPath(rightTip, inkColor, style = Stroke(sw))

        // Shield silhouette: "M 50 14 Q 92 70 50 126 Q 8 70 50 14 Z"
        val shield = Path().apply {
            moveTo(x(50f), y(14f))
            quadraticTo(x(92f), y(70f), x(50f), y(126f))
            quadraticTo(x(8f), y(70f), x(50f), y(14f))
            close()
        }

        // Kenyan tricolor stripes clipped to shield
        clipPath(shield) {
            drawRect(inkColor,  topLeft = Offset(0f, y(0f)),   size = Size(this.size.width, y(40f)))
            drawRect(Color.White, topLeft = Offset(0f, y(40f)), size = Size(this.size.width, y(6f)))
            drawRect(redColor,  topLeft = Offset(0f, y(46f)),  size = Size(this.size.width, y(48f)))
            drawRect(Color.White, topLeft = Offset(0f, y(94f)), size = Size(this.size.width, y(6f)))
            drawRect(greenColor,topLeft = Offset(0f, y(100f)), size = Size(this.size.width, y(40f)))
        }

        // Shield outline
        drawPath(shield, inkColor, style = Stroke(sw))

        // Central oval/diamond "M 50 60 Q 60 70 50 80 Q 40 70 50 60 Z"
        val diamond = Path().apply {
            moveTo(x(50f), y(60f))
            quadraticTo(x(60f), y(70f), x(50f), y(80f))
            quadraticTo(x(40f), y(70f), x(50f), y(60f))
            close()
        }
        drawPath(diamond, Color.White)
        drawPath(diamond, inkColor, style = Stroke(sw * 0.8f))
    }
}

/**
 * Dashed bee-flight path — the curl that wraps from upper-right down behind the bee.
 * Mirrors the SVG path in screen-home.jsx: viewBox 0 0 200 260,
 *   M 169 50 C 230 66, 175 94, 175 132 S 220 182, 175 212 S 80 252, 25 232
 */
@Composable
fun BeeFlightPath(
    modifier: Modifier = Modifier,
    strokeColor: Color = InkDark,
) {
    Canvas(modifier = modifier) {
        fun x(v: Float) = v * size.width / 200f
        fun y(v: Float) = v * size.height / 260f
        val path = Path().apply {
            moveTo(x(169f), y(50f))
            // C 230 66, 175 94, 175 132
            cubicTo(x(230f), y(66f), x(175f), y(94f), x(175f), y(132f))
            // S 220 182, 175 212  → cubic with reflected first control
            // Reflected control of previous (175,94) about (175,132) = (175, 170)
            cubicTo(x(175f), y(170f), x(220f), y(182f), x(175f), y(212f))
            // S 80 252, 25 232  → reflected control of (220,182) about (175,212) = (130, 242)
            cubicTo(x(130f), y(242f), x(80f), y(252f), x(25f), y(232f))
        }
        drawPath(
            path = path,
            color = strokeColor,
            alpha = 0.75f,
            style = Stroke(
                width = 2.2f,
                cap = StrokeCap.Round,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 7f), 0f),
            ),
        )
    }
}

/**
 * Single hexagon cell — used for the honeycomb decorative pattern.
 */
@Composable
fun HoneyHex(
    modifier: Modifier = Modifier,
    fillColor: Color = Color(0xFFFBE07A),
    strokeColor: Color = InkDark,
    strokeWidth: Float = 1.8f,
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = (size.minDimension / 2f) * 0.92f
        val hex = Path().apply {
            for (i in 0 until 6) {
                val angle = Math.PI / 3.0 * i + Math.PI / 6.0
                val px = (cx + r * Math.cos(angle)).toFloat()
                val py = (cy + r * Math.sin(angle)).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawPath(hex, fillColor)
        drawPath(hex, strokeColor, style = Stroke(strokeWidth))
    }
}

/**
 * Chunky card with solid offset drop-shadow — the core design primitive.
 * Uses a custom Layout so the shadow is measured separately from the card.
 *
 * The shadow is a rounded rect drawn at (0, depth) behind the card,
 * creating the flat 2D "pop" effect from the design.
 */
@Composable
fun ChunkyCard(
    modifier: Modifier = Modifier,
    bgColor: Color = SurfaceWhite,
    inkColor: Color = InkDark,
    cornerRadius: Dp = 22.dp,
    depth: Dp = 4.dp,
    borderWidth: Dp = 2.5.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    Layout(
        modifier = modifier,
        content = {
            // child 0: shadow placeholder (sized by Layout)
            Box(Modifier.background(inkColor, shape))
            // child 1: visible card
            Box(
                modifier = Modifier
                    .background(bgColor, shape)
                    .border(borderWidth, inkColor, shape),
                content = content,
            )
        },
    ) { measurables, constraints ->
        val depthPx = depth.roundToPx()
        // Reserve [depthPx] for the shadow at the bottom. Must also clamp minHeight
        // so tight parent constraints (e.g. Modifier.size(44.dp)) don't violate
        // minHeight ≤ maxHeight after the shrink.
        val cardConstraints = if (constraints.hasBoundedHeight) {
            val newMax = (constraints.maxHeight - depthPx).coerceAtLeast(0)
            val newMin = constraints.minHeight.coerceAtMost(newMax)
            constraints.copy(minHeight = newMin, maxHeight = newMax)
        } else {
            constraints
        }
        val card = measurables[1].measure(cardConstraints)
        val shadow = measurables[0].measure(Constraints.fixed(card.width, card.height))
        layout(card.width, card.height + depthPx) {
            shadow.place(0, depthPx)   // shadow behind, shifted down
            card.place(0, 0)           // card on top
        }
    }
}
