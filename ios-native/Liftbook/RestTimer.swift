import SwiftUI
import Foundation
import Foundation
import UserNotifications
import LiftbookCore

/** Global rest timer + local notifications — port of RestTimer / RestNotifService / WorkoutNotif.
 *  iOS: a scheduled notification fires at the end (sound), an ongoing notification shows the
 *  countdown while the app is open, and −15/+15/Passer buttons live on the notification itself. */
@MainActor
final class RestTimerModel: ObservableObject {
    static let shared = RestTimerModel()

    @Published var endAt: Double = 0
    @Published var totalMs: Double = 0
    var exName: String?
    var exMuscle: String?

    private var ticker: Timer?

    static let restId = "liftbook-rest"
    static let endId = "liftbook-rest-end"
    static let workoutId = "liftbook-workout"
    static let category = "liftbook_rest"

    init() {
        restore()
        Task { await Self.registerCategory() }
    }

    func restore() {
        let d = UserDefaults.standard
        let saved = d.double(forKey: "rest_endAt")
        if saved > Date.now.timeIntervalSince1970 {
            endAt = saved
            totalMs = d.double(forKey: "rest_totalMs") > 0 ? d.double(forKey: "rest_totalMs") : (saved - Date.now.timeIntervalSince1970).rounded(.down)
            postOngoing()
            startTicker()
        } else if saved > 0 {
            d.removeObject(forKey: "rest_endAt")
            d.removeObject(forKey: "rest_totalMs")
        }
    }

    static func requestPermission() async -> Bool {
        (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    /// Permission is asked lazily, at the first rest-timer use (Hevy behavior) — not at launch.
    static func ensurePermission() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        if settings.authorizationStatus == .notDetermined {
            _ = await requestPermission()
        }
    }

    static func registerCategory() async {
        let minus = UNNotificationAction(identifier: "minus15", title: "−15s", options: [])
        let plus = UNNotificationAction(identifier: "plus15", title: "+15s", options: [])
        let skip = UNNotificationAction(identifier: "skip", title: L10n.s("Skip", "Passer"), options: [.foreground])
        try? await UNUserNotificationCenter.current().setNotificationCategories([
            UNNotificationCategory(identifier: category, actions: [minus, plus, skip], intentIdentifiers: [])
        ])
    }

    func start(_ sec: Int) {
        endAt = Date.now.timeIntervalSince1970 + Double(sec)
        totalMs = Double(sec) * 1000
        let d = UserDefaults.standard
        d.set(endAt, forKey: "rest_endAt")
        d.set(totalMs, forKey: "rest_totalMs")
        postOngoing()
        Task {
            await Self.ensurePermission()
            scheduleEnd()
        }
        startTicker()
    }

    private func startTicker() {
        guard ticker == nil else { return }
        ticker = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self else { return }
                if self.endAt > 0 && self.endAt <= Date.now.timeIntervalSince1970 {
                    self.finishInApp()
                } else if self.endAt > 0 {
                    self.postOngoing()
                }
            }
        }
    }

    private func stopTicker() {
        ticker?.invalidate()
        ticker = nil
    }

    func plus15() {
        guard endAt > 0 else { return }
        endAt += 15_000
        totalMs += 15_000
        persistEnd()
        postOngoing()
        scheduleEnd()
    }

    func minus15() {
        guard endAt > 0 else { return }
        endAt = max(Date.now.timeIntervalSince1970 + 0.5, endAt - 15_000)
        persistEnd()
        postOngoing()
        scheduleEnd()
    }

    func skip() {
        guard endAt > 0 else { return }
        endAt = Date.now.timeIntervalSince1970
        persistEnd()
    }

    private func finishInApp() {
        endAt = 0
        totalMs = 0
        exName = nil
        exMuscle = nil
        stopTicker()
        UserDefaults.standard.removeObject(forKey: "rest_endAt")
        UserDefaults.standard.removeObject(forKey: "rest_totalMs")
        cancelAll()
    }

    func clear() {
        finishInApp()
    }

    private func persistEnd() {
        UserDefaults.standard.set(endAt > 0 ? endAt : 0, forKey: "rest_endAt")
    }

    private func cancelAll() {
        let nc = UNUserNotificationCenter.current()
        nc.removeDeliveredNotifications(withIdentifiers: [Self.restId])
        nc.removePendingNotificationRequests(withIdentifiers: [Self.endId, Self.restId])
    }

    private func postOngoing() {
        guard endAt > 0 else { return }
        let remain = max(0, endAt - Date.now.timeIntervalSince1970)
        let sec = Int(ceil(remain / 1000))
        let content = UNMutableNotificationContent()
        content.title = L10n.s("Rest", "Repos") + String(format: " %02d:%02d", sec / 60, sec % 60)
        if let n = exName { content.body = n }
        content.sound = nil
        content.categoryIdentifier = Self.category
        let req = UNNotificationRequest(identifier: Self.restId, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(req)
    }

    private func scheduleEnd() {
        let remain = endAt - Date.now.timeIntervalSince1970
        guard remain > 1 else { return }
        let nc = UNUserNotificationCenter.current()
        nc.removePendingNotificationRequests(withIdentifiers: [Self.endId])
        let content = UNMutableNotificationContent()
        content.title = L10n.s("Rest finished", "Repos terminé")
        if let n = exName { content.body = n + " — go !" }
        content.sound = .default
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: remain, repeats: false)
        nc.add(UNNotificationRequest(identifier: Self.endId, content: content, trigger: trigger))
    }

    func setExercise(_ name: String, _ muscle: String) {
        exName = name
        exMuscle = muscle
    }

    // ---------------- workout ongoing chronometer ----------------

    private var workoutTimer: Timer?

    func postWorkout(startedAt: Double) {
        cancelWorkout()
        updateWorkout(startedAt: startedAt)
        workoutTimer = Timer.scheduledTimer(withTimeInterval: 10, repeats: true) { _ in
            Task { @MainActor in self.updateWorkout(startedAt: startedAt) }
        }
    }

    private func updateWorkout(startedAt: Double) {
        let content = UNMutableNotificationContent()
        content.title = L10n.s("Workout", "Entraînement")
        content.body = Calc.fmtClock((Date.now.timeIntervalSince1970 - startedAt) * 1000)
        content.sound = nil
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: Self.workoutId, content: content, trigger: nil))
    }

    func cancelWorkout() {
        workoutTimer?.invalidate()
        workoutTimer = nil
        UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: [Self.workoutId])
    }

    /// Wire the notification action buttons once at app start.
    @MainActor static func wireActions() {
        UNUserNotificationCenter.current().delegate = NotificationDelegate.shared
    }
}

@MainActor
final class NotificationDelegate: NSObject, @preconcurrency UNUserNotificationCenterDelegate {
    static let shared = NotificationDelegate()

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .list]
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        let action = response.actionIdentifier
        await MainActor.run {
            if action == "plus15" { RestTimerModel.shared.plus15() }
            else if action == "minus15" { RestTimerModel.shared.minus15() }
            else if action == "skip" { RestTimerModel.shared.clear() }
        }
    }
}
