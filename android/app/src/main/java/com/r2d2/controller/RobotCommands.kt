package com.r2d2.controller

/**
 * ArduD2 명령 프로토콜 (web serial.ts 와 동일)
 *
 * App → Arduino:
 *   F{0-9}  전진      B{0-9}  후진
 *   L       좌회전    R       우회전    S  정지
 *   M       수동 모드  A       자동 모드
 *   1       Say Hello  2  Play Music  3  Horn
 *
 * Arduino → App:
 *   "TRACKING:1"  타겟 감지
 *   "TRACKING:0"  타겟 없음
 *   "OBSTACLE"    장애물 감지
 */
object RobotCommands {
    fun forward(speed: Int = 7)  = "F${speed.coerceIn(0, 9)}"
    fun backward(speed: Int = 7) = "B${speed.coerceIn(0, 9)}"
    fun turnLeft()   = "L"
    fun turnRight()  = "R"
    fun stop()       = "S"
    fun manualMode() = "M"
    fun autoMode()   = "A"
    fun sayHello()   = "1"
    fun playMusic()  = "2"
    fun horn()       = "3"

    /**
     * 조이스틱 XY → 이동 명령
     * @param x   좌우 (-1 ~ 1, 양수 = 오른쪽)
     * @param y   전후 (-1 ~ 1, 양수 = 전진)
     * @param speedMult  속도 배율 (0 ~ 1)
     */
    fun fromJoystick(x: Float, y: Float, speedMult: Float = 1.0f): String {
        val dead = 0.18f
        val ax = Math.abs(x)
        val ay = Math.abs(y)
        if (ax < dead && ay < dead) return "S"
        val spd = (maxOf(ax, ay) * speedMult * 9).toInt().coerceIn(0, 9)
        return if (ay >= ax) {
            if (y > 0) "F$spd" else "B$spd"
        } else {
            if (x > 0) "R" else "L"
        }
    }
}
