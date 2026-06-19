package com.r2d2.controller

/**
 * ArduD2 명령 프로토콜 (R2D2_main_v5 기준)
 *
 * App → Arduino (Serial1):
 *   F{0-9}  전진      B{0-9}  후진
 *   L       좌회전    R       우회전    S  정지
 *   M       수동 모드  A       자동(팔로잉) 모드
 *   1       Say Hello  2  Play Music  3  Horn
 *   T       상체 시계방향 15도
 *   U       상체 반시계방향 15도
 *   H       상체 원점 복귀
 *   C       타겟 초기화
 *
 * Arduino → App (Serial1.println):
 *   "TRACKING:1"     타겟 감지
 *   "TRACKING:0"     타겟 사라짐
 *   "BODY:{angle}"   상체 회전 완료 + 현재 각도 (예: "BODY:45")
 *   "E:BODY_LIMIT"   상체 최대 회전각 초과 에러
 */
object RobotCommands {
    // ── 하체 이동 ───────────────────────────────────────────────────────
    fun forward(speed: Int = 7)  = "F${speed.coerceIn(0, 9)}"
    fun backward(speed: Int = 7) = "B${speed.coerceIn(0, 9)}"
    fun turnLeft()   = "L"
    fun turnRight()  = "R"
    fun stop()       = "S"

    // ── 모드 ────────────────────────────────────────────────────────────
    fun manualMode() = "M"
    fun autoMode()   = "A"

    // ── 사운드 ──────────────────────────────────────────────────────────
    fun sayHello()   = "1"
    fun playMusic()  = "2"
    fun horn()       = "3"

    // ── 상체 제어 ───────────────────────────────────────────────────────
    fun bodyRight()    = "T"   // 시계방향 15도
    fun bodyLeft()     = "U"   // 반시계방향 15도
    fun bodyHome()     = "H"   // 원점 복귀
    fun clearTarget()  = "C"   // 타겟 초기화

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
