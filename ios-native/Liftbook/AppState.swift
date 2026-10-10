import SwiftUI
import Foundation
import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif
import LiftbookCore
import Security

/** App state — port of Repo.kt + Cloud.kt's sync engine, as an ObservableObject. */

@MainActor
final class Repo: ObservableObject {
    @Published var workouts: [Workout] = []      // startedAt ASC
    @Published var routines: [Routine] = []
    @Published var settings = Settings()
    @Published var draft: Draft?
    @Published var prCache: [String: PrBest] = [:]
    @Published var avatarVersion = 0

    // cloud UI state
    @Published var session: CloudSession?
    @Published var skipped = false
    @Published var authError: String?
    @Published var busy = false
    @Published var googlePending = false
    @Published var syncStatus: String?
    @Published var toastMessage: String?

    // sync meta
    var dirtyAt: Double = 0
    var pushedTs: Double = 0
    var lastSeenRemoteTs: Double = 0
    var lastAccount: String?
    var delW: [Double] = []
    var delR: [String] = []
    var delP: [Double] = []
    var delC: [String] = []            // deleted custom-exercise names (round-trip; Android deletes)
    var photos: [ProgressPhoto] = []   // round-trip only (no photo UI on iOS yet)
    var customs: [CustomExercise] = [] // round-trip only
    private var syncing = false
    private var syncTask: Task<Void, Never>?

    let client: GoTrueClient
    private var toastTask: Task<Void, Never>?

    init() {
        client = GoTrueClient(redirect: "liftbook://auth-callback", appVersion: "1.0")
        load()
        L10n.lang = Locale.current.language.languageCode?.identifier ?? "fr"
        if !["fr", "es", "de"].contains(L10n.lang) { L10n.lang = "en" }
        prCache = Calc.rebuildPrs(&workouts)
    }

    private func load() {
        let d = UserDefaults.standard
        if let w = d.data(forKey: "workouts"), let dec = try? JSONDecoder().decode([Workout].self, from: w) { workouts = dec }
        if let r = d.data(forKey: "routines"), let dec = try? JSONDecoder().decode([Routine].self, from: r) { routines = dec }
        if let s = d.data(forKey: "settings"), let dec = try? JSONDecoder().decode(Settings.self, from: s) { settings = dec }
        else { settings.since = Date.now.timeIntervalSince1970 }
        if let dr = d.data(forKey: "draft"), let dec = try? JSONDecoder().decode(Draft.self, from: dr) { draft = dec }
        if let meta = d.data(forKey: "cloud_meta"),
           let m = try? JSONDecoder().decode(CloudMeta.self, from: meta) {
            dirtyAt = m.dirtyAt; pushedTs = m.pushedTs; lastSeenRemoteTs = m.lastSeenRemoteTs
            lastAccount = m.lastAccount; skipped = m.skipped; delW = m.delW; delR = m.delR; delP = m.delP; delC = m.delC
        }
        if let ph = d.data(forKey: "photos"), let dec = try? JSONDecoder().decode([ProgressPhoto].self, from: ph) { photos = dec }
        if let cs = d.data(forKey: "customs"), let dec = try? JSONDecoder().decode([CustomExercise].self, from: cs) { customs = dec }
        session = Keychain.loadSession()
    }

    struct CloudMeta: Codable {
        var dirtyAt: Double = 0
        var pushedTs: Double = 0
        var lastSeenRemoteTs: Double = 0
        var lastAccount: String?
        var skipped = false
        var delW: [Double] = []
        var delR: [String] = []
        var delP: [Double] = []
        var delC: [String] = []
    }

    private func persist() {
        let d = UserDefaults.standard
        let enc = JSONEncoder()
        d.set(try? enc.encode(workouts), forKey: "workouts")
        d.set(try? enc.encode(routines), forKey: "routines")
        d.set(try? enc.encode(settings), forKey: "settings")
        d.set(try? enc.encode(photos), forKey: "photos")
        d.set(try? enc.encode(customs), forKey: "customs")
    }

    func persistDraft() {
        let enc = JSONEncoder()
        if let dr = draft { UserDefaults.standard.set(try? enc.encode(dr), forKey: "draft") }
        else { UserDefaults.standard.removeObject(forKey: "draft") }
    }

