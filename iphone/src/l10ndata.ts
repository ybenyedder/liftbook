/** Content localization + fuzzy search — port of L10nData.kt, data from exdata.json (extracted from Kotlin). */
import exdata from "./data/exdata.json";
import { getLang } from "./l10n";

export interface ExerciseDef {
  name: string;
  muscle: string;
  equip: string;
}

export const EXERCISES: ExerciseDef[] = exdata.exercises;
export const EX: Record<string, ExerciseDef> = Object.fromEntries(EXERCISES.map((e) => [e.name, e]));
export const MUSCLES: string[] = exdata.muscles;

export const NAME_FR: Record<string, string> = exdata.nameFr;
const MUSCLE_FR: Record<string, string> = exdata.muscleFr;
const EQUIP_FR: Record<string, string> = exdata.equipFr;
const MUSCLE_EXTRA: Record<string, string> = exdata.muscleExtra;
const ALIAS_FR: Record<string, string> = exdata.aliasFr;
const CUES_EN: Record<string, string[]> = exdata.cues;
const CUES_FR: Record<string, string[]> = exdata.cuesFr;
const ARCH_STEPS: Record<string, string[]> = exdata.archSteps;
const ARCH_STEPS_FR: Record<string, string[]> = exdata.archStepsFr;
const EQUIP_HINT_EN: Record<string, string> = exdata.equipHint;

export function name(en: string): string {
  return getLang() === "fr" ? NAME_FR[en] ?? en : en;
}
export function muscle(m: string): string {
  return getLang() === "fr" ? MUSCLE_FR[m] ?? m : m;
}
export function equip(e: string): string {
  return getLang() === "fr" ? EQUIP_FR[e] ?? e : e;
}
export function cues(m: string): string[] {
  return getLang() === "fr" ? CUES_FR[m] ?? CUES_EN[m] ?? [] : CUES_EN[m] ?? [];
}

/** Step-by-step instructions for an exercise, localized. */
export function steps(nameEn: string): string[] {
  const arch = archetypeOf(nameEn);
  return getLang() === "fr" ? ARCH_STEPS_FR[arch] ?? ARCH_STEPS[arch] ?? [] : ARCH_STEPS[arch] ?? [];
}

const EQUIP_HINT_FR: Record<string, string> = {
  Barbell: "Avec une barre chargée de disques.",
  Dumbbell: "Avec des haltères — un dans chaque main sauf indication.",
  Machine: "Sur une machine à pile ou à charges guidées.",
  Cable: "À la station de poulies avec accessoires.",
  Bodyweight: "Sans matériel — le corps sert de résistance.",
  Other: "Avec du matériel spécifique : banc, élastique, kettlebell ou roue.",
};

export function equipHint(e: string): string {
  return getLang() !== "fr" ? EQUIP_HINT_EN[e] ?? "" : EQUIP_HINT_FR[e] ?? EQUIP_HINT_FR.Other;
}

