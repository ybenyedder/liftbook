/** Séance screen — port of Logger.kt (workout logger + routine editor, rest bar, PR badge, picker). */
import React, { useEffect, useMemo, useRef, useState } from "react";
import { Animated, KeyboardAvoidingView, Platform, Pressable, ScrollView, TextInput, View } from "react-native";
import * as Haptics from "expo-haptics";
import * as Calc from "../calc";
import { Repo } from "../repo";
import { ExEntry, SetEntry, newEx, newSet } from "../models";
import { EXERCISES, MUSCLES, matches } from "../l10ndata";
import { Nav } from "../nav";
import { useStore } from "../store";
import { C, FONT, accentColor } from "../theme";
import { s } from "../l10n";
import { exName, muscleName, equipName } from "../l10nshim";
import {
  Alert, AppCard, BigField, BodyMap, Chip, DragHandle, DragList, EmptyState, ExCircle, Menu,
  PromptDialog, Row, Sheet, TF, Txt, fmtRestLabel, toast,
} from "./comps";
import { IllIcon, MIcon } from "./iconsEx";
import { RestTimer, WorkoutNotif } from "../resttimer";
import { useTick } from "./hooks";
import { evaluatePr, PrBadge } from "../preval";

export default function LoggerScreen() {
  useStore(Repo.rev);
  const draft = Repo.draft;
  if (!draft) {
    Nav.toTab({ t: "HomeTab" });
    return null;
  }
  return <LoggerBody key={`${draft.mode}-${draft.routineId ?? "x"}-${draft.startedAt ?? 0}`} />;
}

