/** Post-workout recap — port of WorkoutSummary.kt (editable name, stats, records, final save). */
import React, { useMemo, useState } from "react";
import { Pressable, ScrollView, View } from "react-native";
import * as Calc from "../calc";
import { Repo } from "../repo";
import { Nav } from "../nav";
import { useStore } from "../store";
import { C } from "../theme";
import { s } from "../l10n";
import { exName } from "../l10nshim";
import { Alert, BodyMap, Row, TF, Txt, toast } from "./comps";
import { MIcon } from "./iconsEx";
import { RestTimer, WorkoutNotif } from "../resttimer";

export default function WorkoutSummaryScreen() {
  useStore(Repo.rev);
  const draft = Repo.draft;
  if (!draft || draft.mode !== "workout" || draft.startedAt == null) {
    Nav.pop();
    return null;
  }
  return <SummaryBody />;
}

function SummaryBody() {
  const draft = Repo.draft!;
  const unit = Repo.settings.unit;
  const [name, setName] = useState(draft.name);
  const [showSaveRoutine, setShowSaveRoutine] = useState(false);

  let vol = 0;
  let reps = 0;
  let sets = 0;
  for (const ex of draft.exercises)
    for (const st of ex.sets) {
      if (!st.done || st.kg == null || st.reps == null) continue;
      vol += st.kg * st.reps;
      reps += st.reps;
      sets++;
    }
  const prs = useMemo(() => {
    const out: { ex: string; kind: string; value: number }[] = [];
    for (const ex of draft.exercises) {
      const done = ex.sets.filter((st) => (st.kg ?? 0) > 0 && (st.reps ?? 0) > 0 && st.done);
      if (!done.length) continue;
      const prev = Repo.prFor(ex.name);
      const pw = prev?.weight ?? 0;
      const pe = prev?.e1rm ?? 0;
      const bw = Math.max(...done.map((st) => st.kg!));
      const be = Math.max(...done.map((st) => Calc.e1rm(st.kg!, st.reps!)));
      if (bw > pw) out.push({ ex: ex.name, kind: "Weight", value: bw });
      if (be > pe) out.push({ ex: ex.name, kind: "Est. 1RM", value: be });
    }
    return out;
  }, []);
  const muscles = useMemo(() => new Set(draft.exercises.map((e) => e.muscle)), []);
  const duration = Calc.fmtDur(Date.now() - draft.startedAt!);

  const doFinish = () => {
    const w = Repo.finishWorkout(name);
    RestTimer.clear();
    WorkoutNotif.cancel();
    Nav.toTab({ t: "HomeTab" });
    if (w && w.prs.length > 0)
      toast(s("Workout saved", "Séance enregistrée") + ` · ${w.prs.length} ${s("PRs", "records")} !`);
    else toast(s("Workout saved", "Séance enregistrée"));
  };

  return (
    <View style={{ flex: 1, backgroundColor: C.Bg }}>
      {/* top bar */}
      <Row style={{ paddingHorizontal: 8, paddingVertical: 6 }}>
        <Pressable onPress={() => Nav.pop()} hitSlop={6} style={{ padding: 6 }}>
          <MIcon name="arrow-back" size={24} color={C.Text} />
        </Pressable>
        <Txt weight={800} size={17} style={{ flex: 1, textAlign: "center" }}>
          {s("Workout Summary", "Résumé de la séance")}
        </Txt>
        <View style={{ width: 48 }} />
      </Row>
      <ScrollView style={{ flex: 1 }} contentContainerStyle={{ paddingBottom: 16 }} keyboardShouldPersistTaps="handled">
        <View style={{ paddingHorizontal: 16, paddingVertical: 6 }}>
          <TF value={name} onChange={setName} height={52} weight={700} size={17} />
        </View>
        <Row style={{ paddingHorizontal: 16, paddingVertical: 4, gap: 8 }}>
          <SummaryStat value={duration} label={s("Duration", "Durée")} accent />
          <SummaryStat value={`${Calc.fmtVol(vol, unit)}${Calc.unitLabel(unit)}`} label={s("Volume", "Volume")} />
          <SummaryStat value={`${sets}`} label={s("Sets", "Séries")} />
          <BodyMap front muscles={muscles} />
          <BodyMap front={false} muscles={muscles} />
        </Row>
        {prs.length > 0 ? (
          <Row style={{ paddingHorizontal: 16, paddingVertical: 10 }}>
            <MIcon name="emoji-events" size={17} color={C.Gold} />
            <View style={{ width: 6 }} />
            <Txt weight={800} size={13} color={C.Gold}>
              {s("RECORDS", "RECORDS")}
            </Txt>
          </Row>
        ) : null}
        {prs.map((p, i) => (
          <Row
            key={i}
            style={{
              marginHorizontal: 16,
              marginVertical: 6,
              borderRadius: 10,
              backgroundColor: "#1F3B2C",
              paddingHorizontal: 10,
              paddingVertical: 9,
            }}
          >
            <MIcon name="emoji-events" size={15} color={C.Gold} />
            <View style={{ width: 8 }} />
            <Txt size={13.5} numberOfLines={1} style={{ flex: 1 }}>
              {exName(p.ex)}
            </Txt>
            <Txt weight={700} size={12} color={C.Orange} numberOfLines={1}>
              {`${p.kind === "Weight" ? s("Heaviest Weight", "Plus Gros Poids") : s("Best Est. 1RM", "Meilleure Est. 1RM")} · ${Calc.fmtKg(p.value, unit)}${Calc.unitLabel(unit)}`}
            </Txt>
          </Row>
        ))}
        <Txt weight={800} size={13} color={C.Mut} style={{ paddingHorizontal: 16, paddingVertical: 8 }}>
          {s("Exercises", "Exercices").toUpperCase()}
        </Txt>
        {draft.exercises
          .filter((ex) => ex.sets.some((st) => st.done))
          .map((ex, ei) => (
            <View key={ei} style={{ paddingHorizontal: 16, paddingVertical: 8 }}>
              <Txt weight={700} size={15.5} color="#028CFD" numberOfLines={2}>
                {exName(ex.name)}
              </Txt>
              <View style={{ height: 4 }} />
              {ex.sets
                .filter((st) => st.done && (st.kg != null || st.reps != null))
                .map((st, i) => (
                  <View key={i}>
                    <Row
                      style={{
                        borderRadius: 8,
                        backgroundColor: st.prW || st.prE ? "#1F3B2C" : C.Card,
                        paddingHorizontal: 10,
                        paddingVertical: 8,
                      }}
                    >
                      <Txt size={13} color={C.Mut} style={{ width: 20 }}>
                        {i + 1}
                      </Txt>
                      <Txt weight={600} size={13.5} style={{ flex: 1 }}>
                        {`${st.kg != null ? Calc.fmtKg(st.kg, unit) + Calc.unitLabel(unit) : "—"} × ${st.reps ?? "—"}`}
                      </Txt>
                      {st.prW || st.prE ? <MIcon name="emoji-events" size={14} color={C.Gold} /> : null}
                    </Row>
                    <View style={{ height: 3 }} />
                  </View>
                ))}
            </View>
          ))}
        {draft.notes !== "" ? (
          <View style={{ paddingHorizontal: 16, paddingVertical: 8 }}>
            <Row>
              <MIcon name="notes" size={15} color={C.Mut} />
              <View style={{ width: 6 }} />
              <Txt weight={800} size={13} color={C.Mut}>
                {s("Notes", "Notes").toUpperCase()}
              </Txt>
            </Row>
            <View style={{ height: 6 }} />
            <Txt size={13.5} style={{ lineHeight: 19 }}>
              {draft.notes}
            </Txt>
          </View>
        ) : null}
      </ScrollView>
      {/* final save */}
      <Pressable
        onPress={() => (Repo.draftDiffersFromRoutine() ? setShowSaveRoutine(true) : doFinish())}
        style={{
          marginHorizontal: 16,
          marginBottom: 12,
          height: 50,
          borderRadius: 999,
          backgroundColor: "#028CFD",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <Txt weight={700} size={15} color="#fff">
          TERMINER
        </Txt>
      </Pressable>
      <Alert
        visible={showSaveRoutine}
        title={s("Routine modified", "Routine modifiée")}
        message={s(
          "Save these changes for your next sessions?",
          "Enregistrer les modifications pour les prochaines séances ?",
        )}
        confirmLabel={s("Save", "Enregistrer")}
        dismissLabel={s("Skip", "Ignorer")}
        onConfirm={() => {
          setShowSaveRoutine(false);
          Repo.updateRoutineFromDraft();
          doFinish();
        }}
        onDismissPress={() => {
          setShowSaveRoutine(false);
          doFinish();
        }}
        onDismiss={() => {
          setShowSaveRoutine(false);
          doFinish();
        }}
      />
    </View>
  );
}

function SummaryStat({ value, label, accent = false }: { value: string; label: string; accent?: boolean }) {
  return (
    <View style={{ flex: 1, paddingVertical: 4 }}>
      <Txt size={13} weight={500} color={C.Mut}>
        {label}
      </Txt>
      <View style={{ height: 2 }} />
      <Txt size={21} weight={800} numberOfLines={1} color={accent ? "#028CFD" : C.Text}>
        {value}
      </Txt>
    </View>
  );
}
