/** Content localization + fuzzy search + archetypes — port of L10nData.kt logic. */
import Foundation

/// UI language (injected by the app from the device; tests force it).
public enum L10n {
    nonisolated(unsafe) public static var lang: String = "fr"

    public static func s(_ en: String, _ fr: String, _ es: String? = nil, _ de: String? = nil) -> String {
        switch lang {
        case "fr": return fr
        case "es": return es ?? en
        case "de": return de ?? en
        default: return en
        }
    }

    public static func seriesLabel(_ n: Int, _ name: String) -> String {
        if lang == "fr" { return n > 1 ? "\(n) séries \(name)" : "\(n) série \(name)" }
        if lang == "de" { return "\(n) Sätze \(name)" }
        return "\(n) series \(name)"
    }

    public static func seeMoreExercises(_ n: Int) -> String {
        switch lang {
        case "fr": return n > 1 ? "Voir \(n) exercices en plus" : "Voir 1 exercice en plus"
        case "es": return "Ver \(n) ejercicios más"
        case "de": return "\(n) weitere Übungen anzeigen"
        default: return n > 1 ? "See \(n) more exercises" : "See 1 more exercise"
        }
    }

    public static func relativeTime(_ ms: Double, nowMs: Double) -> String {
        let diff = nowMs - ms
        let hours = Int(diff / 3_600_000)
        if lang != "fr" {
            if hours < 1 { return "\(max(1, Int(diff / 60000))) min ago" }
            if hours < 24 { return "\(hours) h ago" }
            let days = hours / 24
            return days == 1 ? "1 day ago" : "\(days) days ago"
        }
        if hours < 1 { return "il y a \(max(1, Int(diff / 60000))) min" }
        if hours < 24 { return "il y a \(hours) h" }
        let days = hours / 24
        return days == 1 ? "il y a 1 jour" : "il y a \(days) jours"
    }
}

// Display names
public func exName(_ name: String) -> String { L10n.lang == "fr" ? (ExData.nameFr[name] ?? name) : name }
public func muscleName(_ m: String) -> String { L10n.lang == "fr" ? (ExData.muscleFr[m] ?? m) : m }
public func equipName(_ e: String) -> String { L10n.lang == "fr" ? (ExData.equipFr[e] ?? e) : e }

public func cuesFor(_ muscle: String) -> [String] {
    L10n.lang == "fr" ? (ExData.cuesFr[muscle] ?? ExData.cuesEn[muscle] ?? []) : (ExData.cuesEn[muscle] ?? [])
}

/// Step-by-step instructions for an exercise, localized.
public func stepsFor(_ nameEn: String) -> [String] {
    let arch = archetypeOf(nameEn)
    return L10n.lang == "fr" ? (ExData.archStepsFr[arch] ?? ExData.archStepsEn[arch] ?? []) : (ExData.archStepsEn[arch] ?? [])
}

public func equipHintFor(_ e: String) -> String {
    guard L10n.lang == "fr" else { return ExData.equipHintEn[e] ?? "" }
    switch e {
    case "Barbell": return "Avec une barre chargée de disques."
    case "Dumbbell": return "Avec des haltères — un dans chaque main sauf indication."
    case "Machine": return "Sur une machine à pile ou à charges guidées."
    case "Cable": return "À la station de poulies avec accessoires."
    case "Bodyweight": return "Sans matériel — le corps sert de résistance."
    default: return "Avec du matériel spécifique : banc, élastique, kettlebell ou roue."
    }
}

