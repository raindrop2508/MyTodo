import type { Task } from "@/types";

export const download = (filename: string, content: string, mime: string) => {
  const blob = new Blob([content], { type: mime });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
};

export const tasksToCsv = (tasks: Task[]) => {
  const head = ["id", "type", "title", "content", "note", "categoryId", "urgent", "important", "status", "createdAt", "completedAt"];
  const esc = (v: unknown) => {
    const s = v == null ? "" : String(v);
    return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  };
  const rows = tasks.map((t) =>
    head.map((h) => esc((t as unknown as Record<string, unknown>)[h])).join(","),
  );
  return [head.join(","), ...rows].join("\n");
};