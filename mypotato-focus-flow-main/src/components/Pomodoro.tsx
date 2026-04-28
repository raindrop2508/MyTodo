import { useEffect, useRef, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Pause, Play, RotateCcw, X } from "lucide-react";
import { useApp } from "@/store/useApp";
import { useT } from "@/lib/i18n";
import { formatClock } from "@/lib/format";

interface Props {
  open: boolean;
  onClose: () => void;
  taskId: string;
  stepId?: string;
  title: string;
}

const Pomodoro = ({ open, onClose, taskId, stepId, title }: Props) => {
  const t = useT();
  const settings = useApp((s) => s.settings);
  const addSession = useApp((s) => s.addSession);

  const [mode, setMode] = useState<"work" | "break">("work");
  const [running, setRunning] = useState(false);
  const [remaining, setRemaining] = useState(settings.workMin * 60);
  const startedRef = useRef<number | null>(null);

  const total = (mode === "work" ? settings.workMin : settings.breakMin) * 60;
  const pct = 1 - remaining / total;

  useEffect(() => {
    if (open) {
      setMode("work");
      setRunning(false);
      setRemaining(settings.workMin * 60);
      startedRef.current = null;
    }
  }, [open, settings.workMin]);

  useEffect(() => {
    if (!running) return;
    const id = setInterval(() => {
      setRemaining((r) => {
        if (r <= 1) {
          const dur = total;
          addSession({
            taskId,
            stepId,
            kind: mode,
            startedAt: startedRef.current ?? Date.now() - dur * 1000,
            endedAt: Date.now(),
            durationSec: dur,
          });
          const next = mode === "work" ? "break" : "work";
          setMode(next);
          startedRef.current = Date.now();
          return (next === "work" ? settings.workMin : settings.breakMin) * 60;
        }
        return r - 1;
      });
    }, 1000);
    return () => clearInterval(id);
  }, [running, mode, total, taskId, stepId, settings, addSession]);

  const toggle = () => {
    if (!running) startedRef.current = startedRef.current ?? Date.now();
    setRunning((r) => !r);
  };
  const reset = () => {
    setRunning(false);
    setRemaining(total);
    startedRef.current = null;
  };

  const R = 120;
  const C = 2 * Math.PI * R;

  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 z-50 bg-foreground/60 backdrop-blur-md"
          />
          <motion.div
            initial={{ y: 40, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: 20, opacity: 0 }}
            className="fixed inset-x-0 bottom-0 top-0 z-50 mx-auto flex max-w-xl flex-col items-center justify-center p-6"
          >
            <button
              onClick={onClose}
              className="absolute right-5 top-5 flex h-10 w-10 items-center justify-center rounded-full bg-card/80 text-foreground shadow-soft backdrop-blur"
            >
              <X className="h-5 w-5" />
            </button>

            <p className="mb-2 text-xs font-bold uppercase tracking-[0.25em] text-primary">
              {mode === "work" ? t("work") : t("break")}
            </p>
            <p className="mb-8 max-w-xs text-center text-sm text-muted-foreground">{title}</p>

            <div className="relative">
              <svg width="280" height="280" className="-rotate-90">
                <circle
                  cx="140"
                  cy="140"
                  r={R}
                  fill="none"
                  stroke="hsl(var(--muted))"
                  strokeWidth="14"
                />
                <motion.circle
                  cx="140"
                  cy="140"
                  r={R}
                  fill="none"
                  stroke="hsl(var(--primary))"
                  strokeWidth="14"
                  strokeLinecap="round"
                  strokeDasharray={C}
                  strokeDashoffset={C * (1 - pct)}
                  style={{ transition: "stroke-dashoffset 0.9s linear" }}
                />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center">
                <p className="text-6xl font-extrabold tabular-nums tracking-tight">
                  {formatClock(remaining)}
                </p>
                <p className="mt-1 text-xs text-muted-foreground">
                  {Math.round(pct * 100)}%
                </p>
              </div>
            </div>

            <div className="mt-10 flex items-center gap-4">
              <button
                onClick={reset}
                className="flex h-14 w-14 items-center justify-center rounded-full bg-card shadow-elev"
              >
                <RotateCcw className="h-5 w-5" />
              </button>
              <button
                onClick={toggle}
                className="flex h-20 w-20 items-center justify-center rounded-full bg-gradient-primary text-primary-foreground shadow-pop"
              >
                {running ? <Pause className="h-8 w-8" /> : <Play className="h-8 w-8 translate-x-0.5" />}
              </button>
              <div className="h-14 w-14" />
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
};

export default Pomodoro;