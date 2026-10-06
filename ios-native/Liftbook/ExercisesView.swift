import SwiftUI
import Foundation
import LiftbookCore
import Charts

/** Exercises list + detail (Résumé / Historique / Instructions) — port of Exercises.kt. */
struct ExercisesView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    @State private var q = ""
    @State private var mus = "All"

    var filtered: [ExerciseDef] {
        ExData.all
            .filter { (mus == "All" || $0.muscle == mus) && matches(q, $0.name) }
            .sorted { exName($0.name) < exName($1.name) }
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button { nav.pop() } label: {
                    Image(systemName: "chevron.left").font(.system(size: 20)).foregroundColor(C.text).padding(8)
                }
                .buttonStyle(.plain)
                Txt(LS("Exercises", "Exercices"), weight: 800, size: 22)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 8)
            .padding(.top, 12)
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass").font(.system(size: 15)).foregroundColor(C.mut)
                TextField(LS("Search exercises", "Rechercher des exercices"), text: $q)
                    .font(.inter(400, 15))
                    .foregroundColor(C.text)
                    .tint(C.accent)
                    .autocorrectionDisabled()
            }
            .padding(.horizontal, 12)
            .frame(height: 52)
            .background(C.card, in: RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 16)
            .padding(.vertical, 2)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(["All"] + ExData.muscles, id: \.self) { m in
                        Chip(label: m == "All" ? LS("All", "Tous") : muscleName(m), selected: mus == m) { mus = m }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
            }
            ScrollView {
                LazyVStack(spacing: 0, pinnedViews: []) {
                    if filtered.isEmpty {
                        EmptyState(text: LS("No exercises found.", "Aucun exercice trouvé."))
                    } else if q.trimmingCharacters(in: .whitespaces).isEmpty && mus == "All" {
                        let grouped = Dictionary(grouping: filtered, by: { $0.muscle })
                        ForEach(ExData.muscles, id: \.self) { m in
                            if let exs = grouped[m] {
                                Txt(muscleName(m).uppercased(), weight: 700, size: 12, color: C.mut)
                                    .padding(.horizontal, 16)
                                    .padding(.top, 16)
                                    .padding(.bottom, 6)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                ForEach(exs, id: \.name) { e in
                                    row(e)
                                }
                            }
                        }
                    } else {
                        ForEach(filtered, id: \.name) { e in
                            row(e)
                        }
                    }
                    Spacer().frame(height: 12)
                }
            }
        }
        .background(C.bg)
    }

    func row(_ e: ExerciseDef) -> some View {
        Button { nav.push(.exerciseDetail(e.name)) } label: {
            HStack(spacing: 4) {
                VStack(alignment: .leading, spacing: 1) {
                    Txt(exName(e.name), weight: 600, size: 14.5).lineLimit(1)
                    Txt("\(muscleName(e.muscle)) · \(equipName(e.equip))", size: 12, color: C.mut)
                }
                Spacer()
                Image(systemName: "chevron.right").font(.system(size: 13)).foregroundColor(C.mut)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 13)
        }
        .buttonStyle(.plain)
    }
}

