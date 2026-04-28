import { useEffect } from "react";
import { Outlet, useLocation } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import BottomNav from "./BottomNav";
import { useApp } from "@/store/useApp";
import { useI18n } from "@/lib/i18n";

const AppShell = () => {
  const location = useLocation();
  const settings = useApp((s) => s.settings);
  const setLang = useI18n((s) => s.setLang);

  useEffect(() => setLang(settings.lang), [settings.lang, setLang]);

  useEffect(() => {
    const root = document.documentElement;
    const apply = () => {
      const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
      const dark = settings.theme === "dark" || (settings.theme === "system" && prefersDark);
      root.classList.toggle("dark", dark);
    };
    apply();
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    mq.addEventListener("change", apply);
    return () => mq.removeEventListener("change", apply);
  }, [settings.theme]);

  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-xl pb-28">
        <AnimatePresence mode="wait">
          <motion.div
            key={location.pathname}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -4 }}
            transition={{ duration: 0.2, ease: "easeOut" }}
          >
            <Outlet />
          </motion.div>
        </AnimatePresence>
      </div>
      <BottomNav />
    </div>
  );
};

export default AppShell;