"use client";

import { Wifi, WifiOff, Loader2, AlertTriangle, Unplug, Plug } from "lucide-react";

interface StatusBarProps {
  isConnected: boolean;
  isConnecting: boolean;
  isSerialSupported: boolean;
  isObstacle: boolean;
  onConnect: () => void;
}

export function StatusBar({
  isConnected,
  isConnecting,
  isSerialSupported,
  isObstacle,
  onConnect,
}: StatusBarProps) {
  return (
    <div className="flex items-center justify-between px-4 py-3 bg-card/80 backdrop-blur-sm border-b border-border">
      {/* 왼쪽: 연결 상태 */}
      <div className="flex items-center gap-3">
        <div className="relative">
          {isConnected ? (
            <Wifi className="h-5 w-5 text-neon-green" />
          ) : (
            <WifiOff className="h-5 w-5 text-muted-foreground" />
          )}
          {isConnected && (
            <span className="absolute -top-0.5 -right-0.5 h-2 w-2 rounded-full bg-neon-green" />
          )}
        </div>

        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-muted-foreground">상태:</span>
          <span
            className={`text-sm font-semibold ${
              isConnected ? "text-neon-green" : "text-muted-foreground"
            }`}
          >
            {isConnecting ? "연결 중..." : isConnected ? "연결됨" : "연결 안 됨"}
          </span>
        </div>

        {/* 장애물 경고 */}
        {isObstacle && (
          <div
            className="flex items-center gap-1 px-2 py-0.5 rounded bg-neon-orange/10 border border-neon-orange/50"
            style={{ boxShadow: "0 0 8px oklch(0.75 0.20 55 / 0.5)" }}
          >
            <AlertTriangle className="h-3 w-3 text-neon-orange" />
            <span className="text-xs font-mono text-neon-orange">장애물 감지</span>
          </div>
        )}
      </div>

      {/* 오른쪽: 연결 버튼 */}
      <div className="flex items-center gap-3">
        {/* 연결 표시등 */}
        {isConnected && (
          <div
            className="h-3 w-3 rounded-full bg-neon-green animate-pulse-glow"
            style={{ boxShadow: "0 0 8px var(--neon-green), 0 0 16px var(--glow-green)" }}
          />
        )}

        {/* 연결 / 해제 버튼 */}
        {!isSerialSupported ? (
          <span className="text-xs text-destructive font-mono">
            Serial 미지원
          </span>
        ) : (
          <button
            onClick={onConnect}
            disabled={isConnecting}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-mono border transition-all active:scale-95 disabled:opacity-50 ${
              isConnected
                ? "border-destructive/50 text-destructive hover:bg-destructive/10"
                : "border-neon-green/50 text-neon-green hover:bg-neon-green/10"
            }`}
            style={{
              boxShadow: isConnected
                ? "none"
                : "0 0 8px var(--glow-green)",
            }}
          >
            {isConnecting ? (
              <>
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                연결 중
              </>
            ) : isConnected ? (
              <>
                <Unplug className="h-3.5 w-3.5" />
                해제
              </>
            ) : (
              <>
                <Plug className="h-3.5 w-3.5" />
                연결
              </>
            )}
          </button>
        )}
      </div>
    </div>
  );
}