    func persistMeta() {
        let m = CloudMeta(dirtyAt: dirtyAt, pushedTs: pushedTs, lastSeenRemoteTs: lastSeenRemoteTs, lastAccount: lastAccount, skipped: skipped, delW: delW, delR: delR, delP: delP)
        UserDefaults.standard.set(try? JSONEncoder().encode(m), forKey: "cloud_meta")
    }

    private func touch() {
        persistDraft()
        objectWillChange.send()
    }

    func touchPublic() { touch() }

    func queueSave() {
        persist()
    }

    func toast(_ msg: String) {
        toastMessage = msg
        toastTask?.cancel()
        toastTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 2_600_000_000)
            if !Task.isCancelled { self?.toastMessage = nil }
        }
    }

    // ---------- ids ----------

    func nextWorkoutId() -> Int { (workouts.map { $0.id }.max() ?? 0) + 1 }
    func nextRoutineId() -> Int { (routines.map { $0.id }.max() ?? 0) + 1 }
    func nextPos() -> Int { (routines.map { $0.pos }.max() ?? 0) + 1 }
    func workoutById(_ id: Int) -> Workout? { workouts.first { $0.id == id } }
    func routineById(_ id: Int) -> Routine? { routines.first { $0.id == id } }
    func workoutsDesc() -> [Workout] { workouts.sorted { $0.startedAt > $1.startedAt } }

    // ---------- draft lifecycle ----------

    func startWorkout(_ routineId: Int?) {
        let r = routineId.flatMap { routineById($0) }
        let exs: [ExEntry] = r?.exercises.map { ex in
            ExEntry(name: ex.name, muscle: ex.muscle, notes: ex.notes, superset: ex.superset, restSec: ex.restSec,
                    sets: ex.sets.map { SetEntry(kg: $0.kg, reps: $0.reps, done: false) })
        } ?? []
        draft = Draft(mode: "workout", routineId: routineId, name: r?.name ?? "Séance", startedAt: Date.now.timeIntervalSince1970, exercises: exs)
        touch()
    }

    func startRoutine(_ routineId: Int?) {
        let r = routineId.flatMap { routineById($0) }
        draft = Draft(mode: "routine", routineId: routineId, name: r?.name ?? "Nouvelle Routine", exercises: r?.exercises ?? [])
        touch()
    }

    func startRepeat(_ workoutId: Int?) {
        guard let last = workoutId != nil ? workoutById(workoutId!) : workouts.max(by: { $0.startedAt < $1.startedAt }) else { return }
        draft = Draft(
            mode: "workout",
            name: last.name,
            startedAt: Date.now.timeIntervalSince1970,
            notes: last.notes,
            exercises: last.exercises.map { ex in
                ExEntry(name: ex.name, muscle: ex.muscle, notes: ex.notes, superset: ex.superset, restSec: ex.restSec,
                        sets: ex.sets.map { SetEntry(kg: $0.kg, reps: $0.reps, done: false) })
            }
        )
        touch()
    }

    func addExToDraft(_ name: String) {
        guard draft != nil else { return }
        draft!.exercises.append(ExEntry(name: name, muscle: ExData.byName[name]?.muscle ?? "", sets: [SetEntry(kg: nil, reps: nil, done: false)]))
        touch()
    }

    @discardableResult
    func saveRoutine(_ name: String) -> Routine? {
        guard let d = draft, !d.exercises.isEmpty else { return nil }
        if let rid = d.routineId, var existing = routineById(rid) {
            existing.name = name
            existing.exercises = d.exercises
            if let i = routines.firstIndex(where: { $0.id == rid }) { routines[i] = existing }
            draft = nil
            queueSave(); touch(); markDirty()
            return existing
        }
        let r = Routine(id: nextRoutineId(), name: name, exercises: d.exercises, pos: nextPos())
        routines.append(r)
        draft = nil
        queueSave(); touch(); markDirty()
        return r
    }

    @discardableResult
    func finishWorkout(_ name: String) -> Workout? {
        guard let d = draft else { return nil }
        let now = Date.now.timeIntervalSince1970
        var prs: [PrRec] = []
        for ex in d.exercises {
            let done = ex.sets.filter { ($0.kg ?? 0) > 0 && ($0.reps ?? 0) > 0 && $0.done }
            guard !done.isEmpty else { continue }
            let prev = prFor(ex.name)
            let pw = prev?.weight ?? 0
            let pe = prev?.e1rm ?? 0
            let bw = done.map { $0.kg! }.max()!
            let be = done.map { Calc.e1rm($0.kg!, $0.reps!) }.max()!
            if bw > pw { prs.append(PrRec(ex: ex.name, kind: "Weight", value: bw)) }
            if be > pe { prs.append(PrRec(ex: ex.name, kind: "Est. 1RM", value: be)) }
        }
        let w = Workout(
            id: nextWorkoutId(),
            name: name.trimmingCharacters(in: .whitespaces).isEmpty ? "Séance" : name.trimmingCharacters(in: .whitespaces),
            startedAt: d.startedAt ?? now - 3600,
            endedAt: now,
            exercises: d.exercises,
            prs: prs,
            notes: d.notes
        )
        workouts.append(w)
        prCache = Calc.rebuildPrs(&workouts)
        draft = nil
        queueSave(); touch(); markDirty()
        return w
    }

    func discardDraft() {
        draft = nil
        touch()
    }

    func draftDiffersFromRoutine() -> Bool {
        guard let d = draft, let rid = d.routineId, let r = routineById(rid) else { return false }
        if d.exercises.count != r.exercises.count { return true }
        for (de, re) in zip(d.exercises, r.exercises) {
            if de.name != re.name || de.notes != re.notes || de.superset != re.superset || de.restSec != re.restSec || de.sets.count != re.sets.count {
                return true
            }
        }
        return false
    }

    func updateRoutineFromDraft() {
        guard let d = draft, let rid = d.routineId, let i = routines.firstIndex(where: { $0.id == rid }) else { return }
        routines[i].exercises = d.exercises.map { ex in
            ExEntry(name: ex.name, muscle: ex.muscle, notes: ex.notes, superset: ex.superset, restSec: ex.restSec,
                    sets: ex.sets.map { SetEntry(kg: $0.kg, reps: $0.reps, done: false) })
        }
        queueSave(); touch(); markDirty()
    }

    func restoreWorkout(_ w: Workout) {
        workouts.removeAll { $0.id == w.id }
        workouts.append(w)
        workouts.sort { $0.startedAt < $1.startedAt }
        prCache = Calc.rebuildPrs(&workouts)
        queueSave(); markDirty()
    }

    func deleteWorkout(_ id: Int) {
        if let w = workoutById(id) { tombstoneWorkout(w.startedAt) }
        workouts.removeAll { $0.id == id }
        prCache = Calc.rebuildPrs(&workouts)
        queueSave(); markDirty()
    }

    @discardableResult
    func routineFromWorkout(_ workoutId: Int, _ name: String) -> Routine? {
        guard let w = workoutById(workoutId) else { return nil }
        let r = Routine(id: nextRoutineId(), name: name.trimmingCharacters(in: .whitespaces).isEmpty ? w.name : name, exercises: w.exercises, pos: nextPos())
        routines.append(r)
        queueSave(); touch(); markDirty()
        return r
    }

    func renameRoutine(_ id: Int, _ name: String) {
        guard let i = routines.firstIndex(where: { $0.id == id }) else { return }
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        routines[i].name = trimmed.isEmpty ? routines[i].name : trimmed
        queueSave(); touch(); markDirty()
    }

    @discardableResult
    func duplicateRoutine(_ id: Int) -> Routine? {
        guard let r = routineById(id) else { return nil }
        let copy = Routine(id: nextRoutineId(), name: r.name + " (2)", exercises: r.exercises, pos: nextPos())
        routines.append(copy)
        queueSave(); touch(); markDirty()
        return copy
    }

    func deleteRoutine(_ id: Int) {
        if let r = routineById(id) { tombstoneRoutine(r.name) }
        routines.removeAll { $0.id == id }
        queueSave(); markDirty()
    }

    /// Drag & drop reorder (indices == manual pos order).
    func moveRoutine(from: IndexSet, to: Int) {
        routines.move(fromOffsets: from, toOffset: to)
        for i in routines.indices { routines[i].pos = i }
        queueSave(); markDirty()
    }

    func moveExercise(draftIndex from: Int, to: Int) {
        guard var d = draft, from >= 0, from < d.exercises.count, to >= 0, to < d.exercises.count else { return }
        let item = d.exercises.remove(at: from)
        let dest = to > from ? to : to
        d.exercises.insert(item, at: dest)
        draft = d
        touch()
    }

    // ---------- settings ----------

    func setUnit(_ u: String) { settings.unit = u; queueSave(); markDirty() }
    func setRest(_ sec: Int) { settings.restSec = sec; queueSave(); markDirty() }
    func setAccent(_ a: String) { settings.accent = a; queueSave(); markDirty() }
    func setProfile(_ name: String, _ handle: String) { settings.profileName = name; settings.handle = handle; queueSave(); markDirty() }
    func setAvatar(_ url: String) { settings.avatarUrl = url; queueSave(); markDirty() }

    func backupJson() -> String {
        let data = (try? JSONEncoder().encode(BackupData(workouts: workouts, routines: routines))) ?? Data()
        return String(data: data, encoding: .utf8) ?? ""
    }

    func restoreBackup(_ content: String) -> Bool {
        guard let data = content.data(using: .utf8),
              let back = try? JSONDecoder().decode(BackupData.self, from: data) else { return false }
        workouts = back.workouts
        routines = back.routines
        for i in routines.indices { routines[i].pos = i }
        prCache = Calc.rebuildPrs(&workouts)
        queueSave(); touch(); markDirty()
        return true
    }

    func importCsv(_ content: String) -> Int {
        let imported = Calc.parseCsv(content)
        workouts.append(contentsOf: imported)
        if !imported.isEmpty {
            prCache = Calc.rebuildPrs(&workouts)
            queueSave(); markDirty()
        }
        return imported.count
    }

    func importHevy(_ newWorkouts: [Workout], _ newRoutines: [Routine]) -> Int {
        let knownDates = Set(workouts.map { $0.startedAt })
        var added = 0
        for w in newWorkouts where !knownDates.contains(w.startedAt) {
            var fixed = w
            fixed.id = nextWorkoutId()
            workouts.append(fixed)
            added += 1
        }
        var allRoutines = newRoutines
        allRoutines.append(contentsOf: Calc.routinesFromWorkouts(newWorkouts, existingRoutineNames: Set(routines.map { $0.name })))
        for r in allRoutines {
            var fixed = r
            fixed.id = nextRoutineId()
            fixed.pos = nextPos()
            routines.append(fixed)
        }
        if added > 0 || !allRoutines.isEmpty {
            workouts.sort { $0.startedAt < $1.startedAt }
            prCache = Calc.rebuildPrs(&workouts)
            queueSave(); markDirty()
        }
        return added
    }

    func wipe(_ markDirtyFlag: Bool = true) {
        workouts = []
        routines = []
        draft = nil
        photos = []
        customs = []
        prCache = [:]
        queueSave()
        if markDirtyFlag { markDirty() }
    }

    // ---------- cloud sync engine (port of Cloud.kt) ----------

    func markDirty() {
        dirtyAt = Date.now.timeIntervalSince1970
        persistMeta()
        requestSync()
    }

    func currentTombW() -> [Double] { delW }
    func currentTombR() -> [String] { delR }

    func tombstoneWorkout(_ startedAt: Double) {
        if !delW.contains(startedAt) { delW.append(startedAt) }
        if delW.count > 400 { delW.removeFirst() }
        persistMeta()
    }

    func tombstoneRoutine(_ name: String) {
        if !delR.contains(name) { delR.append(name) }
        if delR.count > 400 { delR.removeFirst() }
        persistMeta()
    }

    func requestSync(debounce: Double = 3) {
        guard session != nil else { return }
        syncTask?.cancel()
        syncTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: UInt64(debounce * 1_000_000_000))
            if !Task.isCancelled { await self?.syncNow() }
        }
    }

    func syncNow() async {
        guard let s0 = session, !syncing else { return }
        syncing = true
        defer { syncing = false; objectWillChange.send() }
        do {
            var s = s0
            if s.expiresAt - Date.now.timeIntervalSince1970 < 60 {
                s = try await client.refresh(s)
                session = s
                Keychain.saveSession(s)
            }
            let (remote, remoteTs) = try await client.pull(s)
            let dirty = dirtyAt > pushedTs
            if !dirty {
                if let remote, remoteTs > lastSeenRemoteTs {
                    let oldAvatar = settings.avatarUrl
                    replaceAll(remote)
                    delW = remote.delW
                    delR = remote.delR
                    lastSeenRemoteTs = remoteTs
                    pushedTs = remoteTs
                    dirtyAt = remoteTs
                    persistMeta()
                    await maybeFetchAvatar(previous: oldAvatar)
                    syncStatus = stamp("Synchronisé")
                }
            } else {
                if let remote, remoteTs > lastSeenRemoteTs {
                    _ = mergeRemote(remote)
                    for w in remote.delW where !delW.contains(w) { delW.append(w) }
                    for r in remote.delR where !delR.contains(r) { delR.append(r) }
                    lastSeenRemoteTs = remoteTs
                    persistMeta()
                }
                let ts = max(Date.now.timeIntervalSince1970, lastSeenRemoteTs + 1)
                let payload = snapshot()
                let stored = try await client.push(s, payload: payload, ts: ts)
                pushedTs = ts
                dirtyAt = ts
                lastSeenRemoteTs = stored
                persistMeta()
                syncStatus = stamp("Synchronisé")
            }
        } catch {
            syncStatus = stamp("Sync échouée")
        }
    }

    private func stamp(_ s: String) -> String {
        s + " · " + Calc.fmtTime(Date.now.timeIntervalSince1970)
    }

    private func maybeFetchAvatar(previous: String) async {
        let url = settings.avatarUrl
        guard !url.isEmpty, url != previous, let u = URL(string: url) else { return }
        if let (data, resp) = try? await URLSession.shared.data(from: u),
           (resp as? HTTPURLResponse)?.statusCode ?? 0 < 300 {
            AvatarStore.save(data)
            avatarVersion += 1
        }
    }

    func snapshot() -> SyncPayload {
        SyncPayload(workouts: workouts, routines: routines, settings: settings, delW: delW, delR: delR, photos: photos, delP: delP, customs: customs, delC: delC, v: 1)
    }

    func currentTombP() -> [Double] { delP }

    func replaceAll(_ p: SyncPayload) {
        workouts = p.workouts
        routines = p.routines
        for i in routines.indices { routines[i].pos = i }
        if let ps = p.settings { settings = ps }
        photos = p.photos.sorted { $0.ts < $1.ts }
        delP = p.delP
        delC = p.delC
        customs = p.customs
        prCache = Calc.rebuildPrs(&workouts)
        queueSave()
    }

    @discardableResult
    func mergeRemote(_ p: SyncPayload) -> Bool {
        let localDelW = Set(delW)
        let localDelR = Set(delR)
        let remoteDelW = Set(p.delW)
        let remoteDelR = Set(p.delR)
        let keepW = workouts.filter { !remoteDelW.contains($0.startedAt) }
        let keepR = routines.filter { !remoteDelR.contains($0.name) }
        let remoteDelP = Set(p.delP)
        let keepP = photos.filter { !remoteDelP.contains($0.id) }
        let remoteDelC = Set(p.delC)
        let localDelC = Set(delC)
        let keepC = customs.filter { !remoteDelC.contains($0.name) }
        let knownW = Set(keepW.map { $0.startedAt })
        let knownR = Set(keepR.map { $0.name })
        let knownP = Set(keepP.map { $0.id })
        let addW = p.workouts.filter { !localDelW.contains($0.startedAt) && !knownW.contains($0.startedAt) }
        let addR = p.routines.filter { !localDelR.contains($0.name) && !knownR.contains($0.name) }
        let addP = p.photos.filter { !knownP.contains($0.id) }
        let knownC = Set(keepC.map { $0.name })
        let addC = p.customs.filter { !localDelC.contains($0.name) && !knownC.contains($0.name) }
        if keepW.count == workouts.count, keepR.count == routines.count, keepP.count == photos.count, keepC.count == customs.count, addW.isEmpty, addR.isEmpty, addP.isEmpty, addC.isEmpty { return false }
        workouts = keepW
        for w in addW {
            var f = w
            f.id = nextWorkoutId()
            workouts.append(f)
        }
        workouts.sort { $0.startedAt < $1.startedAt }
        routines = keepR
        for r in addR {
            var f = r
            f.id = nextRoutineId()
            f.pos = nextPos()
            routines.append(f)
        }
        photos = (keepP + addP).sorted { $0.ts < $1.ts }
        for d in p.delP where !delP.contains(d) { delP.append(d) }
        for d in p.delC where !delC.contains(d) { delC.append(d) }
        customs = keepC + addC
        prCache = Calc.rebuildPrs(&workouts)
        queueSave()
        return true
    }

    // ---------- auth ----------

    func signIn(email: String, password: String) async {
        await auth { try await self.client.signIn(email: email, password: password) }
    }

    func signUp(email: String, password: String) async {
        await auth { try await self.client.signUp(email: email, password: password) }
    }

    private func auth(_ op: @escaping () async throws -> CloudSession) async {
        busy = true
        authError = nil
        defer { busy = false; googlePending = false }
        do {
            let s = try await op()
            await applySession(s)
        } catch CloudError.http(let code, let body) {
            authError = GoTrueClient.mapAuthError(code, body)
        } catch {
            authError = "Connexion impossible — vérifie Internet."
        }
    }

    func applySession(_ s: CloudSession) async {
        session = s
        Keychain.saveSession(s)
        if let last = lastAccount, last != s.email {
            // account switch: wipe this device's copy of the previous account's data
            wipe(false)
            delW = []
            delR = []
            delP = []
            delC = []
            photos = []
            customs = []
            dirtyAt = 0
            pushedTs = 0
            lastSeenRemoteTs = 0
        } else if lastAccount == nil, !workouts.isEmpty || !routines.isEmpty {
            dirtyAt = Date.now.timeIntervalSince1970
        }
        lastAccount = s.email
        skipped = false
        persistMeta()
        await syncNow()
    }

    func signOut() {
        if let s = session { Task.detached { await self.client.logout(s) } }
        Keychain.deleteSession()
        session = nil
        skipped = false
        syncStatus = nil
    }

    // Google: ASWebAuthenticationSession is driven from AuthView (needs a presentation context);
    // the app-side result lands here.
    func completeGoogleSession(_ s: CloudSession) async {
        googlePending = false
        await applySession(s)
    }

    // ---------- queries ----------

    func prFor(_ name: String) -> PrBest? { prCache[name] }

    func e1rmSeries(_ name: String) -> [(String, Double)] { Calc.e1rmSeries(name, workouts) }

    /// Previous performance in display units: "82.5kg × 8" (Android Repo.prevFor semantics).
    func prevDisplay(_ name: String) -> [String]? {
        let unit = settings.unit
        for w in workoutsDesc() {
            if let ex = w.exercises.first(where: { $0.name == name && $0.sets.contains { $0.kg != nil || $0.reps != nil } }) {
                return ex.sets.map { s in
                    (s.kg == nil && s.reps == nil) ? "—" : "\(Calc.fmtKg(s.kg, unit))\(Calc.unitLabel(unit)) × \(s.reps.map(String.init) ?? "—")"
                }
            }
        }
        return nil
    }

    func routineLastPerformed(_ r: Routine) -> Double? {
        workoutsDesc().first {
            $0.name == r.name && $0.exercises.contains { e in r.exercises.contains { $0.name == e.name } }
        }?.startedAt
    }

    func prevSetsBefore(_ beforeMs: Double, _ name: String) -> [String]? {
        let unit = settings.unit
        for w in workouts.sorted(by: { $0.startedAt > $1.startedAt }) {
            guard w.startedAt < beforeMs else { continue }
            if let ex = w.exercises.first(where: { $0.name == name && $0.sets.contains { $0.kg != nil || $0.reps != nil } }) {
                return ex.sets.map { s in
                    (s.kg == nil && s.reps == nil) ? "—" : "\(Calc.fmtKg(s.kg, unit))\(Calc.unitLabel(unit)) × \(s.reps.map(String.init) ?? "—")"
                }
            }
        }
        return nil
    }
}

