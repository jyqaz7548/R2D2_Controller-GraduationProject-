package com.r2d2.controller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import com.r2d2.controller.ui.theme.AmberYellow
import com.r2d2.controller.ui.theme.SteelLight
import com.r2d2.controller.ui.theme.RustOrange
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    onMove: (x: Float, y: Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging  by remember { mutableStateOf(false) }
    var centerPx   by remember { mutableStateOf(Offset.Zero) }

    val maxRadius  = 240f  // 베이스 원 반경 (픽셀)
    val canvasSize = 550.dp

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
                .size(canvasSize)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // 첫 터치 감지 (소비하지 않음)
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val cx = size.width  / 2f
                        val cy = size.height / 2f
                        centerPx = Offset(cx, cy)

                        val startDelta = down.position - centerPx
                        val startDist  = sqrt(startDelta.x * startDelta.x + startDelta.y * startDelta.y)

                        // 원 바깥에서 시작한 터치는 무시 → 스크롤에 전달됨
                        if (startDist > maxRadius) return@awaitEachGesture

                        // 원 안에서 시작 → 조이스틱 활성화
                        isDragging = true
                        knobOffset = clampRadius(startDelta, maxRadius)
                        onMove(knobOffset.x / maxRadius, -knobOffset.y / maxRadius)

                        drag(down.id) { change ->
                            change.consume()
                            val delta = change.position - centerPx
                            knobOffset = clampRadius(delta, maxRadius)
                            onMove(knobOffset.x / maxRadius, -knobOffset.y / maxRadius)
                        }

                        // 손 떼거나 취소
                        isDragging = false
                        knobOffset = Offset.Zero
                        onRelease()
                    }
                },
        ) {
            val cx = size.width  / 2f
            val cy = size.height / 2f
            centerPx = Offset(cx, cy)

            // ── 베이스 원 ──────────────────────────────────────────────
            drawCircle(
                color  = Color(0xFF252527),
                radius = maxRadius,
                center = Offset(cx, cy),
            )
            drawCircle(
                color  = if (isDragging) AmberYellow.copy(alpha = 0.5f) else Color(0xFF4A4640),
                radius = maxRadius,
                center = Offset(cx, cy),
                style  = Stroke(width = 3f),
            )

            // ── 십자선 + 내부 원 (가이드) ──────────────────
            drawLine(
                color       = Color(0xFF3C3C40),
                start       = Offset(cx - maxRadius, cy),
                end         = Offset(cx + maxRadius, cy),
                strokeWidth = 1f,
            )
            drawLine(
                color       = Color(0xFF3C3C40),
                start       = Offset(cx, cy - maxRadius),
                end         = Offset(cx, cy + maxRadius),
                strokeWidth = 1f,
            )
            drawCircle(
                color  = Color(0xFF3C3C40),
                radius = maxRadius * 0.6f,
                center = Offset(cx, cy),
                style  = Stroke(width = 1f),
            )

            // ── 노브 ───────────────────────────────────────────────────
            val knobCenter = Offset(cx + knobOffset.x, cy + knobOffset.y)
            val knobRadius = 72f
            val glowAlpha  = if (isDragging) 0.35f else 0.15f

            drawCircle(
                color  = AmberYellow.copy(alpha = glowAlpha),
                radius = knobRadius + 18f,
                center = knobCenter,
            )
            drawCircle(
                color  = AmberYellow,
                radius = knobRadius,
                center = knobCenter,
            )
            drawCircle(
                color  = Color.Black.copy(alpha = 0.3f),
                radius = knobRadius * 0.55f,
                center = knobCenter,
            )

            // ── 방향 화살표 ────────────────────────────────────────────
            val normX =  knobOffset.x / maxRadius
            val normY = -knobOffset.y / maxRadius
            val threshold = 0.3f

            fun arrowAlpha(active: Boolean) = if (active) 1f else 0.2f

            if (normY > threshold * 0.5f)
                drawArrow(Offset(cx, cy - maxRadius - 22f), Direction.UP,    AmberYellow.copy(alpha = arrowAlpha(normY > threshold)))
            if (normY < -threshold * 0.5f)
                drawArrow(Offset(cx, cy + maxRadius + 22f), Direction.DOWN,  AmberYellow.copy(alpha = arrowAlpha(normY < -threshold)))
            if (normX < -threshold * 0.5f)
                drawArrow(Offset(cx - maxRadius - 22f, cy), Direction.LEFT,  AmberYellow.copy(alpha = arrowAlpha(normX < -threshold)))
            if (normX > threshold * 0.5f)
                drawArrow(Offset(cx + maxRadius + 22f, cy), Direction.RIGHT, AmberYellow.copy(alpha = arrowAlpha(normX > threshold)))
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
    val s = 13f
    val path = androidx.compose.ui.graphics.Path()
    when (direction) {
        Direction.UP    -> { path.moveTo(center.x, center.y - s); path.lineTo(center.x - s, center.y + s); path.lineTo(center.x + s, center.y + s) }
        Direction.DOWN  -> { path.moveTo(center.x, center.y + s); path.lineTo(center.x - s, center.y - s); path.lineTo(center.x + s, center.y - s) }
        Direction.LEFT  -> { path.moveTo(center.x - s, center.y); path.lineTo(center.x + s, center.y - s); path.lineTo(center.x + s, center.y + s) }
        Direction.RIGHT -> { path.moveTo(center.x + s, center.y); path.lineTo(center.x - s, center.y - s); path.lineTo(center.x - s, center.y + s) }
    }
    path.close()
    drawPath(path = path, color = color)
}
