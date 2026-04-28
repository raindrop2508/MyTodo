import type { ReactNode } from "react";

interface Props {
  title: string;
  subtitle?: string;
  right?: ReactNode;
}
const PageHeader = ({ title, subtitle, right }: Props) => (
  <header className="flex items-end justify-between px-5 pt-8 pb-4">
    <div>
      <h1 className="text-3xl font-extrabold tracking-tight">{title}</h1>
      {subtitle && <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p>}
    </div>
    {right}
  </header>
);

export default PageHeader;