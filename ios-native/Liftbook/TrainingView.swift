import SwiftUI
import Foundation
import LiftbookCore
import Charts

/** Entraînement tab — port of Routines.kt (view selector, empty workout, routines + native drag reorder). */
struct TrainingView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    @State private var view = 0 // 0 Routines, 1 Calendrier
    @State private var viewMenu = false
    @State private var busyDialog = false
    @State private var pendingAction: (() -> Void)?

    var body: some View {
        VStack(spacing: 0) {
            if view == 1 {
                TrainingCalendarView()
            } else {
                routinesList
            }
        }
        .background(C.bg)
        .overlay {
            if busyDialog {
                WorkoutInProgressDialog(
                    onDismiss: { busyDialog = false; pendingAction = nil },
                    onResume: { busyDialog = false; pendingAction = nil; nav.push(.logger) },
                    onRestart: { busyDialog = false; pendingAction?() }
                )
            }
        }
    }

    func guardStart(_ block: @escaping () -> Void) {
        if repo.draft != nil {
            pendingAction = block
            busyDialog = true
        } else {
            block()
        }
    }

    func startFresh(_ block: @escaping () -> Void) {
        guardStart {
            RestTimerModel.shared.clear()
            repo.discardDraft()
            block()
        }
    }

    var routinesList: some View {
        List {
            Section {
                VStack(spacing: 0) {
                    Button { viewMenu = true } label: {
                        HStack(spacing: 8) {
                            Txt(LS("Training", "Entraînement"), weight: 800, size: 26)
                            Circle().fill(C.card2).frame(width: 26, height: 26)
                                .overlay(Image(systemName: "chevron.down").font(.system(size: 12, weight: .semibold)).foregroundColor(C.text))
                        }
                    }
                    .buttonStyle(.plain)
                    .confirmationDialog("", isPresented: $viewMenu, titleVisibility: .invisible) {
                        Button(LS("Training", "Entraînement")) { view = 0 }
                        Button(LS("Calendar", "Calendrier")) { view = 1 }
                    }
                    Color.clear.frame(height: 6)
                    Button {
                        startFresh {
                            repo.startWorkout(nil)
                            nav.push(.logger)
                        }
                    } label: {
                        HStack(spacing: 10) {
                            Image(systemName: "plus").font(.system(size: 18))
                            Txt(LS("Start an Empty Workout", "Démarrer un Entraînement Vide"), weight: 600, size: 16)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(RoundedRectangle(cornerRadius: 12).fill(C.card))
                        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(C.line, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                    HStack {
                        Txt(LS("Routines", "Routines"), weight: 800, size: 20)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Button {
                            startFresh {
                                repo.startRoutine(nil)
                                nav.push(.logger)
                            }
                        } label: {
                            Image(systemName: "folder.badge.plus").font(.system(size: 20)).foregroundColor(C.text).padding(8)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.top, 18)
                    HStack(spacing: 10) {
                        secondaryButton(text: LS("New routine", "Nouv. Routine"), icon: "plus.square.on.square") {
                            startFresh {
                                repo.startRoutine(nil)
                                nav.push(.logger)
                            }
                        }
                        secondaryButton(text: LS("Explore", "Explorer"), icon: "magnifyingglass") {
                            nav.push(.exercises)
                        }
                    }
                }
                .listRowInsets(EdgeInsets(top: 10, leading: 8, bottom: 4, trailing: 8))
                .listRowBackground(C.bg)
                .listRowSeparator(.hidden)
            }
            Section {
                let routines = repo.routines
                if routines.isEmpty {
                    EmptyState(text: LS("No routines yet.\nTap “New routine” to create one.",
                                         "Aucune routine.\nTouche « Nouvelle routine » pour en créer une."))
                        .listRowBackground(C.bg)
                        .listRowSeparator(.hidden)
                }
                ForEach(routines) { r in
                    RoutineCard(r: r) {
                        startFresh {
                            repo.startWorkout(r.id)
                            nav.push(.logger)
                        }
                    }
                    .listRowBackground(C.bg)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 6, leading: 0, bottom: 6, trailing: 0))
                }
                .onMove { from, to in
                    repo.moveRoutine(from: from, to: to)
                }
                Color.clear.frame(height: 20)
                    .listRowBackground(C.bg)
                    .listRowSeparator(.hidden)
            } header: {
                Txt("\(LS("My routines", "Mes routines")) (\(repo.routines.count))", weight: 500, size: 15, color: C.mut)
                    .listSectionSeparator(.hidden)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .environment(\.editMode, .constant(.active))
    }

    func secondaryButton(text: String, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: icon).font(.system(size: 15))
                Txt(text, weight: 600, size: 14.5)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 46)
            .background(RoundedRectangle(cornerRadius: 12).fill(C.card))
            .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(C.line, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

struct RoutineCard: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    let r: Routine
    let onStart: () -> Void
    @State private var confirmDel = false
    @State private var rename = false
    @State private var cardMenu = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 4) {
                Button { nav.push(.routineDetail(r.id)) } label: {
                    Txt(exName(r.name), weight: 800, size: 19)
                        .lineLimit(1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)
                Menu {
                    Button(LS("View routine", "Voir la routine")) { nav.push(.routineDetail(r.id)) }
                    Button(LS("Rename routine", "Renommer la routine")) { rename = true }
                    Button(LS("Duplicate routine", "Dupliquer la routine")) { repo.duplicateRoutine(r.id) }
                    Button(LS("Delete routine", "Supprimer la routine"), role: .destructive) { confirmDel = true }
                } label: {
                    Image(systemName: "ellipsis").font(.system(size: 17)).foregroundColor(C.mut).padding(8)
                }
            }
            Spacer().frame(height: 6)
            if r.exercises.isEmpty {
                Txt(LS("No exercises yet", "Aucun exercice pour l'instant"), size: 14.5, color: C.mut)
            } else {
                Txt(r.exercises.map { exName($0.name) }.joined(separator: ", "), size: 15, color: C.mut)
                    .lineLimit(2)
                    .lineSpacing(6)
            }
            Spacer().frame(height: 14)
            Button(action: onStart) {
                Txt(LS("Start routine", "Commencer la Routine"), weight: 600, size: 15, color: C.accText)
                    .frame(maxWidth: .infinity)
                    .frame(height: 46)
                    .background(RoundedRectangle(cornerRadius: 12).fill(accentColor(repo.settings.accent)))
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(C.card))
        .overlay(
            Group {
                if rename {
                    PromptAlert(initial: r.name, title: LS("Rename routine", "Renommer la routine"), confirmLabel: LS("Save", "Enregistrer")) { v in
                        repo.renameRoutine(r.id, v)
                        rename = false
                    } onDismiss: { rename = false }
                    .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { rename = false })
                }
                if confirmDel {
                    AlertView(
                        title: LS("Delete routine?", "Supprimer la routine ?"),
                        message: LS("This routine will be removed from your list.", "Cette routine sera retirée de ta liste."),
                        confirmLabel: LS("Delete", "Supprimer"),
                        dismissLabel: LS("Cancel", "Annuler"),
                        confirmColor: C.red,
                        onConfirm: { confirmDel = false; repo.deleteRoutine(r.id) },
                        onDismiss: { confirmDel = false }
                    )
                    .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { confirmDel = false })
                }
            }
        )
    }
}

