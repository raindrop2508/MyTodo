import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Trash2 } from "lucide-react";
import type { Task, TaskType } from "@/types";
import { useApp } from "@/store/useApp";
import { useT } from "@/lib/i18n";
import { cn } from "@/lib/utils";

interface Props {
  open: boolean;
  onClose: () => void;
  task?: Task | null;
}

const TaskEditor = ({ open, onClose, task }: Props) => {
  const t = useT();
  const categories = useApp((s) => s.categories);
  const addTask = useApp((s) => s.addTask);
  const updateTask = useApp((s) => s.updateTask);
  const deleteTask = useApp((s) => s.deleteTask);

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [note, setNote] = useState("");
  const [type, setType] = useState<TaskType>("one-time");
  const [categoryId, setCategoryId] = useState<string | undefined>();
  const [urgent, setUrgent] = useState(false);
  const [important, setImportant] = useState(false);

  useEffect(() => {
    if (open) {
      setTitle(task?.title ?? "");
      setContent(task?.content ?? "");
      setNote(task?.note ?? "");
      setType(task?.type ?? "one-time");
      setCategoryId(task?.categoryId);
      setUrgent(task?.urgent ?? false);
      setImportant(task?.important ?? false);
    }
  }, [open, task]);

  const save = () => {
    if (!title.trim()) return;
    const data = {
      title: title.trim(),
      content: content.trim() || undefined,
      note: note.trim() || undefined,
      type,
      categoryId,
      urgent,
      important,
    };
    if (task) updateTask(task.id, data);
    else addTask(data);
    onClose();
  };

  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 z-50 bg-foreground/40 backdrop-blur-sm"
          />
          <motion.div
            initial={{ y: "100%" }}
            animate={{ y: 0 }}
            exit={{ y: "100%" }}
            transition={{ type: "spring", damping: 32, stiffness: 320 }}
            className="fixed inset-x-0 bottom-0 z-50 mx-auto max-h-[92vh] w-full max-w-xl overflow-y-auto rounded-t-3xl bg-card p-5 shadow-pop"
          >
            <div className="mx-auto mb-4 h-1.5 w-10 rounded-full bg-muted" />
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-xl font-bold">{task ? t("editTask") : t("newTask")}</h2>
              <button onClick={onClose} className="rounded-full p-1.5 text-muted-foreground hover:bg-muted">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-4">
              <input
                autoFocus
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder={t("title")}
                className="w-full rounded-2xl border border-border bg-background px-4 py-3 text-base font-medium outline-none focus:border-primary"
              />
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder={t("content")}
                rows={2}
                className="w-full resize-none rounded-2xl border border-border bg-background px-4 py-3 text-sm outline-none focus:border-primary"
              />

              <div>
                <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">{t("type")}</p>
                <div className="grid grid-cols-2 gap-2">
                  {(["one-time", "long"] as TaskType[]).map((tp) => (
                    <button
                      key={tp}
                      onClick={() => setType(tp)}
                      className={cn(
                        "rounded-xl border px-3 py-2.5 text-sm font-medium transition",
                        type === tp
                          ? "border-primary bg-primary-soft text-primary"
                          : "border-border bg-background text-foreground",
                      )}
                    >
                      {tp === "one-time" ? t("oneTime") : t("longTask")}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">{t("category")}</p>
                <div className="flex flex-wrap gap-2">
                  <button
                    onClick={() => setCategoryId(undefined)}
                    className={cn(
                      "rounded-full border px-3 py-1.5 text-xs font-medium",
                      !categoryId ? "border-primary bg-primary-soft text-primary" : "border-border",
                    )}
                  >
                    {t("none")}
                  </button>
                  {categories.map((c) => (
                    <button
                      key={c.id}
                      onClick={() => setCategoryId(c.id)}
                      className={cn(
                        "inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium",
                        categoryId === c.id ? "border-primary bg-primary-soft text-primary" : "border-border",
                      )}
                    >
                      <span className="h-2 w-2 rounded-full" style={{ background: `hsl(${c.color})` }} />
                      {c.name}
                    </button>
                  ))}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => setUrgent(!urgent)}
                  className={cn(
                    "rounded-xl border px-3 py-2.5 text-sm font-medium",
                    urgent ? "border-priority-u bg-priority-u/10 text-priority-u" : "border-border",
                  )}
                >
                  {t("urgent")}
                </button>
                <button
                  onClick={() => setImportant(!important)}
                  className={cn(
                    "rounded-xl border px-3 py-2.5 text-sm font-medium",
                    important ? "border-priority-i bg-priority-i/10 text-priority-i" : "border-border",
                  )}
                >
                  {t("important")}
                </button>
              </div>

              <textarea
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder={t("note")}
                rows={2}
                className="w-full resize-none rounded-2xl border border-border bg-background px-4 py-3 text-sm outline-none focus:border-primary"
              />
            </div>

            <div className="mt-6 flex items-center gap-2">
              {task && (
                <button
                  onClick={() => {
                    if (confirm(t("confirmDelete"))) {
                      deleteTask(task.id);
                      onClose();
                    }
                  }}
                  className="flex h-12 items-center justify-center rounded-2xl border border-destructive/30 px-4 text-destructive"
                >
                  <Trash2 className="h-5 w-5" />
                </button>
              )}
              <button
                onClick={onClose}
                className="h-12 flex-1 rounded-2xl border border-border text-sm font-semibold"
              >
                {t("cancel")}
              </button>
              <button
                onClick={save}
                disabled={!title.trim()}
                className="h-12 flex-1 rounded-2xl bg-gradient-primary text-sm font-semibold text-primary-foreground shadow-elev disabled:opacity-50"
              >
                {t("save")}
              </button>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
};

export default TaskEditor;