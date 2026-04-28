import { useMemo, useState } from "react";
import { useApp } from "@/store/useApp";
import { useT } from "@/lib/i18n";
import PageHeader from "@/components/layout/PageHeader";
import TaskCard from "@/components/TaskCard";
import EmptyState from "@/components/EmptyState";
import Fab from "@/components/Fab";
import TaskEditor from "@/components/TaskEditor";
import { cn } from "@/lib/utils";
import { priorityOf } from "@/types";

type StatusFilter = "all" | "todo" | "done";
type TypeFilter = "all" | "one-time" | "long";
type QuadFilter = "all" | "ui" | "i" | "u" | "n";

const Tasks = () => {
  const t = useT();
  const tasks = useApp((s) => s.tasks);
  const [status, setStatus] = useState<StatusFilter>("all");
  const [typeF, setTypeF] = useState<TypeFilter>("all");
  const [quad, setQuad] = useState<QuadFilter>("all");
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");

  const filtered = useMemo(() => {
    return tasks.filter((tk) => {
      if (status === "todo" && tk.status === "done") return false;
      if (status === "done" && tk.status !== "done") return false;
      if (typeF !== "all" && tk.type !== typeF) return false;
      if (quad !== "all" && priorityOf(tk) !== quad) return false;
      if (q && !tk.title.toLowerCase().includes(q.toLowerCase())) return false;
      return true;
    });
  }, [tasks, status, typeF, quad, q]);

  const statusFilters: { id: StatusFilter; label: string }[] = [
    { id: "all", label: "All" },
    { id: "todo", label: t("pending") },
    { id: "done", label: t("completed") },
  ];
  const typeFilters: { id: TypeFilter; label: string }[] = [
    { id: "all", label: "All types" },
    { id: "one-time", label: t("oneTime") },
    { id: "long", label: t("longTask") },
  ];
  const quadFilters: { id: QuadFilter; label: string; dot?: string }[] = [
    { id: "all", label: "All priorities" },
    { id: "ui", label: t("quadUI"), dot: "ui" },
    { id: "i", label: t("quadI"), dot: "i" },
    { id: "u", label: t("quadU"), dot: "u" },
    { id: "n", label: t("quadN"), dot: "n" },
  ];

  return (
    <>
      <PageHeader title={t("tasks")} subtitle={`${tasks.length} total`} />

      <div className="px-5">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search tasks…"
          className="mb-3 w-full rounded-2xl border border-border bg-card px-4 py-3 text-sm outline-none focus:border-primary"
        />
        <div className="mb-3 space-y-2">
          <FilterRow
            items={statusFilters}
            active={status}
            onSelect={(v) => setStatus(v as StatusFilter)}
          />
          <FilterRow
            items={typeFilters}
            active={typeF}
            onSelect={(v) => setTypeF(v as TypeFilter)}
          />
          <FilterRow
            items={quadFilters}
            active={quad}
            onSelect={(v) => setQuad(v as QuadFilter)}
          />
        </div>

        {filtered.length === 0 ? (
          <EmptyState title={t("empty")} hint={t("emptyTasks")} />
        ) : (
          <div className="space-y-2">
            {filtered.map((tk) => (
              <TaskCard key={tk.id} task={tk} />
            ))}
          </div>
        )}
      </div>

      <Fab onClick={() => setOpen(true)} label={t("addTask")} variant="circle" />
      <TaskEditor open={open} onClose={() => setOpen(false)} />
    </>
  );
};

interface FilterRowProps {
  items: { id: string; label: string; dot?: string }[];
  active: string;
  onSelect: (v: string) => void;
}
const FilterRow = ({ items, active, onSelect }: FilterRowProps) => (
  <div className="no-scrollbar flex gap-2 overflow-x-auto">
    {items.map((f) => (
      <button
        key={f.id}
        onClick={() => onSelect(f.id)}
        className={cn(
          "flex shrink-0 items-center gap-1.5 rounded-full border px-3.5 py-1.5 text-xs font-semibold transition",
          active === f.id
            ? "border-primary bg-primary text-primary-foreground"
            : "border-border bg-card text-muted-foreground",
        )}
      >
        {f.dot && <span className={`h-2 w-2 rounded-full bg-priority-${f.dot}`} />}
        {f.label}
      </button>
    ))}
  </div>
);

export default Tasks;