/// Map an exercise (canonical EN name) to its instruction archetype — port of archetypeOf() (order critical).
public func archetypeOf(_ name: String) -> String {
    let n = name.lowercased()
    let muscle = ExData.byName[name]?.muscle ?? ""
    func has(_ keys: [String]) -> Bool { keys.contains { n.contains($0) } }
    if n.contains("walking lunge") { return "lunge" }
    if has(["burpee", "jumping jack", "high knee", "butt kick", "jump rope", "rowing machine",
            "treadmill", "cycling", "elliptical", "stair climber", "swimming", "ski erg",
            "jacobs ladder", "assault bike", "versa climber", "sled push", "sled drag",
            "battle rope", "shadow box", "speed skater", "lateral bound"]) { return "conditioning" }
    if n.contains("walking") || n.contains("running") { return "conditioning" }
    if has(["power clean", "hang clean", "squat clean", "clean pull", "clean and",
            "power snatch", "hang snatch", "snatch pull", "snatch balance", "kettlebell snatch"]) { return "olympic" }
    if n.contains("jerk") { return "jerk" }
    if n.contains("thruster") { return "thruster" }
    if has(["box jump", "broad jump", "depth jump", "frog jump"]) { return "plyo_jump" }
    if n.contains("get up") { return "getup" }
    if n.contains("pallof") { return "pallof" }
    if n.contains("bird dog") { return "bird_dog" }
    if has(["bear crawl", "crab walk"]) { return "crawl" }
    if has(["superman", "snow angel"]) { return "superman_hold" }
    if n.contains("pull apart") { return "pull_apart" }
    if n.contains("wood chop") { return "wood_chop" }
    if has(["scaption", "y raise", "w raise"]) { return "scaption" }
    if n.contains("handstand") { return "ohp" }
    if n.contains("hip extension") { return "back_ext" }
    if n == "reverse nordic" { return "squat" }
    if n.contains("incline") && has(["press", "bench"]) { return "incline_bench" }
    if n.contains("decline") && has(["press", "bench"]) { return "decline_bench" }
    if (n.contains("bench press") && !n.contains("close grip")) || n == "machine chest press"
        || n.contains("floor press") || n == "svend press" || n.contains("guillotine") { return "bench" }
    if has(["fly", "crossover", "pec deck", "svend"]) { return "fly" }
    if has(["push up", "pushup"]) { return "pushup" }
    if n.contains("dip") && muscle == "Chest" { return "dip" }
    if has(["shrug", "rack pull", "block pull"]) { return "shrug" }
    if n.contains("lateral raise") { return "lateral_raise" }
    if n.contains("front raise") { return "front_raise" }
    if has(["rear delt", "face pull", "reverse pec deck"]) { return "rear_delt" }
    if has(["upright row", "high pull"]) { return "upright_row" }
    if n.contains("press") && muscle == "Shoulders" { return "ohp" }
    if has(["wrist", "pinch", "rice bucket", "towel wring", "figure 8", "hex dumbbell"]) { return "wrist" }
    if n.contains("curl") { return "curl" }
    if has(["pushdown", "kickback"]) && muscle == "Triceps" { return "triceps_pushdown" }
    if n.contains("extension") && muscle == "Triceps" { return "triceps_overhead" }
    if has(["skullcrusher", "tate", "jm press"]) { return "skullcrusher" }
    if n.contains("close grip bench") { return "skullcrusher" }
    if n.contains("dip") { return "dip" }
    if n.contains("straight arm") { return "straight_arm" }
    if n.contains("pulldown") { return "pulldown" }
    if has(["pull up", "pullup", "chin up", "muscle up", "scap pull"]) { return "pull_up" }
    if n.contains("pullover") { return "pullover" }
    if n.contains("inverted row") { return "pull_up" }
    if has(["cable row", "supported row", "machine row", "t-bar", "incline dumbbell row", "seal row", "meadow row"]) { return "row_supported" }
    if n.contains("row") { return "row_bent" }
    if n.contains("deadlift") && has(["romanian", "single leg"]) { return "rdl" }
    if n.contains("deadlift") { return "deadlift" }
    if n.contains("good morning") { return "good_morning" }
    if (n.contains("extension") && muscle == "Lower back") || n.contains("hyperextension") { return "back_ext" }
    if n.contains("swing") { return "swing" }
    if has(["hip thrust", "glute bridge", "frog pump", "pull through", "kickback"]) { return "hip_thrust" }
    if has(["leg curl", "nordic", "glute ham"]) { return "leg_curl" }
    if has(["calf", "tibialis", "on toes"]) { return "calf" }
    if n.contains("squat") { return "squat" }
    if has(["lunge", "split squat", "step up", "curtsy", "step down"]) { return "lunge" }
    if n.contains("leg press") { return "leg_press" }
    if has(["leg extension", "knee extension"]) { return "leg_ext" }
    if has(["abduction", "clamshell", "fire hydrant", "lateral walk", "monster walk"]) { return "abduction" }
    if n.contains("adduction") { return "adduction" }
    if has(["plank", "hollow", "dead bug", "wall sit", "mountain", "body saw", "arch hold", "v-sit", "l-sit", "l-hang", "vacuum"]) { return "plank" }
    if n.contains("sit up") { return "sit_up" }
    if has(["crunch", "cocoon", "ball pass"]) { return "crunch" }
    if has(["twist", "windshield"]) { return "twist" }
    if has(["leg raise", "knee raise", "v-up", "flutter", "scissor", "dragon flag", "toe to bar", "captain", "jackknife", "leg lower"]) { return "leg_raise" }
    if has(["rollout", "fallout"]) { return "rollout" }
    if n.contains("side bend") { return "twist" }
    if has(["carry", "dead hang"]) { return "carry" }
    switch muscle {
    case "Cardio": return "conditioning"
    case "Abs": return "crunch"
    default: return "bench"
    }
}

