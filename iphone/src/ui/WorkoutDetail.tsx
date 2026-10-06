/** Past workout detail — port of WorkoutDetailScreen (History.kt). */
import React, { useMemo, useRef, useState } from "react";
import { Pressable, ScrollView, Share, View } from "react-native";
import * as Calc from "../calc";
import { Repo } from "../repo";
import { Nav, DeletedUndo } from "../nav";
import { useStore } from "../store";
import { C, accentColor } from "../theme";
import { s } from "../l10n";
import { exName } from "../l10nshim";
import { Alert, AppCard, Menu, Row, Txt, toast } from "./comps";
import { MIcon } from "./iconsEx";

export default function WorkoutDetailScreen({ id }: { id: number }) {
  useStore(Repo.rev);
  const w = Repo.workoutById(id);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [detailMenu, setDetailMenu] = useState(false);
  const anchor = useRef<View>(null);
  const prevSetsAll = useMemo(() => (w ? w.exercises.map((ex) => Repo.prevSetsBefore(w.startedAt, ex.name)) : []), [id, Repo.rev.value]);
  if (!w) return null;
  const accent = accentColor(Repo.settings.accent);
  const unit = Repo.settings.unit;

  const shareWorkout = () => {
    const lines: string[] = [w.name, Calc.fmtDateFull(w.startedAt)];
    lines.push(`Temps ${Calc.fmtDur(w.endedAt - w.startedAt)} · Volume ${Calc.fmtVol(Calc.vol(w), unit)} kg · Records ${w.prs.length}`);
    lines.push("");
    for (const ex of w.exercises) {
      lines.push(`${exName(ex.name)} (${ex.sets.length} séries)`);
      for (const st of ex.sets) {
        lines.push(`  ${st.kg != null ? Calc.fmtKg(st.kg, unit) + " kg" : ""} × ${st.reps ?? "—"}`);
      }
    }
    void Share.share({ message: lines.join("\n") });
  };

  return (
    <View style={{ flex: 1, backgroundColor: C.Bg }}>
      <Row style={{ paddingHorizontal: 8, paddingVertical: 6 }}>
        <Pressable onPress={() => Nav.pop()} hitSlop={6} style={{ padding: 6 }}>
          <MIcon name="arrow-back" size={24} color={C.Text} />
        </Pressable>
        <Txt weight={800} size={16} numberOfLines={1} style={{ flex: 1 }}>
          {w.name}
        </Txt>
        <Pressable ref={anchor as any} onPress={() => setDetailMenu(true)} hitSlop={6} style={{ padding: 6 }}>
          <MIcon name="more-horiz" size={24} color={C.Text} />
        </Pressable>
      </Row>
      <Menu
        visible={detailMenu}
        onClose={() => setDetailMenu(false)}
        anchorRef={anchor}
        items={[
          {
            label: s("Create routine from workout", "Créer une routine depuis cette séance"),
            icon: "create-new-folder",
            onPress: () => {
              Repo.routineFromWorkout(w.id, w.name);
              toast(s("Routine created", "Routine créée"));
            },
          },
          {
            label: s("Repeat this workout", "Refaire cette séance"),
            icon: "refresh",
            onPress: () => {
              if (Repo.draft != null) toast(s("Finish the current workout first", "Termine d'abord la séance en cours"));
              else {
                Repo.startRepeat(w.id);
                Nav.toTab({ t: "TrainingTab" });
                Nav.push({ t: "Logger" });
              }
            },
          },
          { label: s("Share workout", "Partager la séance"), icon: "ios-share", onPress: shareWorkout },
          { label: s("Delete workout", "Supprimer la séance"), icon: "delete", red: true, onPress: () => setConfirmDelete(true) },
        ]}
      />
      <ScrollView contentContainerStyle={{ paddingBottom: 24 }}>
        <Txt size={13} color={C.Mut} style={{ paddingHorizontal: 16, paddingVertical: 2 }}>
          {`${Calc.fmtDateFull(w.startedAt)}, ${Calc.fmtTime(w.startedAt)} — ${Calc.fmtTime(w.endedAt)}`}
        </Txt>
        <Row style={{ paddingHorizontal: 16, paddingVertical: 8, gap: 16 }}>
          <View style={{ flex: 1 }}>
            <Txt size={12} color={C.Mut}>
              {s("Time", "Temps")}
            </Txt>
            <Txt size={14} weight={600}>
              {Calc.fmtDur(w.endedAt - w.startedAt)}
            </Txt>
          </View>
          <View style={{ flex: 1.4 }}>
            <Txt size={12} color={C.Mut}>
              {s("Volume", "Volume")}
            </Txt>
            <Txt size={14} weight={600}>
              {Calc.fmtVol(Calc.vol(w), unit)} kg
            </Txt>
          </View>
          <View style={{ flex: 1 }}>
            <Txt size={12} color={C.Mut}>
              {s("Records", "Records")}
            </Txt>
            <Row>
              <Txt size={13}>🏅</Txt>
              <View style={{ width: 3 }} />
              <Txt size={14} weight={600}>
                {w.prs.length}
              </Txt>
            </Row>
          </View>
          <View style={{ flex: 1 }}>
            <Txt size={12} color={C.Mut}>
              {s("Sets", "Séries")}
            </Txt>
            <Txt size={14} weight={600}>
              {Calc.setsDone(w)}
            </Txt>
          </View>
        </Row>
        {w.prs.length > 0 ? (
          <AppCard>
            <Row style={{ padding: 14, alignItems: "flex-start" }}>
              <MIcon name="emoji-events" size={18} color={accent} />
              <View style={{ width: 10 }} />
              <View style={{ flex: 1 }}>
                {w.prs.map((p, i) => (
                  <Row key={i} style={{ paddingVertical: 3, justifyContent: "space-between" }}>
                    <Txt weight={700} size={13}>{exName(p.ex)}</Txt>
                    <Txt size={12.5} color={C.Mut}>
                      {`${Calc.fmtKg(p.value, unit)} ${Calc.unitLabel(unit)} · ${p.kind}`}
                    </Txt>
                  </Row>
                ))}
              </View>
            </Row>
          </AppCard>
        ) : null}
        {w.notes.trim() !== "" ? (
          <AppCard>
            <Row style={{ padding: 14, alignItems: "flex-start" }}>
              <MIcon name="notes" size={16} color={C.Mut} />
              <View style={{ width: 10 }} />
              <Txt size={14} style={{ lineHeight: 20, flex: 1 }}>
                {w.notes}
              </Txt>
            </Row>
          </AppCard>
        ) : null}
        {w.exercises.map((ex, ei) => {
          const prevSets = prevSetsAll[ei];
          return (
            <AppCard key={`${ex.name}-${ei}`}>
              <View style={{ padding: 14 }}>
                <Pressable onPress={() => Nav.push({ t: "ExerciseDetail", name: ex.name })}>
                  <Row>
                    <View style={{ flex: 1 }}>
                      <Txt weight={700} size={16} numberOfLines={1}>
                        {exName(ex.name)}
                      </Txt>
                      <Txt size={13} color={C.Mut}>
                        {`${ex.sets.length} ${ex.sets.length > 1 ? s("series", "séries") : s("series", "série")}`}
                      </Txt>
                    </View>
                    <MIcon name="chevron-right" size={20} color={C.Mut} />
                  </Row>
                </Pressable>
                <View style={{ height: 6 }} />
                <Row>
                  <Txt size={11} color={C.Mut} style={{ width: 34 }}>
                    SÉRIE
                  </Txt>
                  <Txt size={11} color={C.Mut} style={{ flex: 1.1 }}>
                    {s("PREVIOUS", "PRÉCÉDENTE")}
                  </Txt>
                  <Txt size={11} color={C.Mut} style={{ flex: 1, textAlign: "center" }}>
                    KG
                  </Txt>
                  <Txt size={11} color={C.Mut} style={{ flex: 1, textAlign: "right" }}>
                    RÉPS
                  </Txt>
                  <View style={{ width: 56 }} />
                </Row>
                {ex.sets.map((st, i) => (
                  <Row
                    key={i}
                    style={{
                      paddingVertical: 9,
                      backgroundColor: i % 2 === 1 ? "rgba(42,42,45,0.35)" : "transparent",
                    }}
                  >
                    <Txt size={13} weight={600} color={C.Mut} style={{ width: 34 }}>
                      {i + 1}
                    </Txt>
                    <Txt size={13} color={C.Mut} style={{ flex: 1.1 }} numberOfLines={1}>
                      {prevSets?.[i] ?? "—"}
                    </Txt>
                    <Txt weight={600} size={13} style={{ flex: 1, textAlign: "center" }}>
                      {st.kg != null ? Calc.fmtKg(st.kg, unit) : "—"}
                    </Txt>
                    <Txt weight={600} size={13} style={{ flex: 1, textAlign: "right" }}>
                      {st.reps ?? "—"}
                    </Txt>
                    <View style={{ width: 56, alignItems: "flex-start" }}>
                      {st.prW || st.prE ? (
                        <Txt weight={800} size={9} color={C.AccText} style={{ backgroundColor: accent, borderRadius: 5, paddingHorizontal: 6, paddingVertical: 3 }}>
                          {st.prW ? "WEIGHT PR" : "1RM PR"}
                        </Txt>
                      ) : null}
                    </View>
                  </Row>
                ))}
                {ex.notes !== "" ? (
                  <Row style={{ paddingTop: 6, alignItems: "flex-start" }}>
                    <MIcon name="notes" size={14} color={C.Mut} />
                    <View style={{ width: 6 }} />
                    <Txt size={12.5} color={C.Mut} style={{ flex: 1 }}>
                      {ex.notes}
                    </Txt>
                  </Row>
                ) : null}
              </View>
            </AppCard>
          );
        })}
      </ScrollView>
      <Alert
        visible={confirmDelete}
        title="Supprimer la séance ?"
        message="Cette séance et ses records seront définitivement supprimés."
        confirmLabel="Delete"
        confirmColor={C.Red}
        dismissLabel="Cancel"
        onConfirm={() => {
          setConfirmDelete(false);
          Repo.deleteWorkout(w.id);
          DeletedUndo.set(w);
          Nav.pop();
        }}
        onDismiss={() => setConfirmDelete(false)}
      />
    </View>
  );
}
