"use client";

import { Gamepad2, Eye } from "lucide-react";

interface ModeToggleProps {
  isManualMode: boolean;
  onToggle: () => void;
}

export function ModeToggle({ isManualMode, onToggle }: ModeToggleProps) {
  return (
    <div className="flex flex-col items-center gap-4 px-6 py-5">
      <div className="text-xs font-mono uppercase tracking-widest text-muted-foreground">
        제어 모드
      </div>
      
      <button
        onClick={onToggle}
        className="relative w-full max-w-xs h-14 rounded-full bg-secondary/50 border border-border p-1 transition-all duration-300"
        style={{
          boxShadow: `0 0 20px ${isManualMode ? "var(--glow-cyan)" : "var(--glow-green)"}`,
        }}
      >
        {/* Sliding indicator */}
        <div
          className={`absolute top-1 h-12 w-1/2 rounded-full transition-all duration-300 ease-out ${
            isManualMode
              ? "left-1 bg-neon-cyan"
              : "left-[calc(50%-4px)] bg-neon-green"
          }`}
          style={{
            boxShadow: isManualMode
              ? "0 0 12px var(--neon-cyan)"
              : "0 0 12px var(--neon-green)",
          }}
        />
        
        {/* Labels */}
        <div className="relative flex h-full">
          <div
            className={`flex-1 flex items-center justify-center gap-2 z-10 transition-colors duration-300 ${
              isManualMode ? "text-background font-semibold" : "text-muted-foreground"
            }`}
          >
            <Gamepad2 className="h-4 w-4" />
            <span className="text-sm">수동</span>
          </div>
          <div
            className={`flex-1 flex items-center justify-center gap-2 z-10 transition-colors duration-300 ${
              !isManualMode ? "text-background font-semibold" : "text-muted-foreground"
            }`}
          >
            <Eye className="h-4 w-4" />
            <span className="text-sm">자동</span>
          </div>
        </div>
      </button>
      
      <div className="text-center">
        <p className="text-sm font-medium text-foreground">
          {isManualMode ? "수동 제어 모드" : "HuskyLens 자율 추적 모드"}
        </p>
        <p className="text-xs text-muted-foreground mt-1">
          {isManualMode
            ? "조이스틱으로 로봇을 제어합니다"
            : "감지된 사람을 자동으로 추적합니다"}
        </p>
      </div>
    </div>
  );
}