/** Locally cached profile photo (Documents/avatar.jpg). */
enum AvatarStore {
    static func fileUrl() -> URL? {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first?.appendingPathComponent("avatar.jpg")
    }
    static func save(_ data: Data) {
        if let u = fileUrl() { try? data.write(to: u) }
    }
    static func load() -> Data? {
        if let u = fileUrl() { return try? Data(contentsOf: u) }
        return nil
    }
    static func remove() {
        if let u = fileUrl() { try? FileManager.default.removeItem(at: u) }
    }
}

/** Session at rest: iOS Keychain. */
enum Keychain {
    private static let service = "net.webtvmedia.liftbook"

    static func saveSession(_ s: CloudSession) {
        guard let data = try? JSONEncoder().encode(s) else { return }
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: s.email,
        ]
        _ = SecItemDelete(query as CFDictionary)
        var add = query
        add[kSecValueData as String] = data
        _ = SecItemAdd(add as CFDictionary, nil)
    }

    static func loadSession() -> CloudSession? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecReturnAttributes as String: true,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var out: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &out) == errSecSuccess,
              let dict = out as? [String: Any],
              let data = dict[kSecValueData as String] as? Data else { return nil }
        return try? JSONDecoder().decode(CloudSession.self, from: data)
    }

    static func deleteSession() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
        ]
        _ = SecItemDelete(query as CFDictionary)
    }
}
