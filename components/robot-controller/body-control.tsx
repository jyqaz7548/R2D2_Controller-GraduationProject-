"use client";

import { useRef, useCallback } from "react";
import { Home, RotateCcw, RotateCw } from "lucide-react";
import { BODY_MAX_DEG, BODY_STEP_DEG } from "@/lib/serial";

interface BodyControlProps {
  currentAngle: number;   // Arduino BODY:{angle} 로 확인된 현재 각도
  targetAngle: number;    // 슬라이더 목표 각도
  limitError: boolean;    // E:BODY_LIMIT 수신 여부
  onSetTarget: (angle: number) => void;
  onHome: () => void;
}

export function BodyControl({
  currentAngle,
  targetAngle,
  limitError,
  onSetTarget,
  onHome,
}: BodyControlProps) {
  const sliderRef = useRef<HTMLInputElement>(null);

  // 슬라이더에서 손을 뗄 때 15도 단위 스냅
  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const raw = Number(e.target.value);
      const snapped = Math.round(raw / BODY_STEP_DEG) * BODY_STEP_DEG;
      const clamped = Math.max(-BODY_MAX_DEG, Math.min(BODY_MAX_DEG, snapped));
      onSetTarget(clamped);
    },
    [onSetTarget]
  );

  const delta = targetAngle - currentAngle;
  const isMoving = Math.abs(delta) >= BODY_STEP_DEG;

  // 슬라이더 채우기 비율 (0% = -350, 100% = +350)
  const fillPct = ((targetAngle + BODY_MAX_DEG) / (BODY_MAX_DEG * 2)) * 100;

  return (
    <div className="px-6 py-5 border-t border-border/50">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <RotateCw className="h-4 w-4 text-neon-cyan" />
          <span className="text-xs font-mono uppercase tracking-widest text-muted-foreground">
            상체 회전
          </span>
        </div>
        <button
          onClick={onHome}
          className="flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-mono border border-neon-cyan/40 text-neon-cyan hover:bg-neon-cyan/10 transition-all active:scale-95"
          style={{ boxShadow: "0 0 6px var(--glow-cyan)" }}
        >
          <Home className="h-3 w-3" />
          원점 복귀
        </button>
      </div>

      {/* 에러 경고 */}
      {limitError && (
        <div className="flex items-center gap-2 mb-3 px-3 py-2 rounded-lg bg-destructive/10 border border-destructive/40 text-destructive text-xs font-mono">
          ⚠ 최대 회전각 초과 — 범위 내에서 조작하세요
        </div>
      )}

      {/* 각도 배지 */}
      <div className="flex justify-between items-end mb-4 px-1">
        <div className="text-center">
          <div className="text-[10px] font-mono text-muted-foreground mb-0.5">현재</div>
          <div
            className="text-2xl font-bold font-mono"
            style={{ color: "var(--neon-green)" }}
          >
            {currentAngle}°
          </div>
        </div>

        {/* 이동 중 표시 */}
        {isMoving && (
          <div className="flex items-center gap-1 text-xs font-mono text-amber-400 animate-pulse">
            {delta > 0 ? (
              <RotateCw className="h-3 w-3" />
            ) : (
              <RotateCcw className="h-3 w-3" />
            )}
            {Math.abs(delta)}° 남음
          </div>
        )}

        <div className="text-center">
          <div className="text-[10px] font-mono text-muted-foreground mb-0.5">목표</div>
          <div
            className="text-2xl font-bold font-mono"
            style={{ color: "var(--neon-cyan)" }}
          >
            {targetAngle}°
          </div>
        </div>
      </div>

      {/* 슬라이더 */}
      <div className="relative">
        <input
          ref={sliderRef}
          type="range"
          min={-BODY_MAX_DEG}
          max={BODY_MAX_DEG}
          step={BODY_STEP_DEG}
          value={targetAngle}
          onChange={handleChange}
          className="w-full h-2 appearance-none rounded-full cursor-pointer outline-none"
          style={{
            background: `linear-gradient(to right,
              var(--neon-cyan) 0%,
              var(--neon-cyan) ${fillPct}%,
              #1f2937 ${fillPct}%,
              #1f2937 100%)`,
          }}
        />

        {/* 현재 각도 표시선 */}
        <div
          className="absolute top-1/2 -translate-y-1/2 w-0.5 h-4 bg-neon-green/70 rounded pointer-events-none"
          style={{
            left: `calc(${((currentAngle + BODY_MAX_DEG) / (BODY_MAX_DEG * 2)) * 100}% - 1px)`,
          }}
        />
      </div>

      {/* 범위 레이블 */}
      <div className="flex justify-between mt-1 px-0.5">
        <span className="text-[10px] font-mono text-muted-foreground">-{BODY_MAX_DEG}°</span>
        <span className="text-[10px] font-mono text-muted-foreground">0°</span>
        <span className="text-[10px] font-mono text-muted-foreground">+{BODY_MAX_DEG}°</span>
      </div>

      {/* 슬라이더 thumb 스타일 */}
      <style>{`
        input[type='range']::-webkit-slider-thumb {
          -webkit-appearance: none;
          width: 20px;
          height: 20px;
          border-radius: 50%;
          background: var(--neon-cyan);
          box-shadow: 0 0 8px var(--neon-cyan);
          cursor: grab;
          border: 2px solid #0a0e1a;
        }
        input[type='range']::-webkit-slider-thumb:active {
          cursor: grabbing;
          box-shadow: 0 0 16px var(--neon-cyan);
        }
        input[type='range']::-moz-range-thumb {
          width: 20px;
          height: 20px;
          border-radius: 50%;
          background: var(--neon-cyan);
          box-shadow: 0 0 8px var(--neon-cyan);
          border: 2px solid #0a0e1a;
          cursor: grab;
        }
      `}</style>
    </div>
  );
}
