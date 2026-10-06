/** UI strings: EN default, FR/ES/DE provided; falls back to EN (port of L10n.kt).
 *  lang is injected at app init (expo-localization) so this module stays pure/testable. */

let currentLang = "fr";

export function setLang(l: string) {
  currentLang = l;
}

export function getLang(): string {
  return currentLang;
}

export function s(en: string, fr: string, es?: string, de?: string): string {
  switch (currentLang) {
    case "fr":
      return fr;
    case "es":
      return es ?? en;
    case "de":
      return de ?? en;
    default:
      return en;
  }
}

export function seriesLabel(n: number, name: string): string {
  if (currentLang === "fr") return n > 1 ? `${n} séries ${name}` : `${n} série ${name}`;
  if (currentLang === "de") return `${n} Sätze ${name}`;
  return `${n} series ${name}`;
}

export function seeMoreExercises(n: number): string {
  switch (currentLang) {
    case "fr":
      return n > 1 ? `Voir ${n} exercices en plus` : "Voir 1 exercice en plus";
    case "es":
      return `Ver ${n} ejercicios más`;
    case "de":
      return `${n} weitere Übungen anzeigen`;
    default:
      return n > 1 ? `See ${n} more exercises` : "See 1 more exercise";
  }
}

export function relativeTime(ms: number, nowMs: number): string {
  const diff = nowMs - ms;
  const hours = Math.floor(diff / 3600000);
  if (currentLang !== "fr") {
    if (hours < 1) return `${Math.max(1, Math.floor(diff / 60000))} min ago`;
    if (hours < 24) return `${hours} h ago`;
    const days = Math.floor(hours / 24);
    return days === 1 ? "1 day ago" : `${days} days ago`;
  }
  if (hours < 1) return `il y a ${Math.max(1, Math.floor(diff / 60000))} min`;
  if (hours < 24) return `il y a ${hours} h`;
  const days = Math.floor(hours / 24);
  return days === 1 ? "il y a 1 jour" : `il y a ${days} jours`;
}