function LoggerBody() {
  useStore(Repo.rev);
  const draft = Repo.draft!;
  const isWorkout = draft.mode === "workout";
  const unit = Repo.settings.unit;
  const [showPicker, setShowPicker] = useState(false);
  const [showReplace, setShowReplace] = useState<{ ei: number } | null>(null);
  const [showMenu, setShowMenu] = useState(false);
  const [showDiscard, setShowDiscard] = useState(false);
  const [showNoSets, setShowNoSets] = useState(false);
  const [showDeleteRoutine, setShowDeleteRoutine] = useState(false);
  const [showNotesDialog, setShowNotesDialog] = useState(false);
  const [restDialogFor, setRestDialogFor] = useState<number | null>(null);
  const [nameText, setNameText] = useState(draft.name);
  const menuAnchor = useRef<View>(null);
  const moreAnchor = useRef<View>(null);
  const scrollRef = useRef<ScrollView>(null);
  const scrollOffset = useRef(0);
  const tick = useTick(1000);
  const accent = accentColor(Repo.settings.accent);

  useEffect(() => {
    if (isWorkout && draft.startedAt != null) void WorkoutNotif.post(draft.startedAt);
    else WorkoutNotif.cancel();
    return () => WorkoutNotif.cancel();
  }, [isWorkout, draft.startedAt]);

  const moveExercise = (from: number, to: number) => {
    const list = Repo.draft?.exercises;
    if (!list || from === to || from < 0 || from >= list.length || to < 0 || to >= list.length) return;
    const item = list.splice(from, 1)[0];
    list.splice(to, 0, item);
    Repo.touchPublic();
  };

  const hasData = () => draft.exercises.some((ex) => ex.sets.some((st) => st.kg != null || st.reps != null));
  const anyDone = () => draft.exercises.some((ex) => ex.sets.some((st) => st.done && (st.kg != null || st.reps != null)));

  const trySaveRoutine = () => {
    const n = nameText.trim();
    if (!n) {
      toast(s("Name your routine first", "Donne un nom à ta routine"));
      return;
    }
    if (draft.exercises.length === 0) {
      toast(s("Add at least one exercise", "Ajoute au moins un exercice"));
      return;
    }
    Repo.saveRoutine(n);
    Nav.pop();
    toast(s("Routine saved", "Routine enregistrée"));
  };

  let liveVol = 0;
  let liveSets = 0;
  for (const ex of draft.exercises)
    for (const st of ex.sets) if (st.done && st.kg != null && st.reps != null) { liveVol += st.kg * st.reps; liveSets++; }
  const muscles = useMemo(() => new Set(draft.exercises.map((e) => e.muscle)), [draft.exercises.length, Repo.rev.value]);

  return (
    <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ flex: 1, backgroundColor: C.Bg }} keyboardVerticalOffset={0}>
      <View style={{ flex: 1 }}>
        {isWorkout ? (
          <>
            {/* top bar: ∨ Entraînement | ⏱ | Terminer */}
            <Row style={{ paddingHorizontal: 12, paddingVertical: 6 }}>
              <Pressable
                ref={menuAnchor as any}
                onPress={() => setShowMenu(true)}
                style={{ flexDirection: "row", alignItems: "center", borderRadius: 10, paddingHorizontal: 4, paddingVertical: 6 }}
              >
                <MIcon name="keyboard-arrow-down" size={24} color={C.Text} />
                <View style={{ width: 6 }} />
                <Txt weight={800} size={21} numberOfLines={1}>
                  {s("Training", "Entraînement")}
                </Txt>
              </Pressable>
              <Menu
                visible={showMenu}
                onClose={() => setShowMenu(false)}
                anchorRef={menuAnchor}
                items={[
                  { label: s("Workout notes", "Notes de la séance"), icon: "notes", onPress: () => setShowNotesDialog(true) },
                  { label: s("Resume later", "Reprendre plus tard"), icon: "keyboard-arrow-down", onPress: () => Nav.pop() },
                  {
                    label: s("Discard workout", "Supprimer la séance"),
                    icon: "delete",
                    red: true,
                    onPress: () => {
                      if (hasData()) setShowDiscard(true);
                      else {
                        Repo.discardDraft();
                        Nav.pop();
                        toast("Supprimée");
                      }
                    },
                  },
                ]}
              />
              <View style={{ flex: 1 }} />
              <MIcon name="timer" size={20} color={C.Text} />
              <View style={{ width: 12 }} />
              <Pressable
                onPress={() => (anyDone() ? Nav.push({ t: "WorkoutSummary" }) : setShowNoSets(true))}
                style={{ borderRadius: 12, backgroundColor: accent, paddingHorizontal: 18, paddingVertical: 10 }}
              >
                <Txt weight={600} size={15} color={C.AccText}>
                  {s("Finish", "Terminer")}
                </Txt>
              </Pressable>
            </Row>
            {/* live stats */}
            <Row style={{ paddingHorizontal: 20, paddingVertical: 4, gap: 8 }}>
              <LiveStat value={Calc.fmtClock(Math.max(0, tick - (draft.startedAt ?? tick)))} label={s("Duration", "Durée")} accent />
              <LiveStat value={`${Calc.fmtVol(liveVol, unit)} ${Calc.unitLabel(unit)}`} label={s("Volume", "Volume")} />
              <LiveStat value={`${liveSets}`} label={s("Sets", "Séries")} />
              <BodyMap front muscles={muscles} />
              <BodyMap front={false} muscles={muscles} />
            </Row>
            <View style={{ height: 6 }} />
          </>
        ) : (
          <>
            {/* routine editor top bar */}
            <Row style={{ paddingHorizontal: 12, paddingVertical: 6 }}>
              <Pressable
                onPress={() => {
                  if (hasData()) setShowDiscard(true);
                  else {
                    Repo.discardDraft();
                    Nav.pop();
                  }
                }}
                style={{ borderRadius: 8, paddingHorizontal: 6, paddingVertical: 10 }}
              >
                <Txt weight={600} size={15} color={accent}>
                  {s("Cancel", "Annuler")}
                </Txt>
              </Pressable>
              <Txt weight={800} size={17} style={{ flex: 1, textAlign: "center" }}>
                {draft.routineId != null ? s("Edit Routine", "Modifier la Routine") : s("Create Routine", "Créer une Routine")}
              </Txt>
              <Pressable onPress={trySaveRoutine} style={{ borderRadius: 12, backgroundColor: accent, paddingHorizontal: 16, paddingVertical: 10 }}>
                <Txt weight={600} size={15} color={C.AccText}>
                  {s("Save", "Enregistrer")}
                </Txt>
              </Pressable>
              <Pressable ref={moreAnchor as any} onPress={() => setShowMenu(true)} hitSlop={4} style={{ padding: 4 }}>
                <MIcon name="more-vert" size={17} color={C.Mut} />
              </Pressable>
              <Menu
                visible={showMenu}
                onClose={() => setShowMenu(false)}
                anchorRef={moreAnchor}
                items={[
                  ...(draft.routineId != null
                    ? [{ label: s("Delete routine", "Supprimer la routine"), icon: "delete" as const, red: true, onPress: () => setShowDeleteRoutine(true) }]
                    : []),
                  {
                    label: s("Discard changes", "Ignorer les modifications"),
                    icon: "delete",
                    red: true,
                    onPress: () => {
                      if (hasData()) setShowDiscard(true);
                      else {
                        Repo.discardDraft();
                        Nav.pop();
                        toast("Supprimée");
                      }
                    },
                  },
                ]}
              />
            </Row>
            <View style={{ paddingHorizontal: 16, paddingVertical: 4 }}>
              <BigField
                value={nameText}
                onChange={(v) => {
                  setNameText(v);
                  if (Repo.draft) Repo.draft.name = v;
                }}
                placeholder={s("Routine title", "Titre de la routine")}
              />
            </View>
          </>
        )}

        {/* exercise sections */}
        <ScrollView
          ref={scrollRef}
          style={{ flex: 1 }}
          contentContainerStyle={{ paddingBottom: 150 }}
          onScroll={(e) => (scrollOffset.current = e.nativeEvent.contentOffset.y)}
          scrollEventThrottle={16}
          keyboardShouldPersistTaps="handled"
        >
          {draft.exercises.length === 0 ? (
            <EmptyState
              text={
                isWorkout
                  ? "Aucun exercice.\nTouche « Ajouter un Exercice » pour commencer."
                  : "Cette routine est vide.\nTouche « Ajouter un Exercice » pour la construire."
              }
            />
          ) : (
            <DragList
              count={draft.exercises.length}
              scrollRef={scrollRef}
              scrollOffsetRef={scrollOffset}
              onMove={moveExercise}
              renderItem={({ index, dragging, translateY, onGrab }) => (
                <ExCard
                  key={`${draft.exercises[index].name}-${index}`}
                  ei={index}
                  dragging={dragging}
                  translateY={translateY}
                  onGrab={onGrab}
                  isWorkout={isWorkout}
                  unit={unit}
                  onReplace={() => setShowReplace({ ei: index })}
                  onRest={() => setRestDialogFor(index)}
                />
              )}
            />
          )}
          {/* blue add button */}
          <Pressable
            onPress={() => setShowPicker(true)}
            style={{
              marginHorizontal: 16,
              marginVertical: 8,
              height: 50,
              borderRadius: 12,
              backgroundColor: accent,
              alignItems: "center",
              justifyContent: "center",
              flexDirection: "row",
            }}
          >
            <MIcon name="add" size={20} color={C.AccText} />
            <View style={{ width: 10 }} />
            <Txt weight={600} size={15} color={C.AccText}>
              {s("Add Exercise", "Ajouter un Exercice")}
            </Txt>
          </Pressable>
        </ScrollView>

        {/* bottom dock with rest bar */}
        <View style={{ paddingHorizontal: 12, paddingVertical: 8, paddingBottom: 12 }}>
          <RestBarHost />
        </View>
      </View>

      {/* pickers / dialogs */}
      <Sheet visible={showPicker} onClose={() => setShowPicker(false)}>
        <PickerContent
          onClose={() => setShowPicker(false)}
          onPick={(name) => {
            Repo.addExToDraft(name);
            setShowPicker(false);
            toast(`${exName(name)} ajouté`);
          }}
        />
      </Sheet>
      <Sheet visible={showReplace != null} onClose={() => setShowReplace(null)}>
        <PickerContent
          onClose={() => setShowReplace(null)}
          onPick={(name) => {
            const ei = showReplace?.ei;
            const d = Repo.draft;
            if (d && ei != null && d.exercises[ei]) {
              d.exercises[ei].name = name;
              d.exercises[ei].muscle = EXERCISES.find((e) => e.name === name)?.muscle ?? "";
              Repo.touchPublic();
            }
            setShowReplace(null);
          }}
        />
      </Sheet>
      <RestSheetHost
        visible={restDialogFor != null}
        initialSec={restDialogFor != null ? draft.exercises[restDialogFor]?.restSec ?? Repo.settings.restSec : 90}
        onDismiss={() => setRestDialogFor(null)}
        onDone={(sec) => {
          if (restDialogFor != null && draft.exercises[restDialogFor]) {
            draft.exercises[restDialogFor].restSec = sec;
            Repo.touchPublic();
          }
          setRestDialogFor(null);
        }}
        onReset={() => {
          if (restDialogFor != null && draft.exercises[restDialogFor]) {
            draft.exercises[restDialogFor].restSec = null;
            Repo.touchPublic();
          }
          setRestDialogFor(null);
        }}
      />
      <Alert
        visible={showDiscard}
        title={isWorkout ? s("Discard workout?", "Supprimer la séance ?") : s("Discard changes?", "Ignorer les modifications ?")}
        message={
          isWorkout
            ? "Your logged sets from this session will be lost."
            : "Changes to this routine will be lost."
        }
        confirmLabel="Discard"
        confirmColor={C.Red}
        onConfirm={() => {
          setShowDiscard(false);
          RestTimer.clear();
          Repo.discardDraft();
          Nav.pop();
          toast("Supprimée");
        }}
        onDismiss={() => setShowDiscard(false)}
        dismissLabel={s("Cancel", "Annuler")}
      />
      <Alert
        visible={showNoSets}
        title={s("No sets completed", "Aucune série terminée")}
        message="There is nothing to save yet. Discard this workout?"
        confirmLabel="Discard"
        confirmColor={C.Red}
        onConfirm={() => {
          setShowNoSets(false);
          RestTimer.clear();
          Repo.discardDraft();
          Nav.pop();
          toast("Supprimée");
        }}
        onDismiss={() => setShowNoSets(false)}
        dismissLabel={s("Keep editing", "Continuer")}
      />
      <NotesDialogHost visible={showNotesDialog} onClose={() => setShowNotesDialog(false)} />
      <Alert
        visible={showDeleteRoutine}
        title="Delete routine?"
        message="This routine will be removed from your list."
        confirmLabel={s("Discard", "Supprimer")}
        confirmColor={C.Red}
        onConfirm={() => {
          setShowDeleteRoutine(false);
          if (Repo.draft?.routineId != null) Repo.deleteRoutine(Repo.draft.routineId);
          RestTimer.clear();
          Repo.discardDraft();
          Nav.pop();
          toast("Routine deleted");
        }}
        onDismiss={() => setShowDeleteRoutine(false)}
        dismissLabel="Annuler"
      />
      {isWorkout ? <PrBadgeHost /> : null}
    </KeyboardAvoidingView>
  );
}

