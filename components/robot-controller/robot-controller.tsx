"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { StatusBar } from "./status-bar";
import { ModeToggle } from "./mode-toggle";
import { VirtualJoystick } from "./virtual-joystick";
import { TrackingStatus } from "./tracking-status";
import { SoundEffects } from "./sound-effects";
import { Bot } from "lucide-react";
import { RobotSerial, Commands } from "@/lib/serial";

type SpeedPreset = "LOW" | "MED" | "HIGH";
const SPEED_MAP: Record<SpeedPreset, number> = { LOW: 0.4, MED: 0.7, HIGH: 1.0 };

export function RobotController() {
  const [isConnected, setIsConnected]     = useState(false);
  const [isConnecting, setIsConnecting]   = useState(false);
  const [isManualMode, setIsManualMode]   = useState(true);
  const [isTracking, setIsTracking]       = useState(false);
  const [isObstacle, setIsObstacle]       = useState(false);
  const [speedPreset, setSpeedPreset]     = useState<SpeedPreset>("MED");
  const [isSerialSupported, setIsSerialSupported] = useState(false);

  const serialRef      = useRef<RobotSerial | null>(null);
  const lastCmdRef     = useRef<string>("");
  const obstacleTimer  = useRef<ReturnType<typeof setTimeout> | null>(null);

  // 초기화
  useEffect(() => {
    setIsSerialSupported(RobotSerial.isSupported());

    const serial = new RobotSerial();
    serialRef.current = serial;

    // Arduino → App 수신 파싱
    serial.onData((data) => {
      if (data === "TRACKING:1") setIsTracking(true);
      if (data === "TRACKING:0") setIsTracking(false);
      if (data === "OBSTACLE") {
        setIsObstacle(true);
        if (obstacleTimer.current) clearTimeout(obstacleTimer.current);
        obstacleTimer.current = setTimeout(() => setIsObstacle(false), 2000);
      }
    });

    serial.onDisconnect(() => {
      setIsConnected(false);
      setIsTracking(false);
      setIsObstacle(false);
    });

    return () => { serial.disconnect(); };
  }, []);

  // ─── 연결 / 해제 ─────────────────────────
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
      // 연결 직후 현재 모드 동기화
      await serialRef.current.send(Commands.manualMode());
    }
  };

  // ─── 명령 전송 (중복 방지) ───────────────
  const sendCmd = useCallback(async (cmd: string) => {
    if (!serialRef.current?.isConnected) return;
    if (cmd === lastCmdRef.current) return;
    lastCmdRef.current = cmd;
    await serialRef.current.send(cmd);
  }, []);

  // ─── 조이스틱 ────────────────────────────
  const handleJoystickMove = useCallback(
    ({ x, y }: { x: number; y: number }) => {
      const cmd = Commands.fromJoystick(x, y, SPEED_MAP[speedPreset]);
      sendCmd(cmd);
    },
    [sendCmd, speedPreset]
  );

  const handleJoystickRelease = useCallback(async () => {
    lastCmdRef.current = "";
    await serialRef.current?.send(Commands.stop());
  }, []);

  // ─── 모드 전환 ───────────────────────────
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

  // ─── 사운드 ──────────────────────────────
  const handleSayHello  = async () => { await serialRef.current?.send(Commands.sayHello()); };
  const handlePlayMusic = async () => { await serialRef.current?.send(Commands.playMusic()); };
  const handleHorn      = async () => { await serialRef.current?.send(Commands.horn()); };

  return (
    <div className="min-h-screen flex flex-col bg-background">
      {/* 상태 바 */}
      <StatusBar
        isConnected={isConnected}
        isConnecting={isConnecting}
        isSerialSupported={isSerialSupported}
        isObstacle={isObstacle}
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

      {/* 속도 프리셋 (수동 모드일 때만) */}
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

      {/* 컨트롤 영역 */}
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

      {/* 사운드 */}
      <SoundEffects
        onSayHello={handleSayHello}
        onPlayMusic={handlePlayMusic}
        onHorn={handleHorn}
      />
    </div>
  );
}
