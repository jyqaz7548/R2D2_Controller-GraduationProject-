"use client";

import { Eye, Target, ScanLine } from "lucide-react";

interface TrackingStatusProps {
  isTracking: boolean;
  targetName?: string;
}

export function TrackingStatus({ isTracking, targetName = "사람" }: TrackingStatusProps) {
  return (
    <div className="flex flex-col items-center gap-6 py-8">
      <div className="text-xs font-mono uppercase tracking-widest text-muted-foreground">
        HuskyLens 추적
      </div>

      {/* Scanning visualization */}
      <div className="relative">
        <div
          className="h-40 w-40 rounded-xl border-2 border-neon-green/50 bg-secondary/30 overflow-hidden"
          style={{
            boxShadow: isTracking
              ? "0 0 30px var(--glow-green), inset 0 0 20px var(--glow-green)"
              : "inset 0 0 20px rgba(0,0,0,0.3)",
          }}
        >
          {/* Scan line effect */}
          <div className="absolute inset-0 overflow-hidden">
            <div
              className="absolute inset-x-0 h-1 bg-neon-green/60 animate-scan"
              style={{
                boxShadow: "0 0 10px var(--neon-green), 0 0 20px var(--glow-green)",
              }}
            />
          </div>

          {/* Grid overlay */}
          <div className="absolute inset-0 grid grid-cols-3 grid-rows-3">
            {[...Array(9)].map((_, i) => (
              <div key={i} className="border border-neon-green/10" />
            ))}
          </div>

          {/* Center target */}
          <div className="absolute inset-0 flex items-center justify-center">
            {isTracking ? (
              <div className="relative">
                <Target
                  className="h-12 w-12 text-neon-green animate-pulse"
                  style={{ filter: "drop-shadow(0 0 8px var(--neon-green))" }}
                />
                <div className="absolute -inset-4 border-2 border-neon-green/50 rounded animate-ping" />
              </div>
            ) : (
              <ScanLine
                className="h-12 w-12 text-muted-foreground"
              />
            )}
          </div>

          {/* Corner brackets */}
          <div className="absolute top-2 left-2 h-4 w-4 border-t-2 border-l-2 border-neon-green/70" />
          <div className="absolute top-2 right-2 h-4 w-4 border-t-2 border-r-2 border-neon-green/70" />
          <div className="absolute bottom-2 left-2 h-4 w-4 border-b-2 border-l-2 border-neon-green/70" />
          <div className="absolute bottom-2 right-2 h-4 w-4 border-b-2 border-r-2 border-neon-green/70" />
        </div>

        {/* Eye icon */}
        <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-card px-2">
          <Eye
            className={`h-5 w-5 ${isTracking ? "text-neon-green" : "text-muted-foreground"}`}
            style={isTracking ? { filter: "drop-shadow(0 0 6px var(--neon-green))" } : {}}
          />
        </div>
      </div>

      {/* Status text */}
      <div className="text-center space-y-2">
        <div className="flex items-center justify-center gap-2">
          <div
            className={`h-2 w-2 rounded-full ${
              isTracking ? "bg-neon-green animate-pulse" : "bg-muted-foreground"
            }`}
            style={isTracking ? { boxShadow: "0 0 8px var(--neon-green)" } : {}}
          />
          <span className={`text-sm font-medium ${isTracking ? "text-neon-green" : "text-muted-foreground"}`}>
            {isTracking ? "추적 중" : "탐색 중..."}
          </span>
        </div>
        {isTracking && (
          <p className="text-xs text-muted-foreground">
            추적 대상: <span className="text-foreground">{targetName}</span>
          </p>
        )}
        <p className="text-xs text-muted-foreground max-w-[200px]">
          HuskyLens AI 카메라로 감지된 사람을 자동으로 추적합니다
        </p>
      </div>
    </div>
  );
}