function NotesDialogHost({ visible, onClose }: { visible: boolean; onClose: () => void }) {
  const [notes, setNotes] = useState(Repo.draft?.notes ?? "");
  if (!visible) return null;
  return (
    <Alert
      visible
      title={s("Workout notes", "Notes de la séance")}
      message={undefined}
      confirmLabel={s("Save", "Enregistrer")}
      onConfirm={() => {
        if (Repo.draft) Repo.draft.notes = notes;
        onClose();
      }}
      onDismiss={onClose}
      dismissLabel={s("Cancel", "Annuler")}
      customBody={<TF value={notes} onChange={setNotes} placeholder={s("How did it feel?", "Comment ça s'est passé ?")} multiline height={90} />}
    />
  );
}

/** One exercise section (Hevy: no card box, directly on black). */
function ExCard({
  ei,
  isWorkout,
  unit,
  dragging,
  translateY,
  onGrab,
  onReplace,
  onRest,
}: {
  ei: number;
  isWorkout: boolean;
  unit: string;
  dragging: boolean;
  translateY: Animated.Value;
  onGrab: (pageY: number) => void;
  onReplace: () => void;
  onRest: () => void;
}) {
  useStore(Repo.rev);
  const draft = Repo.draft!;
  const ex = draft.exercises[ei];
  const [menuOpen, setMenuOpen] = useState(false);
  const [showNotes, setShowNotes] = useState(false);
  const anchor = useRef<View>(null);
  if (!ex) return null;
  const isLast = ei >= draft.exercises.length - 1;
  const accent = accentColor(Repo.settings.accent);
  const prev = isWorkout ? Repo.prevFor(ex.name) : null;

  return (
    <Animated.View style={{ transform: [{ translateY: dragging ? translateY : 0 }], zIndex: dragging ? 10 : 0, opacity: dragging ? 0.94 : 1 }}>
      <DragHandle onGrab={onGrab} dragging={dragging}>
        <View style={{ paddingVertical: 8 }}>
          {/* title row */}
          <Pressable onPress={() => Nav.push({ t: "ExerciseDetail", name: ex.name })}>
            <Row style={{ paddingHorizontal: 12, paddingVertical: 6 }}>
              {ex.superset ? (
                <>
                  <View style={{ width: 3, height: 34, borderRadius: 2, backgroundColor: accent }} />
                  <View style={{ width: 8 }} />
                </>
              ) : null}
              <ExCircle muscle={ex.muscle} size={46} />
              <View style={{ width: 10 }} />
              <Txt weight={700} size={17} color={accent} numberOfLines={2} style={{ flex: 1 }}>
                {exName(ex.name)}
              </Txt>
              <Pressable ref={anchor as any} onPress={() => setMenuOpen(true)} hitSlop={4} style={{ padding: 5 }}>
                <MIcon name="more-vert" size={20} color={C.Mut} />
              </Pressable>
              <Menu
                visible={menuOpen}
                onClose={() => setMenuOpen(false)}
                anchorRef={anchor}
                items={[
                  { label: s("Replace exercise", "Remplacer l'exercice"), icon: "swap-horiz", onPress: onReplace },
                  {
                    label: s("Duplicate exercise", "Dupliquer l'exercice"),
                    icon: "content-copy",
                    onPress: () => {
                      const d = Repo.draft;
                      if (d && ei < d.exercises.length) {
                        d.exercises.splice(ei + 1, 0, {
                          name: ex.name, muscle: ex.muscle, notes: ex.notes, superset: ex.superset, restSec: ex.restSec,
                          sets: ex.sets.map((st) => newSet(st.kg, st.reps, st.done)),
                        });
                        Repo.touchPublic();
                      }
                    },
                  },
                  {
                    label: s("Rest timer", "Minuteur de repos") + (ex.restSec != null ? ` : ${ex.restSec}s` : ""),
                    icon: "timer",
                    onPress: onRest,
                  },
                  ...(!isLast
                    ? [
                        {
                          label: ex.superset ? s("Remove superset", "Retirer le superset") : s("Superset with next", "Superset avec le suivant"),
                          icon: "link" as const,
                          onPress: () => {
                            ex.superset = !ex.superset;
                            Repo.touchPublic();
                          },
                        },
                      ]
                    : []),
                  {
                    label: s("Delete exercise", "Supprimer l'exercice"),
                    icon: "delete",
                    red: true,
                    onPress: () => {
                      const d = Repo.draft;
                      if (d && ei < d.exercises.length) d.exercises.splice(ei, 1);
                      Repo.touchPublic();
                    },
                  },
                ]}
              />
            </Row>
          </Pressable>
          {/* notes */}
          <Pressable onPress={() => setShowNotes(true)} style={{ paddingHorizontal: 16, paddingVertical: 6 }}>
            {ex.notes === "" ? (
              <Txt size={15} color={C.Mut}>
                {s("Add notes here…", "Ajouter des notes ici…")}
              </Txt>
            ) : (
              <Row style={{ alignItems: "flex-start" }}>
                <MIcon name="notes" size={14} color={C.Mut} />
                <View style={{ width: 6 }} />
                <Txt size={13.5} color={C.Mut} numberOfLines={4} style={{ lineHeight: 19, flex: 1 }}>
                  {ex.notes}
                </Txt>
              </Row>
            )}
          </Pressable>
          {/* blue rest line */}
          <Pressable onPress={onRest} style={{ marginHorizontal: 12, borderRadius: 8, paddingHorizontal: 4, paddingVertical: 4, flexDirection: "row", alignItems: "center" }}>
            <MIcon name="timer" size={15} color={accent} />
            <View style={{ width: 6 }} />
            <Txt weight={500} size={15} color={accent}>
              {s("Rest: %1$s", "Repos: %1$s").replace("%1$s", fmtRestLabel(ex.restSec ?? Repo.settings.restSec))}
            </Txt>
          </Pressable>
          <View style={{ height: 8 }} />
          {/* column headers */}
          <Row style={{ paddingHorizontal: 12 }}>
            <Txt size={12} color={C.Mut} style={{ width: 38, textAlign: "center" }}>
              {s("SET", "SÉRIE")}
            </Txt>
            {isWorkout ? (
              <Txt size={12} color={C.Mut} style={{ flex: 1.15, textAlign: "center" }}>
                {s("PREVIOUS", "PRÉCÉDENT")}
              </Txt>
            ) : null}
            <Row style={{ flex: 1, justifyContent: "center" }}>
              <MIcon name="fitness-center" size={12} color={C.Mut} />
              <View style={{ width: 3 }} />
              <Txt size={12} color={C.Mut}>
                {unit.toUpperCase()}
              </Txt>
            </Row>
            <Txt size={12} color={C.Mut} style={{ flex: 1, textAlign: "center" }}>
              {s("REPS", "RÉPS")}
            </Txt>
            <View style={{ width: 44, alignItems: "center" }}>
              {isWorkout ? <MIcon name="check" size={15} color={C.Mut} /> : null}
            </View>
          </Row>
          <View style={{ height: 4 }} />
          {ex.sets.map((st, si) => (
            <SetRow
              key={si}
              s={st}
              si={si}
              ei={ei}
              isWorkout={isWorkout}
              unit={unit}
              ex={ex}
              prevText={isWorkout ? prev?.[si] ?? null : null}
            />
          ))}
          {/* add set */}
          <Pressable
            onPress={() => {
              const last = ex.sets[ex.sets.length - 1];
              ex.sets.push(newSet(last?.kg ?? null, last?.reps ?? null, false));
              Repo.touchPublic();
            }}
            style={{ marginHorizontal: 12, marginVertical: 8, borderRadius: 10, backgroundColor: C.Card2, paddingVertical: 12, alignItems: "center", flexDirection: "row", justifyContent: "center" }}
          >
            <MIcon name="add" size={18} color={C.Text} />
            <View style={{ width: 8 }} />
            <Txt weight={500} size={15}>
              {s("Add Set", "Ajouter une Série")}
            </Txt>
          </Pressable>
          <NotesDialog ex={ex} visible={showNotes} onClose={() => setShowNotes(false)} />
        </View>
      </DragHandle>
    </Animated.View>
  );
}

