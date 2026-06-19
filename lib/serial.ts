/**
 * ArduD2 Robot - Web Serial API 통신 레이어  (v2)
 *
 * HC-06 블루투스 모듈은 페어링하면 PC에서 가상 COM 포트로 잡힌다.
 * Web Serial API를 통해 Chrome/Edge 브라우저에서 직접 접근 가능.
 *
 * 지원 환경: Chrome 89+, Edge 89+ (HTTPS 또는 localhost 필수)
 * 모바일: Android Chrome 지원 (iOS 미지원)
 *
 * [v2 버그 수정]
 * - 재연결 불가 버그: disconnect() 시 reader를 cancel()한 뒤 port.close()
 *   → readable 락이 남아있어 port.close()가 실패하던 문제 해결
 * - BT 강제 끊김 후 재연결: 이벤트 리스너 + 루프 finally 양쪽에서
 *   이중 알림 방지
 */

// ─────────────────────────────────────────────
// 명령 프로토콜 (App → Arduino Serial1)
//
// 이동:
//   F{0-9}  전진  (예: F7 = 70% 속도)
//   B{0-9}  후진
//   L       좌회전
//   R       우회전
//   S       정지
//
// 모드:
//   M       수동 모드
//   A       자동(HuskyLens) 모드
//
// 사운드:
//   1       Say Hello
//   2       Play Music
//   3       Horn
//
// Arduino → App:
//   "TRACKING:1\n"   타겟 감지
//   "TRACKING:0\n"   타겟 없음
//   "OBSTACLE\n"     장애물 감지
// ─────────────────────────────────────────────

export const BAUD_RATE = 9600;

export const Commands = {
  forward:  (speed = 7) => `F${Math.min(9, Math.max(0, Math.round(speed)))}`,
  backward: (speed = 7) => `B${Math.min(9, Math.max(0, Math.round(speed)))}`,
  turnLeft:  () => "L",
  turnRight: () => "R",
  stop:      () => "S",
  manualMode: () => "M",
  autoMode:   () => "A",
  sayHello:   () => "1",
  playMusic:  () => "2",
  horn:       () => "3",

  /**
   * 조이스틱 XY → 이동 명령
   * @param x 좌우 (-1~1, 양수=오른쪽)
   * @param y 전후 (-1~1, 양수=전진)
   * @param speedMult 속도 배율 (0~1)
   */
  fromJoystick: (x: number, y: number, speedMult = 1.0): string => {
    const DEAD = 0.18;
    const ax = Math.abs(x), ay = Math.abs(y);
    if (ax < DEAD && ay < DEAD) return "S";
    const spd = Math.round(Math.max(ax, ay) * speedMult * 9);
    if (ay >= ax) return y > 0 ? `F${spd}` : `B${spd}`;
    return x > 0 ? "R" : "L";
  },
};

export class RobotSerial {
  private port: SerialPort | null = null;
  private writer: WritableStreamDefaultWriter<Uint8Array> | null = null;
  // reader를 클래스 필드로 유지 → disconnect()에서 cancel() 가능
  private reader: ReadableStreamDefaultReader<Uint8Array> | null = null;
  private readLoopActive = false;
  private receiveBuffer = "";
  private onDataCb?: (data: string) => void;
  private onDisconnectCb?: () => void;

  // ─── 연결 ───────────────────────────────────
  async connect(): Promise<boolean> {
    if (!RobotSerial.isSupported()) return false;

    // 이전 연결 잔여 상태 초기화
    this.receiveBuffer = "";
    this.readLoopActive = false;

    try {
      this.port = await (navigator as any).serial.requestPort();
      await this.port!.open({ baudRate: BAUD_RATE });
      this.writer = this.port!.writable!.getWriter();
      this.startReadLoop(); // 비동기 루프 시작 (await 안함)

      // 장치가 물리적으로 제거됐을 때 (예: USB 뽑힘)
      this.port!.addEventListener("disconnect", () => this._onPortHardDisconnect());

      return true;
    } catch {
      this.port = null;
      this.writer = null;
      return false;
    }
  }

  // ─── 해제 (사용자 버튼) ─────────────────────
  // readable 락 문제 해결: reader.cancel() → writer.releaseLock() → port.close() 순서
  async disconnect(): Promise<void> {
    this.readLoopActive = false;

    // 1) reader cancel → readable 스트림 락 해제
    const reader = this.reader;
    this.reader = null;
    try { await reader?.cancel(); } catch {}

    // 2) writer 락 해제
    try { this.writer?.releaseLock(); } catch {}
    this.writer = null;

    // 3) 이제 포트를 안전하게 닫을 수 있음
    const port = this.port;
    this.port = null;
    try { await port?.close(); } catch {}
  }

  // ─── 전송 ───────────────────────────────────
  async send(command: string): Promise<boolean> {
    if (!this.writer) return false;
    try {
      await this.writer.write(new TextEncoder().encode(command + "\n"));
      return true;
    } catch { return false; }
  }

  // ─── 수신 루프 ──────────────────────────────
  private async startReadLoop(): Promise<void> {
    if (!this.port?.readable) return;
    this.readLoopActive = true;

    const reader = this.port.readable.getReader();
    this.reader = reader;
    const decoder = new TextDecoder();

    try {
      while (this.readLoopActive) {
        const { value, done } = await reader.read();
        if (done) break;
        if (value) {
          this.receiveBuffer += decoder.decode(value);
          const lines = this.receiveBuffer.split("\n");
          this.receiveBuffer = lines.pop() ?? "";
          for (const l of lines) {
            const t = l.trim();
            if (t) this.onDataCb?.(t);
          }
        }
      }
    } catch {
      // reader.cancel()이 호출되면 여기서 AbortError가 발생 - 정상
    } finally {
      try { reader.releaseLock(); } catch {}
      // 현재 reader가 이 루프의 reader와 같을 때만 null로 세팅
      if (this.reader === reader) this.reader = null;

      // readLoopActive가 아직 true이면 예상치 못한 끊김 (BT 강제 해제)
      if (this.readLoopActive) {
        this.readLoopActive = false;
        try { this.writer?.releaseLock(); } catch {}
        this.writer = null;
        // port는 이미 닫혔으므로 close() 불필요
        this.port = null;
        this.onDisconnectCb?.();
      }
    }
  }

  // ─── 물리적 장치 제거 이벤트 ────────────────
  private _onPortHardDisconnect() {
    // 루프의 finally가 먼저 처리했으면 readLoopActive는 이미 false
    if (!this.readLoopActive) return;
    this.readLoopActive = false;
    this.reader = null;
    this.writer = null;
    this.port = null;
    this.receiveBuffer = "";
    this.onDisconnectCb?.();
  }

  // ─── 유틸 ────────────────────────────────────
  get isConnected() { return this.port !== null; }
  static isSupported() {
    return typeof navigator !== "undefined" && "serial" in (navigator as any);
  }
  onData(cb: (data: string) => void) { this.onDataCb = cb; }
  onDisconnect(cb: () => void)        { this.onDisconnectCb = cb; }
}
