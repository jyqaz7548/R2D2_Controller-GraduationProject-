package com.r2d2.controller

/**
 * R2D2 명령 프로토콜 (R2D2_main_v6 기준)
 *
 * App → Arduino (Serial1, '\n' 종료):
 *   F{0-9}     전진      B{0-9}  후진
 *   L          좌회전    R       우회전    S  정지
 *   M          수동 모드  A       자동(팔로잉) 모드
 *   1          Say Hello  2  Play Music  3  Horn
 *   G{angle}   상체 절대 각도 이동 (예: G90, G-45)  ← v6 신규
 *   T          상체 시계방향 15도   (하위 호환용)
 *   U          상체 반시계방향 15도 (하위 호환용)
 *   H          상체 원점 복귀
 *   C          타겟 초기화
 *
 * Arduino → App (Serial1.println):
 *   "TRACKING:1"     타겟 감지
 *   "TRACKING:0"     타겟 사라짐
 *   "BODY:{angle}"   현재 각도 보고 (도달 시 + 이동 중 500ms마다)
 *   "ESTOP:OK"       비상정지 실행 확인
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
    fun sayHello()   = "1"   // 0001~0006 랜덤
    fun playMusic()  = "2"   // 0007 → 0008 → 정지 (누를 때마다 순환)
    fun dance()         = "3"   // 0009 댄스
    fun shutdown()      = "Z"   // 0012 종료음 (연결 해제 직전 전송)
    fun emergencyStop() = "E"   // 비상정지 — 모든 모터·사운드 즉시 정지
    fun connected()     = "K"   // 블루투스 연결 확인음 (연결 직후 전송)

    // ── 상체 제어 ───────────────────────────────────────────────────────
    /** v6: 절대 각도로 직접 이동 (-350 ~ +350) */
    fun bodyGoto(angle: Int)  = "G${angle.coerceIn(-350, 350)}"
    fun bodyRight()    = "T"   // 시계방향 15도 (하위 호환)
    fun bodyLeft()     = "U"   // 반시계방향 15도 (하위 호환)
    fun bodyHome()      = "H"   // 원점 복귀
    fun bodyZeroReset() = "I"   // 현재 위치를 0도로 수동 영점 초기화
    fun clearTarget()   = "C"   // 타겟 초기화

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
            if (x > 0) "R$spd" else "L$spd"
        }
    }
}
