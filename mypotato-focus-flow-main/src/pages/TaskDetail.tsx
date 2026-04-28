import { useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Check, Edit3, Plus, Trash2, Timer, Flame, Star, Hourglass } from "lucide-react";
import { useApp } from "@/store/useApp";
import { useT } from "@/lib/i18n";
import TaskEditor from "@/components/TaskEditor";
import Pomodoro from "@/components/Pomodoro";
import { cn } from "@/lib/utils";

const TaskDetail = () => {
  const { id } = useParams();
  const nav = useNavigate();
  const t = useT();
  const task = useApp((s) => s.tasks.find((x) => x.id === id));
  const steps = useApp((s) => s.steps.filter((x) => x.taskId === id).sort((a, b) => a.order - b.order));
  const cat = useApp((s) => s.categories.find((c) => c.id === task?.categoryId));
  const sessions = useApp((s) => s.sessions.filter((x) => x.taskId === id && x.kind === "work"));
  const addStep = useApp((s) => s.addStep);
  const toggleStep = useApp((s) => s.toggleStep);
  const deleteStep = useApp((s) => s.deleteStep);
  const updateStep = useApp((s) => s.updateStep);
  const toggleTask = useApp((s) => s.toggleTask);
  const deleteTask = useApp((s) => s.deleteTask);

  const [editing, setEditing] = useState(false);
  const [newStep, setNewStep] = useState("");
  const [pomoOpen, setPomoOpen] = useState(false);
  const [activeStepId, setActiveStepId] = useState<string | undefined>();

  const focusMin = useMemo(
    () => Math.round(sessions.reduce((a, b) => a + b.durationSec, 0) / 60),
    [sessions],
  );

  if (!task) {
    return (
      <div className="px-5 py-10">
        <button onClick={() => nav(-1)} className="text-sm text-muted-foreground">
          ← Back
        </button>
        <p className="mt-6 text-center text-muted-foreground">Task not found.</p>
      </div>
    );
  }

  const done = task.status === "done";

  return (
    <>
      <header className="flex items-center justify-between px-5 pt-6 pb-4">
        <button
          onClick={() => nav(-1)}
          className="flex h-10 w-10 items-center justify-center rounded-full bg-card shadow-soft"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div className="flex gap-2">
          <button
            onClick={() => setEditing(true)}
            className="flex h-10 w-10 items-center justify-center rounded-full bg-card shadow-soft"
          >
            <Edit3 className="h-4.5 w-4.5" />
          </button>
          <button
            onClick={() => {
              if (confirm(t("confirmDelete"))) {
                deleteTask(task.id);
                nav(-1);
              }
            }}
            className="flex h-10 w-10 items-center justify-center rounded-full bg-card text-destructive shadow-soft"
          >
            <Trash2 className="h-4.5 w-4.5" />
          </button>
        </div>
      </header>

      <div className="px-5">
        <div className="rounded-3xl bg-card p-5 shadow-elev">
          <div className="flex items-start gap-3">
            <button
              onClick={() => toggleTask(task.id)}
              className={cn(
                "mt-1 flex h-7 w-7 shrink-0 items-center justify-center rounded-full border-2 transition",
                done
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-muted-foreground/40",
              )}
            >
              {done && <Check className="h-4 w-4" strokeWidth={3} />}
            </button>
            <div className="flex-1">
              <h1 className={cn("text-xl font-bold leading-tight", done && "line-through text-muted-foreground")}>
                {task.title}
              </h1>
              {task.content && <p className="mt-2 text-sm text-muted-foreground">{task.content}</p>}
            </div>
          </div>

          <div className="mt-4 flex flex-wrap gap-2">
            {cat && (
              <span
                className="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium"
                style={{ background: `hsl(${cat.color} / 0.15)`, color: `hsl(${cat.color})` }}
              >
                {cat.name}
              </span>
            )}
            <Pill icon={Hourglass} label={task.type === "long" ? t("longTask") : t("oneTime")} />
            {task.urgent && <Pill icon={Flame} label={t("urgent")} tone="u" />}
            {task.important && <Pill icon={Star} label={t("important")} tone="i" />}
          </div>

          {task.note && (
            <div className="mt-4 rounded-2xl bg-muted/60 p-3 text-sm">
              <p className="mb-1 text-[10.5px] font-bold uppercase tracking-wider text-muted-foreground">
                {t("note")}
              </p>
              {task.note}
            </div>
          )}
        </div>

        {task.type === "long" && (
          <div className="mt-4 grid grid-cols-2 gap-3">
            <button
              onClick={() => {
                setActiveStepId(undefined);
                setPomoOpen(true);
              }}
              className="flex items-center justify-center gap-2 rounded-2xl bg-gradient-primary py-3 text-sm font-semibold text-primary-foreground shadow-elev"
            >
              <Timer className="h-4 w-4" />
              {t("start")} {t("pomodoro")}
            </button>
            <div className="rounded-2xl border border-border bg-card p-3 text-center">
              <p className="text-2xl font-extrabold leading-none">{focusMin}</p>
              <p className="mt-1 text-[11px] text-muted-foreground">
                {t("focusTime")} ({t("minutes")})
              </p>
            </div>
          </div>
        )}

        <section className="mt-6">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-bold uppercase tracking-wider text-muted-foreground">
              {t("steps")}
            </h2>
            <span className="text-xs text-muted-foreground">
              {steps.filter((s) => s.done).length}/{steps.length}
            </span>
          </div>

          <div className="space-y-2">
            {steps.map((s) => (
              <div
                key={s.id}
                className="group flex items-center gap-3 rounded-2xl border border-border/60 bg-card p-3 shadow-soft"
              >
                <button
                  onClick={() => toggleStep(s.id)}
                  className={cn(
                    "flex h-5 w-5 shrink-0 items-center justify-center rounded-md border-2",
                    s.done
                      ? "border-primary bg-primary text-primary-foreground"
                      : "border-muted-foreground/40",
                  )}
                >
                  {s.done && <Check className="h-3 w-3" strokeWidth={3} />}
                </button>
                <input
                  value={s.title}
                  onChange={(e) => updateStep(s.id, { title: e.target.value })}
                  className={cn(
                    "flex-1 bg-transparent text-sm outline-none",
                    s.done && "text-muted-foreground line-through",
                  )}
                />
                {task.type === "long" && (
                  <button
                    onClick={() => {
                      setActiveStepId(s.id);
                      setPomoOpen(true);
                    }}
                    className="rounded-full p-1.5 text-muted-foreground hover:bg-muted hover:text-primary"
                  >
                    <Timer className="h-4 w-4" />
                  </button>
                )}
                <button
                  onClick={() => deleteStep(s.id)}
                  className="rounded-full p-1.5 text-muted-foreground opacity-0 transition group-hover:opacity-100 hover:bg-destructive/10 hover:text-destructive"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}

            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (!newStep.trim()) return;
                addStep({ taskId: task.id, title: newStep.trim() });
                setNewStep("");
              }}
              className="flex items-center gap-2 rounded-2xl border border-dashed border-border bg-card/50 p-3"
            >
              <Plus className="h-4 w-4 text-muted-foreground" />
              <input
                value={newStep}
                onChange={(e) => setNewStep(e.target.value)}
                placeholder={t("addStep")}
                className="flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
              />
            </form>
          </div>
        </section>
      </div>

      <TaskEditor open={editing} onClose={() => setEditing(false)} task={task} />
      <Pomodoro
        open={pomoOpen}
        onClose={() => setPomoOpen(false)}
        taskId={task.id}
        stepId={activeStepId}
        title={task.title}
      />
    </>
  );
};

const Pill = ({
  icon: Icon,
  label,
  tone,
}: {
  icon: typeof Flame;
  label: string;
  tone?: "u" | "i";
}) => (
  <span
    className={cn(
      "inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-medium",
      tone === "u" && "bg-priority-u/15 text-priority-u",
      tone === "i" && "bg-priority-i/15 text-priority-i",
      !tone && "bg-muted text-muted-foreground",
    )}
  >
    <Icon className="h-3 w-3" />
    {label}
  </span>
);

export default TaskDetail;