function NotesDialog({ ex, visible, onClose }: { ex: ExEntry; visible: boolean; onClose: () => void }) {
  const [v, setV] = useState(ex.notes);
  useEffect(() => {
    if (visible) setV(ex.notes);
  }, [visible]);
  if (!visible) return null;
  return (
    <Alert
      visible
      title={exName(ex.name)}
      confirmLabel={s("Save", "Enregistrer")}
      onConfirm={() => {
        ex.notes = v;
        Repo.touchPublic();
        onClose();
      }}
      onDismiss={onClose}
      dismissLabel={s("Cancel", "Annuler")}
      customBody={<TF value={v} onChange={setV} placeholder={s("Exercise notes…", "Notes de l'exercice…")} multiline height={90} />}
    />
  );
}

/** One set row: chip / previous / kg field / reps field / check (or delete in routine editor). */
function SetRow({
  s: st,
  si,
  ei,
  isWorkout,
  unit,
  ex,
  prevText,
}: {
  s: SetEntry;
  si: number;
  ei: number;
  isWorkout: boolean;
  unit: string;
  ex: ExEntry;
  prevText: string | null;
}) {
  useStore(Repo.rev);
  const [menuOpen, setMenuOpen] = useState(false);
  const anchor = useRef<View>(null);
  const isPr = isWorkout && (st.prW || st.prE);
  const scale = useRef(new Animated.Value(st.done ? 1 : 0)).current;
  useEffect(() => {
    Animated.spring(scale, { toValue: st.done ? 1 : 0, useNativeDriver: true, friction: 6 }).start();
  }, [st.done]);

  const delSet = () => {
    const d = Repo.draft;
    if (!d || !d.exercises[ei]) return;
    if (d.exercises[ei].sets.length === 1) d.exercises.splice(ei, 1);
    else d.exercises[ei].sets.splice(si, 1);
    Repo.touchPublic();
  };

  return (
    <Row style={{ paddingHorizontal: 8, paddingVertical: 3, marginHorizontal: 4, borderRadius: 10, backgroundColor: isPr ? C.GreenBg : "transparent", padding: 2 }}>
      <View>
        <Pressable
          ref={anchor as any}
          onPress={() => setMenuOpen(true)}
          style={{
            width: 38,
            height: 38,
            borderRadius: 8,
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          {isPr ? (
            <MIcon name="emoji-events" size={20} color={C.Gold} />
          ) : (
            <View style={{ width: 38, height: 38, borderRadius: 8, backgroundColor: C.Card2, alignItems: "center", justifyContent: "center" }}>
              <Txt weight={600} size={15}>
                {si + 1}
              </Txt>
            </View>
          )}
        </Pressable>
        <Menu
          visible={menuOpen}
          onClose={() => setMenuOpen(false)}
          anchorRef={anchor}
          items={[
            {
              label: s("Copy set", "Copier la série"),
              icon: "content-copy",
              onPress: () => {
                const d = Repo.draft;
                if (d?.exercises[ei]) d.exercises[ei].sets.splice(si + 1, 0, newSet(st.kg, st.reps, false));
                Repo.touchPublic();
              },
            },
            { label: s("Delete set", "Supprimer la série"), icon: "delete", red: true, onPress: delSet },
          ]}
        />
      </View>
      <View style={{ width: 6 }} />
      {isWorkout ? (
        <Txt size={13.5} color={C.Mut} numberOfLines={1} style={{ flex: 1.15, textAlign: "center", paddingHorizontal: 2 }}>
          {prevText ?? "—"}
        </Txt>
      ) : null}
      <SetField init={Calc.fmtKg(st.kg, unit)} hint="-" onChange={(v) => (st.kg = Calc.toKg(v, unit))} keyboardType="decimal-pad" style={{ flex: 1, marginHorizontal: 4 }} />
      <SetField
        init={st.reps != null ? String(st.reps) : ""}
        hint="-"
        keyboardType="number-pad"
        onChange={(v) => {
          const digits = v.replace(/[^\d]/g, "").slice(0, 4);
          st.reps = digits === "" ? null : parseInt(digits, 10);
        }}
        style={{ flex: 1, marginHorizontal: 4 }}
      />
      <View style={{ width: 4 }} />
      {isWorkout ? (
        <Pressable
          onPress={() => {
            st.done = !st.done;
            void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => {});
            if (st.done) {
              evaluatePr(ex, st);
              RestTimer.setExercise(ex.name, ex.muscle);
              void RestTimer.start(ex.restSec ?? Repo.settings.restSec);
            }
            Repo.touchPublic();
          }}
          style={{ width: 44, height: 44, alignItems: "center", justifyContent: "center" }}
        >
          {st.done ? (
            <View style={{ position: "absolute", width: 34, height: 34, borderRadius: 8, backgroundColor: isPr ? C.Green : C.Green }} />
          ) : null}
          <Animated.View style={{ transform: [{ scale: st.done ? scale.interpolate({ inputRange: [0, 1], outputRange: [0.7, 1] }) : 1 }] }}>
            <MIcon name="check" size={22} color={st.done ? "#FFFFFF" : C.Mut} />
          </Animated.View>
        </Pressable>
      ) : (
        <Pressable onPress={delSet} style={{ width: 40, height: 40, alignItems: "center", justifyContent: "center" }}>
          <MIcon name="delete" size={18} color={C.Mut} />
        </Pressable>
      )}
    </Row>
  );
}

function SetField({
  init,
  hint,
  onChange,
  keyboardType,
  style,
}: {
  init: string;
  hint: string;
  onChange: (v: string) => void;
  keyboardType: any;
  style?: any;
}) {
  const [text, setText] = useState(init);
  useEffect(() => {
    setText(init);
  }, [init]);
  return (
    <View
      style={[
        {
          height: 44,
          borderRadius: 10,
          backgroundColor: C.Card2,
          borderWidth: 1,
          borderColor: C.Line2,
          justifyContent: "center",
          alignItems: "center",
        },
        style,
      ]}
    >
      {text === "" ? (
        <Txt size={15} color={C.Mut} style={{ position: "absolute" }}>
          {hint}
        </Txt>
      ) : null}
      <TextInput
        value={text}
        onChangeText={(v) => {
          setText(v);
          onChange(v);
        }}
        keyboardType={keyboardType}
        textAlign="center"
        selectTextOnFocus
        style={{ width: "100%", color: C.Text, fontFamily: FONT[600], fontSize: 16, paddingVertical: 0 }}
      />
    </View>
  );
}

/** Hevy live stat: gray label above the big value. */
function LiveStat({ value, label, accent = false }: { value: string; label: string; accent?: boolean }) {
  return (
    <View style={{ flex: 1, paddingVertical: 4 }}>
      <Txt size={13} weight={500} color={C.Mut}>
        {label}
      </Txt>
      <View style={{ height: 2 }} />
      <Txt size={21} weight={800} color={accent ? accentColor(Repo.settings.accent) : C.Text} numberOfLines={1}>
        {value}
      </Txt>
    </View>
  );
}

// ---------------- rest sheet + bar + PR badge ----------------

function RestSheetHost({
  visible,
  initialSec,
  onDismiss,
  onDone,
  onReset,
}: {
  visible: boolean;
  initialSec: number;
  onDismiss: () => void;
  onDone: (sec: number) => void;
  onReset: () => void;
}) {
  const values = useMemo(() => Array.from({ length: 118 }, (_, i) => (i + 3) * 5), []); // 15s → 10min
  const [sel, setSel] = useState(Math.min(600, Math.max(15, initialSec)));
  useEffect(() => {
    if (visible) setSel(Math.min(600, Math.max(15, initialSec)));
  }, [visible, initialSec]);
  const accent = accentColor(Repo.settings.accent);
  const listRef = useRef<ScrollView>(null);
  useEffect(() => {
    if (visible) {
      const idx = values.indexOf(sel);
      setTimeout(() => listRef.current?.scrollTo({ y: Math.max(0, (idx - 4) * 39), animated: false }), 60);
    }
  }, [visible]);
  if (!visible) return null;
  return (
    <Sheet visible onClose={onDismiss}>
      <View style={{ paddingHorizontal: 20 }}>
        <Txt weight={800} size={17}>
          {s("Rest Timer", "Minuteur de Repos")}
        </Txt>
        <View style={{ height: 4 }} />
        <Txt weight={800} size={26} color={accent}>
          {s("Rest: %1$s", "Repos: %1$s").replace("%1$s", fmtRestLabel(sel))}
        </Txt>
        <View style={{ height: 6 }} />
        <ScrollView ref={listRef} style={{ height: 240 }}>
          {values.map((v) => (
            <Pressable
              key={v}
              onPress={() => setSel(v)}
              style={{
                borderRadius: 10,
                backgroundColor: v === sel ? C.Card2 : "transparent",
                paddingVertical: 9,
                alignItems: "center",
              }}
            >
              <Txt size={v === sel ? 18 : 15} weight={v === sel ? 800 : 400} color={v === sel ? accent : C.Mut}>
                {fmtRestLabel(v)}
              </Txt>
            </Pressable>
          ))}
        </ScrollView>
        <View style={{ height: 10 }} />
        <Row>
          <View style={{ flex: 1 }}>
            <Pressable onPress={onReset} style={{ paddingVertical: 8 }}>
              <Txt weight={600} size={14} color={C.Mut}>
                {s("Use default", "Par défaut")}
              </Txt>
            </Pressable>
          </View>
          <View style={{ flex: 2 }}>
            <Pressable
              onPress={() => onDone(sel)}
              style={{ height: 50, borderRadius: 999, backgroundColor: accent, alignItems: "center", justifyContent: "center" }}
            >
              <Txt weight={700} size={15} color={C.AccText}>
                {s("Done", "Terminé")}
              </Txt>
            </Pressable>
          </View>
        </Row>
        <View style={{ height: 22 }} />
      </View>
    </Sheet>
  );
}

/** Hevy rest bar: blue line on top, −15 / big 01:59 / +15, blue Passer button. */
function RestBarHost() {
  useStore(RestTimer.rev);
  const now = useTick(250);
  const accent = accentColor(Repo.settings.accent);
  if (RestTimer.endAt <= 0) return null;
  const remainingMs = Math.max(0, RestTimer.endAt - now);
  const over = RestTimer.endAt - now <= 0;
  const shown = Math.floor(remainingMs / 1000);
  const text = `${String(Math.floor(shown / 60)).padStart(2, "0")}:${String(shown % 60).padStart(2, "0")}`;
  const fraction = RestTimer.totalMs > 0 ? Math.min(1, Math.max(0, remainingMs / RestTimer.totalMs)) : 0;
  return (
    <View style={{ borderRadius: 14, backgroundColor: C.Card, overflow: "hidden" }}>
      <View style={{ height: 4, backgroundColor: C.Line }}>
        <View style={{ height: 4, width: `${fraction * 100}%`, backgroundColor: accent }} />
      </View>
      <Row style={{ paddingHorizontal: 10, paddingVertical: 8 }}>
        <Pressable onPress={() => RestTimer.minus15()} style={{ borderRadius: 10, backgroundColor: C.Card2, paddingHorizontal: 12, paddingVertical: 9 }}>
          <Txt weight={600} size={15}>
            −15
          </Txt>
        </Pressable>
        <View style={{ flex: 1 }} />
        <Txt size={32} weight={800} color={over ? C.Red : C.Text}>
          {text}
        </Txt>
        <View style={{ flex: 1 }} />
        <Pressable onPress={() => RestTimer.plus15()} style={{ borderRadius: 10, backgroundColor: C.Card2, paddingHorizontal: 12, paddingVertical: 9 }}>
          <Txt weight={600} size={15}>
            +15
          </Txt>
        </Pressable>
        <View style={{ width: 10 }} />
        <Pressable onPress={() => RestTimer.clear()} style={{ borderRadius: 10, backgroundColor: accent, paddingHorizontal: 18, paddingVertical: 11 }}>
          <Txt weight={600} size={15} color={C.AccText}>
            {s("Skip", "Passer")}
          </Txt>
        </Pressable>
      </Row>
    </View>
  );
}

/** One-shot "record battu" pill shown at the top of the workout screen. */
function PrBadgeHost() {
  useStore(PrBadge.rev);
  const b = PrBadge.current;
  const [shown, setShown] = useState<typeof b>(null);
  useEffect(() => {
    if (b) {
      setShown(b);
      const t = setTimeout(() => {
        PrBadge.clear();
        setShown(null);
      }, 4200);
      return () => clearTimeout(t);
    }
  }, [b]);
  if (!b && !shown) return null;
  const badge = b ?? shown;
  if (!badge) return null;
  return (
    <View pointerEvents="box-none" style={{ position: "absolute", top: 0, left: 0, right: 0, alignItems: "center" }}>
      <Row
        style={{
          marginHorizontal: 20,
          marginVertical: 6,
          borderRadius: 999,
          backgroundColor: C.Card2,
          paddingHorizontal: 8,
          paddingVertical: 8,
        }}
      >
        <ExCircle muscle={badge.muscle} size={40} />
        <View style={{ width: 10 }} />
        <View style={{ flex: 1 }}>
          <Txt weight={700} size={13.5} numberOfLines={1}>
            {exName(badge.ex)}
          </Txt>
          <Txt weight={700} size={12.5} color={C.Orange} numberOfLines={1}>
            {badge.kind} - {badge.value}
          </Txt>
        </View>
        <Pressable onPress={() => { PrBadge.clear(); setShown(null); }} hitSlop={6} style={{ padding: 4 }}>
          <MIcon name="close" size={16} color={C.Mut} />
        </Pressable>
      </Row>
    </View>
  );
}

// ---------------- exercise picker ----------------

function PickerContent({ onClose, onPick }: { onClose: () => void; onPick: (name: string) => void }) {
  const [q, setQ] = useState("");
  const [mus, setMus] = useState("All");
  const accent = accentColor(Repo.settings.accent);
  const filtered = useMemo(
    () =>
      EXERCISES.filter((e) => (mus === "All" || e.muscle === mus) && matches(q, e.name)).sort((a, b) =>
        exName(a.name).localeCompare(exName(b.name), "fr"),
      ),
    [q, mus],
  );
  return (
    <View style={{ maxHeight: 520 }}>
      <Row style={{ paddingHorizontal: 12, paddingVertical: 8 }}>
        <Txt weight={700} size={16} style={{ flex: 1 }}>
          {s("Select exercises", "Choisir des exercices")}
        </Txt>
        <Pressable onPress={onClose} style={{ borderRadius: 999, paddingHorizontal: 12, paddingVertical: 6 }}>
          <Txt weight={700} size={14} color={accent}>
            {s("DONE", "OK")}
          </Txt>
        </Pressable>
      </Row>
      <View style={{ paddingHorizontal: 16 }}>
        <TF value={q} onChange={setQ} placeholder={s("Search exercises", "Rechercher des exercices")} leadingIcon="search" />
      </View>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ paddingHorizontal: 16, paddingVertical: 8, gap: 8 }}>
        {["All", ...MUSCLES].map((m) => (
          <Chip key={m} label={m === "All" ? s("All", "Tous") : muscleName(m)} selected={mus === m} onClick={() => setMus(m)} />
        ))}
      </ScrollView>
      <ScrollView style={{ maxHeight: 380 }} keyboardShouldPersistTaps="handled">
        {filtered.length === 0 ? (
          <EmptyState text={s("No exercises found.", "Aucun exercice trouvé.")} slim />
        ) : (
          filtered.map((e) => (
            <Pressable key={e.name} onPress={() => onPick(e.name)} style={{ paddingHorizontal: 20, paddingVertical: 10, flexDirection: "row", alignItems: "center" }}>
              <IllIcon muscle={e.muscle} size={46} />
              <View style={{ width: 14 }} />
              <View style={{ flex: 1 }}>
                <Txt weight={700} size={15} numberOfLines={2}>
                  {exName(e.name)}
                </Txt>
                <Txt size={12.5} color={C.Mut}>
                  {muscleName(e.muscle)} · {equipName(e.equip)}
                </Txt>
              </View>
              <View style={{ width: 10 }} />
              <View style={{ width: 30, height: 30, borderRadius: 15, backgroundColor: accent, alignItems: "center", justifyContent: "center" }}>
                <MIcon name="add" size={17} color={C.AccText} />
              </View>
            </Pressable>
          ))
        )}
        <View style={{ height: 20 }} />
      </ScrollView>
    </View>
  );
}
