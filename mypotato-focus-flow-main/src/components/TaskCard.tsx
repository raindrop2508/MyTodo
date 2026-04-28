import { motion, useMotionValue, useTransform, animate } from "framer-motion";
import { Check, Clock, Flame, Star, Hourglass } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useApp } from "@/store/useApp";
import type { Task } from "@/types";
import { cn } from "@/lib/utils";
import { useMemo } from "react";

const priorityRing: Record<string, string> = {
  ui: "bg-priority-ui",
  i: "bg-priority-i",
  u: "bg-priority-u",
  n: "bg-priority-n",
};

const TaskCard = ({ task }: { task: Task }) => {
  const nav = useNavigate();
  const toggleTask = useApp((s) => s.toggleTask);
  const steps = useApp((s) => s.steps.filter((st) => st.taskId === task.id));
  const cat = useApp((s) => s.categories.find((c) => c.id === task.categoryId));

  const x = useMotionValue(0);
  const bg = useTransform(x, [0, 120], ["hsl(var(--card))", "hsl(var(--success) / 0.18)"]);
  const checkOpacity = useTransform(x, [40, 110], [0, 1]);

  const done = task.status === "done";
  const priority =
    task.urgent && task.important ? "ui" : task.important ? "i" : task.urgent ? "u" : "n";

  const progress = useMemo(() => {
    if (steps.length === 0) return null;
    const d = steps.filter((s) => s.done).length;
    return { d, total: steps.length, pct: Math.round((d / steps.length) * 100) };
  }, [steps]);

  return (
    <motion.div className="relative">
      <div className="absolute inset-0 flex items-center rounded-2xl bg-success/15 pl-5">
        <motion.div style={{ opacity: checkOpacity }} className="flex items-center gap-2 text-success">
          <Check className="h-5 w-5" />
          <span className="text-sm font-medium">Complete</span>
        </motion.div>
      </div>
      <motion.div
        drag="x"
        dragConstraints={{ left: 0, right: 140 }}
        dragElastic={0.15}
        style={{ x, background: bg }}
        onDragEnd={(_, info) => {
          if (info.offset.x > 90) {
            toggleTask(task.id);
            animate(x, 0, { duration: 0.25 });
          } else {
            animate(x, 0, { type: "spring", stiffness: 400, damping: 30 });
          }
        }}
        onClick={() => nav(`/task/${task.id}`)}
        className="relative flex cursor-pointer items-start gap-3 rounded-2xl border border-border/60 p-4 shadow-soft active:cursor-grabbing"
      >
        <button
          onClick={(e) => {
            e.stopPropagation();
            toggleTask(task.id);
          }}
          className={cn(
            "mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2 transition-all",
            done
              ? "border-primary bg-primary text-primary-foreground"
              : "border-muted-foreground/40 hover:border-primary",
          )}
        >
          {done && <Check className="h-3.5 w-3.5" strokeWidth={3} />}
        </button>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span className={cn("h-2 w-2 shrink-0 rounded-full", priorityRing[priority])} />
            <h3
              className={cn(
                "truncate text-[15px] font-semibold leading-tight",
                done && "text-muted-foreground line-through decoration-muted-foreground/60",
              )}
            >
              {task.title}
            </h3>
          </div>

          {task.content && (
            <p className="mt-1 line-clamp-1 text-[13px] text-muted-foreground">{task.content}</p>
          )}

          <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-[11.5px] text-muted-foreground">
            {cat && (
              <span className="inline-flex items-center gap-1.5">
                <span
                  className="h-1.5 w-1.5 rounded-full"
                  style={{ background: `hsl(${cat.color})` }}
                />
                {cat.name}
              </span>
            )}
            {task.type === "long" && (
              <span className="inline-flex items-center gap-1">
                <Hourglass className="h-3 w-3" /> Long
              </span>
            )}
            {task.urgent && (
              <span className="inline-flex items-center gap-1 text-priority-u">
                <Flame className="h-3 w-3" /> Urgent
              </span>
            )}
            {task.important && (
              <span className="inline-flex items-center gap-1 text-priority-i">
                <Star className="h-3 w-3" /> Important
              </span>
            )}
            {progress && (
              <span className="inline-flex items-center gap-1">
                <Clock className="h-3 w-3" />
                {progress.d}/{progress.total}
              </span>
            )}
          </div>

          {progress && (
            <div className="mt-2 h-1 w-full overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-primary transition-all"
                style={{ width: `${progress.pct}%` }}
              />
            </div>
          )}
        </div>
      </motion.div>
    </motion.div>
  );
};

export default TaskCard;