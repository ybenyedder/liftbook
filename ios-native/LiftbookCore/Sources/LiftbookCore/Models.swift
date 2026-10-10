/** Data model — 1:1 port of the Android app's Models.kt. */

public struct SetEntry: Codable, Equatable, Sendable {
    public var kg: Double?
    public var reps: Int?
    public var done: Bool
    public var prW: Bool
    public var prE: Bool

    public init(kg: Double? = nil, reps: Int? = nil, done: Bool = true, prW: Bool = false, prE: Bool = false) {
        self.kg = kg
        self.reps = reps
        self.done = done
        self.prW = prW
        self.prE = prE
    }
}

public struct ExEntry: Codable, Equatable, Sendable {
    public var name: String
    public var muscle: String
    public var notes: String
    public var superset: Bool
    public var restSec: Int?
    public var sets: [SetEntry]

    public init(name: String, muscle: String, notes: String = "", superset: Bool = false, restSec: Int? = nil, sets: [SetEntry] = []) {
        self.name = name
        self.muscle = muscle
        self.notes = notes
        self.superset = superset
        self.restSec = restSec
        self.sets = sets
    }
}

public struct PrRec: Codable, Equatable, Sendable {
    public var ex: String
    public var kind: String   // "Weight" | "Est. 1RM"
    public var value: Double
    public init(ex: String, kind: String, value: Double) { self.ex = ex; self.kind = kind; self.value = value }
}

public struct Workout: Codable, Equatable, Sendable, Identifiable {
    public var id: Int
    public var name: String
    public var startedAt: Double
    public var endedAt: Double
    public var exercises: [ExEntry]
    public var prs: [PrRec]
    public var notes: String

    public init(id: Int, name: String, startedAt: Double, endedAt: Double, exercises: [ExEntry] = [], prs: [PrRec] = [], notes: String = "") {
        self.id = id
        self.name = name
        self.startedAt = startedAt
        self.endedAt = endedAt
        self.exercises = exercises
        self.prs = prs
        self.notes = notes
    }
}

public struct Routine: Codable, Equatable, Sendable, Identifiable {
    public var id: Int
    public var name: String
    public var exercises: [ExEntry]
    public var pos: Int

    public init(id: Int, name: String, exercises: [ExEntry] = [], pos: Int = 0) {
        self.id = id
        self.name = name
        self.exercises = exercises
        self.pos = pos
    }
}

public struct Settings: Codable, Equatable, Sendable {
    public var unit: String = "kg"
    public var restSec: Int = 90
    public var theme: String = "dark"
    public var accent: String = "blue"
    public var profileName: String = "Athlète"
    public var handle: String = "athlete"
    public var since: Double = 0
    public var avatarUrl: String = ""

    public init() {}
}

public struct PrBest: Equatable, Sendable {
    public var weight: Double
    public var weightDate: Double
    public var e1rm: Double
    public var e1rmDate: Double
    public init(weight: Double, weightDate: Double, e1rm: Double, e1rmDate: Double) {
        self.weight = weight
        self.weightDate = weightDate
        self.e1rm = e1rm
        self.e1rmDate = e1rmDate
    }
}

/// Workout in progress or routine being edited.
public struct Draft: Codable, Equatable, Sendable {
    public var mode: String              // "workout" | "routine"
    public var routineId: Int?
    public var name: String
    public var startedAt: Double?
    public var notes: String
    public var exercises: [ExEntry]

    public init(mode: String, routineId: Int? = nil, name: String, startedAt: Double? = nil, notes: String = "", exercises: [ExEntry] = []) {
        self.mode = mode
        self.routineId = routineId
        self.name = name
        self.startedAt = startedAt
        self.notes = notes
        self.exercises = exercises
    }
}

public struct WeekStats: Sendable {
    public var count: Int
    public var vol: Double
    public var reps: Int
    public var prs: Int
}

public struct BackupData: Codable, Sendable {
    public var workouts: [Workout]
    public var routines: [Routine]
    public init(workouts: [Workout], routines: [Routine]) { self.workouts = workouts; self.routines = routines }
}

/// Full cloud snapshot pushed/pulled per account (deletions ride along as tombstones).
public struct SyncPayload: Codable, Equatable, Sendable {
    public var workouts: [Workout]
    public var routines: [Routine]
    public var settings: Settings?
    public var delW: [Double]
    public var delR: [String]
    public var photos: [ProgressPhoto]
    public var delP: [Double]
    public var v: Int

    public init(workouts: [Workout] = [], routines: [Routine] = [], settings: Settings? = nil, delW: [Double] = [], delR: [String] = [], photos: [ProgressPhoto] = [], delP: [Double] = [], v: Int = 1) {
        self.workouts = workouts
        self.routines = routines
        self.settings = settings
        self.delW = delW
        self.delR = delR
        self.photos = photos
        self.delP = delP
        self.v = v
    }
}

/// Progress photo metadata (pixels live per-platform; `remote` is the Storage path).
/// The iOS app has no photo UI yet — this exists so snapshots round-trip photos
/// without dropping them when this device pushes.
public struct ProgressPhoto: Codable, Equatable, Sendable, Identifiable {
    public var id: Double
    public var ts: Double
    public var wId: Double?
    public var note: String
    public var kg: Double?
    public var remote: String

    public init(id: Double, ts: Double, wId: Double? = nil, note: String = "", kg: Double? = nil, remote: String = "") {
        self.id = id
        self.ts = ts
        self.wId = wId
        self.note = note
        self.kg = kg
        self.remote = remote
    }

    /// Android (kotlinx) omits fields at their default value (empty note, null kg...)
    /// and cannot decode JSON nulls into its non-null String fields — so decoding is
    /// lenient (missing → default) and encoding omits nils, never writing null.
    private enum CodingKeys: String, CodingKey { case id, ts, wId, note, kg, remote }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Double.self, forKey: .id)
        ts = try c.decode(Double.self, forKey: .ts)
        wId = try c.decodeIfPresent(Double.self, forKey: .wId)
        note = try c.decodeIfPresent(String.self, forKey: .note) ?? ""
        kg = try c.decodeIfPresent(Double.self, forKey: .kg)
        remote = try c.decodeIfPresent(String.self, forKey: .remote) ?? ""
    }

    public func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(ts, forKey: .ts)
        if let w = wId { try c.encode(w, forKey: .wId) }
        try c.encode(note, forKey: .note)
        if let k = kg { try c.encode(k, forKey: .kg) }
        try c.encode(remote, forKey: .remote)
    }
}