/** Map an exercise (canonical EN name) to its instruction archetype — port of archetypeOf() (order critical). */
export function archetypeOf(name: string): string {
  const n = name.toLowerCase();
  const muscleOf = EX[name]?.muscle ?? "";
  const has = (...keys: string[]) => keys.some((k) => n.includes(k));
  if (n.includes("walking lunge")) return "lunge";
  if (
    has(
      "burpee", "jumping jack", "high knee", "butt kick", "jump rope", "rowing machine",
      "treadmill", "cycling", "elliptical", "stair climber", "swimming", "ski erg",
      "jacobs ladder", "assault bike", "versa climber", "sled push", "sled drag",
      "battle rope", "shadow box", "speed skater", "lateral bound",
    )
  )
    return "conditioning";
  if (n.includes("walking") || n.includes("running")) return "conditioning";
  if (
    has(
      "power clean", "hang clean", "squat clean", "clean pull", "clean and",
      "power snatch", "hang snatch", "snatch pull", "snatch balance", "kettlebell snatch",
    )
  )
    return "olympic";
  if (n.includes("jerk")) return "jerk";
  if (n.includes("thruster")) return "thruster";
  if (has("box jump", "broad jump", "depth jump", "frog jump")) return "plyo_jump";
  if (n.includes("get up")) return "getup";
  if (n.includes("pallof")) return "pallof";
  if (n.includes("bird dog")) return "bird_dog";
  if (has("bear crawl", "crab walk")) return "crawl";
  if (has("superman", "snow angel")) return "superman_hold";
  if (n.includes("pull apart")) return "pull_apart";
  if (n.includes("wood chop")) return "wood_chop";
  if (has("scaption", "y raise", "w raise")) return "scaption";
  if (n.includes("handstand")) return "ohp";
  if (n.includes("hip extension")) return "back_ext";
  if (n === "reverse nordic") return "squat";
  if (n.includes("incline") && has("press", "bench")) return "incline_bench";
  if (n.includes("decline") && has("press", "bench")) return "decline_bench";
  if ((n.includes("bench press") && !n.includes("close grip")) || n === "machine chest press" || n.includes("floor press") || n === "svend press" || n.includes("guillotine"))
    return "bench";
  if (has("fly", "crossover", "pec deck", "svend")) return "fly";
  if (has("push up", "pushup")) return "pushup";
  if (n.includes("dip") && muscleOf === "Chest") return "dip";
  if (has("shrug", "rack pull", "block pull")) return "shrug";
  if (n.includes("lateral raise")) return "lateral_raise";
  if (n.includes("front raise")) return "front_raise";
  if (has("rear delt", "face pull", "reverse pec deck")) return "rear_delt";
  if (has("upright row", "high pull")) return "upright_row";
  if (n.includes("press") && muscleOf === "Shoulders") return "ohp";
  if (has("wrist", "pinch", "rice bucket", "towel wring", "figure 8", "hex dumbbell")) return "wrist";
  if (n.includes("curl")) return "curl";
  if (has("pushdown", "kickback") && muscleOf === "Triceps") return "triceps_pushdown";
  if (n.includes("extension") && muscleOf === "Triceps") return "triceps_overhead";
  if (has("skullcrusher", "tate", "jm press")) return "skullcrusher";
  if (n.includes("close grip bench")) return "skullcrusher";
  if (n.includes("dip")) return "dip";
  if (n.includes("straight arm")) return "straight_arm";
  if (n.includes("pulldown")) return "pulldown";
  if (has("pull up", "pullup", "chin up", "muscle up", "scap pull")) return "pull_up";
  if (n.includes("pullover")) return "pullover";
  if (n.includes("inverted row")) return "pull_up";
  if (has("cable row", "supported row", "machine row", "t-bar", "incline dumbbell row", "seal row", "meadow row")) return "row_supported";
  if (n.includes("row")) return "row_bent";
  if (n.includes("deadlift") && has("romanian", "single leg")) return "rdl";
  if (n.includes("deadlift")) return "deadlift";
  if (n.includes("good morning")) return "good_morning";
  if ((n.includes("extension") && muscleOf === "Lower back") || n.includes("hyperextension")) return "back_ext";
  if (n.includes("swing")) return "swing";
  if (has("hip thrust", "glute bridge", "frog pump", "pull through", "kickback")) return "hip_thrust";
  if (has("leg curl", "nordic", "glute ham")) return "leg_curl";
  if (has("calf", "tibialis", "on toes")) return "calf";
  if (n.includes("squat")) return "squat";
  if (has("lunge", "split squat", "step up", "curtsy", "step down")) return "lunge";
  if (n.includes("leg press")) return "leg_press";
  if (has("leg extension", "knee extension")) return "leg_ext";
  if (has("abduction", "clamshell", "fire hydrant", "lateral walk", "monster walk")) return "abduction";
  if (n.includes("adduction")) return "adduction";
  if (has("plank", "hollow", "dead bug", "wall sit", "mountain", "body saw", "arch hold", "v-sit", "l-sit", "l-hang", "vacuum")) return "plank";
  if (n.includes("sit up")) return "sit_up";
  if (has("crunch", "cocoon", "ball pass")) return "crunch";
  if (has("twist", "windshield")) return "twist";
  if (
    has("leg raise", "knee raise", "v-up", "flutter", "scissor", "dragon flag", "toe to bar", "captain", "jackknife", "leg lower")
  )
    return "leg_raise";
  if (has("rollout", "fallout")) return "rollout";
  if (n.includes("side bend")) return "twist";
  if (has("carry", "dead hang")) return "carry";
  switch (muscleOf) {
    case "Cardio":
      return "conditioning";
    case "Abs":
      return "crunch";
    default:
      return "bench";
  }
}

