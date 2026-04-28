import { NavLink, useLocation } from "react-router-dom";
import { CalendarCheck, ListTodo, BarChart3, Settings as SettingsIcon } from "lucide-react";
import { motion } from "framer-motion";
import { useT } from "@/lib/i18n";
import { cn } from "@/lib/utils";

const BottomNav = () => {
  const t = useT();
  const loc = useLocation();
  const items = [
    { to: "/", icon: CalendarCheck, label: t("today") },
    { to: "/tasks", icon: ListTodo, label: t("tasks") },
    { to: "/stats", icon: BarChart3, label: t("stats") },
    { to: "/settings", icon: SettingsIcon, label: t("settings") },
  ];
  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 px-3 pb-3">
      <div className="mx-auto flex max-w-xl items-center justify-around rounded-3xl border border-border/50 bg-card/85 px-2 py-2 shadow-pop backdrop-blur-xl">
        {items.map(({ to, icon: Icon, label }) => {
          const active = to === "/" ? loc.pathname === "/" : loc.pathname.startsWith(to);
          return (
            <NavLink
              key={to}
              to={to}
              className="relative flex flex-1 flex-col items-center gap-0.5 px-2 py-1.5"
            >
              <div className="relative flex h-9 w-16 items-center justify-center">
                {active && (
                  <motion.div
                    layoutId="navpill"
                    className="absolute inset-0 rounded-full bg-primary-soft"
                    transition={{ type: "spring", stiffness: 380, damping: 30 }}
                  />
                )}
                <Icon
                  className={cn(
                    "relative h-5 w-5 transition-colors",
                    active ? "text-primary" : "text-muted-foreground",
                  )}
                  strokeWidth={active ? 2.4 : 1.8}
                />
              </div>
              <span
                className={cn(
                  "text-[10.5px] font-medium tracking-tight",
                  active ? "text-primary" : "text-muted-foreground",
                )}
              >
                {label}
              </span>
            </NavLink>
          );
        })}
      </div>
    </nav>
  );
};

export default BottomNav;