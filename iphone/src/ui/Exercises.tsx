/** Exercises list + detail (Résumé / Historique / Instructions) — port of Exercises.kt. */
import React, { useMemo, useState } from "react";
import { Animated, Easing, Pressable, ScrollView, View } from "react-native";
import * as Calc from "../calc";
import { Repo } from "../repo";
import { EX, EXERCISES, MUSCLES, cues, equipHint, matches, steps } from "../l10ndata";
import { Nav } from "../nav";
import { useStore } from "../store";
import { C, accentColor } from "../theme";
import { s } from "../l10n";
import { exName, muscleName, equipName } from "../l10nshim";
import { AppCard, Chip, EmptyState, LineChart, PrimaryButton, Row, TF, Txt, toast } from "./comps";
import { IllIcon, MIcon } from "./iconsEx";

export default function ExercisesScreen() {
  const [q, setQ] = useState("");
  const [mus, setMus] = useState("All");
  const filtered = useMemo(
    () => EXERCISES.filter((e) => (mus === "All" || e.muscle === mus) && matches(q, e.name)).sort((a, b) => exName(a.name).localeCompare(exName(b.name), "fr")),
    [q, mus],
  );
  const grouped = useMemo(() => {
    const m = new Map<string, typeof filtered>();
    for (const e of filtered) {
      if (!m.has(e.muscle)) m.set(e.muscle, [] as any);
      m.get(e.muscle)!.push(e);
    }
    return [...m.entries()];
  }, [filtered]);

  return (
    <View style={{ flex: 1, backgroundColor: C.Bg }}>
      <Row style={{ paddingHorizontal: 8, paddingTop: 12 }}>
        <Pressable onPress={() => Nav.pop()} hitSlop={6} style={{ padding: 6 }}>
          <MIcon name="arrow-back" size={24} color={C.Text} />
        </Pressable>
        <Txt weight={800} size={22} style={{ flex: 1 }}>
          {s("Exercises", "Exercices")}
        </Txt>
      </Row>
      <View style={{ paddingHorizontal: 16, paddingVertical: 2 }}>
        <TF value={q} onChange={setQ} placeholder={s("Search exercises", "Rechercher des exercices")} leadingIcon="search" />
      </View>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ paddingHorizontal: 16, paddingVertical: 6, gap: 8 }}>
        {["All", ...MUSCLES].map((m) => (
          <Chip key={m} label={m === "All" ? s("All", "Tous") : muscleName(m)} selected={mus === m} onClick={() => setMus(m)} />
        ))}
      </ScrollView>
      <ScrollView style={{ flex: 1 }} contentContainerStyle={{ paddingBottom: 12 }} keyboardShouldPersistTaps="handled">
        {filtered.length === 0 ? (
          <EmptyState text={s("No exercises found.", "Aucun exercice trouvé.")} />
        ) : q.trim() === "" && mus === "All" ? (
          grouped.map(([muscleName_, exs]) => (
            <View key={muscleName_}>
              <Txt weight={700} size={12} color={C.Mut} style={{ paddingHorizontal: 16, paddingTop: 16, paddingBottom: 6 }}>
                {muscleName(muscleName_).toUpperCase()}
              </Txt>
              {exs.map((e) => (
                <ExerciseRow key={e.name} name={e.name} muscle={e.muscle} equip={e.equip} />
              ))}
            </View>
          ))
        ) : (
          filtered.map((e) => <ExerciseRow key={e.name} name={e.name} muscle={e.muscle} equip={e.equip} />)
        )}
        <View style={{ height: 12 }} />
      </ScrollView>
    </View>
  );
}

