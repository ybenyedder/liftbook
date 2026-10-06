/** History list with filters + calendar — port of History.kt HistoryScreen. */
import React, { useMemo, useState } from "react";
import { Pressable, ScrollView, View } from "react-native";
import * as Calc from "../calc";
import { Repo } from "../repo";
import { Workout } from "../models";
import { Nav } from "../nav";
import { useStore } from "../store";
import { C, accentColor } from "../theme";
import { s } from "../l10n";
import { matches } from "../l10ndata";
import { exName } from "../l10nshim";
import { AppCard, EmptyState, Row, TF, Txt } from "./comps";
import { MIcon } from "./iconsEx";

export default function HistoryScreen() {
  useStore(Repo.rev);
  const now = new Date();
  const [viewYear, setViewYear] = useState(now.getFullYear());
  const [viewMonth, setViewMonth] = useState(now.getMonth() + 1);
  const [showCalendar, setShowCalendar] = useState(false);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState(0); // 0=Toutes 1=Semaine 2=Mois 3=Année
  const accent = accentColor(Repo.settings.accent);

  const byDay = useMemo(() => {
    const m = new Map<string, Workout[]>();
    for (const w of Repo.workouts) {
      const k = Calc.dayKey(w.startedAt);
      if (!m.has(k)) m.set(k, []);
      m.get(k)!.push(w);
    }
    return m;
  }, [Repo.rev.value]);

  const cutoff = useMemo(() => {
    const n = Date.now();
    return filter === 1 ? n - 7 * 86400000 : filter === 2 ? n - 30 * 86400000 : filter === 3 ? n - 365 * 86400000 : 0;
  }, [filter]);

  const filteredWorkouts = useMemo(
    () =>
      Repo.workoutsDesc().filter((w) => {
        const q = query.trim();
        return (
          w.startedAt >= cutoff &&
          (q === "" || w.name.toLowerCase().includes(q.toLowerCase()) || w.exercises.some((e) => matches(q, e.name)))
        );
      }),
    [Repo.rev.value, query, filter],
  );

  const groups = useMemo(() => buildGroups(filteredWorkouts), [Repo.rev.value, query, filter]);

  return (
    <View style={{ flex: 1, backgroundColor: C.Bg }}>
      <ScrollView contentContainerStyle={{ paddingBottom: 16 }} keyboardShouldPersistTaps="handled">
        <Row style={{ paddingHorizontal: 8, paddingTop: 12 }}>
          <Pressable onPress={() => Nav.pop()} hitSlop={6} style={{ padding: 6 }}>
            <MIcon name="arrow-back" size={24} color={C.Text} />
          </Pressable>
          <Txt weight={800} size={22} style={{ flex: 1 }}>
            {s("History", "Historique")}
          </Txt>
          <Pressable onPress={() => setShowCalendar(!showCalendar)} hitSlop={6} style={{ padding: 6 }}>
            <MIcon name="calendar-month" size={24} color={C.Text} />
          </Pressable>
        </Row>
        <View style={{ paddingHorizontal: 16, paddingVertical: 6 }}>
          <TF value={query} onChange={setQuery} placeholder={s("Search workouts", "Rechercher des séances")} leadingIcon="search" height={46} weight={400} size={14} />
        </View>
        <Row style={{ paddingHorizontal: 16, paddingVertical: 6, gap: 8 }}>
          {[s("All", "Toutes"), s("This week", "Cette semaine"), s("This month", "Ce mois"), s("This year", "Cette année")].map((label, i) => (
            <Pressable
              key={label}
              onPress={() => setFilter(i)}
              style={{
                borderRadius: 999,
                backgroundColor: filter === i ? accent : C.Card,
                borderWidth: 1,
                borderColor: filter === i ? accent : C.Line,
                paddingHorizontal: 12,
                paddingVertical: 6,
              }}
            >
              <Txt size={12} weight={600} color={filter === i ? C.AccText : C.Mut}>
                {label}
              </Txt>
            </Pressable>
          ))}
        </Row>
        {showCalendar ? (
          <CalendarCard
            year={viewYear}
            month={viewMonth}
            byDay={byDay}
            onPrev={() => (viewMonth === 1 ? (setViewMonth(12), setViewYear(viewYear - 1)) : setViewMonth(viewMonth - 1))}
            onNext={() => (viewMonth === 12 ? (setViewMonth(1), setViewYear(viewYear + 1)) : setViewMonth(viewMonth + 1))}
            onDay={(key) => byDay.get(key)?.[0] && Nav.push({ t: "WorkoutDetail", id: byDay.get(key)![0].id })}
          />
        ) : null}
        {groups.length === 0 ? (
          <EmptyState text="Aucune séance enregistrée." />
        ) : (
          groups.map(([label, ws]) => (
            <View key={label}>
              <Row style={{ paddingHorizontal: 16, paddingTop: 14, paddingBottom: 8 }}>
                <Txt size={15} weight={500} color={C.Mut}>
                  {label}
                </Txt>
                <MIcon name="expand-more" size={18} color={C.Mut} />
              </Row>
              {ws.map((w) => (
                <HistoryRow key={w.id} w={w} onClick={() => Nav.push({ t: "WorkoutDetail", id: w.id })} />
              ))}
            </View>
          ))
        )}
      </ScrollView>
    </View>
  );
}

