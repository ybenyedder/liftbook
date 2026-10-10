import XCTest
@testable import LiftbookCore

final class LogicTests: XCTestCase {
    override func setUp() {
        super.setUp()
        L10n.lang = "fr"
    }

    func testExerciseDatabase() {
        XCTAssertGreaterThanOrEqual(ExData.all.count, 600)
        XCTAssertEqual(Set(ExData.all.map { $0.name }).count, ExData.all.count)
        for e in ExData.all {
            XCTAssertNotNil(ExData.nameFr[e.name], "FR name missing: \(e.name)")
            XCTAssertFalse(stepsFor(e.name).isEmpty, "steps missing: \(e.name)")
        }
    }

    func testFuzzyFrSearch() {
        func hits(_ q: String) -> [String] {
            ExData.all.filter { matches(q, $0.name) }.map { $0.name }
        }
        XCTAssertTrue(hits("tirage poitrine").contains("Chest Supported Row"))
        XCTAssertTrue(hits("dc").contains("Barbell Bench Press"))
        XCTAssertTrue(hits("sdt").contains("Deadlift"))
        XCTAssertTrue(hits("rowing poulie assis prise en v").contains("V-Bar Cable Row"))
        XCTAssertFalse(hits("goblet").contains { ExData.byName[$0]?.muscle == "Calves" })
    }

    func testArchetypes() {
        XCTAssertEqual(archetypeOf("Barbell Bench Press"), "bench")
        XCTAssertEqual(archetypeOf("Incline Dumbbell Bench Press"), "incline_bench")
        XCTAssertEqual(archetypeOf("Barbell Curl"), "curl")
        XCTAssertEqual(archetypeOf("Deadlift"), "deadlift")
        XCTAssertEqual(archetypeOf("Romanian Deadlift"), "rdl")
        XCTAssertEqual(archetypeOf("Plank"), "plank")
        XCTAssertEqual(archetypeOf("Treadmill"), "conditioning")
    }

    func testHevyCsvImport() {
        let header = "title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,set_index,set_type,weight_kg,reps,distance_km,duration_seconds,rpe"
        func row(_ ex: String, _ i: Int, _ kg: String, _ reps: String) -> String {
            "\"Push Day\",\"18 sept. 2026, 17:31\",\"18 sept. 2026, 18:20\",\"\",\"" + ex + "\",\"\",\"\",\(i),\"regular\",\(kg),\(reps),,"
        }
        let csv = [header, row("Développé Couché (Barre)", 1, "70", "8"), row("Développé Couché (Barre)", 2, "70", "8"), row("Tirage Vertical", 1, "60", "10")].joined(separator: "\n")
        let (ws, rs) = Calc.parseHevyCsv(csv)
        XCTAssertEqual(ws.count, 1)
        XCTAssertEqual(ws[0].name, "Push Day")
        XCTAssertEqual(ws[0].exercises.count, 2)
        XCTAssertEqual(ws[0].exercises[0].name, "Barbell Bench Press")
        XCTAssertEqual(ws[0].exercises[0].sets.count, 2)
        XCTAssertTrue(rs.isEmpty)
    }

    func testStrongCsvImport() {
        let csv = "Date,Workout Name,Exercise Name,Set Order,Weight,Weight Unit,Reps\n\"2026-09-18 17:31:00\",\"Bench\",\"Squat (Barre)\",1,100,kg,5"
        let (ws, _) = Calc.parseHevyCsv(csv)
        XCTAssertEqual(ws.count, 1)
        XCTAssertEqual(ws[0].exercises[0].name, "Barbell Squat")
        XCTAssertEqual(ws[0].exercises[0].sets[0].kg, 100)
    }

    func testTemplateCsvBecomesRoutine() {
        let csv = "exercise_name,set_index,weight_kg,reps\nDéveloppé Couché (Barre),1,70,8\nTirage Vertical,1,60,10"
        let (ws, rs) = Calc.parseHevyCsv(csv)
        XCTAssertTrue(ws.isEmpty)
        XCTAssertEqual(rs.count, 1)
        XCTAssertEqual(rs[0].exercises.count, 2)
    }