function ExerciseRow({ name, muscle, equip }: { name: string; muscle: string; equip: string }) {
  return (
    <Pressable onPress={() => Nav.push({ t: "ExerciseDetail", name })} style={{ paddingHorizontal: 16, paddingVertical: 13, flexDirection: "row", alignItems: "center" }}>
      <View style={{ flex: 1 }}>
        <Txt weight={600} size={14.5} numberOfLines={1}>
          {exName(name)}
        </Txt>
        <Txt size={12} color={C.Mut}>
          {muscleName(muscle)} · {equipName(equip)}
        </Txt>
      </View>
      <MIcon name="chevron-right" size={16} color={C.Mut} />
    </Pressable>
  );
}

// ---------------- detail ----------------

export function ExerciseDetailScreen({ name }: { name: string }) {
  useStore(Repo.rev);
  const [tab, setTab] = useState(0); // 0=Résumé 1=Historique 2=Instructions
  const [period, setPeriod] = useState(3); // 0=3m 1=6m 2=1y 3=all
  const unit = Repo.settings.unit;
  const def = EX[name];
  const accent = accentColor(Repo.settings.accent);
  const underline = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    Animated.timing(underline, { toValue: tab, duration: 250, easing: Easing.inOut(Easing.ease), useNativeDriver: true }).start();
  }, [tab]);
  const historySessions = useMemo(
    () => Repo.workoutsDesc().map((w) => ({ w, ex: w.exercises.find((e) => e.name === name) })).filter((x) => x.ex != null) as { w: any; ex: any }[],
    [Repo.rev.value, name],
  );

  return (
    <View style={{ flex: 1, backgroundColor: C.Bg }}>
      <Row style={{ paddingHorizontal: 8, paddingVertical: 6 }}>
        <Pressable onPress={() => Nav.pop()} hitSlop={6} style={{ padding: 6 }}>
          <MIcon name="arrow-back" size={24} color={C.Text} />
        </Pressable>
        <Txt weight={700} size={17} numberOfLines={1} style={{ flex: 1 }}>
          {exName(name)}
        </Txt>
      </Row>
      {/* tabs */}
      <View style={{ paddingHorizontal: 16 }}>
        <Row>
          {[s("Summary", "Résumé"), s("History", "Historique"), s("Instructions", "Instructions")].map((label, i) => (
            <Pressable key={label} onPress={() => setTab(i)} style={{ flex: 1, alignItems: "center", paddingVertical: 10 }}>
              <Txt size={14} weight={tab === i ? 700 : 500} color={tab === i ? C.Text : C.Mut}>
                {label}
              </Txt>
            </Pressable>
          ))}
        </Row>
        <View style={{ height: 2 }}>
          <Animated.View style={{ width: "33.33%", height: 2, backgroundColor: accent, transform: [{ translateX: underline.interpolate({ inputRange: [0, 2], outputRange: [0, 200] }) }] }} />
        </View>
      </View>
      <ScrollView style={{ flex: 1 }} contentContainerStyle={{ paddingBottom: 20 }}>
        {tab === 0 ? (
          <SummaryTab name={name} period={period} setPeriod={setPeriod} unit={unit} />
        ) : tab === 1 ? (
          historySessions.length === 0 ? (
            <EmptyState text={s("No sessions logged yet.", "Aucune séance enregistrée.")} />
          ) : (
            historySessions.map(({ w, ex }) => (
              <View key={w.id} style={{ paddingBottom: 14 }}>
                <Row style={{ paddingHorizontal: 16, paddingVertical: 8 }}>
                  <Txt weight={700} size={14} style={{ flex: 1 }}>
                    {Calc.fmtDateFull(w.startedAt)}
                  </Txt>
                  <Txt size={13} color={C.Mut}>
                    {Calc.fmtVol(ex.sets.filter((st: any) => st.done).reduce((a: number, st: any) => a + (st.kg ?? 0) * (st.reps ?? 0), 0), unit)} kg
                  </Txt>
                </Row>
                <Row style={{ paddingHorizontal: 16 }}>
                  <Txt size={11} color={C.Mut} style={{ flex: 1 }}>
                    SÉRIE
                  </Txt>
                  <Txt size={11} color={C.Mut} style={{ flex: 1, textAlign: "center" }}>
                    KG
                  </Txt>
                  <Txt size={11} color={C.Mut} style={{ flex: 1, textAlign: "right" }}>
                    RÉPS
                  </Txt>
                </Row>
                {ex.sets.map((st: any, si: number) => (
                  <Row key={si} style={{ paddingHorizontal: 16, paddingVertical: 9, backgroundColor: si % 2 === 1 ? "rgba(42,42,45,0.35)" : "transparent" }}>
                    <Txt size={13} color={C.Mut} style={{ flex: 1 }}>
                      {si + 1}
                    </Txt>
                    <Txt weight={600} size={13} style={{ flex: 1, textAlign: "center" }}>
                      {st.kg != null ? Calc.fmtKg(st.kg, unit) : "—"}
                    </Txt>
                    <Txt weight={600} size={13} style={{ flex: 1, textAlign: "right" }}>
                      {st.reps ?? "—"}
                    </Txt>
                  </Row>
                ))}
              </View>
            ))
          )
        ) : (
          <InstructionsTab name={name} />
        )}
      </ScrollView>
      {Repo.draft?.mode === "workout" ? (
        <PrimaryButton
          text={s("Add to Current Workout", "Ajouter à la séance en cours")}
          onClick={() => {
            Repo.addExToDraft(name);
            Nav.pop();
            toast(`${exName(name)} ajouté`);
          }}
          style={{ marginHorizontal: 16, marginVertical: 8 }}
          leading={<MIcon name="add" size={17} color="#fff" />}
        />
      ) : null}
    </View>
  );
}

