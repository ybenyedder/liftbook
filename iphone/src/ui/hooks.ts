import { useEffect, useState } from "react";

/** Re-render every `ms` (live clock / countdowns). */
export function useTick(ms = 1000): number {
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), ms);
    return () => clearInterval(t);
  }, [ms]);
  return now;
}
