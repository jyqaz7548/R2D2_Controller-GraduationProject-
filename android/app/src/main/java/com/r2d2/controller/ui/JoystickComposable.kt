package com.r2d2.controller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r2d2.controller.ui.theme.NeonCyan
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    onMove: (x: Float, y: Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // knobOffset: 중심 기준 픽셀 오프셋
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging  by remember { mutableStateOf(false) }
    var centerPx   by remember { mutableStateOf(Offset.Zero) }

    val maxRadius = 160f   // 픽셀 단위 최대 반경

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text  = "이동 제어",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6B7280),
            letterSpacing = 2.sp,
        )

        Canvas(
            modifier = Modifier
                .size(320.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            isDragging = true
                            centerPx   = Offset(size.width / 2f, size.height / 2f)
                            val delta = startOffset - centerPx
                            knobOffset = clampRadius(delta, maxRadius)
                            val norm = knobOffset / maxRadius
                            onMove(norm.x, -norm.y)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val delta = change.position - centerPx
                            knobOffset = clampRadius(delta, maxRadius)
                            val norm = knobOffset / maxRadius
                            onMove(norm.x, -norm.y)
                        },
                        onDragEnd = {
                            isDragging = false
                            knobOffset = Offset.Zero
                            onRelease()
                        },
                        onDragCancel = {
                            isDragging = false
                            knobOffset = Offset.Zero
                            onRelease()
                        },
                    )
                },
        ) {
            val cx = size.width  / 2f
            val cy = size.height / 2f
            centerPx = Offset(cx, cy)

            // ── 베이스 원 ──────────────────────────────────
            drawCircle(
                color  = Color(0xFF1F2937),
                radius = maxRadius,
                center = Offset(cx, cy),
            )
            drawCircle(
                color  = if (isDragging) NeonCyan.copy(alpha = 0.25f) else Color(0xFF374151),
                radius = maxRadius,
                center = Offset(cx, cy),
                style  = Stroke(width = 2f),
            )

            // ── 십자선 + 내부 원 (가이드) ──────────────────
            drawLine(
                color       = Color(0xFF374151),
                start       = Offset(cx - maxRadius, cy),
                end         = Offset(cx + maxRadius, cy),
                strokeWidth = 1f,
            )
            drawLine(
                color       = Color(0xFF374151),
                start       = Offset(cx, cy - maxRadius),
                end         = Offset(cx, cy + maxRadius),
                strokeWidth = 1f,
            )
            drawCircle(
                color  = Color(0xFF374151),
                radius = maxRadius * 0.6f,
                center = Offset(cx, cy),
                style  = Stroke(width = 1f),
            )

            // ── 노브 ───────────────────────────────────────
            val knobCenter = Offset(cx + knobOffset.x, cy + knobOffset.y)
            val knobRadius = 52f
            val glowAlpha  = if (isDragging) 0.35f else 0.15f

            drawCircle(
                color  = NeonCyan.copy(alpha = glowAlpha),
                radius = knobRadius + 16f,
                center = knobCenter,
            )
            drawCircle(
                color  = NeonCyan,
                radius = knobRadius,
                center = knobCenter,
            )
            drawCircle(
                color  = Color.Black.copy(alpha = 0.3f),
                radius = knobRadius * 0.55f,
                center = knobCenter,
            )

            // ── 방향 화살표 ────────────────────────────────
            val normX =  knobOffset.x / maxRadius
            val normY = -knobOffset.y / maxRadius
            val threshold = 0.3f

            fun arrowAlpha(active: Boolean) = if (active) 1f else 0.2f

            // 위
            if (normY > threshold * 0.5f) {
                drawArrow(
                    center    = Offset(cx, cy - maxRadius - 20f),
                    direction = Direction.UP,
                    color     = NeonCyan.copy(alpha = arrowAlpha(normY > threshold)),
                )
            }
            // 아래
            if (normY < -threshold * 0.5f) {
                drawArrow(
                    center    = Offset(cx, cy + maxRadius + 20f),
                    direction = Direction.DOWN,
                    color     = NeonCyan.copy(alpha = arrowAlpha(normY < -threshold)),
                )
            }
            // 왼쪽
            if (normX < -threshold * 0.5f) {
                drawArrow(
                    center    = Offset(cx - maxRadius - 20f, cy),
                    direction = Direction.LEFT,
                    color     = NeonCyan.copy(alpha = arrowAlpha(normX < -threshold)),
                )
            }
            // 오른쪽
            if (normX > threshold * 0.5f) {
                drawArrow(
                    center    = Offset(cx + maxRadius + 20f, cy),
                    direction = Direction.RIGHT,
                    color     = NeonCyan.copy(alpha = arrowAlpha(normX > threshold)),
                )
            }
        }

        // 좌표 표시
        val normX =  knobOffset.x / maxRadius
        val normY = -knobOffset.y / maxRadius
        Text(
            text  = "X: ${"%.2f".format(normX)}   Y: ${"%.2f".format(normY)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6B7280),
        )
    }
}

// ── 유틸 ──────────────────────────────────────────────────────────────────────

private fun clampRadius(offset: Offset, max: Float): Offset {
    val dist = sqrt(offset.x * offset.x + offset.y * offset.y)
    return if (dist <= max) offset else offset * (max / dist)
}

private enum class Direction { UP, DOWN, LEFT, RIGHT }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    center: Offset,
    direction: Direction,
    color: Color,
) {
    val s = 12f
    val path = androidx.compose.ui.graphics.Path()
    when (direction) {
        Direction.UP    -> {
            path.moveTo(center.x,     center.y - s)
            path.lineTo(center.x - s, center.y + s)
            path.lineTo(center.x + s, center.y + s)
        }
        Direction.DOWN  -> {
            path.moveTo(center.x,     center.y + s)
            path.lineTo(center.x - s, center.y - s)
            path.lineTo(center.x + s, center.y - s)
        }
        Direction.LEFT  -> {
            path.moveTo(center.x - s, center.y)
            path.lineTo(center.x + s, center.y - s)
            path.lineTo(center.x + s, center.y + s)
        }
        Direction.RIGHT -> {
            path.moveTo(center.x + s, center.y)
            path.lineTo(center.x - s, center.y - s)
            path.lineTo(center.x - s, center.y + s)
        }
    }
    path.close()
    drawPath(path = path, color = color)
}
