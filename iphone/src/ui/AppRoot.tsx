/** Root: init (repo/cloud/timer/fonts/lang), auth gate, tab bar + stack, global undo bar. Port of App.kt + MainActivity. */
import React, { useEffect, useState } from "react";
import { AppState, Linking, Platform, Pressable, ScrollView, StyleSheet, View } from "react-native";
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context";
import { StatusBar } from "expo-status-bar";
import * as Font from "expo-font";
import * as Localization from "expo-localization";
import {
  Inter_400Regular,
  Inter_500Medium,
  Inter_600SemiBold,
  Inter_700Bold,
  Inter_800ExtraBold,
} from "@expo-google-fonts/inter";
import { C, accentColor } from "../theme";
import { Nav, DeletedUndo } from "../nav";
import { Repo } from "../repo";
import { Cloud } from "../cloud";
import { RestTimer, WorkoutNotif, wireNotificationActions } from "../resttimer";
import { setLang, s } from "../l10n";
import { useStore } from "../store";
import { MIcon, IconName } from "./icons";
import { AvatarImg, Row, Txt, ToastHost, toast } from "./comps";
import AuthScreen from "./Auth";
import HomeScreen from "./Home";
import TrainingScreen from "./Training";
import ProfileScreen from "./Profile";
import SettingsScreen from "./Settings";
import WorkoutDetailScreen from "./WorkoutDetail";
import RoutineDetailScreen from "./RoutineDetail";
import HistoryScreen from "./History";
import ExercisesScreen, { ExerciseDetailScreen } from "./Exercises";
import LoggerScreen from "./Logger";
import WorkoutSummaryScreen from "./WorkoutSummary";
import { Workout } from "../models";

function Booting() {
  return (
    <View style={{ flex: 1, backgroundColor: C.Bg, alignItems: "center", justifyContent: "center" }}>
      <View style={{ width: 58, height: 58, borderRadius: 16, backgroundColor: C.Accent, alignItems: "center", justifyContent: "center" }}>
        <MIcon name="fitness-center" size={30} color="#fff" />
      </View>
      <Txt weight={800} size={24} style={{ marginTop: 14 }}>
        Liftbook
      </Txt>
    </View>
  );
}

export default function AppRoot() {
  const [fontsLoaded, setFontsLoaded] = useState(false);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let mounted = true;
    (async () => {
      await Font.loadAsync({
        Inter_400Regular,
        Inter_500Medium,
        Inter_600SemiBold,
        Inter_700Bold,
        Inter_800ExtraBold,
      });
      if (!mounted) return;
      setFontsLoaded(true);
      const forced = Platform.OS === "web" ? localStorage.getItem("liftbook_lang") : null;
      const loc = forced ?? Localization.getLocales()[0]?.languageCode ?? "fr";
      setLang(["fr", "es", "de"].includes(loc) ? loc : "en");
      await Repo.init();
      await Cloud.init();
      await RestTimer.init();
      // dev-only web preview: fill with the deterministic demo seed (localStorage flag)
      if (Platform.OS === "web" && localStorage.getItem("liftbook_seed") === "1" && Repo.workouts.length === 0) {
        const [ws, rs] = require("../calc").seed(Date.now());
        Repo.workouts = ws;
        Repo.routines = rs;
        Repo.routines.forEach((r: any, i: number) => (r.pos = i));
        Repo.prCache = require("../calc").rebuildPrs(ws);
        Repo.touchPublic();
      }
      void wireNotificationActions(() => {});
      void RestTimer.requestPermission();
      setReady(true);
      // Google OAuth deep link (cold start + warm)
      Linking.getInitialURL().then((url) => url && void Cloud.handleAuthRedirect(url));
      const sub = Linking.addEventListener("url", (e) => void Cloud.handleAuthRedirect(e.url));
      const appSub = AppState.addEventListener("change", (st) => {
        if (st === "active") {
          void Cloud.clearGooglePendingIfStale();
          Cloud.requestSync(400);
        } else if (st === "background") {
          void Repo.persistDraftNow();
        }
      });
      return () => {
        sub.remove();
        appSub.remove();
      };
    })();
    return () => {
      mounted = false;
    };
  }, []);

  if (!fontsLoaded || !ready) return <Booting />;
  return (
    <SafeAreaProvider>
      <StatusBar style="light" />
      <Root />
    </SafeAreaProvider>
  );
}

