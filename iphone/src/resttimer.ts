/** Global rest timer + local notifications — port of RestTimer / RestNotifService / WorkoutNotif.
 *  iOS: countdown lives in-app; a scheduled notification fires at the end (foreground-safe),
 *  an ongoing notification shows the remaining time while the app is open, and the
 *  −15/+15/Passer buttons live on the notification via a category. */
import { Platform } from "react-native";
import AsyncStorage from "@react-native-async-storage/async-storage";
import Notifications, { SchedulableTriggerInputTypes } from "expo-notifications";
import { makeRev } from "./store";
import { fmtClock } from "./calc";
import { s } from "./l10n";

const isWeb = Platform.OS === "web";
const REST_ID = "liftbook-rest";
const END_ID = "liftbook-rest-end";
const WORKOUT_ID = "liftbook-workout";
const CATEGORY = "liftbook_rest";

class RestTimerImpl {
  endAt = 0;
  totalMs = 0;
  exName: string | null = null;
  exMuscle: string | null = null;
  rev = makeRev();
  private ready = false;
  private tickTimer: ReturnType<typeof setInterval> | null = null;

  async init() {
    if (this.ready) return;
    this.ready = true;
    try {
      const raw = await AsyncStorage.getItem("rest_timer");
      if (raw) {
        const o = JSON.parse(raw);
        if (o.endAt > Date.now()) {
          this.endAt = o.endAt;
          this.totalMs = o.totalMs > 0 ? o.totalMs : Math.floor((o.endAt - Date.now()) / 1000) * 1000;
          void this.postOngoing();
          this.startTicker();
        } else if (o.endAt > 0) {
          await AsyncStorage.removeItem("rest_timer");
        }
      }
    } catch {}
    if (!isWeb) {
      try {
        await Notifications.setNotificationCategoryAsync(CATEGORY, [
          { identifier: "minus15", buttonTitle: "−15s", options: { opensAppToForeground: false } },
          { identifier: "plus15", buttonTitle: "+15s", options: { opensAppToForeground: false } },
          { identifier: "skip", buttonTitle: s("Skip", "Passer"), options: { opensAppToForeground: true } },
        ]);
      } catch {}
    }
    this.rev.bump();
  }

  private notify() {
    this.rev.bump();
  }

  async requestPermission(): Promise<boolean> {
    if (isWeb) return false;
    try {
      const cur = await Notifications.getPermissionsAsync();
      if (cur.granted) return true;
      const res = await Notifications.requestPermissionsAsync({ ios: { allowAlert: true, allowSound: true, allowBadge: true } });
      return res.granted;
    } catch {
      return false;
    }
  }

  async start(sec: number) {
    this.endAt = Date.now() + sec * 1000;
    this.totalMs = sec * 1000;
    try {
      await AsyncStorage.setItem("rest_timer", JSON.stringify({ endAt: this.endAt, totalMs: this.totalMs }));
    } catch {}
    void this.postOngoing();
    void this.scheduleEnd();
    this.startTicker();
    this.notify();
  }

  private startTicker() {
    if (this.tickTimer) return;
    this.tickTimer = setInterval(() => {
      if (this.endAt > 0 && this.endAt - Date.now() <= 0) {
        // finished on its own while the app is open
        this.finishInApp();
      } else if (this.endAt > 0) {
        void this.postOngoing();
      }
    }, 1000);
  }

  private stopTicker() {
    if (this.tickTimer) {
      clearInterval(this.tickTimer);
      this.tickTimer = null;
    }
  }

  /** +15s from the notification button. */
  plus15() {
    if (this.endAt > 0) {
      this.endAt += 15000;
      this.totalMs += 15000;
      void this.persistEnd();
      void this.postOngoing();
      void this.scheduleEnd();
      this.notify();
    }
  }

  /** −15s from the notification button (never below 0.5 s left). */
  minus15() {
    if (this.endAt > 0) {
      this.endAt = Math.max(Date.now() + 500, this.endAt - 15000);
      void this.persistEnd();
      void this.postOngoing();
      void this.scheduleEnd();
      this.notify();
    }
  }

  /** Jump straight to the finished state ("Passer"). */
  skip() {
    if (this.endAt > 0) {
      this.endAt = Date.now();
      void this.persistEnd();
      void this.postOngoing();
      this.notify();
    }
  }

