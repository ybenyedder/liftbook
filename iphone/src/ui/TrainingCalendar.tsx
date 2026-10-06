/** "Entraînement ▾ → Calendrier" — port of TrainingCalendar.kt (month view, workout days in blue). */
import React, { useMemo, useState } from "react";
import { Pressable, View } from "react-native";
import * as Calc from "../calc";
import { Repo } from "../repo";
import { Nav } from "../nav";
import { useStore } from "../store";
import { C, accentColor } from "../theme";
import { s, getLang } from "../l10n";
import { Row, Txt } from "./comps";
import { MIcon } from "./icons";

function daysInMonth(y: number, m: number) {
  return new Date(y, m, 0).getDate();
}
/** 1=Monday..7=Sunday for the 1st of the month. */
function firstDow(y: number, m: number) {
  const d = new Date(y, m - 1, 1).getDay();
  return d === 0 ? 7 : d;
}

export default function TrainingCalendar() {
  useStore(Repo.rev);
  const unit = Repo.settings.unit;
  const now = new Date();
  const [ym, setYm] = useState({ y: now.getFullYear(), m: now.getMonth() + 1 });

  const byDay = useMemo(() => {
    const m = new Map<string, { id: number; startedAt: number }>();
    for (const w of Repo.workouts) {
      const k = Calc.dayKey(w.startedAt);
      const cur = m.get(k);
      if (!cur || w.startedAt > cur.startedAt) m.set(k, { id: w.id, startedAt: w.startedAt });
    }
    return m;
  }, [Repo.rev.value]);

  const lead = firstDow(ym.y, ym.m);
  const len = daysInMonth(ym.y, ym.m);
  const cells: (number | null)[] = [...Array(lead).fill(null), ...Array.from({ length: len }, (_, i) => i + 1)];
  const monthWorkouts = useMemo(
    () => Repo.workouts.filter((w) => { const { y, m } = Calc.localDate(w.startedAt); return y === ym.y && m === ym.m; }),
    [Repo.rev.value, ym],
  );
  const today = new Date();
  const isCurMonth = ym.y === today.getFullYear() && ym.m === today.getMonth() + 1;
  const monthLabel = Calc.monthLabel(new Date(ym.y, ym.m - 1, 1).getTime(), getLang() === "fr" ? "fr-FR" : "en-US");
  const accent = accentColor(Repo.settings.accent);

  return (
    <View>
      {/* month navigation */}
      <Row style={{ paddingHorizontal: 8, paddingVertical: 12 }}>
        <Pressable onPress={() => setYm(ym.m === 1 ? { y: ym.y - 1, m: 12 } : { y: ym.y, m: ym.m - 1 })} hitSlop={6} style={{ padding: 6 }}>
          <MIcon name="chevron-left" size={24} color={C.Text} />
        </Pressable>
        <Txt weight={800} size={18} style={{ flex: 1, textAlign: "center" }}>
          {monthLabel.charAt(0).toUpperCase() + monthLabel.slice(1)}
        </Txt>
        <Pressable
          disabled={isCurMonth}
          onPress={() => setYm(ym.m === 12 ? { y: ym.y + 1, m: 1 } : { y: ym.y, m: ym.m + 1 })}
          hitSlop={6}
          style={{ padding: 6 }}
        >
          <MIcon name="chevron-right" size={24} color={isCurMonth ? C.Mut : C.Text} />
        </Pressable>
      </Row>
      {/* weekday header (Monday-first) */}
      <Row style={{ paddingHorizontal: 16 }}>
        {[s("L", "L"), s("M", "M"), s("M", "M"), s("J", "J"), s("V", "V"), s("S", "S"), s("D", "D")].map((d, i) => (
          <Txt key={i} size={12} weight={700} color={C.Mut} style={{ flex: 1, textAlign: "center" }}>
            {d}
          </Txt>
        ))}
      </Row>
      <View style={{ height: 8 }} />
      {/* day grid */}
      <View style={{ paddingHorizontal: 16 }}>
        {Array.from({ length: Math.ceil(cells.length / 7) }, (_, w) => (
          <Row key={w}>
            {cells.slice(w * 7, w * 7 + 7).map((d, c) => (
              <View key={c} style={{ flex: 1, paddingVertical: 5, alignItems: "center" }}>
                {d != null ? (
                  <DayCell
                    d={d}
                    has={byDay.has(`${ym.y}-${String(ym.m).padStart(2, "0")}-${String(d).padStart(2, "0")}`)}
                    id={byDay.get(`${ym.y}-${String(ym.m).padStart(2, "0")}-${String(d).padStart(2, "0")}`)?.id ?? null}
                    isToday={isCurMonth && d === today.getDate()}
                    accent={accent}
                  />
                ) : (
                  <View style={{ width: 40 }} />
                )}
              </View>
            ))}
          </Row>
        ))}
      </View>
      <View style={{ height: 16 }} />
      {/* month summary */}
      {monthWorkouts.length > 0 ? (
        <Txt size={13} color={C.Mut} style={{ paddingHorizontal: 16 }}>
          {monthWorkouts.length} {s("workouts", "séances")}  ·  {Calc.fmtVol(monthWorkouts.reduce((a, w) => a + Calc.vol(w), 0), unit)} kg
        </Txt>
      ) : (
        <Txt size={13} color={C.Mut} style={{ paddingHorizontal: 16 }}>
          {s("No workouts this month.", "Aucune séance ce mois-ci.")}
        </Txt>
      )}
      <View style={{ height: 24 }} />
    </View>
  );
}

function DayCell({ d, has, id, isToday, accent }: { d: number; has: boolean; id: number | null; isToday: boolean; accent: string }) {
  return (
    <Pressable
      disabled={!has}
      onPress={() => id != null && Nav.push({ t: "WorkoutDetail", id })}
      style={{
        width: 40,
        height: 40,
        borderRadius: 20,
        backgroundColor: has ? accent : "transparent",
        borderWidth: !has && isToday ? 1.5 : 0,
        borderColor: C.Mut,
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      <Txt size={14.5} weight={has || isToday ? 700 : 400} color={has ? C.AccText : C.Text}>
        {d}
      </Txt>
    </Pressable>
  );
}
