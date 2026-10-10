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
