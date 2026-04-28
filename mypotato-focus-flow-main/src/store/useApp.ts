import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { Category, PomodoroSession, Settings, Task, TaskStep } from "@/types";

const uid = () => Math.random().toString(36).slice(2, 10) + Date.now().toString(36).slice(-4);

interface AppState {
  tasks: Task[];
  steps: TaskStep[];
  categories: Category[];
  sessions: PomodoroSession[];
  settings: Settings;

  addTask: (t: Omit<Task, "id" | "createdAt" | "updatedAt" | "status"> & { status?: Task["status"] }) => Task;
  updateTask: (id: string, patch: Partial<Task>) => void;
  deleteTask: (id: string) => void;
  toggleTask: (id: string) => void;

  addStep: (s: Omit<TaskStep, "id" | "createdAt" | "done" | "order"> & { done?: boolean }) => TaskStep;
  updateStep: (id: string, patch: Partial<TaskStep>) => void;
  deleteStep: (id: string) => void;
  toggleStep: (id: string) => void;

  addSession: (s: Omit<PomodoroSession, "id">) => void;

  addCategory: (c: Omit<Category, "id">) => Category;
  deleteCategory: (id: string) => void;

  setSettings: (patch: Partial<Settings>) => void;

  importData: (data: Partial<Pick<AppState, "tasks" | "steps" | "categories" | "sessions" | "settings">>) => void;
  resetAll: () => void;
}

const defaultCategories: Category[] = [
  { id: "c-work", name: "Work", color: "32 78% 52%" },
  { id: "c-life", name: "Life", color: "168 55% 42%" },
  { id: "c-study", name: "Study", color: "212 80% 56%" },
  { id: "c-health", name: "Health", color: "4 78% 56%" },
];

const defaultSettings: Settings = {
  theme: "system",
  lang: "en",
  workMin: 25,
  breakMin: 5,
};

export const useApp = create<AppState>()(
  persist(
    (set, get) => ({
      tasks: [],
      steps: [],
      categories: defaultCategories,
      sessions: [],
      settings: defaultSettings,

      addTask: (t) => {
        const now = Date.now();
        const task: Task = {
          id: uid(),
          createdAt: now,
          updatedAt: now,
          status: t.status ?? "todo",
          ...t,
        };
        set({ tasks: [task, ...get().tasks] });
        return task;
      },
      updateTask: (id, patch) =>
        set({
          tasks: get().tasks.map((t) =>
            t.id === id ? { ...t, ...patch, updatedAt: Date.now() } : t,
          ),
        }),
      deleteTask: (id) =>
        set({
          tasks: get().tasks.filter((t) => t.id !== id),
          steps: get().steps.filter((s) => s.taskId !== id),
        }),
      toggleTask: (id) => {
        const t = get().tasks.find((x) => x.id === id);
        if (!t) return;
        const done = t.status !== "done";
        get().updateTask(id, {
          status: done ? "done" : "todo",
          completedAt: done ? Date.now() : undefined,
        });
      },

      addStep: (s) => {
        const order = get().steps.filter((x) => x.taskId === s.taskId).length;
        const step: TaskStep = {
          id: uid(),
          createdAt: Date.now(),
          done: s.done ?? false,
          order,
          ...s,
        };
        set({ steps: [...get().steps, step] });
        return step;
      },
      updateStep: (id, patch) =>
        set({ steps: get().steps.map((s) => (s.id === id ? { ...s, ...patch } : s)) }),
      deleteStep: (id) => set({ steps: get().steps.filter((s) => s.id !== id) }),
      toggleStep: (id) => {
        const s = get().steps.find((x) => x.id === id);
        if (!s) return;
        const done = !s.done;
        get().updateStep(id, { done, completedAt: done ? Date.now() : undefined });
      },

      addSession: (s) => set({ sessions: [...get().sessions, { id: uid(), ...s }] }),

      addCategory: (c) => {
        const cat: Category = { id: uid(), ...c };
        set({ categories: [...get().categories, cat] });
        return cat;
      },
      deleteCategory: (id) =>
        set({ categories: get().categories.filter((c) => c.id !== id) }),

      setSettings: (patch) => set({ settings: { ...get().settings, ...patch } }),

      importData: (data) =>
        set({
          tasks: data.tasks ?? get().tasks,
          steps: data.steps ?? get().steps,
          categories: data.categories ?? get().categories,
          sessions: data.sessions ?? get().sessions,
          settings: { ...get().settings, ...(data.settings ?? {}) },
        }),

      resetAll: () =>
        set({
          tasks: [],
          steps: [],
          sessions: [],
          categories: defaultCategories,
          settings: defaultSettings,
        }),
    }),
    { name: "mypotato-store-v1" },
  ),
);