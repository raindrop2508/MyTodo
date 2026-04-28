import { Sprout } from "lucide-react";

const EmptyState = ({ title, hint }: { title: string; hint?: string }) => (
  <div className="flex flex-col items-center justify-center py-16 text-center">
    <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-primary-soft text-primary">
      <Sprout className="h-8 w-8" />
    </div>
    <p className="text-base font-semibold">{title}</p>
    {hint && <p className="mt-1 max-w-xs text-sm text-muted-foreground">{hint}</p>}
  </div>
);

export default EmptyState;