/** "Entraînement ▾ → Calendrier" — port of TrainingCalendar.kt. */
struct TrainingCalendarView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    @State private var month = Date()

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                HStack {
                    Button { month = cal.date(byAdding: .month, value: -1, to: month) ?? month } label: {
                        Image(systemName: "chevron.left").font(.system(size: 18)).foregroundColor(C.text).padding(6)
                    }
                    .buttonStyle(.plain)
                    Spacer()
                    Txt(Calc.monthLabel(month.timeIntervalSince1970).capitalizedFirst, weight: 800, size: 18)
                    Spacer()
                    Button {
                        if month < Calendar.current.startOfMonth(for: Date.now) {
                            month = cal.date(byAdding: .month, value: 1, to: month) ?? month
                        }
                    } label: {
                        Image(systemName: "chevron.right").font(.system(size: 18))
                            .foregroundColor(month < cal.startOfMonth(for: Date.now) ? C.text : C.mut)
                            .padding(6)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 12)
                let byDay = Dictionary(grouping: repo.workouts, by: { Calc.dayKey($0.startedAt) })
                let days = cal.monthDays(month)
                let latestByDay = Dictionary(uniqueKeysWithValues: byDay.map { k, v in (k, v.max { $0.startedAt < $1.startedAt }!) })
                HStack(spacing: 0) {
                    ForEach(["L", "M", "M", "J", "V", "S", "D"], id: \.self) { d in
                        Txt(d, weight: 700, size: 12, color: C.mut).frame(maxWidth: .infinity)
                    }
                }
                .padding(.horizontal, 16)
                Spacer().frame(height: 8)
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 0), count: 7), spacing: 10) {
                    ForEach(days, id: \.self) { date in
                        if let date {
                            let key = Calc.dayKey(date.timeIntervalSince1970 * 1000)
                            DayCell(date: date, workout: latestByDay[key])
                        } else {
                            Color.clear.frame(width: 40, height: 40)
                        }
                    }
                }
                .padding(.horizontal, 16)
                Spacer().frame(height: 16)
                let monthWorkouts = repo.workouts.filter { cal.isDate($0.date, equalTo: month, toGranularity: .month) }
                if monthWorkouts.isEmpty {
                    Txt(LS("No workouts this month.", "Aucune séance ce mois-ci."), size: 13, color: C.mut)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                } else {
                    Txt("\(monthWorkouts.count) \(LS("workouts", "séances"))  ·  \(Calc.fmtVol(monthWorkouts.reduce(0) { $0 + Calc.vol($1) }, repo.settings.unit)) kg",
                        size: 13, color: C.mut)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                }
                Spacer().frame(height: 24)
            }
        }
    }

    var cal: Calendar {
        var c = Calendar(identifier: .gregorian)
        c.firstWeekday = 2
        c.locale = Locale(identifier: "fr_FR")
        return c
    }
}