function Root() {
  const cloudUi = useStore(Cloud.uiStore);
  useStore(Nav.rev);

  // pendingStartEmpty (widget / quick start)
  useEffect(() => {
    if (Nav.pendingStartEmpty) {
      Nav.pendingStartEmpty = false;
      if (Repo.draft == null) Repo.startWorkout(null);
      Nav.push({ t: "Logger" });
    }
  });

  if (cloudUi.session == null && !cloudUi.skipped) return <AuthScreen />;

  return (
    <View style={{ flex: 1, backgroundColor: C.Bg }}>
      <SafeAreaView edges={["top"]} style={{ flex: 1, backgroundColor: C.Bg }}>
        <CurrentScreen />
      </SafeAreaView>
      {Nav.atTab && <TabBar />}
      <UndoBar />
      <ToastHost />
    </View>
  );
}

function CurrentScreen() {
  const cur = Nav.current;
  switch (cur.t) {
    case "HomeTab":
      return <HomeScreen />;
    case "TrainingTab":
      return <TrainingScreen />;
    case "ProfileTab":
      return <ProfileScreen />;
    case "Settings":
      return <SettingsScreen />;
    case "WorkoutDetail":
      return <WorkoutDetailScreen id={cur.id} />;
    case "ExerciseDetail":
      return <ExerciseDetailScreen name={cur.name} />;
    case "RoutineDetail":
      return <RoutineDetailScreen id={cur.id} />;
    case "History":
      return <HistoryScreen />;
    case "Exercises":
      return <ExercisesScreen />;
    case "Logger":
      return <LoggerScreen />;
    case "WorkoutSummary":
      return <WorkoutSummaryScreen />;
  }
}

const TABS: { screen: { t: any }; label: string; icon: IconName }[] = [
  { screen: { t: "HomeTab" }, label: s("Home", "Accueil"), icon: "home" },
  { screen: { t: "TrainingTab" }, label: s("Training", "Entraînement"), icon: "fitness-center" },
  { screen: { t: "ProfileTab" }, label: s("Profile", "Profil"), icon: "person" },
];

function TabBar() {
  const accent = accentColor(Repo.settings.accent);
  return (
    <SafeAreaView edges={["bottom"]} style={{ backgroundColor: C.NavBar }}>
      <Row style={{ height: 54 }}>
        {TABS.map((t) => {
          const sel = Nav.current.t === t.screen.t;
          return (
            <Pressable key={t.label} onPress={() => Nav.toTab(t.screen)} style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: 2 }}>
              <MIcon name={t.icon} size={24} color={sel ? accent : C.Mut} />
              <Txt weight={500} size={11} color={sel ? accent : C.Mut}>
                {t.label}
              </Txt>
            </Pressable>
          );
        })}
      </Row>
    </SafeAreaView>
  );
}

/** Global 5-second undo for a deleted workout — survives navigation. */
function UndoBar() {
  const undo = useStore(DeletedUndo.rev);
  useEffect(() => {
    if (DeletedUndo.workout) {
      const w: Workout = DeletedUndo.workout;
      const t = setTimeout(() => DeletedUndo.set(null), 5000);
      return () => clearTimeout(t);
    }
  }, [undo]);
  const victim = DeletedUndo.workout;
  if (!victim) return null;
  return (
    <View style={{ position: "absolute", left: 16, right: 16, bottom: 84 }} pointerEvents="box-none">
      <Row
        style={{
          backgroundColor: C.Card2,
          borderRadius: 14,
          paddingHorizontal: 16,
          paddingVertical: 12,
          shadowColor: "#000",
          shadowOpacity: 0.4,
          shadowRadius: 10,
          elevation: 6,
        }}
      >
        <Txt size={14} style={{ flex: 1 }}>
          {s("Workout deleted", "Séance supprimée")}
        </Txt>
        <Pressable
          onPress={() => {
            Repo.restoreWorkout(victim);
            DeletedUndo.set(null);
          }}
        >
          <Txt weight={700} size={14} color={accentColor(Repo.settings.accent)}>
            {s("UNDO", "ANNULER")}
          </Txt>
        </Pressable>
      </Row>
    </View>
  );
}
