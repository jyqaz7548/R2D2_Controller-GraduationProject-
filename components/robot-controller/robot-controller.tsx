"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { StatusBar } from "./status-bar";
import { ModeToggle } from "./mode-toggle";
import { VirtualJoystick } from "./virtual-joystick";
import { TrackingStatus } from "./tracking-status";
import { SoundEffects } from "./sound-effects";
import { BodyControl } from "./body-control";
import { Bot } from "lucide-react";
import { RobotSerial, Commands, BODY_STEP_DEG } from "@/lib/serial";

type SpeedPreset = "LOW" | "MED" | "HIGH";
const SPEED_MAP: Record<SpeedPreset, number> = { LOW: 0.4, MED: 0.7, HIGH: 1.0 };

export function RobotController() {
  const [isConnected, setIsConnected]     = useState(false);
  const [isConnecting, setIsConnecting]   = useState(false);
  const [isManualMode, setIsManualMode]   = useState(true);
  const [isTracking, setIsTracking]       = useState(false);
  const [speedPreset, setSpeedPreset]     = useState<SpeedPreset>("MED");
  const [isSerialSupported, setIsSerialSupported] = useState(false);

  // ── 상체 상태 ────────────────────────────────────────────────────
  const [bodyAngle, setBodyAngle]         = useState(0);   // Arduino 확인 각도
  const [targetBodyAngle, setTargetBodyAngle] = useState(0); // 슬라이더 목표
  const [bodyLimitError, setBodyLimitError]   = useState(false);
  const bodyMovingRef  = useRef(false);  // Arduino 이동 중 여부
  const limitErrorTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const serialRef  = useRef<RobotSerial | null>(null);
  const lastCmdRef = useRef<string>("");

  // ─── 초기화 ─────────────────────────────────────────────────────
  useEffect(() => {
    setIsSerialSupported(RobotSerial.isSupported());

    const serial = new RobotSerial();
    serialRef.current = serial;

    serial.onData((data) => {
      if (data === "TRACKING:1") { setIsTracking(true);  return; }
      if (data === "TRACKING:0") { setIsTracking(false); return; }

      if (data.startsWith("BODY:")) {
        const angle = parseInt(data.slice(5), 10);
        if (!isNaN(angle)) {
          setBodyAngle(angle);
          bodyMovingRef.current = false;
          // 목표 각도에 아직 못 도달했으면 다음 스텝 전송
          sendNextBodyStep(angle);
        }
        return;
      }

      if (data === "E:BODY_LIMIT") {
        setBodyLimitError(true);
        // 현재 각도로 목표 리셋 (더 이상 명령 안 보냄)
        setTargetBodyAngle((prev) => { bodyMovingRef.current = false; return prev; });
        if (limitErrorTimer.current) clearTimeout(limitErrorTimer.current);
        limitErrorTimer.current = setTimeout(() => setBodyLimitError(false), 3000);
        return;
      }
    });

    serial.onDisconnect(() => {
      setIsConnected(false);
      setIsTracking(false);
      setBodyAngle(0);
      setTargetBodyAngle(0);
      bodyMovingRef.current = false;
    });

    return () => { serial.disconnect(); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ─── 연결 / 해제 ────────────────────────────────────────────────
  const handleConnect = async () => {
    if (!serialRef.current) return;
    if (isConnected) {
      await serialRef.current.disconnect();
      setIsConnected(false);
      return;
    }
    setIsConnecting(true);
    const ok = await serialRef.current.connect();
    setIsConnecting(false);
    if (ok) {
      setIsConnected(true);
      await serialRef.current.send(Commands.manualMode());
    }
  };

  // ─── 명령 전송 (중복 방지) ───────────────────────────────────────
  const sendCmd = useCallback(async (cmd: string) => {
    if (!serialRef.current?.isConnected) return;
    if (cmd === lastCmdRef.current) return;
    lastCmdRef.current = cmd;
    await serialRef.current.send(cmd);
  }, []);

  // ─── 조이스틱 ───────────────────────────────────────────────────
  const handleJoystickMove = useCallback(
    ({ x, y }: { x: number; y: number }) => {
      sendCmd(Commands.fromJoystick(x, y, SPEED_MAP[speedPreset]));
    },
    [sendCmd, speedPreset]
  );

  const handleJoystickRelease = useCallback(async () => {
    lastCmdRef.current = "";
    await serialRef.current?.send(Commands.stop());
  }, []);

  // ─── 모드 전환 ───────────────────────────────────────────────────
  const handleModeToggle = async () => {
    const next = !isManualMode;
    setIsManualMode(next);
    if (next) {
      setIsTracking(false);
      await serialRef.current?.send(Commands.manualMode());
    } else {
      await serialRef.current?.send(Commands.autoMode());
    }
  };

  // ─── 사운드 ─────────────────────────────────────────────────────
  const handleSayHello  = async () => { await serialRef.current?.send(Commands.sayHello()); };
  const handlePlayMusic = async () => { await serialRef.current?.send(Commands.playMusic()); };
  const handleHorn      = async () => { await serialRef.current?.send(Commands.horn()); };

  // ─── 상체 제어 ──────────────────────────────────────────────────
  /**
   * 현재 각도 → 목표 각도로 한 스텝씩 이동
   * Arduino가 BODY:{angle} 응답할 때마다 호출됨
   */
  const sendNextBodyStep = useCallback(
    (current: number) => {
      // targetBodyAngle은 클로저로 캡처되지 않으므로 ref로 관리
      const target = targetBodyAngleRef.current;
      const delta  = target - current;
      if (Math.abs(delta) < BODY_STEP_DEG / 2) return;  // 도달
      if (bodyMovingRef.current) return;
      bodyMovingRef.current = true;
      const cmd = delta > 0 ? Commands.bodyRight() : Commands.bodyLeft();
      serialRef.current?.send(cmd);
    },
    []
  );

  // targetBodyAngle을 ref로도 추적 (sendNextBodyStep 클로저 문제 해결)
  const targetBodyAngleRef = useRef(0);

  const handleSetTargetBodyAngle = useCallback((angle: number) => {
    setTargetBodyAngle(angle);
    targetBodyAngleRef.current = angle;
    // 현재 각도 기준으로 즉시 첫 스텝 전송
    sendNextBodyStep(bodyAngle);
  }, [bodyAngle, sendNextBodyStep]);

  const handleBodyHome = useCallback(async () => {
    setTargetBodyAngle(0);
    targetBodyAngleRef.current = 0;
    bodyMovingRef.current = true;
    await serialRef.current?.send(Commands.bodyHome());
  }, []);

  return (
    <div className="min-h-screen flex flex-col bg-background">
      {/* 상태 바 */}
      <StatusBar
        isConnected={isConnected}
        isConnecting={isConnecting}
        isSerialSupported={isSerialSupported}
        isObstacle={false}
        onConnect={handleConnect}
      />

      {/* 헤더 */}
      <div className="flex items-center justify-center gap-3 py-4 border-b border-border/50">
        <div
          className="p-2 rounded-lg bg-primary/10"
          style={{ boxShadow: "0 0 15px var(--glow-green)" }}
        >
          <Bot className="h-6 w-6 text-primary" />
        </div>
        <h1 className="text-lg font-bold text-foreground tracking-wide">
          ArduD2 컨트롤러
        </h1>
      </div>

      {/* 모드 토글 */}
      <ModeToggle isManualMode={isManualMode} onToggle={handleModeToggle} />

      {/* 속도 프리셋 (수동 모드) */}
      {isManualMode && (
        <div className="flex justify-center gap-2 px-6 pb-2">
          {(["LOW", "MED", "HIGH"] as SpeedPreset[]).map((p) => (
            <button
              key={p}
              onClick={() => setSpeedPreset(p)}
              className={`px-4 py-1 rounded-full text-xs font-mono border transition-all ${
                speedPreset === p
                  ? "border-neon-cyan bg-neon-cyan/20 text-neon-cyan"
                  : "border-border text-muted-foreground hover:border-neon-cyan/50"
              }`}
              style={speedPreset === p ? { boxShadow: "0 0 8px var(--glow-cyan)" } : {}}
            >
              {p}
            </button>
          ))}
        </div>
      )}

      <div className="h-px bg-border mx-6" />

      {/* 조이스틱 / 트래킹 */}
      <div className="flex-1 flex items-center justify-center py-6">
        {isManualMode ? (
          <VirtualJoystick
            onMove={handleJoystickMove}
            onRelease={handleJoystickRelease}
          />
        ) : (
          <TrackingStatus isTracking={isTracking} targetName="사람" />
        )}
      </div>

      {/* 상체 슬라이더 */}
      <BodyControl
        currentAngle={bodyAngle}
        targetAngle={targetBodyAngle}
        limitError={bodyLimitError}
        onSetTarget={handleSetTargetBodyAngle}
        onHome={handleBodyHome}
      />

      {/* 사운드 */}
      <SoundEffects
        onSayHello={handleSayHello}
        onPlayMusic={handlePlayMusic}
        onHorn={handleHorn}
      />
    </div>
  );
}
