/** Entraînement tab — port of Routines.kt TrainingScreen (view selector, empty workout, routines + drag reorder). */
import React, { useRef, useState } from "react";
import { Animated, Pressable, ScrollView, View } from "react-native";
import { Repo } from "../repo";
import { Routine } from "../models";
import { Nav } from "../nav";
import { useStore } from "../store";
import { C, accentColor } from "../theme";
import { s } from "../l10n";
import { exName } from "../l10nshim";
import {
  Alert, DragHandle, DragList, EmptyState, Menu, PromptDialog, Row, Txt, WorkoutInProgressDialog,
} from "./comps";
import { MIcon } from "./icons";
import TrainingCalendar from "./TrainingCalendar";
import { RestTimer } from "../resttimer";

export default function TrainingScreen() {
  useStore(Repo.rev);
  const routines = Repo.routines;
  const [expanded, setExpanded] = useState(true);
  const [view, setView] = useState(0); // 0=Routines 1=Calendrier
  const [viewMenu, setViewMenu] = useState(false);
  const viewAnchor = useRef<View>(null);
  const scrollRef = useRef<ScrollView>(null);
  const scrollOffset = useRef(0);

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

  return (
    <View style={{ flex: 1, backgroundColor: C.Bg }}>
      <ScrollView
        ref={scrollRef}
        style={{ flex: 1 }}
        contentContainerStyle={{ paddingBottom: 24 }}
        onScroll={(e) => (scrollOffset.current = e.nativeEvent.contentOffset.y)}
        scrollEventThrottle={16}
        keyboardShouldPersistTaps="handled"
      >
        {/* header with view selector */}
        <Row style={{ paddingHorizontal: 8, paddingTop: 10 }}>
          <Pressable
            onPress={() => setViewMenu(true)}
            ref={viewAnchor as any}
            style={{ flexDirection: "row", alignItems: "center", borderRadius: 10, paddingHorizontal: 8, paddingVertical: 4 }}
          >
            <Txt weight={800} size={26}>
              {s("Training", "Entraînement")}
            </Txt>
            <View style={{ width: 8 }} />
            <View style={{ width: 26, height: 26, borderRadius: 13, backgroundColor: C.Card2, alignItems: "center", justifyContent: "center" }}>
              <MIcon name="expand-more" size={17} color={C.Text} />
            </View>
          </Pressable>
        </Row>
        <Menu
          visible={viewMenu}
          onClose={() => setViewMenu(false)}
          anchorRef={viewAnchor}
          items={[
            { label: s("Training", "Entraînement"), icon: view === 0 ? "check" : undefined, onPress: () => setView(0) },
            { label: s("Calendar", "Calendrier"), icon: view === 1 ? "check" : undefined, onPress: () => setView(1) },
          ]}
        />
        {view === 1 ? (
          <TrainingCalendar />
        ) : (
          <>
            <View style={{ height: 6 }} />
            {/* start an empty workout — bordered dark button */}
            <Pressable
              onPress={() =>
                startFresh(() => {
                  Repo.startWorkout(null);
                  Nav.push({ t: "Logger" });
                })
              }
              style={{
                marginHorizontal: 16,
                borderRadius: 12,
                backgroundColor: C.Card,
                borderWidth: 1,
                borderColor: C.Line,
                paddingVertical: 15,
                alignItems: "center",
                flexDirection: "row",
                justifyContent: "center",
              }}
            >
              <MIcon name="add" size={20} color={C.Text} />
              <View style={{ width: 10 }} />
              <Txt weight={600} size={16}>
                {s("Start an Empty Workout", "Démarrer un Entraînement Vide")}
              </Txt>
            </Pressable>
            {/* Routines header */}
            <Row style={{ paddingHorizontal: 16, paddingRight: 8, paddingTop: 18 }}>
              <Txt weight={800} size={20} style={{ flex: 1 }}>
                {s("Routines", "Routines")}
              </Txt>
              <Pressable
                onPress={() =>
                  startFresh(() => {
                    Repo.startRoutine(null);
                    Nav.push({ t: "Logger" });
                  })
                }
                hitSlop={6}
                style={{ paddingHorizontal: 8 }}
              >
                <MIcon name="create-new-folder" size={24} color={C.Text} />
              </Pressable>
            </Row>
            {/* Nouv. Routine / Explorer */}
            <Row style={{ paddingHorizontal: 16, gap: 10, paddingTop: 2 }}>
              <SecondaryButton
                text={s("New routine", "Nouv. Routine")}
                icon="post-add"
                onPress={() =>
                  startFresh(() => {
                    Repo.startRoutine(null);
                    Nav.push({ t: "Logger" });
                  })
                }
              />
              <SecondaryButton text={s("Explore", "Explorer")} icon="search" onPress={() => Nav.push({ t: "Exercises" })} />
            </Row>
            {/* Mes routines (N) collapsible */}
            <Pressable onPress={() => setExpanded(!expanded)} style={{ paddingHorizontal: 16, paddingVertical: 14, flexDirection: "row", alignItems: "center" }}>
              <MIcon name="expand-more" size={18} color={C.Mut} />
              <View style={{ width: 8 }} />
              <Txt weight={500} size={15} color={C.Mut}>
                {s("My routines (%1$d)", "Mes routines (%1$d)").replace("%1$d", String(routines.length))}
              </Txt>
            </Pressable>
            {expanded ? (
              routines.length === 0 ? (
                <EmptyState text={s("No routines yet.\nTap “New routine” to create one.", "Aucune routine.\nTouche « Nouvelle routine » pour en créer une.")} />
              ) : (
                <DragList
                  count={routines.length}
                  scrollRef={scrollRef}
                  scrollOffsetRef={scrollOffset}
                  onMove={(from, to) => Repo.moveRoutine(from, to)}
                  renderItem={({ index, dragging, translateY, onGrab }) => (
                    <RoutineCard
                      key={routines[index].id}
                      r={routines[index]}
                      dragging={dragging}
                      translateY={translateY}
                      onGrab={onGrab}
                      onStart={() =>
                        startFresh(() => {
                          Repo.startWorkout(routines[index].id);
                          Nav.push({ t: "Logger" });
                        })
                      }
                    />
                  )}
                />
              )
            ) : null}
          </>
        )}
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
    </View>
  );
}