// ---------- fuzzy search (accent-insensitive, multi-token, FR + EN + muscle + aliases) ----------

export function normalize(s: string): string {
  return s
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/'/g, " ");
}

/** Levenshtein edit distance (typo tolerance). */
export function lev(a: string, b: string): number {
  if (a === b) return 0;
  if (!a.length) return b.length;
  if (!b.length) return a.length;
  let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  let cur = new Array(b.length + 1).fill(0);
  for (let i = 1; i <= a.length; i++) {
    cur[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      cur[j] = Math.min(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost);
    }
    const tmp = prev;
    prev = cur;
    cur = tmp;
  }
  return prev[b.length];
}

const NOISE_TOKENS = new Set([
  "en", "de", "du", "la", "le", "les", "un", "une", "des", "a", "au", "aux",
  "et", "ou", "avec", "sur", "pour", "dans", "par", "pose", "prise", "grip", "the", "and",
]);

/** Light stemming variants tried for a token: raw, -ing, -es, -s. */
function stems(token: string): string[] {
  const out = [token];
  if (token.length >= 5 && token.endsWith("ing")) out.push(token.slice(0, -3));
  if (token.length >= 4 && token.endsWith("es")) out.push(token.slice(0, -2));
  if (token.length >= 4 && token.endsWith("s")) out.push(token.slice(0, -1));
  return out;
}

function tokenMatches(token: string, words: string[], allowTypo: boolean): boolean {
  const variants = stems(token);
  if (words.some((w) => variants.some((v) => w.includes(v)))) return true;
  if (!allowTypo) return false;
  const maxDist = token.length >= 5 ? 2 : token.length === 4 ? 1 : 0;
  if (maxDist === 0) return false;
  return words.some((w) =>
    variants.some((v) => Math.abs(w.length - v.length) <= maxDist && lev(v, w) <= maxDist),
  );
}

const nameWordsCache = new Map<string, string[]>();
const attrWordsCache = new Map<string, string[]>();

/** True if every word of the query appears (or nearly, typos allowed on the name) in the exercise name FR/EN, muscle or equipment. */
export function matches(q: string, nameEn: string): boolean {
  const nq = normalize(q).trim();
  if (!nq) return true;
  const def = EX[nameEn];
  let nameWords = nameWordsCache.get(nameEn);
  if (!nameWords) {
    nameWords = `${normalize(nameEn)} ${normalize(NAME_FR[nameEn] ?? "")} ${normalize(ALIAS_FR[nameEn] ?? "")}`
      .split(" ")
      .filter((x) => x !== "");
    nameWordsCache.set(nameEn, nameWords);
  }
  let attrWords = attrWordsCache.get(nameEn);
  if (!attrWords) {
    attrWords = `${normalize(def?.muscle ?? "")} ${normalize(MUSCLE_FR[def?.muscle ?? ""] ?? "")} ${normalize(
      MUSCLE_EXTRA[def?.muscle ?? ""] ?? "",
    )} ${normalize(def?.equip ?? "")} ${normalize(EQUIP_FR[def?.equip ?? ""] ?? "")}`
      .split(" ")
      .filter((x) => x !== "");
    attrWordsCache.set(nameEn, attrWords);
  }
  return nq
    .split(" ")
    .filter((t) => t !== "" && !NOISE_TOKENS.has(t.trim()))
    .every((t) => tokenMatches(t.trim(), nameWords!, true) || tokenMatches(t.trim(), attrWords!, false));
}