extension Calendar {
    func monthDays(_ month: Date) -> [Date?] {
        let first = startOfMonth(for: month)
        let lead = (firstWeekday - 1 + 7) % 7  // Monday-first
        let count = range(of: .day, in: .month, for: first)?.count ?? 30
        var out: [Date?] = Array(repeating: nil, count: lead)
        for d in 0..<count {
            out.append(byAdding: .day, value: d, to: first)
        }
        return out
    }

    func startOfMonth(for d: Date) -> Date {
        let c = dateComponents([.year, .month], from: d)
        return self.date(from: c) ?? d
    }
}

struct DayCell: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    let date: Date
    let workout: Workout?
    var body: some View {
        let has = workout != nil
        let isToday = Calendar.current.isDateInToday(date)
        return Button {
            if let w = workout { nav.push(.workoutDetail(w.id)) }
        } label: {
            Txt("\(Calendar.current.component(.day, from: date))",
                weight: has || isToday ? 700 : 400, size: 14.5,
                color: has ? C.accText : C.text)
                .frame(width: 40, height: 40)
                .background(Circle().fill(has ? accentColor(repo.settings.accent) : Color.clear))
                .overlay(Circle().strokeBorder(isToday && !has ? C.mut : Color.clear, lineWidth: 1.5))
        }
        .buttonStyle(.plain)
        .disabled(!has)
    }
}

extension String {
    var capitalizedFirst: String { prefix(1).uppercased() + dropFirst() }
}

extension Workout {
    var date: Date { Date(timeIntervalSince1970: startedAt) }
}

