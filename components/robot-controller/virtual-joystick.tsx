"use client";

import { useRef, useState, useCallback, useEffect } from "react";
import { ArrowUp, ArrowDown, ArrowLeft, ArrowRight } from "lucide-react";

interface JoystickPosition {
  x: number;
  y: number;
}

interface VirtualJoystickProps {
  onMove?: (position: JoystickPosition) => void;
  onRelease?: () => void;
}

export function VirtualJoystick({ onMove, onRelease }: VirtualJoystickProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [position, setPosition] = useState<JoystickPosition>({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);

  const maxDistance = 60;

  const handleMove = useCallback(
    (clientX: number, clientY: number) => {
      if (!containerRef.current) return;

      const rect = containerRef.current.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;

      let deltaX = clientX - centerX;
      let deltaY = clientY - centerY;

      const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

      if (distance > maxDistance) {
        deltaX = (deltaX / distance) * maxDistance;
        deltaY = (deltaY / distance) * maxDistance;
      }

      const normalizedX = deltaX / maxDistance;
      const normalizedY = -deltaY / maxDistance;

      setPosition({ x: deltaX, y: deltaY });
      onMove?.({ x: normalizedX, y: normalizedY });
    },
    [onMove, maxDistance]
  );

  const handleStart = useCallback(
    (clientX: number, clientY: number) => {
      setIsDragging(true);
      handleMove(clientX, clientY);
    },
    [handleMove]
  );

  const handleEnd = useCallback(() => {
    setIsDragging(false);
    setPosition({ x: 0, y: 0 });
    onRelease?.();
  }, [onRelease]);

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (isDragging) {
        handleMove(e.clientX, e.clientY);
      }
    };

    const handleTouchMove = (e: TouchEvent) => {
      if (isDragging && e.touches[0]) {
        e.preventDefault();
        handleMove(e.touches[0].clientX, e.touches[0].clientY);
      }
    };

    const handleMouseUp = () => {
      if (isDragging) {
        handleEnd();
      }
    };

    const handleTouchEnd = () => {
      if (isDragging) {
        handleEnd();
      }
    };

    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);
    window.addEventListener("touchmove", handleTouchMove, { passive: false });
    window.addEventListener("touchend", handleTouchEnd);

    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
      window.removeEventListener("touchmove", handleTouchMove);
      window.removeEventListener("touchend", handleTouchEnd);
    };
  }, [isDragging, handleMove, handleEnd]);

  const getDirectionOpacity = (direction: "up" | "down" | "left" | "right") => {
    const threshold = 0.3;
    const normalizedX = position.x / maxDistance;
    const normalizedY = -position.y / maxDistance;

    switch (direction) {
      case "up":
        return normalizedY > threshold ? 1 : 0.3;
      case "down":
        return normalizedY < -threshold ? 1 : 0.3;
      case "left":
        return normalizedX < -threshold ? 1 : 0.3;
      case "right":
        return normalizedX > threshold ? 1 : 0.3;
    }
  };

  return (
    <div className="flex flex-col items-center gap-4">
      <div className="text-xs font-mono uppercase tracking-widest text-muted-foreground">
        이동 제어
      </div>

      <div className="relative">
        {/* Direction indicators */}
        <div
          className="absolute -top-8 left-1/2 -translate-x-1/2 transition-opacity duration-150"
          style={{ opacity: getDirectionOpacity("up") }}
        >
          <ArrowUp className="h-6 w-6 text-neon-cyan" />
        </div>
        <div
          className="absolute -bottom-8 left-1/2 -translate-x-1/2 transition-opacity duration-150"
          style={{ opacity: getDirectionOpacity("down") }}
        >
          <ArrowDown className="h-6 w-6 text-neon-cyan" />
        </div>
        <div
          className="absolute -left-8 top-1/2 -translate-y-1/2 transition-opacity duration-150"
          style={{ opacity: getDirectionOpacity("left") }}
        >
          <ArrowLeft className="h-6 w-6 text-neon-cyan" />
        </div>
        <div
          className="absolute -right-8 top-1/2 -translate-y-1/2 transition-opacity duration-150"
          style={{ opacity: getDirectionOpacity("right") }}
        >
          <ArrowRight className="h-6 w-6 text-neon-cyan" />
        </div>

        {/* Joystick base */}
        <div
          ref={containerRef}
          className="relative h-40 w-40 rounded-full bg-secondary/30 border-2 border-border"
          style={{
            boxShadow: isDragging
              ? "inset 0 0 30px var(--glow-cyan), 0 0 20px var(--glow-cyan)"
              : "inset 0 0 20px rgba(0,0,0,0.3)",
          }}
          onMouseDown={(e) => handleStart(e.clientX, e.clientY)}
          onTouchStart={(e) => {
            if (e.touches[0]) {
              handleStart(e.touches[0].clientX, e.touches[0].clientY);
            }
          }}
        >
          {/* Grid lines */}
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div className="absolute h-full w-px bg-border/50" />
            <div className="absolute w-full h-px bg-border/50" />
            <div className="absolute h-[70%] w-[70%] rounded-full border border-border/30" />
          </div>

          {/* Joystick knob */}
          <div
            className={`absolute top-1/2 left-1/2 h-16 w-16 rounded-full transition-shadow duration-150 cursor-grab ${
              isDragging ? "cursor-grabbing" : ""
            }`}
            style={{
              transform: `translate(calc(-50% + ${position.x}px), calc(-50% + ${position.y}px))`,
              background: `radial-gradient(circle at 30% 30%, var(--neon-cyan), var(--accent))`,
              boxShadow: isDragging
                ? "0 0 20px var(--neon-cyan), 0 0 40px var(--glow-cyan)"
                : "0 0 10px var(--neon-cyan)",
            }}
          >
            <div className="absolute inset-2 rounded-full bg-background/20" />
          </div>
        </div>
      </div>

      {/* Coordinates display */}
      <div className="flex gap-4 font-mono text-xs text-muted-foreground">
        <span>
          X: <span className="text-neon-cyan">{(position.x / maxDistance).toFixed(2)}</span>
        </span>
        <span>
          Y: <span className="text-neon-cyan">{(-position.y / maxDistance).toFixed(2)}</span>
        </span>
      </div>
    </div>
  );
}
