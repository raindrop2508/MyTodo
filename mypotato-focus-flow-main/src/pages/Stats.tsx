import { useMemo, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useApp } from "@/store/useApp";
import { useT } from "@/lib/i18n";
import PageHeader from "@/components/layout/PageHeader";
import { cn } from "@/lib/utils";
import { daysAgo, isSameDay } from "@/lib/format";

type Range = "daily" | "weekly" | "monthly";
const rangeDays: Record<Range, number> = { daily: 7, weekly: 28, monthly: 90 };

const Stats = () => {
  const t = useT();
  const tasks = useApp((s) => s.tasks);
  const steps = useApp((s) => s.steps);
  const sessions = useApp((s) => s.sessions);
  const categories = useApp((s) => s.categories);
  const [range, setRange] = useState<Range>("daily");

  const days = rangeDays[range];

  const focusByCat = useMemo(() => {
    const arr = [];
    for (let i = days - 1; i >= 0; i--) {
      const d = daysAgo(i);
      const row: Record<string, number | string> = {
        label: d.toLocaleDateString(undefined, { month: "short", day: "numeric" }),
      };
      for (const c of categories) row[c.name] = 0;
      row["Other"] = 0;
      for (const s of sessions) {
        if (s.kind !== "work" || !isSameDay(s.endedAt, d.getTime())) continue;
        const task = tasks.find((tk) => tk.id === s.taskId);
        const cat = categories.find((c) => c.id === task?.categoryId);
        const key = cat?.name ?? "Other";
        row[key] = ((row[key] as number) ?? 0) + s.durationSec / 60;
      }
      arr.push(row);
    }
    return arr;
  }, [sessions, tasks, categories, days]);

  const completion = useMemo(() => {
    const cutoff = daysAgo(days - 1).getTime();
    const inRange = tasks.filter((t) => t.createdAt >= cutoff || (t.completedAt ?? 0) >= cutoff);
    const done = inRange.filter((t) => t.status === "done").length;
    const pending = inRange.length - done;
    return [
      { name: t("completed"), value: done, color: "hsl(var(--primary))" },
      { name: t("pending"), value: pending, color: "hsl(var(--muted-foreground))" },
    ];
  }, [tasks, days, t]);

  const stepTimeline = useMemo(() => {
    const arr = [];
    for (let i = days - 1; i >= 0; i--) {
      const d = daysAgo(i).getTime();
      const count = steps.filter((s) => s.done && s.completedAt && isSameDay(s.completedAt, d)).length;
      arr.push({
        label: new Date(d).toLocaleDateString(undefined, { month: "short", day: "numeric" }),
        steps: count,
      });
    }
    return arr;
  }, [steps, days]);

  const totals = useMemo(() => {
    const focusMin = Math.round(
      sessions.filter((s) => s.kind === "work").reduce((a, b) => a + b.durationSec, 0) / 60,
    );
    const workSessions = sessions.filter((s) => s.kind === "work").length;
    const completedTasks = tasks.filter((t) => t.status === "done").length;
    return { focusMin, workSessions, completedTasks };
  }, [sessions, tasks]);

  const ranges: { id: Range; label: string }[] = [
    { id: "daily", label: t("daily") },
    { id: "weekly", label: t("weekly") },
    { id: "monthly", label: t("monthly") },
  ];

  const allCatNames = [...categories.map((c) => c.name), "Other"];
  const catColor = (name: string) => {
    const c = categories.find((x) => x.name === name);
    return c ? `hsl(${c.color})` : "hsl(var(--muted-foreground))";
  };

  return (
    <>
      <PageHeader title={t("stats")} subtitle={t("overview")} />

      <div className="px-5">
        <div className="no-scrollbar mb-4 flex gap-2 overflow-x-auto">
          {ranges.map((r) => (
            <button
              key={r.id}
              onClick={() => setRange(r.id)}
              className={cn(
                "shrink-0 rounded-full border px-4 py-1.5 text-xs font-semibold",
                range === r.id
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-card text-muted-foreground",
              )}
            >
              {r.label}
            </button>
          ))}
        </div>

        <div className="mb-4 grid grid-cols-3 gap-3">
          <KPI value={totals.focusMin} label={`${t("focusTime")} (${t("minutes")})`} />
          <KPI value={totals.workSessions} label={t("sessions")} />
          <KPI value={totals.completedTasks} label={t("completed")} />
        </div>

        <ChartCard title={t("timeDistribution")}>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={focusByCat} margin={{ top: 8, right: 4, left: -16, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
              <YAxis tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
              <Tooltip
                contentStyle={{
                  background: "hsl(var(--card))",
                  border: "1px solid hsl(var(--border))",
                  borderRadius: 12,
                  fontSize: 12,
                }}
              />
              {allCatNames.map((name) => (
                <Bar key={name} dataKey={name} stackId="a" fill={catColor(name)} radius={[6, 6, 0, 0]} />
              ))}
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title={t("completionRate")}>
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie
                data={completion}
                dataKey="value"
                innerRadius={50}
                outerRadius={80}
                paddingAngle={3}
              >
                {completion.map((d, i) => (
                  <Cell key={i} fill={d.color} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  background: "hsl(var(--card))",
                  border: "1px solid hsl(var(--border))",
                  borderRadius: 12,
                  fontSize: 12,
                }}
              />
            </PieChart>
          </ResponsiveContainer>
          <div className="mt-2 flex justify-center gap-4 text-xs">
            {completion.map((d) => (
              <span key={d.name} className="inline-flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full" style={{ background: d.color }} />
                {d.name}: <strong>{d.value}</strong>
              </span>
            ))}
          </div>
        </ChartCard>

        <ChartCard title={t("stepTimeline")}>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={stepTimeline} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
              <YAxis tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" allowDecimals={false} />
              <Tooltip
                contentStyle={{
                  background: "hsl(var(--card))",
                  border: "1px solid hsl(var(--border))",
                  borderRadius: 12,
                  fontSize: 12,
                }}
              />
              <Line
                type="monotone"
                dataKey="steps"
                stroke="hsl(var(--accent))"
                strokeWidth={3}
                dot={{ r: 3, fill: "hsl(var(--accent))" }}
                activeDot={{ r: 5 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>
    </>
  );
};

const KPI = ({ value, label }: { value: number | string; label: string }) => (
  <div className="rounded-2xl border border-border/60 bg-card p-3 text-center shadow-soft">
    <p className="text-2xl font-extrabold leading-none">{value}</p>
    <p className="mt-1 text-[10.5px] text-muted-foreground">{label}</p>
  </div>
);

const ChartCard = ({ title, children }: { title: string; children: React.ReactNode }) => (
  <div className="mb-4 rounded-3xl border border-border/60 bg-card p-4 shadow-soft">
    <h3 className="mb-3 text-sm font-bold">{title}</h3>
    {children}
  </div>
);

export default Stats;