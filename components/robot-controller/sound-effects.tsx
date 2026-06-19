"use client";

import { useState } from "react";
import { Hand, Music, Bell } from "lucide-react";

interface SoundButtonProps {
  icon: React.ReactNode;
  label: string;
  color: "cyan" | "green" | "orange";
  onClick?: () => void;
}

function SoundButton({ icon, label, color, onClick }: SoundButtonProps) {
  const [isPressed, setIsPressed] = useState(false);

  const colorClasses = {
    cyan: {
      bg: "bg-neon-cyan/10",
      border: "border-neon-cyan/50",
      text: "text-neon-cyan",
      glow: "var(--neon-cyan)",
      glowLight: "var(--glow-cyan)",
    },
    green: {
      bg: "bg-neon-green/10",
      border: "border-neon-green/50",
      text: "text-neon-green",
      glow: "var(--neon-green)",
      glowLight: "var(--glow-green)",
    },
    orange: {
      bg: "bg-neon-orange/10",
      border: "border-neon-orange/50",
      text: "text-neon-orange",
      glow: "var(--neon-orange)",
      glowLight: "oklch(0.75 0.20 55 / 0.5)",
    },
  };

  const colors = colorClasses[color];

  return (
    <button
      onClick={() => {
        setIsPressed(true);
        onClick?.();
        setTimeout(() => setIsPressed(false), 200);
      }}
      className={`flex flex-col items-center gap-2 px-4 py-3 rounded-xl border-2 transition-all duration-150 active:scale-95 ${colors.bg} ${colors.border}`}
      style={{
        boxShadow: isPressed
          ? `0 0 20px ${colors.glow}, inset 0 0 15px ${colors.glowLight}`
          : `0 0 10px ${colors.glowLight}`,
      }}
    >
      <div
        className={`p-2 rounded-lg ${colors.bg} ${colors.text}`}
        style={isPressed ? { filter: `drop-shadow(0 0 8px ${colors.glow})` } : {}}
      >
        {icon}
      </div>
      <span className={`text-xs font-medium ${colors.text}`}>{label}</span>
    </button>
  );
}

interface SoundEffectsProps {
  onSayHello?: () => void;
  onPlayMusic?: () => void;
  onHorn?: () => void;
}

export function SoundEffects({ onSayHello, onPlayMusic, onHorn }: SoundEffectsProps) {
  return (
    <div className="px-4 py-4 border-t border-border bg-card/50">
      <div className="text-xs font-mono uppercase tracking-widest text-muted-foreground text-center mb-4">
        사운드
      </div>
      
      <div className="grid grid-cols-3 gap-3">
        <SoundButton
          icon={<Hand className="h-6 w-6" />}
          label="인사하기"
          color="cyan"
          onClick={onSayHello}
        />
        <SoundButton
          icon={<Music className="h-6 w-6" />}
          label="음악 재생"
          color="green"
          onClick={onPlayMusic}
        />
        <SoundButton
          icon={<Bell className="h-6 w-6" />}
          label="경적"
          color="orange"
          onClick={onHorn}
        />
      </div>
    </div>
  );
}
