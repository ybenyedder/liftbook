/** Pure workout math + formatting + CSV import + demo seed — port of Calc.kt. */
import Foundation

public enum Calc {
    public static let LB = 2.2046226

    public static func e1rm(_ kg: Double, _ reps: Int) -> Double {
        reps > 0 ? kg * (1 + Double(reps) / 30.0) : kg
    }

    public static func round125(_ x: Double) -> Double {
        (x / 1.25).rounded() * 1.25
    }

    public static func vol(_ w: Workout) -> Double {
        w.exercises.reduce(0) { acc, ex in
            acc + ex.sets.reduce(0) { a, s in
                (s.done && s.kg != nil && s.reps != nil) ? a + s.kg! * Double(s.reps!) : a
            }
        }
    }

    public static func reps(_ w: Workout) -> Int {
        w.exercises.reduce(0) { acc, ex in
            acc + ex.sets.reduce(0) { a, s in (s.done && s.reps != nil) ? a + s.reps! : a }
        }
    }

    public static func setsDone(_ w: Workout) -> Int {
        w.exercises.reduce(0) { $0 + $1.sets.filter { $0.done && ($0.kg != nil || $0.reps != nil || $0.mins != nil || $0.km != nil) }.count }
    }

    /// Cardio exercises (muscle == "Cardio") log minutes/km instead of weight × reps.
    /// Resolves built-ins AND registered customs (Android reads its mutable EX map).
    public static func isCardioName(_ name: String) -> Bool {
        ExDataPlus.lookup(name)?.muscle == "Cardio"
    }

    /// "22min · 5.2km" for a cardio set; "1h05" past the hour; "—" when empty. Exact port of Calc.kt.
    public static func fmtCardioSet(_ mins: Int?, _ km: Double?) -> String {
        var parts: [String] = []
        if let mins, mins > 0 {
            if mins >= 60 {
                let rem = mins % 60
                parts.append("\(mins / 60)h" + (rem > 0 ? String(format: "%02d", Int32(rem)) : ""))
            } else {
                parts.append("\(mins)min")
            }
        }
        if let km, km > 0 { parts.append(trim(km) + "km") }
        return parts.isEmpty ? "—" : parts.joined(separator: " · ")
    }

    static func r1(_ x: Double) -> Double { (x * 10).rounded() / 10 }

    static func trim(_ x: Double) -> String {
        x.rounded() == x ? String(Int(x.rounded())) : String(format: "%.1f", locale: Locale(identifier: "en_US"), r1(x))
    }

    public static func fmtKg(_ kg: Double?, _ unit: String) -> String {
        guard let kg else { return "" }
        let v = unit == "lb" ? kg * LB : kg
        return trim(r1(v))
    }

    /// Display label for the weight unit, Hevy style.
    public static func unitLabel(_ unit: String) -> String { unit == "lb" ? "lbs" : "kg" }

    /// Plain number (1 decimal max) for input fields / CSV export.
    public static func trimNum(_ x: Double) -> String { trim(x) }

    /// Plate breakdown for a target total — exact port of Calc.kt `platesForSide`.
    /// Greedy per side (largest plate first); `targetKg`/`barKg` are in DISPLAY units
    /// (kg when unit == "kg", lb otherwise — bars 45/35/25 and lb plates in that mode).
    /// `barKg == 0` means "no bar": everything goes on plates. When the target is not
    /// exactly reachable, `totalKg` is the closest reachable load (bar + 2 × per side).
    public static func platesForSide(targetKg: Double, barKg: Double, unit: String) -> PlateBreakdown {
        let avail: [Double] = unit == "lb" ? [45, 35, 25, 10, 5, 2.5] : [25, 20, 15, 10, 5, 2.5, 1.25]
        var perSide = ((targetKg - barKg) / 2 * 100).rounded(.down) / 100
        if perSide < 0 { perSide = 0 }
        var out: [(weight: Double, count: Int)] = []
        for p in avail {
            let n = Int((perSide / p + 1e-9).rounded(.down))
            if n > 0 {
                out.append((weight: p, count: n))
                perSide -= Double(n) * p
            }
        }
        let perSideTotal = out.reduce(0.0) { $0 + $1.weight * Double($1.count) }
        return PlateBreakdown(perSide: out, totalKg: barKg + 2 * perSideTotal)
    }

    public static func toKg(_ text: String, _ unit: String) -> Double? {
        let cleaned = text.trimmingCharacters(in: .whitespaces).replacingOccurrences(of: ",", with: ".")
        guard let v = Double(cleaned), v > 0 else { return nil }
        return unit == "lb" ? v / LB : v
    }

    public static func fmtVol(_ kg: Double, _ unit: String) -> String {
        let v = (unit == "lb" ? kg * LB : kg).rounded()
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.locale = Locale(identifier: "fr_FR")
        f.groupingSeparator = "\u{202F}"
        return f.string(from: NSNumber(value: v)) ?? String(Int(v))
    }

    public static func toDisplay(_ kg: Double, _ unit: String) -> Double { unit == "lb" ? kg * LB : kg }

    public static func fmtDur(_ ms: Double) -> String {
        let totalMin = Int(ms / 60000)
        let h = totalMin / 60, m = totalMin % 60
        return h > 0 ? "\(h)h \(m)m" : "\(m)m"
    }