function SecondaryButton({ text, icon, onPress }: { text: string; icon: any; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      style={{
        flex: 1,
        height: 46,
        borderRadius: 12,
        backgroundColor: C.Card,
        borderWidth: 1,
        borderColor: C.Line,
        alignItems: "center",
        justifyContent: "center",
        flexDirection: "row",
      }}
    >
      <MIcon name={icon} size={17} color={C.Text} />
      <View style={{ width: 8 }} />
      <Txt weight={600} size={14.5}>
        {text}
      </Txt>
    </Pressable>
  );
}

function RoutineCard({
  r,
  dragging,
  translateY,
  onGrab,
  onStart,
}: {
  r: Routine;
  dragging: boolean;
  translateY: Animated.Value;
  onGrab: (pageY: number) => void;
  onStart: () => void;
}) {
  const [cardMenu, setCardMenu] = useState(false);
  const [confirmDel, setConfirmDel] = useState(false);
  const [rename, setRename] = useState(false);
  const anchor = useRef<View>(null);
  const accent = accentColor(Repo.settings.accent);
  return (
    <Animated.View style={{ transform: [{ translateY: dragging ? translateY : 0 }], zIndex: dragging ? 10 : 0 }}>
      <DragHandle onGrab={onGrab} dragging={dragging}>
        <Pressable onPress={() => Nav.push({ t: "RoutineDetail", id: r.id })} style={{ marginHorizontal: 16, marginVertical: 6, borderRadius: 16, backgroundColor: dragging ? C.Card2 : C.Card, padding: 16 }}>
          <Row>
            <Txt weight={800} size={19} numberOfLines={1} style={{ flex: 1 }}>
              {exName(r.name)}
            </Txt>
            <Pressable ref={anchor as any} onPress={() => setCardMenu(true)} hitSlop={6} style={{ paddingHorizontal: 4 }}>
              <MIcon name="more-horiz" size={22} color={C.Mut} />
            </Pressable>
            <Menu
              visible={cardMenu}
              onClose={() => setCardMenu(false)}
              anchorRef={anchor}
              items={[
                { label: s("View routine", "Voir la routine"), icon: "chevron-right", onPress: () => Nav.push({ t: "RoutineDetail", id: r.id }) },
                { label: s("Rename routine", "Renommer la routine"), icon: "edit", onPress: () => setRename(true) },
                { label: s("Duplicate routine", "Dupliquer la routine"), icon: "content-copy", onPress: () => Repo.duplicateRoutine(r.id) },
                { label: s("Delete routine", "Supprimer la routine"), icon: "delete", red: true, onPress: () => setConfirmDel(true) },
              ]}
            />
          </Row>
          <View style={{ height: 6 }} />
          {r.exercises.length === 0 ? (
            <Txt size={14.5} color={C.Mut} style={{ lineHeight: 21 }}>
              {s("No exercises yet", "Aucun exercice pour l'instant")}
            </Txt>
          ) : (
            <Txt size={15} color={C.Mut} numberOfLines={2} style={{ lineHeight: 21 }}>
              {r.exercises.map((ex) => exName(ex.name)).join(", ")}
            </Txt>
          )}
          <View style={{ height: 14 }} />
          <Pressable onPress={onStart} style={{ height: 46, borderRadius: 12, backgroundColor: accent, alignItems: "center", justifyContent: "center" }}>
            <Txt weight={600} size={15} color={C.AccText}>
              {s("Start routine", "Commencer la Routine")}
            </Txt>
          </Pressable>
        </Pressable>
      </DragHandle>
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
        }}
        onDismiss={() => setConfirmDel(false)}
        dismissLabel={s("Cancel", "Annuler")}
      />
    </Animated.View>
  );
}