    func testLocalizedDates() {
        XCTAssertNotNil(Calc.parseExportDate("18 sept. 2026, 17:31"))
        XCTAssertNotNil(Calc.parseExportDate("18 Sep 2026, 17:31"))
        XCTAssertNotNil(Calc.parseExportDate("18 März 2026, 17:31"))
        XCTAssertNotNil(Calc.parseExportDate("5 9月 2026, 20:39"))
        XCTAssertNotNil(Calc.parseExportDate("2026-09-18T17:31:00+02:00"))
        XCTAssertNotNil(Calc.parseExportDate("1772000000"))
    }

    func testLegacyCsvImport() {
        let csv = "Date;Heure;Exercice;Serie;KG;Reps\n5 sept. 2026;17:31;Développé Couché (Barre);1;70;8\n5 sept. 2026;17:31;Tirage Vertical;1;60;10"
        let ws = Calc.parseCsv(csv)
        XCTAssertEqual(ws.count, 1)
        XCTAssertEqual(ws[0].exercises.count, 2)
        XCTAssertEqual(ws[0].exercises[0].name, "Barbell Bench Press")
    }

    func testPrEngine() {
        var (ws, _) = Calc.seed(nowMs: Date(timeIntervalSince1970: 1_772_000_000).timeIntervalSince1970 * 1000)
        let cache = Calc.rebuildPrs(&ws)
        let totalPrs = ws.reduce(0) { $0 + $1.prs.count }
        XCTAssertGreaterThan(totalPrs, 0)
        let bench = cache["Barbell Bench Press"]
        XCTAssertNotNil(bench)
        XCTAssertGreaterThan(bench!.weight, 70)
    }

    func testRoutinesFromWorkouts() {
        var (ws, _) = Calc.seed(nowMs: 1_772_000_000_000)
        let routines = Calc.routinesFromWorkouts(ws, existingRoutineNames: ["Push Day"])
        let names = routines.map { $0.name }
        XCTAssertTrue(names.contains("Pull Day"))
        XCTAssertTrue(names.contains("Leg Day"))
        XCTAssertFalse(names.contains("Push Day"))
    }

    func testMathAndSeedDeterminism() {
        XCTAssertEqual(Calc.e1rm(100, 0), 100)
        XCTAssertEqual(Calc.e1rm(100, 30), 200, accuracy: 0.001)
        XCTAssertEqual(Calc.round125(71.3), 71.25, accuracy: 0.001)
        let (a, _) = Calc.seed(nowMs: 1_772_000_000_000)
        let (b, _) = Calc.seed(nowMs: 1_772_000_000_000)
        XCTAssertEqual(a.count, b.count)
        XCTAssertEqual(a.first.map { String($0.startedAt) }, b.first.map { String($0.startedAt) })
    }

    func testFrDisplayNames() {
        XCTAssertEqual(exName("Barbell Bench Press"), "Développé Couché (Barre)")
        XCTAssertEqual(exName("V-Bar Cable Row"), "Tirage Horizontal (Poulie, V-Bar)")
        XCTAssertEqual(exName("Deadlift"), "Soulevé de Terre")
    }

    func testFmtClockAndVol() {
        XCTAssertEqual(Calc.fmtClock(4 * 60_000 + 37_000), "04:37")
        XCTAssertEqual(Calc.fmtDur(3_720_000), "1h 2m")
        XCTAssertEqual(Calc.fmtKg(70.25, "kg"), "70.3")
        XCTAssertEqual(Calc.fmtKg(70.0, "kg"), "70")
    }
}

final class CloudHelpersTests: XCTestCase {
    // ---------------- cardio (SetEntry mins/km) ----------------

