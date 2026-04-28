import { Plus } from "lucide-react";
import { motion } from "framer-motion";

interface Props {
  onClick: () => void;
  label?: string;
  variant?: "extended" | "circle";
}
const Fab = ({ onClick, label, variant = "extended" }: Props) => {
  const isCircle = variant === "circle";
  return (
    <motion.button
      onClick={onClick}
      whileTap={{ scale: 0.92 }}
      whileHover={{ y: -2 }}
      className={
        isCircle
          ? "fixed bottom-24 right-5 z-30 flex h-16 w-16 items-center justify-center rounded-full bg-gradient-primary text-primary-foreground shadow-pop sm:right-[max(1.25rem,calc(50vw-16rem))]"
          : "fixed bottom-24 right-5 z-30 flex h-14 items-center gap-2 rounded-2xl bg-gradient-primary px-5 text-primary-foreground shadow-pop sm:right-[max(1.25rem,calc(50vw-16rem))]"
      }
      aria-label={label ?? "Add"}
    >
      <Plus className={isCircle ? "h-7 w-7" : "h-6 w-6"} strokeWidth={2.6} />
      {!isCircle && label && <span className="text-sm font-semibold">{label}</span>}
    </motion.button>
  );
};

export default Fab;