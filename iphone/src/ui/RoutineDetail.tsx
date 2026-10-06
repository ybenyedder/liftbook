/** Routine detail — port of RoutineDetailScreen (Routines.kt). */
import React, { useMemo, useRef, useState } from "react";
import { Pressable, ScrollView, Share, View } from "react-native";
import * as Calc from "../calc";
import { Repo } from "../repo";
import { Workout } from "../models";
import { Nav } from "../nav";
import { useStore } from "../store";
import { C, accentColor } from "../theme";
import { s } from "../l10n";
import { exName } from "../l10nshim";
import { Alert, LineChart, Menu, PrimaryButton, PromptDialog, Row, Txt, WorkoutInProgressDialog, toast } from "./comps";
import { IllIcon, MIcon } from "./iconsEx";
import { RestTimer } from "../resttimer";

export default function RoutineDetailScreen({ id }: { id: number }) {
  useStore(Repo.rev);
  const r = Repo.routineById(id);
  const unit = Repo.settings.unit;
  const [metric, setMetric] = useState(0); // 0=Volume 1=Réps 2=Durée
  const [rMenu, setRMenu] = useState(false);
  const [confirmDel, setConfirmDel] = useState(false);
  const [rename, setRename] = useState(false);
  const menuAnchor = useRef<View>(null);
  const [busyDialog, setBusyDialog] = useState(false);
  const pendingAction = useRef<(() => void) | null>(null);
  const guard = (block: () => void) => {
    if (Repo.draft != null) {
      pendingAction.current = block;
      setBusyDialog(true);
    } else block();
  };
  const startFresh = (block: () => void) =>
    guard(() => {
      RestTimer.clear();
      Repo.discardDraft();
      block();
    });

  if (!r) {
    Nav.pop();
    return null;
  }

  const sessions = useMemo<Workout[]>(
    () =>
      [...Repo.workouts]
        .sort((a, b) => a.startedAt - b.startedAt)
        .filter((w) => w.exercises.some((e) => r.exercises.some((re) => re.name === e.name)))
        .slice(-12),
    [Repo.rev.value, r.id],
  );
  const series: [string, number][] =
    sessions.length > 0
      ? sessions
          .map((w) => {
            const v = metric === 0 ? Calc.vol(w) : metric === 1 ? Calc.reps(w) : (w.endedAt - w.startedAt) / 60000;
            return [Calc.fmtDateShort(w.startedAt), v] as [string, number];
          })
          .slice(-10)
      : [];
  const volTargets = r.exercises.reduce((a, e) => a + e.sets.reduce((b, st) => b + (st.kg ?? 0) * (st.reps ?? 0), 0), 0);
  const accent = accentColor(Repo.settings.accent);

  const doShare = () => {
    const lines: string[] = [r.name, ""];
    for (const ex of r.exercises) {
      lines.push(`${exName(ex.name)} (${ex.sets.length} séries)`);
      ex.sets.forEach((st, i) => {
        const kgLabel = st.kg != null ? `${Calc.fmtKg(st.kg, unit)} kg` : "";
        lines.push(`  ${i + 1}. ${kgLabel} × ${st.reps ?? "—"}`);
      });
    }
    void Share.share({ message: lines.join("\n") });
  };

  return (
    <View style={{ flex: 1, backgroundColor: C.Bg }}>
      {/* top bar */}
      <Row style={{ paddingHorizontal: 8, paddingVertical: 6 }}>
        <Pressable onPress={() => Nav.pop()} hitSlop={6} style={{ padding: 6 }}>
          <MIcon name="arrow-back" size={24} color={C.Text} />
        </Pressable>
        <Txt weight={600} size={16} style={{ flex: 1, textAlign: "center" }}>
          {s("Routine", "Routine")}
        </Txt>
        <Pressable onPress={doShare} hitSlop={6} style={{ padding: 6 }}>
          <MIcon name="ios-share" size={24} color={C.Text} />
        </Pressable>
        <Pressable ref={menuAnchor as any} onPress={() => setRMenu(true)} hitSlop={6} style={{ padding: 6 }}>
          <MIcon name="more-horiz" size={24} color={C.Text} />
        </Pressable>
      </Row>
      <Menu
        visible={rMenu}
        onClose={() => setRMenu(false)}
        anchorRef={menuAnchor}
        items={[
          {
            label: s("Edit routine", "Modifier la routine"),
            icon: "edit",
            onPress: () =>
              startFresh(() => {
                Repo.startRoutine(r.id);
                Nav.push({ t: "Logger" });
              }),
          },
          { label: s("Rename routine", "Renommer la routine"), icon: "drive-file-rename-outline", onPress: () => setRename(true) },
          {
            label: s("Duplicate routine", "Dupliquer la routine"),
            icon: "content-copy",
            onPress: () => {
              Repo.duplicateRoutine(r.id);
              toast(s("Routine duplicated", "Routine dupliquée"));
            },
          },
          { label: s("Delete routine", "Supprimer la routine"), icon: "delete", red: true, onPress: () => setConfirmDel(true) },
        ]}
      />
      <ScrollView contentContainerStyle={{ paddingBottom: 24 }} keyboardShouldPersistTaps="handled">
        <Txt weight={800} size={26} style={{ paddingHorizontal: 16, paddingTop: 8 }}>
          {r.name}
        </Txt>
        <Txt size={14} color={C.Mut} style={{ paddingHorizontal: 16, paddingTop: 2, paddingBottom: 12 }}>
          {s("Created by %1$s", "Créée par %1$s").replace("%1$s", Repo.settings.handle)}
        </Txt>
        <PrimaryButton
          text="Commencer la Routine"
          onClick={() =>
            startFresh(() => {
              Repo.startWorkout(r.id);
              Nav.push({ t: "Logger" });
            })
          }
          style={{ marginHorizontal: 16 }}
        />
        {sessions.length > 0 ? (
          <View>
            <Row style={{ paddingHorizontal: 16, paddingTop: 18, paddingBottom: 4, alignItems: "flex-end" }}>
              <Txt weight={800} size={19}>
                {metric === 0
                  ? `${Calc.fmtVol(volTargets, unit)} kg`
                  : metric === 1
                    ? `${r.exercises.reduce((a, e) => a + e.sets.reduce((b, st) => b + (st.reps ?? 0), 0), 0)} réps`
                    : "—"}
              </Txt>
              <View style={{ width: 8 }} />
              <Txt weight={600} size={14} color={accent}>
                {sessions.length ? Calc.fmtDateShort(sessions[sessions.length - 1].startedAt) : ""}
              </Txt>
              <View style={{ flex: 1 }} />
              <Txt weight={600} size={14} color={accent}>
                {s("3 months", "3 derniers mois")}
              </Txt>
            </Row>
            <LineChart points={series} fmtLabel={(v) => (metric === 2 ? `${Math.round(v)}m` : Calc.fmtVol(v, unit))} />
            <Row style={{ paddingHorizontal: 16, paddingVertical: 8, gap: 8 }}>
              {[s("Volume", "Volume"), s("Reps", "Réps"), s("Duration", "Durée")].map((label, i) => (
                <Pressable
                  key={label}
                  onPress={() => setMetric(i)}
                  style={{
                    borderRadius: 999,
                    backgroundColor: metric === i ? accent : C.Card2,
                    paddingHorizontal: 16,
                    paddingVertical: 8,
                  }}
                >
                  <Txt weight={600} size={13.5} color={metric === i ? C.AccText : C.Text}>
                    {label}
                  </Txt>
                </Pressable>
              ))}
            </Row>
          </View>
        ) : null}
        {/* exercises */}
        <Row style={{ paddingHorizontal: 16, paddingVertical: 10 }}>
          <Txt size={18} color={C.Mut} style={{ flex: 1 }}>
            {s("Exercises", "Exercices")}
          </Txt>
          <Pressable
            onPress={() =>
              startFresh(() => {
                Repo.startRoutine(r.id);
                Nav.push({ t: "Logger" });
              })
            }
          >
            <Txt weight={600} size={15} color={accent}>
              {s("Edit Routine", "Modifier la Routine")}
            </Txt>
          </Pressable>
        </Row>
        {r.exercises.map((ex, ei) => {
          const effective = ex.restSec ?? Repo.settings.restSec;
          const m = Math.floor(effective / 60);
          const sec = effective % 60;
          return (
            <View key={ei} style={{ paddingBottom: 18 }}>
              <Row style={{ paddingHorizontal: 16 }}>
                <View style={{ width: 42, height: 42, borderRadius: 21, backgroundColor: C.Card2, alignItems: "center", justifyContent: "center" }}>
                  <IllIcon muscle={ex.muscle} size={30} />
                </View>
                <View style={{ width: 14 }} />
                <Pressable onPress={() => Nav.push({ t: "ExerciseDetail", name: ex.name })} style={{ flex: 1 }}>
                  <Txt weight={600} size={18} color={accent} numberOfLines={1}>
                    {exName(ex.name)}
                  </Txt>
                </Pressable>
              </Row>
              <Row style={{ paddingHorizontal: 16, paddingVertical: 10 }}>
                <MIcon name="timer" size={17} color={accent} />
                <View style={{ width: 8 }} />
                <Txt weight={500} size={14.5} color={accent}>
                  {s("Rest Timer: %1$s", "Minuteur de Repos: %1$s").replace("%1$s", `${m > 0 ? `${m}min ` : ""}${sec}s`)}
                </Txt>
              </Row>
              <Row style={{ paddingHorizontal: 16 }}>
                <Txt size={12} color={C.Mut} style={{ flex: 1 }}>
                  {s("SET", "SÉRIE")}
                </Txt>
                <Txt size={12} color={C.Mut} style={{ flex: 1, textAlign: "center" }}>
                  KG
                </Txt>
                <Txt size={12} color={C.Mut} style={{ flex: 1, textAlign: "right" }}>
                  {s("REPS", "RÉPS")}
                </Txt>
              </Row>
              {ex.sets.map((st, si) => (
                <Row
                  key={si}
                  style={{
                    paddingHorizontal: 16,
                    paddingVertical: 10,
                    backgroundColor: si % 2 === 1 ? C.Card : "transparent",
                  }}
                >
                  <Txt size={15} style={{ flex: 1 }}>
                    {si + 1}
                  </Txt>
                  <Txt size={15} style={{ flex: 1, textAlign: "center" }}>
                    {st.kg != null ? Calc.fmtKg(st.kg, unit) : "—"}
                  </Txt>
                  <Txt size={15} style={{ flex: 1, textAlign: "right" }}>
                    {st.reps ?? "—"}
                  </Txt>
                </Row>
              ))}
            </View>
          );
        })}
      </ScrollView>
      <WorkoutInProgressDialog
        visible={busyDialog}
        onDismiss={() => {
          setBusyDialog(false);
          pendingAction.current = null;
        }}
        onResume={() => {
          setBusyDialog(false);
          pendingAction.current = null;
          Nav.push({ t: "Logger" });
        }}
        onRestart={() => {
          setBusyDialog(false);
          pendingAction.current?.();
        }}
      />
      <PromptDialog
        visible={rename}
        title={s("Rename routine", "Renommer la routine")}
        initial={r.name}
        confirmLabel={s("Save", "Enregistrer")}
        onConfirm={(v) => {
          Repo.renameRoutine(r.id, v);
          setRename(false);
        }}
        onDismiss={() => setRename(false)}
      />
      <Alert
        visible={confirmDel}
        title={s("Delete routine?", "Supprimer la routine ?")}
        message={s("This routine will be removed from your list.", "Cette routine sera retirée de ta liste.")}
        confirmLabel={s("Delete", "Supprimer")}
        confirmColor={C.Red}
        onConfirm={() => {
          setConfirmDel(false);
          Repo.deleteRoutine(r.id);
          Nav.pop();
        }}
        onDismiss={() => setConfirmDel(false)}
        dismissLabel={s("Cancel", "Annuler")}
      />
    </View>
  );
}