/** Routine detail — port of RoutineDetailScreen (charts via Swift Charts). */
struct RoutineDetailView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    let id: Int
    @State private var metric = 0
    @State private var confirmDel = false
    @State private var rename = false
    @State private var rMenu = false
    @State private var busyDialog = false
    @State private var pendingAction: (() -> Void)?

    var body: some View {
        Group {
            if let r = repo.routineById(id) {
                content(r: r)
            } else {
                Color.clear.onAppear { nav.pop() }
            }
        }
    }

    func guardStart(_ block: @escaping () -> Void) {
        if repo.draft != nil { pendingAction = block; busyDialog = true } else { block() }
    }

    func startFresh(_ block: @escaping () -> Void) {
        guardStart {
            RestTimerModel.shared.clear()
            repo.discardDraft()
            block()
        }
    }

    func content(r: Routine) -> some View {
        let sessions = repo.workouts
            .filter { w in w.exercises.contains { e in r.exercises.contains { $0.name == e.name } } }
            .suffix(12)
        return ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 0) {
                    Button { nav.pop() } label: {
                        Image(systemName: "chevron.left").font(.system(size: 20)).foregroundColor(C.text).padding(8)
                    }
                    .buttonStyle(.plain)
                    Spacer()
                    Txt(LS("Routine", "Routine"), weight: 600, size: 16)
                    Spacer()
                    ShareLink(item: shareText(r)) {
                        Image(systemName: "square.and.arrow.up").font(.system(size: 18)).foregroundColor(C.text).padding(8)
                    }
                    Menu {
                        Button(LS("Edit routine", "Modifier la routine")) {
                            startFresh {
                                repo.startRoutine(r.id)
                                nav.push(.logger)
                            }
                        }
                        Button(LS("Rename routine", "Renommer la routine")) { rename = true }
                        Button(LS("Duplicate routine", "Dupliquer la routine")) { _ = repo.duplicateRoutine(r.id) }
                        Button(LS("Delete routine", "Supprimer la routine"), role: .destructive) { confirmDel = true }
                    } label: {
                        Image(systemName: "ellipsis").font(.system(size: 18)).foregroundColor(C.text).padding(8)
                    }
                }
                Txt(r.name, weight: 800, size: 26)
                    .padding(.leading, 16)
                    .padding(.top, 8)
                Txt("\(LS("Created by", "Créée par")) \(repo.settings.handle)", size: 14, color: C.mut)
                    .padding(.leading, 16)
                    .padding(.top, 2)
                    .padding(.bottom, 12)
                PrimaryButton(text: "Commencer la Routine") {
                    startFresh {
                        repo.startWorkout(r.id)
                        nav.push(.logger)
                    }
                }
                .padding(.horizontal, 16)
                if !sessions.isEmpty {
                    chartBlock(r: r, sessions: Array(sessions))
                }
                HStack {
                    Txt(LS("Exercises", "Exercices"), size: 18, color: C.mut)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        startFresh {
                            repo.startRoutine(r.id)
                            nav.push(.logger)
                        }
                    } label: {
                        Txt(LS("Edit Routine", "Modifier la Routine"), weight: 600, size: 15, color: accentColor(repo.settings.accent))
                    }
                    .buttonStyle(.plain)
                }
                .padding(16)
                ForEach(Array(r.exercises.enumerated()), id: \.offset) { _, ex in
                    exerciseBlock(ex)
                }
                Spacer().frame(height: 24)
            }
        }
        .background(C.bg)
        .overlay {
            if busyDialog {
                WorkoutInProgressDialog(
                    onDismiss: { busyDialog = false; pendingAction = nil },
                    onResume: { busyDialog = false; pendingAction = nil; nav.push(.logger) },
                    onRestart: { busyDialog = false; pendingAction?() }
                )
            }
            if rename {
                PromptAlert(initial: r.name, title: LS("Rename routine", "Renommer la routine"), confirmLabel: LS("Save", "Enregistrer")) { v in
                    repo.renameRoutine(r.id, v)
                    rename = false
                } onDismiss: { rename = false }
                .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { rename = false })
            }
            if confirmDel {
                AlertView(
                    title: LS("Delete routine?", "Supprimer la routine ?"),
                    message: LS("This routine will be removed from your list.", "Cette routine sera retirée de ta liste."),
                    confirmLabel: LS("Delete", "Supprimer"),
                    dismissLabel: LS("Cancel", "Annuler"),
                    confirmColor: C.red,
                    onConfirm: { confirmDel = false; repo.deleteRoutine(r.id); nav.pop() },
                    onDismiss: { confirmDel = false }
                )
                .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { confirmDel = false })
            }
        }
    }

    func shareText(_ r: Routine) -> String {
        var lines = [r.name, ""]
        for ex in r.exercises {
            lines.append("\(exName(ex.name)) (\(ex.sets.count) séries)")
            for (i, st) in ex.sets.enumerated() {
                let kg = st.kg != nil ? "\(Calc.fmtKg(st.kg, repo.settings.unit)) kg" : ""
                lines.append("  \(i + 1). \(kg) × \(st.reps.map(String.init) ?? "—")")
            }
        }
        return lines.joined(separator: "\n")
    }

    @ViewBuilder
    func chartBlock(r: Routine, sessions: [Workout]) -> some View {
        let accent = accentColor(repo.settings.accent)
        let totalReps = r.exercises.reduce(0) { $0 + $1.sets.reduce(0) { $0 + ($1.reps ?? 0) } }
        let totalLabel: String = metric == 0 ? "\(Calc.fmtVol(volTargetFor(r), repo.settings.unit)) kg" : metric == 1 ? "\(totalReps) réps" : "—"
        let series: [(String, Double)] = sessions.suffix(10).map { w in
            let v = metric == 0 ? Calc.vol(w) : metric == 1 ? Double(Calc.reps(w)) : (w.endedAt - w.startedAt) / 60000
            return (Calc.fmtDateShort(w.startedAt), v)
        }
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .bottom, spacing: 8) {
                Txt(totalLabel, weight: 800, size: 19)
                Txt(sessions.last.map { Calc.fmtDateShort($0.startedAt) } ?? "", weight: 600, size: 14, color: accent)
                Spacer()
                Txt(LS("3 months", "3 derniers mois"), weight: 600, size: 14, color: accent)
            }
            .padding(.horizontal, 16)
            .padding(.top, 18)
            Chart(series.indices, id: \.self) { i in
                AreaMark(x: .value("Date", i), y: .value("V", series[i].1))
                    .foregroundStyle(LinearGradient(colors: [accent.opacity(0.35), .clear], startPoint: .top, endPoint: .bottom))
                    .interpolationMethod(.catmullRom)
                LineMark(x: .value("Date", i), y: .value("V", series[i].1))
                    .foregroundStyle(accent)
                    .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round))
                    .interpolationMethod(.catmullRom)
            }
            .chartXAxis(.hidden)
            .chartYAxis(.hidden)
            .frame(height: 150)
            .padding(.horizontal, 16)
            HStack(spacing: 8) {
                ForEach(0..<3, id: \.self) { idx in
                    let labels = [LS("Volume", "Volume"), LS("Reps", "Réps"), LS("Duration", "Durée")]
                    Button { metric = idx } label: {
                        Txt(labels[idx], weight: 600, size: 13.5, color: metric == idx ? C.accText : C.text)
                            .padding(.horizontal, 16).padding(.vertical, 8)
                            .background(Capsule().fill(metric == idx ? accent : C.card2))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }

    func volTargetFor(_ r: Routine) -> Double {
        r.exercises.reduce(0) { $0 + $1.sets.reduce(0) { $0 + ($1.kg ?? 0) * Double($1.reps ?? 0) } }
    }

    func exerciseBlock(_ ex: ExEntry) -> some View {
        let effective = ex.restSec ?? repo.settings.restSec
        let m = effective / 60
        let s = effective % 60
        return VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 14) {
                Circle().fill(C.card2).frame(width: 42, height: 42)
                    .overlay(IllIcon(muscle: ex.muscle, size: 30))
                Button { nav.push(.exerciseDetail(ex.name)) } label: {
                    Txt(exName(ex.name), weight: 600, size: 18, color: accentColor(repo.settings.accent))
                        .lineLimit(1)
                }
                .buttonStyle(.plain)
                Spacer()
            }
            .padding(.horizontal, 16)
            HStack(spacing: 8) {
                Image(systemName: "timer").font(.system(size: 14)).foregroundColor(accentColor(repo.settings.accent))
                Txt("\(LS("Rest Timer", "Minuteur de Repos")): \(m > 0 ? "\(m)min " : "")\(s)s",
                    weight: 500, size: 14.5, color: accentColor(repo.settings.accent))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            HStack(spacing: 0) {
                Txt(LS("SET", "SÉRIE"), size: 12, color: C.mut).frame(maxWidth: .infinity, alignment: .leading)
                Txt("KG", size: 12, color: C.mut).frame(maxWidth: .infinity)
                Txt(LS("REPS", "RÉPS"), size: 12, color: C.mut).frame(maxWidth: .infinity, alignment: .trailing)
            }
            .padding(.horizontal, 16)
            ForEach(Array(ex.sets.enumerated()), id: \.offset) { si, st in
                HStack(spacing: 0) {
                    Txt("\(si + 1)", size: 15).frame(maxWidth: .infinity, alignment: .leading)
                    Txt(st.kg != nil ? Calc.fmtKg(st.kg, repo.settings.unit) : "—", size: 15).frame(maxWidth: .infinity)
                    Txt(st.reps.map(String.init) ?? "—", size: 15).frame(maxWidth: .infinity, alignment: .trailing)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(si % 2 == 1 ? C.card : Color.clear)
            }
            Spacer().frame(height: 18)
        }
    }
}