    /// Live per-second clock label: 04:37, or 1:12:45 past the hour.
    public static func fmtClock(_ ms: Double) -> String {
        let s = Int(ms / 1000)
        let h = s / 3600, m = (s % 3600) / 60, sec = s % 60
        return h > 0 ? String(format: "%d:%02d:%02d", h, m, sec) : String(format: "%02d:%02d", m, sec)
    }

    // ---- dates (local timezone, device calendar like the Android app) ----

    static var cal: Calendar {
        var c = Calendar(identifier: .gregorian)
        c.locale = Locale(identifier: "fr_FR")
        return c
    }

    static func fmtIntl(_ ms: Double, locale: String, dateStyle: DateFormatter.Style, timeStyle: DateFormatter.Style = .none) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: locale)
        f.dateStyle = dateStyle
        f.timeStyle = timeStyle
        return f.string(from: Date(timeIntervalSince1970: ms / 1000))
    }

    public static func fmtDateShort(_ ms: Double) -> String { fmtIntl(ms, locale: "fr_FR", dateStyle: .short) }
    public static func fmtDateFull(_ ms: Double) -> String { fmtIntl(ms, locale: "fr_FR", dateStyle: .full) }
    public static func fmtTime(_ ms: Double) -> String { fmtIntl(ms, locale: "fr_FR", dateStyle: .none, timeStyle: .short) }
    public static func monthLabel(_ ms: Double, locale: String = "fr_FR") -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: locale)
        f.setLocalizedDateFormatFromTemplate("MMMM yyyy")
        return f.string(from: Date(timeIntervalSince1970: ms / 1000))
    }
    public static func monthShort(_ ms: Double, locale: String = "fr_FR") -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: locale)
        f.setLocalizedDateFormatFromTemplate("MMM")
        return f.string(from: Date(timeIntervalSince1970: ms / 1000))
    }
    public static func dayName(_ ms: Double, locale: String) -> String {
        fmtIntl(ms, locale: locale, dateStyle: .full).split(separator: " ").first.map(String.init) ?? ""
    }

    public static func localDate(_ ms: Double) -> DateComponents {
        cal.dateComponents([.year, .month, .day, .weekday], from: Date(timeIntervalSince1970: ms / 1000))
    }

    public static func dayKey(_ ms: Double) -> String {
        let c = localDate(ms)
        return String(format: "%04d-%02d-%02d", c.year!, c.month!, c.day!)
    }

    public static func weekStart(_ ms: Double) -> Double {
        let d = Date(timeIntervalSince1970: ms / 1000)
        let ws = cal.dateInterval(of: .weekOfYear, for: d)?.start ?? cal.startOfDay(for: d)
        return ws.timeIntervalSince1970 * 1000
    }

    public static func streak(_ workouts: [Workout], nowMs: Double) -> Int {
        let days = Set(workouts.map { dayKey($0.startedAt) })
        var now = nowMs
        if !days.contains(dayKey(now)) { now -= 86_400_000 }
        var n = 0
        while days.contains(dayKey(now)) {
            n += 1
            now -= 86_400_000
        }
        return n
    }

    public static func totalVol(_ workouts: [Workout]) -> Double { workouts.reduce(0) { $0 + vol($1) } }

    public static func weekStats(_ workouts: [Workout], nowMs: Double) -> WeekStats {
        let ws = weekStart(nowMs)
        let inWeek = workouts.filter { $0.startedAt >= ws }
        return WeekStats(
            count: inWeek.count,
            vol: inWeek.reduce(0) { $0 + vol($1) },
            reps: inWeek.reduce(0) { $0 + reps($1) },
            prs: inWeek.reduce(0) { $0 + $1.prs.count }
        )
    }

    public static func weekly(_ workouts: [Workout], _ n: Int, _ unit: String, _ nowMs: Double) -> [(String, Double)] {
        let ws = weekStart(nowMs)
        return stride(from: n - 1, through: 0, by: -1).map { i in
            let start = ws - Double(i) * 7 * 86_400_000
            let end = start + 7 * 86_400_000
            let v = workouts.filter { $0.startedAt >= start && $0.startedAt < end }.reduce(0) { $0 + vol($1) }
            return (fmtDateShort(start), toDisplay(v, unit))
        }
    }

    public static func muscleDist(_ workouts: [Workout], _ n: Int) -> [(String, Double)] {
        var m: [String: Double] = [:]
        for w in workouts {
            for ex in w.exercises {
                for s in ex.sets where s.done && s.kg != nil && s.reps != nil {
                    m[ex.muscle, default: 0] += s.kg! * Double(s.reps!)
                }
            }
        }
        return m.sorted { $0.value > $1.value }.prefix(n).map { ($0.key, $0.value) }
    }

    public static func e1rmSeries(_ name: String, _ workouts: [Workout], maxPts: Int = 12) -> [(String, Double)] {
        var pts: [(String, Double)] = []
        for w in workouts.sorted(by: { $0.startedAt < $1.startedAt }) {
            guard let ex = w.exercises.first(where: { $0.name == name }) else { continue }
            let best = ex.sets
                .filter { ($0.kg ?? 0) > 0 && ($0.reps ?? 0) > 0 }
                .map { e1rm($0.kg!, $0.reps!) }
                .max()
            if let best { pts.append((fmtDateShort(w.startedAt), best)) }
        }
        return Array(pts.suffix(maxPts))
    }

    /// Previous performance of an exercise: per set index, "82.5 × 8" (kg units).
    public static func prevFor(_ name: String, _ workouts: [Workout]) -> [String]? {
        for w in workouts.sorted(by: { $0.startedAt > $1.startedAt }) {
            if let ex = w.exercises.first(where: { $0.name == name && $0.sets.contains { $0.kg != nil || $0.reps != nil } }) {
                return ex.sets.map { s in
                    (s.kg == nil && s.reps == nil) ? "—" : "\(fmtKg(s.kg, "kg")) × \(s.reps.map(String.init) ?? "—")"
                }
            }
        }
        return nil
    }

    /// Recompute PR cache, stamp per-set flags and per-workout PR lists (chronological).
    /// (inout: per-set PR flags and per-workout record lists are written back into the array)
    @discardableResult
    public static func rebuildPrs(_ workouts: inout [Workout]) -> [String: PrBest] {
        var cache: [String: PrBest] = [:]
        let sortedIdx = workouts.indices.sorted { workouts[$0].startedAt < workouts[$1].startedAt }
        for wi in sortedIdx {
            var prs: [PrRec] = []
            for ei in workouts[wi].exercises.indices {
                let name = workouts[wi].exercises[ei].name
                let prev = cache[name]
                let pw = prev?.weight ?? 0
                let pe = prev?.e1rm ?? 0
                for si in workouts[wi].exercises[ei].sets.indices {
                    workouts[wi].exercises[ei].sets[si].prW = false
                    workouts[wi].exercises[ei].sets[si].prE = false
                    let s = workouts[wi].exercises[ei].sets[si]
                    guard let kg = s.kg, let reps = s.reps, s.done else { continue }
                    if kg > 0 && kg > pw { workouts[wi].exercises[ei].sets[si].prW = true }
                    if kg > 0 && e1rm(kg, reps) > pe { workouts[wi].exercises[ei].sets[si].prE = true }
                }
                let done = workouts[wi].exercises[ei].sets.filter { $0.kg != nil && $0.reps != nil && $0.done && $0.kg! > 0 }
                if !done.isEmpty {
                    let bw = done.map { $0.kg! }.max()!
                    let be = done.map { e1rm($0.kg!, $0.reps!) }.max()!
                    if bw > pw { prs.append(PrRec(ex: name, kind: "Weight", value: bw)) }
                    if be > pe { prs.append(PrRec(ex: name, kind: "Est. 1RM", value: be)) }
                    let oldW = prev?.weight ?? 0
                    let oldE = prev?.e1rm ?? 0
                    cache[name] = PrBest(
                        weight: max(bw, oldW),
                        weightDate: bw > oldW ? workouts[wi].startedAt : (prev?.weightDate ?? workouts[wi].startedAt),
                        e1rm: max(be, oldE),
                        e1rmDate: be > oldE ? workouts[wi].startedAt : (prev?.e1rmDate ?? workouts[wi].startedAt)
                    )
                }
            }
            workouts[wi].prs = prs
        }
        return cache
    }

    // ---------------- legacy CSV import (Date;Heure;Exercice;Serie;KG;Reps[;Min;Km]) ----------------

    public static func parseCsv(_ content: String) -> [Workout] {
        struct Key: Hashable { let date: String; let time: String }
        var groups: [Key: [String: [(Int, Double?, Int?, Int?, Double?)]]] = [:]
        var exOrder: [Key: [String]] = [:]
        var order: [Key] = []
        let allLines: [Substring] = content.split(separator: "\n")
        for lineSub in allLines.dropFirst() {
            let line = String(lineSub)
            guard !line.trimmingCharacters(in: .whitespaces).isEmpty else { continue }
            let sep: Character = line.contains(";") ? ";" : ","
            let parts = line.split(separator: sep, omittingEmptySubsequences: false).map { $0.trimmingCharacters(in: .whitespaces) }
            guard parts.count >= 6, let si = Int(parts[3]) else { continue }
            let date = parts[0], time = parts[1], exName = parts[2]
            let kgTxt = parts[4].replacingOccurrences(of: ",", with: ".")
            let kg = kgTxt.isEmpty ? nil : Double(kgTxt)
            let reps = Int(parts[5])
            // cardio columns (v1.48 export): Min;Km — missing/blank on strength rows and legacy files
            let minsTxt = parts.count > 6 ? parts[6].replacingOccurrences(of: ",", with: ".") : ""
            let mins = minsTxt.isEmpty ? nil : Double(minsTxt).map { Int($0) }
            let kmTxt = parts.count > 7 ? parts[7].replacingOccurrences(of: ",", with: ".") : ""
            let km = kmTxt.isEmpty ? nil : Double(kmTxt)
            let key = Key(date: date, time: time)
            if groups[key] == nil { order.append(key); exOrder[key] = [] }
            if groups[key, default: [:]][exName] == nil { exOrder[key]!.append(exName) }
            groups[key, default: [:]][exName, default: []].append((si, kg, reps, mins, km))
        }
        order.sort { $0.date < $1.date }
        var workouts: [Workout] = []
        for (i, key) in order.enumerated() {
            let ms = parseFrDate(key.date) ?? Date.now.timeIntervalSince1970 * 1000 - Double(order.count - i) * 3_600_000
            let exercises = (exOrder[key] ?? []).map { frName in
                let sets = groups[key]![frName, default: []]
                let canonical = ExData.nameFr.first { $0.value == frName }?.key ?? frName
                let muscle = ExDataPlus.lookup(canonical)?.muscle ?? ""
                return ExEntry(name: canonical, muscle: muscle, sets: sets.sorted { $0.0 < $1.0 }.map { SetEntry(kg: $0.1, reps: $0.2, mins: $0.3, km: $0.4, done: true) })
            }
            workouts.append(Workout(id: 10_000_000 + i, name: "Séance", startedAt: ms, endedAt: ms + 3_600_000, exercises: exercises))
        }
        return workouts
    }

    /// Full CSV export — exact port of Util.kt `exportCsv` (l. 53-83):
    /// `Date;Heure;Exercice;<kg|lbs>;Reps;Min;Km` (';' separator), one line per set,
    /// "d MMM yyyy" French date WITH the year (parseCsv needs it), HH:mm time,
    /// exercise = FR display name (';' → ','), Min/Km columns carry the cardio values.
    public static func exportCsvText(workouts: [Workout], unit: String) -> String {
        let df = DateFormatter()
        df.locale = Locale(identifier: "fr_FR")
        df.dateFormat = "d MMM yyyy"
        var lines = ["Date;Heure;Exercice;Serie;\(unitLabel(unit));Reps;Min;Km"]
        for w in workouts.sorted(by: { $0.startedAt < $1.startedAt }) {
            let date = df.string(from: Date(timeIntervalSince1970: w.startedAt / 1000))
            let time = fmtTime(w.startedAt)
            for ex in w.exercises {
                for (i, st) in ex.sets.enumerated() {
                    lines.append(
                        "\(date);\(time);\(exName(ex.name).replacingOccurrences(of: ";", with: ","));\(i + 1);"
                            + "\(st.kg.map { fmtKg($0, unit) } ?? "");\(st.reps.map(String.init) ?? "");"
                            + "\(st.mins.map(String.init) ?? "");\(st.km.map { trimNum($0) } ?? "")"
                    )
                }
            }
        }
        return lines.joined(separator: "\n") + "\n"
    }

        /// "d MMM yyyy" with French month names ("5 sept. 2026").
    public static func parseFrDate(_ s: String) -> Double? {
        let f = DateFormatter()
        f.locale = Locale(identifier: "fr_FR")
        f.dateFormat = "d MMM yyyy"
        if let d = f.date(from: s.trimmingCharacters(in: .whitespaces)) { return d.timeIntervalSince1970 * 1000 }
        // tolerant short form without the period ("5 sept 2026")
        f.dateFormat = "d MMM yyyy"
        return nil
    }

    // ---------------- Hevy / Strong CSV import ----------------

    static let monthsEn = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    static let monthsFr = ["janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."]
    static let monthsDe = ["Jan.", "Feb.", "März", "Apr.", "Mai", "Juni", "Juli", "Aug.", "Sept.", "Okt.", "Nov.", "Dez."]
    static let monthsIt = ["gen.", "feb.", "mar.", "apr.", "mag.", "giu.", "lug.", "ago.", "set.", "ott.", "nov.", "dic."]
    static let monthsEs = ["ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sept.", "oct.", "nov.", "dic."]
    static let monthsPt = ["jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez."]
    static var monthTables: [[String]] { [monthsEn, monthsFr, monthsDe, monthsIt, monthsEs, monthsPt] }

    static func normMonthToken(_ t: String) -> String {
        t.lowercased().replacingOccurrences(of: ".", with: "")
            .folding(options: .diacriticInsensitive, locale: nil)
    }

    static func matchMonth(_ token: String, _ table: [String]) -> Int? {
        let t = normMonthToken(token)
        guard !t.isEmpty else { return nil }
        let candidates = table.enumerated().map { (normMonthToken($0.element), $0.offset) }.sorted { $0.0.count > $1.0.count }
        for (name, idx) in candidates where t.hasPrefix(name) || name.hasPrefix(t) {
            return idx
        }
        return nil
    }

    /// Hevy's own export date: "18 Sep 2026, 17:31" — month names follow the app language.
    static func parseLocalizedDate(_ s: String) -> Double? {
        var target = s
        // Japanese month form "5 9月 2026, 20:39"
        if let mr = target.range(of: #"(\d{1,2})\s*月"#, options: .regularExpression) {
            let digits = target[mr].filter(\.isNumber)
            if let m = Int(digits), (1...12).contains(m) {
                target = target.replacingCharacters(in: mr, with: monthsEn[m - 1])
            }
        }
        guard let m = target.range(of: #"^(\d{1,2})\s+([^\s\d]+)\s+(\d{4})[,\s]+(\d{1,2}):(\d{2})$"#, options: .regularExpression) else { return nil }
        let parts = target[m] // re-extract via match groups below
        _ = parts
        let comps = target[m].split(whereSeparator: { $0.isWhitespace || $0 == "," })
        guard comps.count == 4, let day = Int(comps[0]), let year = Int(comps[2]) else { return nil }
        let timeParts = comps[3].split(separator: ":")
        guard timeParts.count == 2, let hour = Int(timeParts[0]), let minute = Int(timeParts[1]) else { return nil }
        for table in monthTables {
            if let mo = matchMonth(String(comps[1]), table) {
                var c = DateComponents()
                c.year = year; c.month = mo + 1; c.day = day; c.hour = hour; c.minute = minute
                if let d = cal.date(from: c) { return d.timeIntervalSince1970 * 1000 }
            }
        }
        return nil
    }

    /// Parse a date as emitted by Hevy/Strong exports.
    public static func parseExportDate(_ raw: String) -> Double? {
        let s = raw.trimmingCharacters(in: .whitespaces).trimmingCharacters(in: CharacterSet(charactersIn: "\""))
        if s.isEmpty { return nil }
        if let n = Double(s), s.allSatisfy({ $0.isNumber }) {
            return n > 10_000_000_000 ? n : n * 1000
        }
        let iso = s.replacingOccurrences(of: " ", with: "T")
        if let t = isoDate(iso) { return t }
        if let t = isoDate(iso.endsWith("Z") ? iso : iso + "Z") { return t }
        // local datetime without zone
        if let mr = iso.range(of: #"^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$"#, options: .regularExpression) {
            let cstr = String(iso[mr])
            let p = cstr.split(whereSeparator: { "-T:".contains($0) }).map(String.init)
            if p.count >= 5, let y = Int(p[0]), let mo = Int(p[1]), let d = Int(p[2]), let h = Int(p[3]), let mi = Int(p[4]) {
                var c = DateComponents()
                c.year = y; c.month = mo; c.day = d; c.hour = h; c.minute = mi; c.second = p.count > 5 ? Int(p[5]) ?? 0 : 0
                if let date = cal.date(from: c) { return date.timeIntervalSince1970 * 1000 }
            }
        }
        if let t = parseLocalizedDate(s) { return t }
        // Strong-style: "Mon Jan 02 17:00:00 GMT+01:00 2023"
        if let mr = s.range(of: #"^[A-Za-z]{3}\s+([A-Za-z]{3})\s+(\d{2})\s+(\d{2}):(\d{2}):(\d{2})\s+GMT([+-]\d{2}):?(\d{2})\s+(\d{4})$"#, options: .regularExpression) {
            let matched = String(s[mr])
            let p = matched.split(separator: " ").map(String.init)
            guard p.count == 6 else { return nil }
            if let mo = monthsEn.firstIndex(where: { $0.lowercased() == p[1].lowercased() }),
               let day = Int(p[2]), let year = Int(p[5]) {
                let tp = p[3].split(separator: ":").map(String.init)
                let offStr = p[4].replacingOccurrences(of: "GMT", with: "")
                let offParts = offStr.split(separator: ":").map(String.init)
                guard tp.count == 3, offParts.count == 2,
                      let h = Int(tp[0]), let mi = Int(tp[1]), let se = Int(tp[2]),
                      let oh = Int(offParts[0]), let om = Int(offParts[1]) else { return nil }
                var c = DateComponents()
                c.year = year; c.month = mo + 1; c.day = day
                c.hour = h; c.minute = mi; c.second = se
                c.timeZone = TimeZone(secondsFromGMT: oh * 3600 + (oh < 0 ? -om : om) * 60)
                if let d = cal.date(from: c) { return d.timeIntervalSince1970 * 1000 }
            }
        }
        return nil
    }

    static func isoDate(_ s: String) -> Double? {
        ISO8601DateFormatter().date(from: s).map { $0.timeIntervalSince1970 * 1000 }
    }

    /// Full-file CSV reader (quotes with embedded commas/newlines honored).
    public static func readCsvTable(_ content: String) -> [[String]] {
        var rows: [[String]] = []
        var cur: [String] = []
        var cell = ""
        var inQ = false
        for c in content {
            if c == "\"" {
                inQ.toggle()
            } else if !inQ && (c == "," || c == ";") {
                cur.append(cell.trimmingCharacters(in: .whitespaces)); cell = ""
            } else if !inQ && c == "\n" {
                cur.append(cell.trimmingCharacters(in: .whitespaces)); cell = ""
                rows.append(cur); cur = []
            } else if !inQ && c == "\r" {
                // skip
            } else {
                cell.append(c)
            }
        }
        if !cur.isEmpty {
            cur.append(cell.trimmingCharacters(in: .whitespaces))
            rows.append(cur)
        }
        return rows.filter { !$0.allSatisfy { $0.isEmpty } }
    }

    static func csvNorm(_ s: String) -> String {
        s.lowercased().replacingOccurrences(of: "_", with: "").replacingOccurrences(of: " ", with: "").replacingOccurrences(of: ".", with: "")
    }

    struct HevyCols {
        var date: Int?
        var endDate: Int?
        var exercise: Int
        var weight: Int?
        var reps: Int?
        var order: Int?
        var seconds: Int?
        var done: Int?
        var name: Int?
        var unit: Int?
        var notes: Int?
        var superset: Int?
    }

    /// Fuzzy column mapping — Strong-style headers as well as snake_case ones.
    static func mapColumns(_ header: [String]) -> HevyCols? {
        let cols = header.map(csvNorm)
        func findFirst(_ keys: [String], _ not: [String] = []) -> Int? {
            let i = cols.firstIndex { c in keys.contains { c.contains($0) } && !not.contains { c.contains($0) } }
            return i
        }
        guard let exercise = findFirst(["exercisename", "exercise"]) else { return nil }
        return HevyCols(
            date: findFirst(["startdate", "start", "date"], ["end", "duration"]),
            endDate: findFirst(["enddate", "endtime"]),
            exercise: exercise,
            weight: findFirst(["weightkg", "weightlbs", "weight"], ["unit"]),
            reps: findFirst(["reps", "repetitions"]),
            order: findFirst(["setorder", "setindex", "setno"]),
            seconds: findFirst(["seconds", "durations", "durationsec"]),
            done: findFirst(["completed", "checked"]),
            name: findFirst(["workoutname", "title", "name"], ["exercise"]),
            unit: findFirst(["weightunit", "unit"]),
            notes: findFirst(["exnotes", "exercisenotes", "notes"], ["workout"]),
            superset: findFirst(["superset"])
        )
    }

    /// Map a Hevy/Strong exercise label to our canonical EN key.
    public static func canonicalExercise(_ label: String) -> String {
        let t = label.trimmingCharacters(in: .whitespaces)
        if ExData.byName[t] != nil { return t }
        let clean = csvNorm(t).replacingOccurrences(of: "(", with: "").replacingOccurrences(of: ")", with: "")
        for (en, fr) in ExData.nameFr where csvNorm(fr).replacingOccurrences(of: "(", with: "").replacingOccurrences(of: ")", with: "") == clean {
            return en
        }
        for (en, fr) in ExData.nameFr
        where clean.count >= 5 && csvNorm(fr).replacingOccurrences(of: "(", with: "").replacingOccurrences(of: ")", with: "").hasPrefix(clean) {
            return en
        }
        return t
    }

    struct HRow {
        var date: Double?
        var endDate: Double?
        var ex: String
        var kg: Double?
        var reps: Int?
        var order: Int
        var done: Bool
        var workoutName: String?
        var notes: String
        var superset: Bool
    }

    /// Parse a Hevy (or Strong) export CSV. Rows are one per set; files without a date column are routine templates.
    public static func parseHevyCsv(_ content: String) -> ([Workout], [Routine]) {
        let table = readCsvTable(content)
        guard table.count >= 2, let cols = mapColumns(table[0]) else { return ([], []) }
        var rows: [HRow] = []
        for i in 1..<table.count {
            let p = table[i]
            guard p.count > cols.exercise else { continue }
            let exRaw = p[cols.exercise]
            guard !exRaw.isEmpty else { continue }
            let unitLb = cols.unit.map { (p.indices.contains($0) ? p[$0] : "").lowercased().contains("lb") } ?? false
            var kg: Double?
            if let j = cols.weight {
                let raw = p.indices.contains(j) ? p[j].replacingOccurrences(of: ",", with: ".") : ""
                if let v = Double(raw) { kg = unitLb ? v / LB : v }
            }
            let dateStr = cols.date.map { p.indices.contains($0) ? p[$0] : "" } ?? ""
            let date = parseExportDate(dateStr)
            let doneRaw = cols.done.map { p.indices.contains($0) ? p[$0].lowercased() : "true" } ?? "true"
            let supCell = cols.superset.map { p.indices.contains($0) ? p[$0].trimmingCharacters(in: .whitespaces) : "" } ?? ""
            let sup = !supCell.isEmpty && supCell != "0"
            let repsF = cols.reps.map { p.indices.contains($0) ? Double(p[$0]) : nil } ?? nil
            rows.append(HRow(
                date: date,
                endDate: cols.endDate.flatMap { j in p.indices.contains(j) ? parseExportDate(p[j]) : nil } ?? date,
                ex: exRaw,
                kg: kg,
                reps: repsF.map { Int(truncating: $0 as NSNumber) },
                order: cols.order.flatMap { j in p.indices.contains(j) ? Int(p[j]) : nil } ?? rows.count,
                done: !["false", "0", "no", "warmup"].contains(doneRaw),
                workoutName: cols.name.flatMap { j in p.indices.contains(j) && !p[j].isEmpty ? p[j] : nil },
                notes: cols.notes.map { j in p.indices.contains(j) ? p[j] : "" } ?? "",
                superset: sup
            ))
        }
        guard !rows.isEmpty else { return ([], []) }

        // No date column anywhere → template/routine file
        if rows.allSatisfy({ $0.date == nil }) {
            var exs: [String: [SetEntry]] = [:]
            var exOrder: [String] = []
            var sups: Set<String> = []
            for r in rows {
                if exs[r.ex] == nil { exOrder.append(r.ex) }
                exs[r.ex, default: []].append(SetEntry(kg: r.kg, reps: r.reps, done: true))
                if r.superset { sups.insert(r.ex) }
            }
            let routine = Routine(
                id: 0,
                name: rows.first?.workoutName ?? "Routine Hevy",
                exercises: exOrder.map { label in
                    let canon = canonicalExercise(label)
                    return ExEntry(name: canon, muscle: ExDataPlus.lookup(canon)?.muscle ?? "", superset: sups.contains(label), sets: exs[label]!)
                }
            )
            return ([], [routine])
        }

        // Workouts: group by consecutive rows sharing the same date
        var workouts: [Workout] = []
        var i = 0
        var id = 10_000_000
        while i < rows.count {
            guard let key = rows[i].date else { i += 1; continue }
            var j = i
            var exs: [String: [SetEntry]] = [:]
            var exOrder: [String] = []
            var names: [String: String] = [:]
            var sups: Set<String> = []
            while j < rows.count, rows[j].date == key {
                let row = rows[j]
                if exs[row.ex] == nil { exOrder.append(row.ex) }
                exs[row.ex, default: []].append(SetEntry(kg: row.kg, reps: row.reps, done: row.done))
                if let n = row.workoutName { names[row.ex] = n }
                if row.superset { sups.insert(row.ex) }
                j += 1
            }
            let end = rows.first { $0.date == key }?.endDate ?? key
            workouts.append(Workout(
                id: id,
                name: names.values.first ?? "Séance Hevy",
                startedAt: key,
                endedAt: max(end, key + 60_000),
                exercises: exOrder.map { label in
                    let canon = canonicalExercise(label)
                    return ExEntry(name: canon, muscle: ExDataPlus.lookup(canon)?.muscle ?? "", superset: sups.contains(label), sets: exs[label]!)
                }
            ))
            id += 1
            i = j
        }
        workouts.sort { $0.startedAt < $1.startedAt }
        return (workouts, [])
    }

    /// Hevy never exports templates — rebuild routines from imported workout names.
    public static func routinesFromWorkouts(_ workouts: [Workout], existingRoutineNames: Set<String>) -> [Routine] {
        var taken = Set(existingRoutineNames.map { $0.lowercased() })
        var out: [Routine] = []
        var id = 10_000_000
        var groups: [String: [Workout]] = [:]
        for w in workouts {
            groups[w.name.trimmingCharacters(in: .whitespaces), default: []].append(w)
        }
        for (name, group) in groups {
            guard !name.isEmpty, !taken.contains(name.lowercased()) else { continue }
            let latest = group.max { $0.startedAt < $1.startedAt }!
            out.append(Routine(
                id: id,
                name: name,
                exercises: latest.exercises.map { ex in
                    ExEntry(name: ex.name, muscle: ex.muscle, superset: ex.superset, restSec: ex.restSec, sets: ex.sets.map { SetEntry(kg: $0.kg, reps: $0.reps, mins: $0.mins, km: $0.km, done: $0.done) })
                }
            ))
            id += 1
            taken.insert(name.lowercased())
        }
        return out
    }

    // ---------------- deterministic demo seed ----------------

    struct SeedRow {
        let name: String
        let nSets: Int
        let reps: Int
        let base: Double
        let inc: Double
    }

    public static func seed(nowMs: Double) -> ([Workout], [Routine]) {
        var s: UInt64 = 987_654_321
        func rnd() -> Double {
            s = (s &* 1_103_515_245 &+ 12_345) % 2_147_483_648
            return Double(s) / 2_147_483_648.0
        }
        let T: [String: [SeedRow]] = [
            "push": [
                SeedRow(name: "Barbell Bench Press", nSets: 4, reps: 8, base: 70.0, inc: 1.1),
                SeedRow(name: "Incline Dumbbell Bench Press", nSets: 3, reps: 10, base: 24.0, inc: 0.35),
                SeedRow(name: "Machine Chest Press", nSets: 3, reps: 12, base: 55.0, inc: 0.8),
                SeedRow(name: "Lateral Raise", nSets: 3, reps: 15, base: 9.0, inc: 0.2),
                SeedRow(name: "Tricep Pushdown", nSets: 3, reps: 12, base: 27.0, inc: 0.4),
                SeedRow(name: "Overhead Cable Extension", nSets: 3, reps: 11, base: 22.0, inc: 0.35),
            ],
            "pull": [
                SeedRow(name: "Lat Pulldown", nSets: 4, reps: 10, base: 62.0, inc: 0.8),
                SeedRow(name: "Seated Cable Row", nSets: 3, reps: 10, base: 57.0, inc: 0.8),
                SeedRow(name: "Dumbbell Row", nSets: 3, reps: 10, base: 32.0, inc: 0.4),
                SeedRow(name: "Face Pull", nSets: 3, reps: 15, base: 22.0, inc: 0.25),
                SeedRow(name: "Barbell Curl", nSets: 3, reps: 10, base: 32.0, inc: 0.3),
                SeedRow(name: "Hammer Curl", nSets: 3, reps: 12, base: 15.0, inc: 0.25),
            ],
            "legs": [
                SeedRow(name: "Barbell Squat", nSets: 4, reps: 8, base: 97.0, inc: 1.4),
                SeedRow(name: "Romanian Deadlift", nSets: 3, reps: 10, base: 92.0, inc: 1.0),
                SeedRow(name: "Leg Press", nSets: 3, reps: 12, base: 185.0, inc: 2.2),
                SeedRow(name: "Lying Leg Curl", nSets: 3, reps: 12, base: 46.0, inc: 0.4),
                SeedRow(name: "Standing Calf Raise", nSets: 4, reps: 15, base: 85.0, inc: 0.9),
                SeedRow(name: "Cable Crunch", nSets: 3, reps: 15, base: 31.0, inc: 0.4),
            ],
        ]
        let names = ["push": "Push Day", "pull": "Pull Day", "legs": "Leg Day"]
        let cycle = ["push", "pull", "rest", "legs", "rest", "push", "pull", "rest", "legs", "rest"]

        var workouts: [Workout] = []
        var wid = 1
        for back in stride(from: 83, through: 0, by: -1) {
            let kind: String = back < 5 ? ["push", "pull", "legs", "push", "pull"][4 - back] : cycle[(83 - back) % cycle.count]
            if kind == "rest" { continue }
            if back > 6 && rnd() < 0.10 { continue }
            var start: Double
            if back == 0 {
                start = nowMs - ((110 + rnd() * 30) * 60000).rounded()
            } else {
                var c = DateComponents()
                c.day = -back
                c.hour = 17 + Int(rnd() * 3)
                c.minute = Int(rnd() * 60)
                let day = cal.date(byAdding: c, to: Date(timeIntervalSince1970: nowMs / 1000)) ?? Date()
                let comps = cal.dateComponents([.year, .month, .day], from: day)
                var c2 = DateComponents()
                c2.year = comps.year; c2.month = comps.month; c2.day = comps.day; c2.hour = c.hour; c2.minute = c.minute
                start = (cal.date(from: c2) ?? day).timeIntervalSince1970 * 1000
            }
            var end = start + ((55 + rnd() * 40) * 60000).rounded()
            if end > nowMs - 30000 { end = nowMs - 30000 }
            if end <= start { continue }
            let weekIdx = (83 - back) / 7
            let exs = T[kind]!.map { row -> ExEntry in
                let kg = round125(row.base + row.inc * Double(weekIdx))
                var sets: [SetEntry] = []
                for _ in 0..<row.nSets {
                    let rv = rnd() < 0.22 ? row.reps - 1 : (rnd() > 0.9 ? row.reps + 1 : row.reps)
                    sets.append(SetEntry(kg: kg, reps: max(4, rv), done: true))
                }
                return ExEntry(name: row.name, muscle: ExData.byName[row.name]?.muscle ?? "Quads", sets: sets)
            }
            workouts.append(Workout(id: wid, name: names[kind]!, startedAt: start, endedAt: end, exercises: exs))
            wid += 1
        }

        func routineRows(_ rows: [SeedRow]) -> [ExEntry] {
            rows.map { r in
                ExEntry(name: r.name, muscle: ExData.byName[r.name]?.muscle ?? "", sets: (0..<r.nSets).map { _ in SetEntry(kg: round125(r.base), reps: r.reps, done: true) })
            }
        }
        var rid = 1
        let routines = [
            Routine(id: rid, name: "Push Day", exercises: routineRows(T["push"]!)),
            Routine(id: rid + 1, name: "Pull Day", exercises: routineRows(T["pull"]!)),
            Routine(id: rid + 2, name: "Leg Day", exercises: routineRows(T["legs"]!)),
            Routine(id: rid + 3, name: "Upper Body", exercises: [
                ExEntry(name: "Barbell Bench Press", muscle: "Chest", sets: (0..<4).map { _ in SetEntry(kg: 72.5, reps: 8) }),
                ExEntry(name: "Lat Pulldown", muscle: "Lats", sets: (0..<4).map { _ in SetEntry(kg: 62.0, reps: 10) }),
                ExEntry(name: "Seated Cable Row", muscle: "Lats", sets: (0..<3).map { _ in SetEntry(kg: 57.0, reps: 10) }),
                ExEntry(name: "Machine Shoulder Press", muscle: "Shoulders", sets: (0..<3).map { _ in SetEntry(kg: 40.0, reps: 10) }),
                ExEntry(name: "Barbell Curl", muscle: "Biceps", sets: (0..<3).map { _ in SetEntry(kg: 32.5, reps: 10) }),
                ExEntry(name: "Tricep Pushdown", muscle: "Triceps", sets: (0..<3).map { _ in SetEntry(kg: 27.5, reps: 12) }),
            ]),
        ]
        return (workouts, routines)
    }
}

/// Result of `Calc.platesForSide`: per-side (plate weight, count) pairs largest-first
/// (empty when the bar alone reaches the target) plus the closest reachable total.
public struct PlateBreakdown: Sendable {
    public let perSide: [(weight: Double, count: Int)]
    public let totalKg: Double
    public init(perSide: [(weight: Double, count: Int)] = [], totalKg: Double = 0) {
        self.perSide = perSide
        self.totalKg = totalKg
    }
}

extension Dictionary {
    subscript(cannonicalSafe key: Key) -> Value? {
        self[key]
    }
}

extension String {
    func endsWith(_ s: String) -> Bool { hasSuffix(s) }
}