// ---------- fuzzy search (accent-insensitive, multi-token, FR + EN + muscle + aliases) ----------

public func normalize(_ s: String) -> String {
    s.lowercased()
        .folding(options: .diacriticInsensitive, locale: nil)
        .replacingOccurrences(of: "'", with: " ")
}

/// Levenshtein edit distance (typo tolerance).
public func lev(_ a: String, _ b: String) -> Int {
    if a == b { return 0 }
    if a.isEmpty { return b.count }
    if b.isEmpty { return a.count }
    var prev = Array(0...b.count)
    var cur = [Int](repeating: 0, count: b.count + 1)
    for i in 1...a.count {
        cur[0] = i
        let ac = a[a.index(a.startIndex, offsetBy: i - 1)]
        for j in 1...b.count {
            let bc = b[b.index(b.startIndex, offsetBy: j - 1)]
            let cost = ac == bc ? 0 : 1
            cur[j] = min(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
        }
        swap(&prev, &cur)
    }
    return prev[b.count]
}

let noiseTokens: Set<String> = [
    "en", "de", "du", "la", "le", "les", "un", "une", "des", "a", "au", "aux",
    "et", "ou", "avec", "sur", "pour", "dans", "par", "pose", "prise", "grip", "the", "and",
]

/// Light stemming variants tried for a token: raw, -ing, -es, -s.
func stems(_ token: String) -> [String] {
    var out = [token]
    if token.count >= 5 && token.hasSuffix("ing") { out.append(String(token.dropLast(3))) }
    if token.count >= 4 && token.hasSuffix("es") { out.append(String(token.dropLast(2))) }
    if token.count >= 4 && token.hasSuffix("s") { out.append(String(token.dropLast(1))) }
    return out
}

func tokenMatches(_ token: String, _ words: [String], allowTypo: Bool) -> Bool {
    let variants = stems(token)
    if words.contains(where: { w in variants.contains(where: { v in w.contains(v) }) }) { return true }
    if !allowTypo { return false }
    let maxDist = token.count >= 5 ? 2 : (token.count == 4 ? 1 : 0)
    if maxDist == 0 { return false }
    return words.contains { w in
        variants.contains { v in
            abs(w.count - v.count) <= maxDist && lev(v, w) <= maxDist
        }
    }
}

var nameWordsCache: [String: [String]] = [:]
var attrWordsCache: [String: [String]] = [:]

/// True if every word of the query appears (or nearly, typos allowed on the name) in the exercise name FR/EN, muscle or equipment.
public func matches(_ q: String, _ nameEn: String) -> Bool {
    let nq = normalize(q).trimmingCharacters(in: .whitespaces)
    if nq.isEmpty { return true }
    let def = ExData.byName[nameEn]
    let nameWords: [String]
    if let c = nameWordsCache[nameEn] {
        nameWords = c
    } else {
        nameWords = "\(normalize(nameEn)) \(normalize(ExData.nameFr[nameEn] ?? "")) \(normalize(ExData.aliasFr[nameEn] ?? ""))"
            .split(separator: " ").map(String.init).filter { !$0.isEmpty }
        nameWordsCache[nameEn] = nameWords
    }
    let attrWords: [String]
    if let c = attrWordsCache[nameEn] {
        attrWords = c
    } else {
        let m = def?.muscle ?? ""
        let e = def?.equip ?? ""
        attrWords = "\(normalize(m)) \(normalize(ExData.muscleFr[m] ?? "")) \(normalize(ExData.muscleExtra[m] ?? "")) \(normalize(e)) \(normalize(ExData.equipFr[e] ?? ""))"
            .split(separator: " ").map(String.init).filter { !$0.isEmpty }
        attrWordsCache[nameEn] = attrWords
    }
    return nq.split(separator: " ")
        .map { $0.trimmingCharacters(in: .whitespaces) }
        .filter { !($0.isEmpty || noiseTokens.contains($0)) }
        .allSatisfy { tokenMatches($0, nameWords, allowTypo: true) || tokenMatches($0, attrWords, allowTypo: false) }
}