import { useEffect, useRef } from "react";

function SummaryTab({ name, period, setPeriod, unit }: { name: string; period: number; setPeriod: (n: number) => void; unit: string }) {
  useStore(Repo.rev);
  const now = Date.now();
  const cutoff = period === 0 ? now - 91 * 86400000 : period === 1 ? now - 182 * 86400000 : period === 2 ? now - 365 * 86400000 : 0;
  const sessions = Repo.workouts
    .filter((w) => w.startedAt >= cutoff)
    .map((w) => ({ w, ex: w.exercises.find((e) => e.name === name) }))
    .filter((x) => x.ex != null) as { w: any; ex: any }[];
  const heaviest: [string, number][] = sessions.map(({ w, ex }) => [
    Calc.fmtDateShort(w.startedAt),
    Math.max(0, ...ex.sets.map((st: any) => st.kg ?? 0)),
  ]);
  const volumes: [string, number][] = sessions.map(({ w, ex }) => [
    Calc.fmtDateShort(w.startedAt),
    ex.sets.filter((st: any) => st.done).reduce((a: number, st: any) => a + (st.kg ?? 0) * (st.reps ?? 0), 0),
  ]);
  if (heaviest.length === 0) {
    return <EmptyState text={s("No data yet.\nLog this exercise to see charts.", "Aucune donnée.\nEnregistre cet exercice pour voir les graphiques.")} />;
  }
  const pr = Repo.prFor(name);
  const best1rm = Math.max(...sessions.flatMap(({ ex }) => ex.sets.filter((st: any) => (st.kg ?? 0) > 0 && (st.reps ?? 0) > 0).map((st: any) => Calc.e1rm(st.kg, st.reps))));
  const bestSet = Math.max(...sessions.flatMap(({ ex }) => ex.sets.map((st: any) => st.kg ?? 0)));
  const lastTop = Math.max(0, ...sessions[sessions.length - 1].ex.sets.map((st: any) => st.kg ?? 0));
  const delta = lastTop - bestSet;
  const pct = bestTopPct(bestSet, delta);
  return (
    <View>
      <Row style={{ paddingHorizontal: 16, paddingVertical: 12, gap: 8 }}>
        {[s("3m", "3m"), s("6m", "6m"), s("1y", "1a"), s("All", "Tout")].map((label, i) => (
          <Pressable
            key={label}
            onPress={() => setPeriod(i)}
            style={{
              borderRadius: 999,
              backgroundColor: period === i ? C.Card2 : C.Card,
              borderWidth: 1,
              borderColor: period === i ? accentColor(Repo.settings.accent) : C.Line,
              paddingHorizontal: 14,
              paddingVertical: 7,
            }}
          >
            <Txt weight={600} size={12.5} color={period === i ? accentColor(Repo.settings.accent) : C.Mut}>
              {label}
            </Txt>
          </Pressable>
        ))}
      </Row>
      <AppCard>
        <View style={{ padding: 14 }}>
          <Row>
            <MIcon name="emoji-events" size={16} color={accentColor(Repo.settings.accent)} />
            <View style={{ width: 7 }} />
            <Txt weight={700} size={14}>
              {s("Records", "Records")}
            </Txt>
          </Row>
          <View style={{ height: 8 }} />
          <Row style={{ gap: 14 }}>
            <View style={{ flex: 1 }}>
              <Txt size={11.5} color={C.Mut}>
                {s("Best est. 1RM", "1RM max est.")}
              </Txt>
              <Txt weight={700} size={14}>
                {Calc.fmtKg(best1rm, unit)} kg
              </Txt>
            </View>
            <View style={{ flex: 1 }}>
              <Txt size={11.5} color={C.Mut}>
                {s("Heaviest set", "Série lourde")}
              </Txt>
              <Txt weight={700} size={14}>
                {Calc.fmtKg(bestSet, unit)} kg
              </Txt>
            </View>
            <View style={{ flex: 1 }}>
              <Txt size={11.5} color={C.Mut}>
                {s("Sessions", "Séances")}
              </Txt>
              <Txt weight={700} size={14}>
                {sessions.length}
              </Txt>
            </View>
          </Row>
          {pr ? (
            <Txt size={11.5} color={C.Mut} style={{ paddingTop: 8 }}>
              {s("PR set on ", "Record établi le ") + Calc.fmtDateShort(pr.weightDate)}
            </Txt>
          ) : null}
          {sessions.length >= 2 ? (
            <Txt size={12} weight={600} color={delta < 0 ? C.Red : accentColor(Repo.settings.accent)} style={{ paddingTop: 8 }}>
              {s("Last session vs best", "Dernière séance vs record") + " : " + (delta >= 0 ? "+" : "") + Calc.fmtKg(delta, unit) + ` kg (${pct}%)`}
            </Txt>
          ) : null}
        </View>
      </AppCard>
      <AppCard>
        <View style={{ padding: 14 }}>
          <Txt size={13} weight={500} color={C.Mut}>
            {s("Heaviest weight", "Poids le plus lourd")}
          </Txt>
          <View style={{ height: 6 }} />
          <Txt weight={700} size={19}>
            {Calc.fmtKg(heaviest[heaviest.length - 1][1], unit)} kg
          </Txt>
          <LineChart points={heaviest} fmtLabel={(v) => Calc.fmtKg(v, unit)} />
        </View>
      </AppCard>
      <AppCard>
        <View style={{ padding: 14 }}>
          <Txt size={13} weight={500} color={C.Mut}>
            {s("Total volume", "Volume total")}
          </Txt>
          <View style={{ height: 6 }} />
          <Txt weight={700} size={19}>
            {Calc.fmtVol(volumes.reduce((a, v) => a + v[1], 0), unit)} kg
          </Txt>
          <LineChart points={volumes} fmtLabel={(v) => Calc.fmtVol(v, unit)} />
        </View>
      </AppCard>
    </View>
  );
}