function buildGroups(desc: Workout[]): [string, Workout[]][] {
  const ws = Calc.weekStart(Date.now());
  const groups: [string, Workout[]][] = [];
  for (const w of desc) {
    const label =
      w.startedAt >= ws
        ? "Cette semaine"
        : w.startedAt >= ws - 7 * 86400000
          ? "Semaine dernière"
          : new Intl.DateTimeFormat("fr-FR", { month: "long", year: "numeric" }).format(new Date(w.startedAt));
    const g = groups.find((x) => x[0] === label);
    if (g) g[1].push(w);
    else groups.push([label, [w]]);
  }
  return groups;
}

function CalendarCard({
  year,
  month,
  byDay,
  onPrev,
  onNext,
  onDay,
}: {
  year: number;
  month: number;
  byDay: Map<string, Workout[]>;
  onPrev: () => void;
  onNext: () => void;
  onDay: (key: string) => void;
}) {
  const today = new Date();
  const firstDow = (() => {
    const d = new Date(year, month - 1, 1).getDay();
    return d === 0 ? 7 : d;
  })();
  const days = new Date(year, month, 0).getDate();
  const rows = Math.ceil((firstDow + days) / 7);
  const label = new Intl.DateTimeFormat("fr-FR", { month: "long", year: "numeric" }).format(new Date(year, month - 1, 1));
  return (
    <AppCard>
      <View style={{ paddingVertical: 8 }}>
        <Row style={{ paddingHorizontal: 6 }}>
          <Pressable onPress={onPrev} hitSlop={6} style={{ padding: 6 }}>
            <MIcon name="chevron-left" size={24} color={C.Text} />
          </Pressable>
          <Txt weight={800} size={14.5} style={{ flex: 1, textAlign: "center" }}>
            {label.charAt(0).toUpperCase() + label.slice(1)}
          </Txt>
          <Pressable onPress={onNext} hitSlop={6} style={{ padding: 6 }}>
            <MIcon name="chevron-right" size={24} color={C.Text} />
          </Pressable>
        </Row>
        <Row style={{ paddingHorizontal: 10 }}>
          {["Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di"].map((d) => (
            <Txt key={d} size={9.5} weight={800} color={C.Mut} style={{ flex: 1, textAlign: "center" }}>
              {d}
            </Txt>
          ))}
        </Row>
        <View style={{ paddingHorizontal: 10 }}>
          {Array.from({ length: rows }, (_, r) => (
            <Row key={r} style={{ height: 38 }}>
              {Array.from({ length: 7 }, (_, c) => {
                const dayNum = r * 7 + c - firstDow + 1;
                if (dayNum < 1 || dayNum > days) return <View key={c} style={{ flex: 1, padding: 1 }} />;
                const key = `${String(year).padStart(4, "0")}-${String(month).padStart(2, "0")}-${String(dayNum).padStart(2, "0")}`;
                const has = (byDay.get(key)?.length ?? 0) > 0;
                const isToday = today.getFullYear() === year && today.getMonth() + 1 === month && today.getDate() === dayNum;
                return (
                  <View key={c} style={{ flex: 1, padding: 1 }}>
                    <Pressable
                      disabled={!has}
                      onPress={() => onDay(key)}
                      style={{
                        flex: 1,
                        borderRadius: 10,
                        backgroundColor: has ? C.Card2 : "transparent",
                        borderWidth: isToday ? 1.5 : 0,
                        borderColor: accentColor(Repo.settings.accent),
                        alignItems: "center",
                        justifyContent: "center",
                      }}
                    >
                      <Txt size={12.5} weight={600}>
                        {dayNum}
                      </Txt>
                      {has ? <View style={{ position: "absolute", bottom: 2, width: 5, height: 5, borderRadius: 3, backgroundColor: accentColor(Repo.settings.accent) }} /> : null}
                    </Pressable>
                  </View>
                );
              })}
            </Row>
          ))}
        </View>
      </View>
    </AppCard>
  );
}

export function HistoryRow({ w, onClick }: { w: Workout; onClick: () => void }) {
  const d = new Date(w.startedAt);
  const dow = new Intl.DateTimeFormat("fr-FR", { weekday: "narrow" }).format(d).toUpperCase();
  return (
    <Pressable
      onPress={onClick}
      style={{
        marginHorizontal: 16,
        marginVertical: 5,
        borderRadius: 14,
        backgroundColor: C.Card,
        borderWidth: 1,
        borderColor: C.Line,
        padding: 11,
        flexDirection: "row",
        alignItems: "center",
      }}
    >
      <View style={{ width: 46, borderRadius: 10, backgroundColor: C.Card2, paddingVertical: 6, alignItems: "center" }}>
        <Txt size={9.5} weight={800} color={C.Mut}>
          {dow}
        </Txt>
        <Txt size={17} weight={800}>
          {d.getDate()}
        </Txt>
      </View>
      <View style={{ width: 12 }} />
      <View style={{ flex: 1 }}>
        <Txt weight={700} size={16} numberOfLines={1}>
          {w.name}
        </Txt>
        <Txt size={13} color={C.Mut}>
          {Calc.fmtTime(w.startedAt)} — {Calc.fmtTime(w.endedAt)}
        </Txt>
      </View>
      <Txt size={15} weight={600}>
        {Calc.fmtVol(Calc.vol(w), Repo.settings.unit)} kg
      </Txt>
    </Pressable>
  );
}
