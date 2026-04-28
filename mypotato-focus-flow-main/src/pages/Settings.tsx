import { useRef } from "react";
import { Download, Upload, FileJson, FileSpreadsheet, Sun, Moon, Monitor, Languages, Trash2 } from "lucide-react";
import { useApp } from "@/store/useApp";
import { useT } from "@/lib/i18n";
import PageHeader from "@/components/layout/PageHeader";
import { download, tasksToCsv } from "@/lib/io";
import type { Lang, ThemeMode } from "@/types";
import { cn } from "@/lib/utils";

const Settings = () => {
  const t = useT();
  const settings = useApp((s) => s.settings);
  const setSettings = useApp((s) => s.setSettings);
  const tasks = useApp((s) => s.tasks);
  const steps = useApp((s) => s.steps);
  const sessions = useApp((s) => s.sessions);
  const categories = useApp((s) => s.categories);
  const importData = useApp((s) => s.importData);
  const resetAll = useApp((s) => s.resetAll);
  const fileRef = useRef<HTMLInputElement>(null);

  const exportJson = () => {
    const payload = { tasks, steps, sessions, categories, settings, exportedAt: Date.now() };
    download(`mypotato-${Date.now()}.json`, JSON.stringify(payload, null, 2), "application/json");
  };
  const exportCsv = () => download(`mypotato-tasks-${Date.now()}.csv`, tasksToCsv(tasks), "text/csv");

  const onImport = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (!f) return;
    f.text().then((txt) => {
      try {
        const data = JSON.parse(txt);
        importData(data);
      } catch {
        alert("Invalid JSON file");
      }
    });
    e.target.value = "";
  };

  const themes: { id: ThemeMode; label: string; icon: typeof Sun }[] = [
    { id: "system", label: t("system"), icon: Monitor },
    { id: "light", label: t("light"), icon: Sun },
    { id: "dark", label: t("dark"), icon: Moon },
  ];

  return (
    <>
      <PageHeader title={t("settings")} subtitle={t("appName")} />

      <div className="space-y-5 px-5">
        <Section title={t("theme")} icon={Sun}>
          <div className="grid grid-cols-3 gap-2">
            {themes.map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                onClick={() => setSettings({ theme: id })}
                className={cn(
                  "flex flex-col items-center gap-1.5 rounded-2xl border px-3 py-3 text-xs font-medium",
                  settings.theme === id
                    ? "border-primary bg-primary-soft text-primary"
                    : "border-border bg-card",
                )}
              >
                <Icon className="h-5 w-5" />
                {label}
              </button>
            ))}
          </div>
        </Section>

        <Section title={t("language")} icon={Languages}>
          <div className="grid grid-cols-2 gap-2">
            {(["en", "zh"] as Lang[]).map((l) => (
              <button
                key={l}
                onClick={() => setSettings({ lang: l })}
                className={cn(
                  "rounded-2xl border px-3 py-3 text-sm font-semibold",
                  settings.lang === l
                    ? "border-primary bg-primary-soft text-primary"
                    : "border-border bg-card",
                )}
              >
                {l === "en" ? t("english") : t("chinese")}
              </button>
            ))}
          </div>
        </Section>

        <Section title={t("pomodoro")} icon={Sun}>
          <div className="grid grid-cols-2 gap-3">
            <NumField
              label={t("workMin")}
              value={settings.workMin}
              onChange={(workMin) => setSettings({ workMin })}
            />
            <NumField
              label={t("breakMin")}
              value={settings.breakMin}
              onChange={(breakMin) => setSettings({ breakMin })}
            />
          </div>
        </Section>

        <Section title={t("importExport")} icon={Download}>
          <div className="grid grid-cols-2 gap-2">
            <ActionBtn icon={FileJson} label={t("exportJson")} onClick={exportJson} />
            <ActionBtn icon={FileSpreadsheet} label={t("exportCsv")} onClick={exportCsv} />
            <ActionBtn icon={Upload} label={t("importJson")} onClick={() => fileRef.current?.click()} />
            <ActionBtn
              icon={Trash2}
              label="Reset"
              tone="destructive"
              onClick={() => {
                if (confirm("Erase all tasks and data?")) resetAll();
              }}
            />
          </div>
          <input
            ref={fileRef}
            type="file"
            accept="application/json"
            onChange={onImport}
            className="hidden"
          />
        </Section>

        <p className="pt-2 text-center text-[11px] text-muted-foreground">
          {t("appName")} · local-first · v1.0
        </p>
      </div>
    </>
  );
};

const Section = ({ title, icon: Icon, children }: { title: string; icon: typeof Sun; children: React.ReactNode }) => (
  <div className="rounded-3xl border border-border/60 bg-card p-4 shadow-soft">
    <div className="mb-3 flex items-center gap-2">
      <Icon className="h-4 w-4 text-primary" />
      <h2 className="text-sm font-bold">{title}</h2>
    </div>
    {children}
  </div>
);

const NumField = ({ label, value, onChange }: { label: string; value: number; onChange: (n: number) => void }) => (
  <label className="block">
    <p className="mb-1 text-[11px] font-medium text-muted-foreground">{label}</p>
    <input
      type="number"
      min={1}
      max={120}
      value={value}
      onChange={(e) => onChange(Math.max(1, Number(e.target.value) || 1))}
      className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:border-primary"
    />
  </label>
);

const ActionBtn = ({
  icon: Icon,
  label,
  onClick,
  tone,
}: {
  icon: typeof Download;
  label: string;
  onClick: () => void;
  tone?: "destructive";
}) => (
  <button
    onClick={onClick}
    className={cn(
      "flex items-center gap-2 rounded-2xl border px-3 py-3 text-sm font-medium",
      tone === "destructive"
        ? "border-destructive/30 text-destructive hover:bg-destructive/10"
        : "border-border bg-card hover:bg-muted",
    )}
  >
    <Icon className="h-4 w-4" />
    {label}
  </button>
);

export default Settings;