    func testCardioSetRoundTripInSyncPayload() throws {
        // iOS encode → lenient decode, nils omitted (kotlinx cannot decode JSON nulls)
        let w = Workout(id: 1, name: "Cardio", startedAt: 1_772_000_000_000, endedAt: 1_772_000_100_000, exercises: [
            ExEntry(name: "Treadmill", muscle: "Cardio", sets: [
                SetEntry(mins: 22, km: 5.2, done: true),
                SetEntry(kg: 100, reps: 8, done: true),
            ])
        ])
        let p = SyncPayload(workouts: [w], v: 4)
        let json = String(data: try JSONEncoder().encode(p), encoding: .utf8)!
        XCTAssertTrue(json.contains("\"mins\":22"), "mins must be encoded: \(json.prefix(300))")
        XCTAssertTrue(json.contains("\"km\":5.2"))
        XCTAssertFalse(json.contains(":null"), "iOS never encodes nulls")
        let back = try JSONDecoder().decode(SyncPayload.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(back.v, 4)
        XCTAssertEqual(back.workouts[0].exercises[0].sets[0], SetEntry(mins: 22, km: 5.2, done: true))
        XCTAssertEqual(back.workouts[0].exercises[0].sets[1].mins, nil)
        XCTAssertEqual(back.workouts[0].exercises[0].sets[1].km, nil)

        // Android kotlinx JSON (defaults/nulls omitted) decodes leniently
        let android = """
        {"workouts":[{"id":1,"name":"Cardio","startedAt":1772000000000,"endedAt":1772000100000,
          "exercises":[{"name":"Treadmill","muscle":"Cardio","sets":[
            {"mins":22,"km":5.2,"done":true},
            {"kg":100.0,"reps":8,"done":true}
          ]}]}],"v":4}
        """
        let dec = try JSONDecoder().decode(SyncPayload.self, from: android.data(using: .utf8)!)
        let s = dec.workouts[0].exercises[0].sets
        XCTAssertEqual(s[0].mins, 22)
        XCTAssertEqual(s[0].km ?? 0, 5.2, accuracy: 1e-9)
        XCTAssertNil(s[0].kg)
        XCTAssertNil(s[0].reps)
        XCTAssertEqual(s[1].kg ?? 0, 100, accuracy: 1e-9)
        XCTAssertEqual(s[1].reps, 8)
        XCTAssertNil(s[1].mins)
        XCTAssertNil(s[1].km)
    }

    func testFmtCardioSetAndDetection() {
        XCTAssertEqual(Calc.fmtCardioSet(22, 5.2), "22min · 5.2km")
        XCTAssertEqual(Calc.fmtCardioSet(65, nil), "1h05")
        XCTAssertEqual(Calc.fmtCardioSet(60, nil), "1h")
        XCTAssertEqual(Calc.fmtCardioSet(nil, 5.0), "5km")
        XCTAssertEqual(Calc.fmtCardioSet(nil, nil), "—")
        XCTAssertTrue(Calc.isCardioName("Treadmill"))
        XCTAssertFalse(Calc.isCardioName("Barbell Bench Press"))
        // customs resolve through the registry (Android registers them into EX)
        CustomRegistry.set([CustomExercise(name: "Mon Cardio Perso", muscle: "Cardio")])
        XCTAssertTrue(Calc.isCardioName("Mon Cardio Perso"))
        CustomRegistry.set([])
        XCTAssertFalse(Calc.isCardioName("Mon Cardio Perso"))
    }

    func testParseCsvCardioColumns() {
        // port of Android test n°17 — cardio columns Min;Km
        let csv = "Date;Heure;Exercice;Serie;KG;Reps;Min;Km\n5 janv. 2026;18:00;Tapis de Course;1;;;22;5.2"
        let ws = Calc.parseCsv(csv)
        XCTAssertEqual(ws.count, 1)
        let s = ws[0].exercises[0].sets[0]
        XCTAssertEqual(s.mins, 22)
        XCTAssertEqual(s.km ?? 0, 5.2, accuracy: 1e-9)
        XCTAssertNil(s.kg)
        XCTAssertNil(s.reps)
        // legacy 6-column strength rows still parse (Min/Km absent → nil)
        let legacy = Calc.parseCsv("Date;Heure;Exercice;Serie;KG;Reps\n5 janv. 2026;18:00;Développé Couché (Barre);1;80;8")
        let s2 = legacy[0].exercises[0].sets[0]
        XCTAssertEqual(s2.kg, 80)
        XCTAssertEqual(s2.reps, 8)
        XCTAssertNil(s2.mins)
        XCTAssertNil(s2.km)
    }

    // ---------------- plate calculator ----------------

    func testPlatesForSide() {
        // Sans barre : 50 kg → 25 kg par côté (tout en disques)
        let noBar = Calc.platesForSide(targetKg: 50, barKg: 0, unit: "kg")
        XCTAssertEqual(noBar.perSide.count, 1)
        XCTAssertEqual(noBar.perSide[0].weight, 25.0)
        XCTAssertEqual(noBar.perSide[0].count, 1)
        XCTAssertEqual(noBar.totalKg, 50, accuracy: 1e-9)
        // Barre 20 kg : 50 kg → 15 kg par côté
        let withBar = Calc.platesForSide(targetKg: 50, barKg: 20, unit: "kg")
        XCTAssertEqual(withBar.perSide.reduce(0.0) { $0 + $1.weight * Double($1.count) }, 15.0, accuracy: 1e-9)
        XCTAssertEqual(withBar.totalKg, 50, accuracy: 1e-9)
        // 100 kg barre 20 → 25+15 par côté
        let hundred = Calc.platesForSide(targetKg: 100, barKg: 20, unit: "kg")
        XCTAssertEqual(hundred.perSide.map { $0.weight }, [25.0, 15.0])
        XCTAssertEqual(hundred.totalKg, 100, accuracy: 1e-9)
        // livres : 135 lb barre 45 → une plaque 45 lb par côté
        let lb = Calc.platesForSide(targetKg: 135, barKg: 45, unit: "lb")
        XCTAssertEqual(lb.perSide.map { $0.weight }, [45.0])
        XCTAssertEqual(lb.totalKg, 135, accuracy: 1e-9)
        // non atteignable : 47,3 kg barre 20 → 12,5/côté (10+2,5) → total le plus proche 45
        let near = Calc.platesForSide(targetKg: 47.3, barKg: 20, unit: "kg")
        XCTAssertEqual(near.perSide.map { $0.weight }, [10.0, 2.5])
        XCTAssertEqual(near.totalKg, 45, accuracy: 1e-9)
        // cible sous la barre → barre seule
        let under = Calc.platesForSide(targetKg: 10, barKg: 20, unit: "kg")
        XCTAssertTrue(under.perSide.isEmpty)
        XCTAssertEqual(under.totalKg, 20, accuracy: 1e-9)
    }

    // ---------------- CSV export (Util.kt exportCsv) ----------------

    func testExportCsvText() {
        var c = DateComponents()
        c.year = 2026; c.month = 9; c.day = 5; c.hour = 17; c.minute = 31
        let ts = (Calc.cal.date(from: c) ?? Date()).timeIntervalSince1970 * 1000
        let w = Workout(id: 1, name: "Mixte", startedAt: ts, endedAt: ts + 3_600_000, exercises: [
            ExEntry(name: "Barbell Bench Press", muscle: "Chest", sets: [SetEntry(kg: 80, reps: 8)]),
            ExEntry(name: "Treadmill", muscle: "Cardio", sets: [SetEntry(mins: 22, km: 5.2)]),
        ])
        let csv = Calc.exportCsvText(workouts: [w], unit: "kg")
        let lines = csv.split(separator: "\n").map(String.init)
        XCTAssertEqual(lines[0], "Date;Heure;Exercice;Serie;kg;Reps;Min;Km")
        XCTAssertEqual(lines[1], "5 sept. 2026;17:31;Développé Couché (Barre);1;80;8;;")
        XCTAssertEqual(lines[2], "5 sept. 2026;17:31;Tapis de Course;1;;;22;5.2")
        XCTAssertTrue(csv.hasSuffix("\n"))
        // lb header renames the weight column; ';' in an exercise name is neutralized
        let lbCsv = Calc.exportCsvText(workouts: [], unit: "lb")
        XCTAssertEqual(lbCsv.split(separator: "\n").first.map(String.init), "Date;Heure;Exercice;Serie;lbs;Reps;Min;Km")
    }

    // ---------------- custom exercises ----------------

    func testCustomNameCollision() {
        XCTAssertTrue(CustomRegistry.nameTaken("Barbell Bench Press", customs: []))                 // clé EN exacte
        XCTAssertTrue(CustomRegistry.nameTaken("Developpe Couche (Barre)", customs: []))            // nom FR, sans accents ni casse
        XCTAssertTrue(CustomRegistry.nameTaken("barbell bench press", customs: []))                 // casse seule
        XCTAssertTrue(CustomRegistry.nameTaken("Développé Couché (Barre)", customs: []))            // accents
        XCTAssertFalse(CustomRegistry.nameTaken("Zedtest Unique Alpha", customs: []))
        // collision avec un custom existant
        let customs = [CustomExercise(name: "Tirage Majorette", muscle: "Lats")]
        XCTAssertTrue(CustomRegistry.nameTaken("tirage majorette", customs: customs))
        XCTAssertFalse(CustomRegistry.nameTaken("Tirage Majorette 2", customs: customs))
        // nom vide → libre
        XCTAssertFalse(CustomRegistry.nameTaken("  ", customs: customs))
    }

    // ---------------- MiniZip ----------------

    func testMiniZipReadsDeflateAndStored() throws {
        let url = try XCTUnwrap(Bundle.module.url(forResource: "hevy", withExtension: "zip", subdirectory: "fixtures"))
        let data = try Data(contentsOf: url)
        let entries = try XCTUnwrap(MiniZip.entries(data: data))
        XCTAssertEqual(entries.map { $0.name }, ["workouts_sessions.csv", "spreadsheet_templates.csv"])
        let texts = try XCTUnwrap(MiniZip.csvTexts(data: data))
        XCTAssertEqual(texts.count, 2)
        XCTAssertTrue(texts[0].hasPrefix("title,start_time,end_time,description,exercise_title"))   // DEFLATE entry
        XCTAssertTrue(texts[0].contains("Tapis de Course"))
        XCTAssertTrue(texts[1].hasPrefix("exercise_name,set_index,weight_kg,reps"))                  // STORED entry
        // round-trips through the Hevy parser like the Android zip flow
        let (ws, rs) = Calc.parseHevyCsv(texts[0])
        XCTAssertEqual(ws.count, 2)
        XCTAssertEqual(rs.count, 0)
        // non-zip input → nil
        XCTAssertNil(MiniZip.entries(data: Data("not a zip".utf8)))
        // non-csv entries are filtered out by csvEntries
        XCTAssertNotNil(MiniZip.csvEntries(data: data))
    }

    // ---------------- lenient decode of real Android snapshots ----------------

    func testDecodesMinimalAndroidSnapshot() throws {
        // kotlinx omits fields at their defaults at EVERY level: settings, workouts,
        // exercises and sets — the payload must still decode with sane defaults.
        let android = """
        {"workouts":[{"id":7,"name":"Push","startedAt":1772000000000,"endedAt":1772003600000,
          "exercises":[{"name":"Deadlift","muscle":"Lower back","sets":[{"kg":100.0,"reps":5}]}]}],
         "settings":{"unit":"lb","restSec":120,"profileName":"Y","handle":"y","since":1771000000000},
         "v":4}
        """
        let p = try JSONDecoder().decode(SyncPayload.self, from: android.data(using: .utf8)!)
        XCTAssertEqual(p.settings?.unit, "lb")
        XCTAssertEqual(p.settings?.restSec, 120)
        XCTAssertEqual(p.settings?.avatarUrl, "")     // omitted at default
        XCTAssertEqual(p.settings?.theme, "dark")
        let s = p.workouts[0].exercises[0].sets[0]
        XCTAssertEqual(s.kg, 100)
        XCTAssertEqual(s.reps, 5)
        XCTAssertTrue(s.done)                          // omitted at default true
        XCTAssertFalse(s.prW)
        XCTAssertEqual(p.workouts[0].notes, "")
        XCTAssertEqual(p.workouts[0].prs, [])
    }

    // ---------------- BackupData with photos ----------------

    func testBackupDataWithPhotos() throws {
        let b = BackupData(
            workouts: [Workout(id: 1, name: "W", startedAt: 1, endedAt: 2)],
            routines: [],
            photos: [ProgressPhoto(id: 3, ts: 1_791_591_638_113, wId: 1, note: "après séance", kg: 78.2, remote: "uid/ph_3.jpg")]
        )
        let data = try JSONEncoder().encode(b)
        let back = try JSONDecoder().decode(BackupData.self, from: data)
        XCTAssertEqual(back.photos, b.photos)
        // legacy backup without the photos field → empty default
        let legacy = try JSONDecoder().decode(BackupData.self, from: Data("{\"workouts\":[],\"routines\":[]}".utf8))
        XCTAssertEqual(legacy.photos, [])
    }

    func testSha256KnownVectors() {
        // FIPS vectors
        XCTAssertEqual(Data("abc".utf8).sha256().map { String(format: "%02x", $0) }.joined(),
                       "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
        XCTAssertEqual(Data("".utf8).sha256().map { String(format: "%02x", $0) }.joined(),
                       "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        let long = String(repeating: "a", count: 1_000)
        XCTAssertEqual(Data(long.utf8).sha256().map { String(format: "%02x", $0) }.joined(),
                       "41edece42d63e8d9bf515a9ba6932e1c20cbc9f5a5d134645adb5db1b9737ea3")
    }

    func testPkceVerifierFormat() {
        let v = GoTrueClient.randomPkceVerifier()
        XCTAssertGreaterThanOrEqual(v.count, 60)
        XCTAssertFalse(v.contains("+"))
        XCTAssertFalse(v.contains("/"))
        XCTAssertFalse(v.contains("="))
    }

    func testAuthErrorMapping() {
        XCTAssertEqual(GoTrueClient.mapAuthError(429, ""), "Trop d'essais — réessaie dans un instant.")
        XCTAssertEqual(GoTrueClient.mapAuthError(400, "{\"error\":\"invalid_grant\"}"), "Email ou mot de passe incorrect.")
    }

    func testSyncPayloadRoundTrip() throws {
        let p = SyncPayload(
            workouts: [Workout(id: 1, name: "W", startedAt: 1, endedAt: 2, exercises: [ExEntry(name: "Deadlift", muscle: "Lower back", sets: [SetEntry(kg: 100, reps: 5)])])],
            routines: [],
            settings: Settings(),
            delW: [42],
            delR: ["A"],
            photos: [ProgressPhoto(id: 3, ts: 1791591638113, wId: 1, note: "après séance", kg: 78.2, remote: "uid/ph_3.jpg")],
            delP: [7, 9],
            v: 2
        )
        let data = try JSONEncoder().encode(p)
        let back = try JSONDecoder().decode(SyncPayload.self, from: data)
        XCTAssertEqual(back, p)
    }

    func testSyncPayloadDecodesAndroidCustomsJson() throws {
        // kotlinx omits `equip` at its "Other" default
        let json = """
        {"workouts":[],"routines":[],"delW":[],"delR":[],"photos":[],"delP":[],
         "customs":[{"name":"Tirage Majorette","muscle":"Back"},{"name":"Mollets Assis","muscle":"Calves","equip":"Machine"}],"v":3}
        """
        let p = try JSONDecoder().decode(SyncPayload.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(p.customs.count, 2)
        XCTAssertEqual(p.customs[0].equip, "Other")
        XCTAssertEqual(p.customs[1].equip, "Machine")
        // v3 snapshot without delC decodes with an empty default
        XCTAssertEqual(p.delC, [])
        let back = try JSONDecoder().decode(SyncPayload.self, from: JSONEncoder().encode(p))
        XCTAssertEqual(back.customs, p.customs)
    }

    func testSyncPayloadRoundTripsDelC() throws {
        // v4: custom-exercise deletions pushed by Android must survive the iOS round-trip
        let json = """
        {"workouts":[],"routines":[],"delW":[],"delR":[],"photos":[],"delP":[],
         "customs":[],"delC":["Tirage Majorette","Zedtest"],"v":4}
        """
        let p = try JSONDecoder().decode(SyncPayload.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(p.delC, ["Tirage Majorette", "Zedtest"])
        let back = try JSONDecoder().decode(SyncPayload.self, from: JSONEncoder().encode(p))
        XCTAssertEqual(back.delC, p.delC)
    }

    func testSyncPayloadDecodesAndroidPhotosJson() throws {
        // real payload shape pushed by the Android app (Long ids as numbers, optional fields)
        let json = """
        {"workouts":[],"routines":[],"settings":null,"delW":[],"delR":[],
         "photos":[{"id":1,"ts":1791591157289,"kg":78.2,"note":"","remote":"uid/ph_1.jpg"},
                   {"id":3,"ts":1791591638113,"wId":1,"remote":"uid/ph_3.jpg"}],
         "delP":[3],"v":2}
        """
        let p = try JSONDecoder().decode(SyncPayload.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(p.photos.count, 2)
        XCTAssertEqual(p.photos[0].kg, 78.2)
        XCTAssertEqual(p.photos[1].wId, 1)
        XCTAssertEqual(p.photos[1].note, "")
        XCTAssertEqual(p.delP, [3])
    }
}
