export type TaskType = "one-time" | "long";
export type TaskStatus = "todo" | "in-progress" | "done" | "archived";
export type Priority = "ui" | "i" | "u" | "n"; // urgent+important, important, urgent, neither

export interface Category {
  id: string;
  name: string;
  color: string; // hsl tailwind class fragment, e.g. "32 78% 52%"
  icon?: string;
}

export interface TaskStep {
  id: string;
  taskId: string;
  title: string;
  note?: string;
  done: boolean;
  order: number;
  createdAt: number;
  completedAt?: number;
}

export interface Task {
  id: string;
  type: TaskType;
  title: string;
  content?: string;
  note?: string;
  categoryId?: string;
  urgent: boolean;
  important: boolean;
  status: TaskStatus;
  dueAt?: number;
  createdAt: number;
  updatedAt: number;
  completedAt?: number;
}

export interface PomodoroSession {
  id: string;
  taskId: string;
  stepId?: string;
  startedAt: number;
  endedAt: number;
  durationSec: number;
  kind: "work" | "break";
}

export type ThemeMode = "system" | "light" | "dark";
export type Lang = "en" | "zh";

export interface Settings {
  theme: ThemeMode;
  lang: Lang;
  workMin: number;
  breakMin: number;
}

export const priorityOf = (t: Task): Priority =>
  t.urgent && t.important ? "ui" : t.important ? "i" : t.urgent ? "u" : "n";