function bestTopPct(best: number, delta: number): number {
  if (best <= 0) return 0;
  return Math.round((delta / best) * 100);
}

function InstructionsTab({ name }: { name: string }) {
  const def = EX[name];
  const [playing, setPlaying] = useState(true);
  const pulse = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    if (playing) {
      Animated.loop(Animated.sequence([Animated.timing(pulse, { toValue: 1, duration: 700, useNativeDriver: true }), Animated.timing(pulse, { toValue: 0, duration: 700, useNativeDriver: true })])).start();
    } else pulse.setValue(0);
  }, [playing]);
  const stepList = steps(name);
  const cueList = cues(def?.muscle ?? "");
  return (
    <View>
      <AppCard>
        <View style={{ backgroundColor: "#000", alignItems: "center", paddingVertical: 14 }}>
          <Animated.View style={{ transform: [{ scale: pulse.interpolate({ inputRange: [0, 1], outputRange: [1, 1.06] }) }], opacity: pulse.interpolate({ inputRange: [0, 1], outputRange: [0.9, 1] }) }}>
            <IllIcon muscle={def?.muscle ?? "Chest"} size={170} />
          </Animated.View>
          <Pressable
            onPress={() => setPlaying(!playing)}
            style={{ position: "absolute", right: 10, bottom: 10, width: 38, height: 38, borderRadius: 19, backgroundColor: C.Card2, alignItems: "center", justifyContent: "center" }}
          >
            <MIcon name={playing ? "pause" : "play-arrow"} size={19} color={C.Text} />
          </Pressable>
        </View>
      </AppCard>
      <AppCard>
        <View style={{ padding: 14 }}>
          <Txt weight={700} size={14}>
            {equipName(def?.equip ?? "")}
          </Txt>
          {equipHint(def?.equip ?? "") !== "" ? (
            <Txt size={13} color={C.Mut} style={{ lineHeight: 18, paddingTop: 6 }}>
              {equipHint(def?.equip ?? "")}
            </Txt>
          ) : null}
        </View>
      </AppCard>
      <AppCard>
        <View style={{ padding: 14 }}>
          <Txt weight={700} size={11} color={C.Mut}>
            {s("PRIMARY MUSCLE", "MUSCLE PRINCIPAL")}
          </Txt>
          <View style={{ height: 6 }} />
          <Txt weight={700} size={15}>
            {muscleName(def?.muscle ?? "")}
          </Txt>
        </View>
      </AppCard>
      {stepList.length > 0 ? (
        <AppCard>
          <View style={{ padding: 14 }}>
            <Txt weight={700} size={14}>
              {s("How to perform", "Comment réaliser l'exercice")}
            </Txt>
            <View style={{ height: 4 }} />
            {stepList.map((step, si) => (
              <Row key={si} style={{ paddingVertical: 7, alignItems: "flex-start" }}>
                <View style={{ width: 22, height: 22, borderRadius: 11, backgroundColor: accentColor(Repo.settings.accent), alignItems: "center", justifyContent: "center" }}>
                  <Txt weight={700} size={12} color="#fff">
                    {si + 1}
                  </Txt>
                </View>
                <View style={{ width: 10 }} />
                <Txt size={14} style={{ lineHeight: 20, flex: 1 }}>
                  {step}
                </Txt>
              </Row>
            ))}
          </View>
        </AppCard>
      ) : null}
      {cueList.length > 0 ? (
        <AppCard>
          <View style={{ padding: 14 }}>
            <Txt weight={700} size={11} color={C.Mut}>
              {s("TIPS", "CONSEILS")}
            </Txt>
            <View style={{ height: 4 }} />
            {cueList.map((cue, ci) => (
              <Row key={ci} style={{ paddingVertical: 6, alignItems: "flex-start" }}>
                <Txt weight={700} size={14} color={accentColor(Repo.settings.accent)} style={{ width: 14 }}>
                  •
                </Txt>
                <Txt size={14} style={{ lineHeight: 20, flex: 1 }}>
                  {cue}
                </Txt>
              </Row>
            ))}
          </View>
        </AppCard>
      ) : null}
    </View>
  );
}
