import { useMemo, useState } from "react";
import { motion } from "framer-motion";
import { useApp } from "@/store/useApp";
import { useT } from "@/lib/i18n";
import { isSameDay, daysAgo } from "@/lib/format";
import PageHeader from "@/components/layout/PageHeader";
import TaskCard from "@/components/TaskCard";
import EmptyState from "@/components/EmptyState";
import Fab from "@/components/Fab";
import TaskEditor from "@/components/TaskEditor";
import { priorityOf } from "@/types";
import { cn } from "@/lib/utils";

const QUAD_ORDER = ["ui", "i", "u", "n"] as const;
const labelKey = { ui: "quadUI", i: "quadI", u: "quadU", n: "quadN" } as const;
type QuadFilter = "all" | "ui" | "i" | "u" | "n";

const Index = () => {
  const t = useT();
  const tasks = useApp((s) => s.tasks);
  const sessions = useApp((s) => s.sessions);
  const [open, setOpen] = useState(false);
  const [quad, setQuad] = useState<QuadFilter>("all");

  const todayTasks = useMemo(
    () =>
      tasks.filter(
        (x) =>
          x.status !== "archived" &&
          (x.status !== "done" || (x.completedAt && isSameDay(x.completedAt, Date.now()))),
      ),
    [tasks],
  );

  const grouped = useMemo(() => {
    const g: Record<string, typeof tasks> = { ui: [], i: [], u: [], n: [] };
    for (const tk of todayTasks) g[priorityOf(tk)].push(tk);
    return g;
  }, [todayTasks]);

  const visibleQuads: readonly ("ui" | "i" | "u" | "n")[] =
    quad === "all" ? QUAD_ORDER : [quad];
  const totalVisible = visibleQuads.reduce((a, q) => a + grouped[q].length, 0);

  const stats = useMemo(() => {
    const today = Date.now();
    const todayDone = tasks.filter((x) => x.completedAt && isSameDay(x.completedAt, today)).length;
    const focusMin = Math.round(
      sessions
        .filter((s) => s.kind === "work" && isSameDay(s.endedAt, today))
        .reduce((a, b) => a + b.durationSec, 0) / 60,
    );
    let streak = 0;
    for (let i = 0; i < 60; i++) {
      const day = daysAgo(i).getTime();
      const has = tasks.some((x) => x.completedAt && isSameDay(x.completedAt, day));
      if (has) streak++;
      else if (i > 0) break;
    }
    return { todayDone, focusMin, streak };
  }, [tasks, sessions]);

  const dateStr = new Date().toLocaleDateString(undefined, {
    weekday: "long",
    month: "long",
    day: "numeric",
  });

  return (
    <>
      <PageHeader title={t("today")} subtitle={dateStr} />

      <section className="mx-5 mb-6 overflow-hidden rounded-3xl bg-gradient-primary p-5 text-primary-foreground shadow-pop">
        <p className="text-xs font-semibold uppercase tracking-widest opacity-80">{t("welcome")}</p>
        <p className="mt-1 text-lg font-semibold leading-snug">{t("tagline")}</p>
        <div className="mt-4 grid grid-cols-3 gap-3">
          <Stat n={stats.todayDone} label={t("completed")} />
          <Stat n={`${stats.focusMin}`} label={`${t("focusTime")} (${t("minutes")})`} />
          <Stat n={stats.streak} label={t("streak")} />
        </div>
      </section>

      <div className="px-5">
        <div className="no-scrollbar mb-4 flex gap-2 overflow-x-auto">
          {(["all", ...QUAD_ORDER] as QuadFilter[]).map((q) => (
            <button
              key={q}
              onClick={() => setQuad(q)}
              className={cn(
                "flex shrink-0 items-center gap-1.5 rounded-full border px-3.5 py-1.5 text-xs font-semibold transition",
                quad === q
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-card text-muted-foreground",
              )}
            >
              {q !== "all" && (
                <span className={`h-2 w-2 rounded-full bg-priority-${q}`} />
              )}
              {q === "all" ? "All" : t(labelKey[q])}
            </button>
          ))}
        </div>

        {totalVisible === 0 ? (
          <EmptyState title={t("empty")} hint={t("emptyToday")} />
        ) : (
          <div className="space-y-6">
            {visibleQuads.map((q) =>
              grouped[q].length > 0 ? (
                <motion.div
                  key={q}
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                >
                  <div className="mb-2 flex items-center gap-2">
                    <span className={`h-2.5 w-2.5 rounded-full bg-priority-${q}`} />
                    <h2 className="text-sm font-bold tracking-tight">
                      {t(labelKey[q])}
                    </h2>
                    <span className="text-xs text-muted-foreground">{grouped[q].length}</span>
                  </div>
                  <div className="space-y-2">
                    {grouped[q].map((tk) => (
                      <TaskCard key={tk.id} task={tk} />
                    ))}
                  </div>
                </motion.div>
              ) : null,
            )}
          </div>
        )}
      </div>

      <Fab onClick={() => setOpen(true)} label={t("addTask")} variant="circle" />
      <TaskEditor open={open} onClose={() => setOpen(false)} />
    </>
  );
};

const Stat = ({ n, label }: { n: number | string; label: string }) => (
  <div>
    <p className="text-2xl font-extrabold leading-none">{n}</p>
    <p className="mt-1 text-[11px] font-medium opacity-80">{label}</p>
  </div>
);

export default Index;