  clear() {
    this.endAt = 0;
    this.totalMs = 0;
    this.exName = null;
    this.exMuscle = null;
    this.stopTicker();
    void AsyncStorage.removeItem("rest_timer").catch(() => {});
    void this.cancelNotifs();
    this.notify();
  }

  /** Timer elapsed with the app open → show the "Terminé" state briefly, then clean up. */
  private finishInApp() {
    this.endAt = 0;
    this.totalMs = 0;
    this.exName = null;
    this.exMuscle = null;
    this.stopTicker();
    void AsyncStorage.removeItem("rest_timer").catch(() => {});
    void this.cancelNotifs();
    this.notify();
  }

  private async persistEnd() {
    try {
      await AsyncStorage.setItem("rest_timer", JSON.stringify({ endAt: this.endAt > 0 ? this.endAt : 0, totalMs: this.totalMs }));
    } catch {}
  }

  private async cancelNotifs() {
    if (isWeb) return;
    try {
      await Notifications.dismissNotificationAsync(REST_ID);
      await Notifications.cancelScheduledNotificationAsync(END_ID);
    } catch {}
  }

  private async postOngoing() {
    if (isWeb) return;
    const remain = Math.max(0, this.endAt - Date.now());
    if (this.endAt <= 0) return;
    const sec = Math.ceil(remain / 1000);
    try {
      await Notifications.scheduleNotificationAsync({
        identifier: REST_ID,
        content: {
          title: s("Rest", "Repos") + ` ${String(Math.floor(sec / 60)).padStart(2, "0")}:${String(sec % 60).padStart(2, "0")}`,
          body: this.exName ? this.exName : undefined,
          sound: false,
          sticky: true,
          categoryIdentifier: CATEGORY,
        },
        trigger: null,
      });
    } catch {}
  }

  /** The notification that actually fires when the rest ends. */
  private async scheduleEnd() {
    if (isWeb) return;
    const remain = this.endAt - Date.now();
    if (remain <= 0) return;
    try {
      await Notifications.cancelScheduledNotificationAsync(END_ID);
      await Notifications.scheduleNotificationAsync({
        identifier: END_ID,
        content: {
          title: s("Rest finished", "Repos terminé"),
          body: this.exName ? `${this.exName} — ${s("go!", "go !")}` : undefined,
          sound: true,
        },
        trigger: { type: SchedulableTriggerInputTypes.DATE, date: this.endAt } as any,
      });
    } catch {}
  }

  setExercise(name: string, muscle: string) {
    this.exName = name;
    this.exMuscle = muscle;
    this.notify();
  }
}

export const RestTimer = new RestTimerImpl();

// ---------------- workout ongoing chronometer ----------------

class WorkoutNotifImpl {
  private timer: ReturnType<typeof setInterval> | null = null;

  async post(startedAt: number) {
    if (isWeb) return;
    this.cancelTimer();
    await this.update(startedAt);
    this.timer = setInterval(() => void this.update(startedAt), 10000);
  }

  private async update(startedAt: number) {
    if (isWeb) return;
    try {
      await Notifications.scheduleNotificationAsync({
        identifier: WORKOUT_ID,
        content: {
          title: s("Workout", "Entraînement"),
          body: fmtClock(Date.now() - startedAt),
          sound: false,
          sticky: true,
        },
        trigger: null,
      });
    } catch {}
  }

  private cancelTimer() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  cancel() {
    this.cancelTimer();
    if (isWeb) return;
    void Notifications.dismissNotificationAsync(WORKOUT_ID).catch(() => {});
  }
}

export const WorkoutNotif = new WorkoutNotifImpl();

/** Wire the notification action buttons once at app start. */
export async function wireNotificationActions(onSkipRest: () => void) {
  if (isWeb) return;
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowBanner: true,
      shouldShowList: true,
      shouldPlaySound: false,
      shouldSetBadge: false,
    }),
  });
  Notifications.addNotificationResponseReceivedListener((resp) => {
    const action = resp.actionIdentifier;
    if (action === "plus15") RestTimer.plus15();
    else if (action === "minus15") RestTimer.minus15();
    else if (action === "skip") {
      RestTimer.clear();
      onSkipRest();
    }
  });
}