struct ExerciseDetailView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    let name: String
    @State private var tab = 0
    @State private var period = 3
    @State private var playing = true

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button { nav.pop() } label: {
                    Image(systemName: "chevron.left").font(.system(size: 20)).foregroundColor(C.text).padding(8)
                }
                .buttonStyle(.plain)
                Txt(exName(name), weight: 700, size: 17).lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
            tabsHeader
            ScrollView {
                if tab == 0 { summaryTab } else if tab == 1 { historyTab } else { instructionsTab }
                Spacer().frame(height: 20)
            }
            if repo.draft?.mode == "workout" {
                Button {
                    repo.addExToDraft(name)
                    nav.pop()
                    repo.toast("\(exName(name)) ajouté")
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "plus").font(.system(size: 15))
                        Txt(LS("Add to Current Workout", "Ajouter à la séance en cours"), weight: 700, size: 15, color: C.accText)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(C.accent, in: Capsule())
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
        }
        .background(C.bg)
    }

    var tabsHeader: some View {
        let tabs = [LS("Summary", "Résumé"), LS("History", "Historique"), LS("Instructions", "Instructions")]
        return VStack(spacing: 0) {
            HStack(spacing: 0) {
                ForEach(0..<3, id: \.self) { i in
                    Button { withAnimation(.easeInOut(duration: 0.25)) { tab = i } } label: {
                        Txt(tabs[i], weight: tab == i ? 700 : 500, size: 14, color: tab == i ? C.text : C.mut)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                    }
                    .buttonStyle(.plain)
                }
            }
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    Rectangle().fill(C.line).frame(height: 0.5)
                    Rectangle().fill(accentCol(repo.settings.accent))
                        .frame(width: g.size.width / 3, height: 2)
                        .offset(x: g.size.width / 3 * CGFloat(tab))
                }
            }
            .frame(height: 2)
        }
        .padding(.horizontal, 16)
    }

    var summaryTab: some View {
        let unit = repo.settings.unit
        let cutoffSec: Double = period == 0 ? -91 * 86400 : period == 1 ? -182 * 86400 : period == 2 ? -365 * 86400 : -.infinity
        let now = Date.now.timeIntervalSince1970
        let sessions = repo.workouts
            .filter { $0.startedAt / 1000 >= now + cutoffSec }
            .compactMap { w -> (Workout, ExEntry)? in
                w.exercises.first { $0.name == name }.map { (w, $0) }
            }
        let heaviest: [(Int, Double)] = sessions.map { w, ex in (Int(w.startedAt / 1000), (ex.sets.map { $0.kg ?? 0 }.max() ?? 0)) }
        let volumes: [(Int, Double)] = sessions.map { w, ex in (Int(w.startedAt / 1000), ex.sets.filter { $0.done }.reduce(0) { $0 + ($1.kg ?? 0) * Double($1.reps ?? 0) }) }
        return VStack(spacing: 0) {
            if heaviest.isEmpty {
                EmptyState(text: LS("No data yet.\nLog this exercise to see charts.",
                                     "Aucune donnée.\nEnregistre cet exercice pour voir les graphiques."))
            } else {
                HStack(spacing: 8) {
                    ForEach(0..<4, id: \.self) { i in
                        let labels = [LS("3m", "3m"), LS("6m", "6m"), LS("1y", "1a"), LS("All", "Tout")]
                        Button { period = i } label: {
                            Txt(labels[i], weight: 600, size: 12.5, color: period == i ? accentCol(repo.settings.accent) : C.mut)
                                .padding(.horizontal, 14).padding(.vertical, 7)
                                .background(period == i ? C.card2 : C.card, in: Capsule())
                                .overlay(Capsule().strokeBorder(period == i ? accentCol(repo.settings.accent) : C.line, lineWidth: 1))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                recordsCard(sessions: sessions, heaviest: heaviest, volumes: volumes, unit: unit)
                chartCard(title: LS("Heaviest weight", "Poids le plus lourd"),
                          value: "\(Calc.fmtKg(heaviest.last?.1, unit)) kg",
                          points: heaviest, fmt: { Calc.fmtKg($0, unit) })
                chartCard(title: LS("Total volume", "Volume total"),
                          value: "\(Calc.fmtVol(volumes.reduce(0) { $0 + $1.1 }, unit)) kg",
                          points: volumes, fmt: { Calc.fmtVol($0, unit) })
            }
        }
    }

    func recordsCard(sessions: [(Workout, ExEntry)], heaviest: [(Int, Double)], volumes: [(Int, Double)], unit: String) -> some View {
        let best1rm = sessions.flatMap { _, ex in ex.sets.filter { ($0.kg ?? 0) > 0 && ($0.reps ?? 0) > 0 }.map { Calc.e1rm($0.kg!, $0.reps!) } }.max()
        let bestSet = sessions.flatMap { _, ex in ex.sets.compactMap { $0.kg } }.max()
        let pr = repo.prFor(name)
        let lastTop = sessions.last?.1.sets.map { $0.kg ?? 0 }.max() ?? 0
        let delta = (bestSet ?? 0) > 0 ? lastTop - bestSet! : 0
        let pct = (bestSet ?? 0) > 0 ? Int(delta / bestSet! * 100) : 0
        return AppCard {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 7) {
                    Image(systemName: "trophy.fill").font(.system(size: 13)).foregroundColor(accentCol(repo.settings.accent))
                    Txt(LS("Records", "Records"), weight: 700, size: 14)
                }
                HStack(spacing: 14) {
                    VStack(alignment: .leading, spacing: 2) {
                        Txt(LS("Best est. 1RM", "1RM max est."), size: 11.5, color: C.mut)
                        Txt("\(Calc.fmtKg(best1rm, unit)) kg", weight: 700, size: 14)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    VStack(alignment: .leading, spacing: 2) {
                        Txt(LS("Heaviest set", "Série lourde"), size: 11.5, color: C.mut)
                        Txt("\(Calc.fmtKg(bestSet, unit)) kg", weight: 700, size: 14)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    VStack(alignment: .leading, spacing: 2) {
                        Txt(LS("Sessions", "Séances"), size: 11.5, color: C.mut)
                        Txt("\(sessions.count)", weight: 700, size: 14)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                if let pr {
                    Txt(LS("PR set on ", "Record établi le ") + Calc.fmtDateShort(pr.weightDate), size: 11.5, color: C.mut)
                }
                if sessions.count >= 2 {
                    Txt("\(LS("Last session vs best", "Dernière séance vs record")) : \(delta >= 0 ? "+" : "")\(Calc.fmtKg(delta, unit)) kg (\(pct)%)",
                        weight: 600, size: 12, color: delta < 0 ? C.red : accentCol(repo.settings.accent))
                }
            }
        }
    }

    func chartCard(title: String, value: String, points: [(Int, Double)], fmt: @escaping (Double) -> String) -> some View {
        let accent = accentCol(repo.settings.accent)
        return AppCard {
            VStack(alignment: .leading, spacing: 6) {
                Txt(title, weight: 500, size: 13, color: C.mut)
                Txt(value, weight: 700, size: 19)
                Chart(points, id: \.0) { p in
                    AreaMark(x: .value("T", Date(timeIntervalSince1970: Double(p.0))), y: .value("V", p.1))
                        .foregroundStyle(LinearGradient(colors: [accent.opacity(0.35), .clear], startPoint: .top, endPoint: .bottom))
                        .interpolationMethod(.catmullRom)
                    LineMark(x: .value("T", Date(timeIntervalSince1970: Double(p.0))), y: .value("V", p.1))
                        .foregroundStyle(accent)
                        .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round))
                        .interpolationMethod(.catmullRom)
                }
                .chartXAxis(.hidden)
                .chartYAxis(.hidden)
                .frame(height: 130)
            }
        }
    }

    var historyTab: some View {
        let unit = repo.settings.unit
        let sessions = repo.workoutsDesc().compactMap { w -> (Workout, ExEntry)? in
            w.exercises.first { $0.name == name }.map { (w, $0) }
        }
        return Group {
            if sessions.isEmpty {
                EmptyState(text: LS("No sessions logged yet.", "Aucune séance enregistrée."))
            } else {
                ForEach(sessions, id: \.0.id) { w, ex in
                    VStack(alignment: .leading, spacing: 0) {
                        HStack {
                            Txt(Calc.fmtDateFull(w.startedAt), weight: 700, size: 14)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Txt("\(Calc.fmtVol(ex.sets.filter { $0.done }.reduce(0) { $0 + ($1.kg ?? 0) * Double($1.reps ?? 0) }, unit)) kg",
                                size: 13, color: C.mut)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        HStack(spacing: 0) {
                            Txt("SÉRIE", size: 11, color: C.mut).frame(maxWidth: .infinity, alignment: .leading)
                            Txt("KG", size: 11, color: C.mut).frame(maxWidth: .infinity)
                            Txt("RÉPS", size: 11, color: C.mut).frame(maxWidth: .infinity, alignment: .trailing)
                        }
                        .padding(.horizontal, 16)
                        ForEach(Array(ex.sets.enumerated()), id: \.offset) { si, st in
                            HStack(spacing: 0) {
                                Txt("\(si + 1)", size: 13, color: C.mut).frame(maxWidth: .infinity, alignment: .leading)
                                Txt(st.kg != nil ? Calc.fmtKg(st.kg, unit) : "—", weight: 600, size: 13).frame(maxWidth: .infinity)
                                Txt(st.reps.map(String.init) ?? "—", weight: 600, size: 13).frame(maxWidth: .infinity, alignment: .trailing)
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 9)
                            .background(si % 2 == 1 ? C.card2.opacity(0.35) : Color.clear)
                        }
                        Spacer().frame(height: 14)
                    }
                }
            }
        }
    }

    var instructionsTab: some View {
        let def = ExData.byName[name]
        let muscle = def?.muscle ?? "Chest"
        let steps = stepsFor(name)
        let cueList = cuesFor(muscle)
        return VStack(spacing: 0) {
            AppCard {
                ZStack(alignment: .bottomTrailing) {
                    IllIcon(muscle: muscle, size: 170)
                        .frame(maxWidth: .infinity)
                        .scaleEffect(playing ? 1.04 : 1.0)
                        .animation(.easeInOut(duration: 0.7).repeatForever(autoreverses: true), value: playing)
                        .padding(.vertical, 14)
                    Button { playing.toggle() } label: {
                        Image(systemName: playing ? "pause.fill" : "play.fill")
                            .font(.system(size: 14))
                            .foregroundColor(C.text)
                            .frame(width: 38, height: 38)
                            .background(C.card2, in: Circle())
                    }
                    .buttonStyle(.plain)
                    .padding(10)
                }
                .background(Color.black)
            }
            AppCard {
                VStack(alignment: .leading, spacing: 6) {
                    Txt(equipName(def?.equip ?? ""), weight: 700, size: 14)
                    if !equipHintFor(def?.equip ?? "").isEmpty {
                        Txt(equipHintFor(def?.equip ?? ""), size: 13, color: C.mut)
                    }
                }
            }
            AppCard {
                VStack(alignment: .leading, spacing: 6) {
                    Txt(LS("PRIMARY MUSCLE", "MUSCLE PRINCIPAL"), weight: 700, size: 11, color: C.mut)
                    Txt(muscleName(def?.muscle ?? ""), weight: 700, size: 15)
                }
            }
            if !steps.isEmpty {
                AppCard {
                    VStack(alignment: .leading, spacing: 0) {
                        Txt(LS("How to perform", "Comment réaliser l'exercice"), weight: 700, size: 14)
                            .padding(.bottom, 4)
                        ForEach(Array(steps.enumerated()), id: \.offset) { si, step in
                            HStack(alignment: .top, spacing: 10) {
                                Txt("\(si + 1)", weight: 700, size: 12, color: C.accText)
                                    .frame(width: 22, height: 22)
                                    .background(accentCol(repo.settings.accent), in: Circle())
                                Txt(step, size: 14)
                                    .lineSpacing(6)
                            }
                            .padding(.vertical, 7)
                        }
                    }
                }
            }
            if !cueList.isEmpty {
                AppCard {
                    VStack(alignment: .leading, spacing: 0) {
                        Txt(LS("TIPS", "CONSEILS"), weight: 700, size: 11, color: C.mut)
                            .padding(.bottom, 4)
                        ForEach(cueList, id: \.self) { cue in
                            HStack(alignment: .top, spacing: 0) {
                                Txt("•", weight: 700, size: 14, color: accentCol(repo.settings.accent))
                                    .frame(width: 14, alignment: .leading)
                                Txt(cue, size: 14)
                                    .lineSpacing(6)
                            }
                            .padding(.vertical, 6)
                        }
                    }
                }
            }
        }
    }
}
