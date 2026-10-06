import SwiftUI
import LiftbookCore

/** History list with filters + calendar — port of History.kt. */
struct HistoryView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    @State private var query = ""
    @State private var filter = 0
    @State private var showCalendar = false
    @State private var viewDate = Date.now

    var filtered: [Workout] {
        let cutoff: Double = filter == 1 ? -7 * 86400 : filter == 2 ? -30 * 86400 : filter == 3 ? -365 * 86400 : -.infinity
        let q = query.trimmingCharacters(in: .whitespaces)
        let now = Date.now.timeIntervalSince1970
        return repo.workoutsDesc().filter { w in
            w.startedAt / 1000 >= now + cutoff && (q.isEmpty
                || w.name.lowercased().contains(q.lowercased())
                || w.exercises.contains { matches(q, $0.name) })
        }
    }

    var groups: [(String, [Workout])] {
        let ws = Calc.weekStart(Date.now.timeIntervalSince1970)
        var out: [(String, [Workout])] = []
        for w in filtered {
            let label = w.startedAt >= ws ? "Cette semaine"
                : w.startedAt >= ws - 7 * 86_400_000 ? "Semaine dernière"
                : Calc.monthLabel(w.startedAt)
            if let i = out.firstIndex(where: { $0.0 == label }) { out[i].1.append(w) }
            else { out.append((label, [w])) }
        }
        return out
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Button { nav.pop() } label: {
                        Image(systemName: "chevron.left").font(.system(size: 20)).foregroundColor(C.text).padding(8)
                    }
                    .buttonStyle(.plain)
                    Txt(LS("History", "Historique"), weight: 800, size: 22)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button { showCalendar.toggle() } label: {
                        Image(systemName: "calendar").font(.system(size: 20)).foregroundColor(C.text).padding(8)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 8)
                .padding(.top, 12)
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass").font(.system(size: 14)).foregroundColor(C.mut)
                    TextField(LS("Search workouts", "Rechercher des séances"), text: $query)
                        .font(.inter(400, 14))
                        .foregroundColor(C.text)
                        .tint(C.accent)
                        .autocorrectionDisabled()
                }
                .padding(.horizontal, 12)
                .frame(height: 46)
                .background(RoundedRectangle(cornerRadius: 12).fill(C.card))
                .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(C.line, lineWidth: 1))
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
                HStack(spacing: 8) {
                    ForEach(0..<4, id: \.self) { i in
                        let labels = [LS("All", "Toutes"), LS("This week", "Cette semaine"), LS("This month", "Ce mois"), LS("This year", "Cette année")]
                        Button { filter = i } label: {
                            Txt(labels[i], weight: 600, size: 12, color: filter == i ? C.accText : C.mut)
                                .padding(.horizontal, 12).padding(.vertical, 6)
                                .background(Capsule().fill(filter == i ? accentColor(repo.settings.accent) : C.card))
                                .overlay(Capsule().strokeBorder(filter == i ? accentColor(repo.settings.accent) : C.line, lineWidth: 1))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
                if showCalendar {
                    calendarCard
                }
                if groups.isEmpty {
                    EmptyState(text: "Aucune séance enregistrée.")
                } else {
                    ForEach(groups, id: \.0) { g in
                        HStack(spacing: 6) {
                            Txt(g.0, weight: 500, size: 15, color: C.mut)
                            Image(systemName: "chevron.down").font(.system(size: 12)).foregroundColor(C.mut)
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 14)
                        .padding(.bottom, 8)
                        ForEach(g.1) { w in
                            Button { nav.push(.workoutDetail(w.id)) } label: { HistoryRow(w: w) }
                                .buttonStyle(.plain)
                        }
                    }
                }
                Spacer().frame(height: 16)
            }
        }
        .background(C.bg)
    }

    var byDay: [String: [Workout]] {
        Dictionary(grouping: repo.workouts, by: { Calc.dayKey($0.startedAt) })
    }

    var calendarCard: some View {
        let cal = Calendar.current
        let days = cal.monthDays(viewDate)
        return AppCard {
            VStack(spacing: 0) {
                HStack {
                    Button {
                        viewDate = cal.date(byAdding: .month, value: -1, to: viewDate) ?? viewDate
                    } label: {
                        Image(systemName: "chevron.left").font(.system(size: 16)).foregroundColor(C.text).padding(6)
                    }
                    .buttonStyle(.plain)
                    Txt(Calc.monthLabel(viewDate.timeIntervalSince1970).capitalizedFirst, weight: 800, size: 14.5)
                        .frame(maxWidth: .infinity)
                    Button {
                        viewDate = cal.date(byAdding: .month, value: 1, to: viewDate) ?? viewDate
                    } label: {
                        Image(systemName: "chevron.right").font(.system(size: 16)).foregroundColor(C.text).padding(6)
                    }
                    .buttonStyle(.plain)
                }
                HStack(spacing: 0) {
                    ForEach(["Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di"], id: \.self) { d in
                        Txt(d, weight: 800, size: 9.5, color: C.mut).frame(maxWidth: .infinity)
                    }
                }
                .padding(.horizontal, 10)
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 1), count: 7), spacing: 0) {
                    ForEach(days.indices, id: \.self) { i in
                        if let date = days[i] {
                            let key = Calc.dayKey(date.timeIntervalSince1970 * 1000)
                            let has = !(byDay[key]?.isEmpty ?? true)
                            let isToday = cal.isDateInToday(date)
                            Button {
                                if let w = byDay[key]?.first { nav.push(.workoutDetail(w.id)) }
                            } label: {
                                Txt("\(cal.component(.day, from: date))", weight: 600, size: 12.5)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 38)
                                    .background(RoundedRectangle(cornerRadius: 10).fill(has ? C.card2 : Color.clear))
                                    .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(isToday ? accentColor(repo.settings.accent) : Color.clear, lineWidth: 1.5))
                                    .overlay(alignment: .bottom) {
                                        if has {
                                            Circle().fill(accentColor(repo.settings.accent)).frame(width: 5, height: 5).padding(.bottom, 3)
                                        }
                                    }
                            }
                            .buttonStyle(.plain)
                            .disabled(!has)
                        } else {
                            Color.clear.frame(height: 38)
                        }
                    }
                }
                .padding(.horizontal, 10)
            }
        }
    }
}

struct HistoryRow: View {
    @EnvironmentObject var repo: Repo
    let w: Workout
    var body: some View {
        let d = Date(timeIntervalSince1970: w.startedAt)
        let dow = d.formatted(.dateTime.weekday(.narrow)).uppercased()
        return HStack(spacing: 12) {
            VStack(spacing: 0) {
                Txt(dow, weight: 800, size: 9.5, color: C.mut)
                Txt("\(Calendar.current.component(.day, from: d))", weight: 800, size: 17)
            }
            .frame(width: 46)
            .padding(.vertical, 6)
            .background(RoundedRectangle(cornerRadius: 10).fill(C.card2))
            VStack(alignment: .leading, spacing: 1) {
                Txt(w.name, weight: 700, size: 16).lineLimit(1)
                Txt("\(Calc.fmtTime(w.startedAt)) — \(Calc.fmtTime(w.endedAt))", size: 13, color: C.mut)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Txt("\(Calc.fmtVol(Calc.vol(w), repo.settings.unit)) kg", weight: 600, size: 15)
        }
        .padding(11)
        .background(RoundedRectangle(cornerRadius: 14).fill(C.card))
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(C.line, lineWidth: 1))
        .padding(.horizontal, 16)
        .padding(.vertical, 5)
    }
}

/** Past workout detail — port of WorkoutDetailScreen. */
struct WorkoutDetailView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    let id: Int
    @State private var confirmDelete = false
    @State private var detailMenu = false

    var body: some View {
        Group {
            if let w = repo.workoutById(id) {
                content(w: w)
            } else {
                Color.clear
            }
        }
    }

    func content(w: Workout) -> some View {
        let unit = repo.settings.unit
        return ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Button { nav.pop() } label: {
                        Image(systemName: "chevron.left").font(.system(size: 20)).foregroundColor(C.text).padding(8)
                    }
                    .buttonStyle(.plain)
                    Txt(w.name, weight: 800, size: 16).lineLimit(1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Menu {
                        Button(LS("Create routine from workout", "Créer une routine depuis cette séance")) {
                            repo.routineFromWorkout(w.id, w.name)
                            repo.toast(LS("Routine created", "Routine créée"))
                        }
                        Button(LS("Repeat this workout", "Refaire cette séance")) {
                            if repo.draft != nil {
                                repo.toast(LS("Finish the current workout first", "Termine d'abord la séance en cours"))
                            } else {
                                repo.startRepeat(w.id)
                                nav.toTab(1)
                                nav.push(.logger)
                            }
                        }
                        Button(LS("Share workout", "Partager la séance")) {
                            // handled via ShareLink below in menu — menu items are actions; use share sheet
                            shareWorkout = shareText(w)
                            showShare = true
                        }
                        Button(LS("Delete workout", "Supprimer la séance"), role: .destructive) { confirmDelete = true }
                    } label: {
                        Image(systemName: "ellipsis").font(.system(size: 18)).foregroundColor(C.text).padding(8)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 6)
                Txt("\(Calc.fmtDateFull(w.startedAt)), \(Calc.fmtTime(w.startedAt)) — \(Calc.fmtTime(w.endedAt))",
                    size: 13, color: C.mut)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 2)
                HStack(alignment: .top, spacing: 16) {
                    statCol(LS("Time", "Temps"), Calc.fmtDur(w.endedAt - w.startedAt), flex: 1)
                    statCol(LS("Volume", "Volume"), "\(Calc.fmtVol(Calc.vol(w), unit)) kg", flex: 1.4)
                    VStack(alignment: .leading, spacing: 2) {
                        Txt(LS("Records", "Records"), size: 12, color: C.mut)
                        HStack(spacing: 3) {
                            Text("🏅").font(.system(size: 13))
                            Txt("\(w.prs.count)", weight: 600, size: 14)
                        }
                    }
                    statCol(LS("Sets", "Séries"), "\(Calc.setsDone(w))", flex: 1)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                if !w.prs.isEmpty {
                    AppCard {
                        HStack(alignment: .top, spacing: 10) {
                            Image(systemName: "trophy.fill").font(.system(size: 15)).foregroundColor(accentColor(repo.settings.accent))
                            VStack(spacing: 3) {
                                ForEach(Array(w.prs.enumerated()), id: \.offset) { _, p in
                                    HStack {
                                        Txt(exName(p.ex), weight: 700, size: 13)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                        Txt("\(Calc.fmtKg(p.value, unit)) \(Calc.unitLabel(unit)) · \(p.kind)", size: 12.5, color: C.mut)
                                    }
                                }
                            }
                        }
                    }
                }
                if !w.notes.trimmingCharacters(in: .whitespaces).isEmpty {
                    AppCard {
                        HStack(alignment: .top, spacing: 10) {
                            Image(systemName: "note.text").font(.system(size: 13)).foregroundColor(C.mut)
                            Txt(w.notes, size: 14)
                        }
                    }
                }
                ForEach(Array(w.exercises.enumerated()), id: \.offset) { ei, ex in
                    exerciseCard(w: w, ei: ei, ex: ex, unit: unit)
                }
                Spacer().frame(height: 24)
            }
        }
        .background(C.bg)
        .overlay {
            if confirmDelete {
                AlertView(
                    title: "Supprimer la séance ?",
                    message: "Cette séance et ses records seront définitivement supprimés.",
                    confirmLabel: "Delete",
                    dismissLabel: "Cancel",
                    confirmColor: C.red,
                    onConfirm: {
                        confirmDelete = false
                        repo.deleteWorkout(w.id)
                        nav.deletedUndo = w
                        nav.pop()
                    },
                    onDismiss: { confirmDelete = false }
                )
                .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { confirmDelete = false })
            }
        }
        .sheet(isPresented: $showShare) {
            if let txt = shareWorkout {
                ShareSheet(items: [txt])
            }
        }
    }

    @State private var showShare = false
    @State private var shareWorkout: String?

    func statCol(_ label: String, _ value: String, flex: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Txt(label, size: 12, color: C.mut)
            Txt(value, weight: 600, size: 14)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    func exerciseCard(w: Workout, ei: Int, ex: ExEntry, unit: String) -> some View {
        let prevSets = repo.prevSetsBefore(w.startedAt, ex.name)
        return AppCard {
            VStack(alignment: .leading, spacing: 6) {
                Button { nav.push(.exerciseDetail(ex.name)) } label: {
                    HStack(spacing: 4) {
                        VStack(alignment: .leading, spacing: 1) {
                            Txt(exName(ex.name), weight: 700, size: 16).lineLimit(1)
                            Txt("\(ex.sets.count) \(ex.sets.count > 1 ? LS("series", "séries") : LS("series", "série"))", size: 13, color: C.mut)
                        }
                        Spacer()
                        Image(systemName: "chevron.right").font(.system(size: 14)).foregroundColor(C.mut)
                    }
                }
                .buttonStyle(.plain)
                HStack(spacing: 0) {
                    Txt("SÉRIE", size: 11, color: C.mut).frame(width: 34, alignment: .leading)
                    Txt(LS("PREVIOUS", "PRÉCÉDENTE"), size: 11, color: C.mut).frame(maxWidth: 1.1, alignment: .leading)
                    Txt("KG", size: 11, color: C.mut).frame(maxWidth: .infinity)
                    Txt("RÉPS", size: 11, color: C.mut).frame(maxWidth: .infinity, alignment: .trailing)
                    Color.clear.frame(width: 56)
                }
                ForEach(Array(ex.sets.enumerated()), id: \.offset) { i, s in
                    HStack(spacing: 0) {
                        Txt("\(i + 1)", weight: 600, size: 13, color: C.mut).frame(width: 34, alignment: .leading)
                        Txt(prevSets != nil && i < prevSets!.count ? prevSets![i] : "—", size: 13, color: C.mut)
                            .frame(maxWidth: 1.1, alignment: .leading)
                            .lineLimit(1)
                        Txt(s.kg != nil ? Calc.fmtKg(s.kg, unit) : "—", weight: 600, size: 13).frame(maxWidth: .infinity)
                        Txt(s.reps.map(String.init) ?? "—", weight: 600, size: 13).frame(maxWidth: .infinity, alignment: .trailing)
                        HStack {
                            if s.prW || s.prE {
                                Txt(s.prW ? "WEIGHT PR" : "1RM PR", weight: 800, size: 9, color: C.accText)
                                    .padding(.horizontal, 6).padding(.vertical, 3)
                                    .background(RoundedRectangle(cornerRadius: 5).fill(accentColor(repo.settings.accent)))
                            }
                        }
                        .frame(width: 56, alignment: .leading)
                    }
                    .padding(.vertical, 9)
                    .background(i % 2 == 1 ? C.card2.opacity(0.35) : Color.clear)
                }
                if !ex.notes.isEmpty {
                    HStack(alignment: .top, spacing: 6) {
                        Image(systemName: "note.text").font(.system(size: 11)).foregroundColor(C.mut)
                        Txt(ex.notes, size: 12.5, color: C.mut)
                    }
                }
            }
        }
    }

    func shareText(_ w: Workout) -> String {
        let unit = repo.settings.unit
        var lines = [w.name, Calc.fmtDateFull(w.startedAt)]
        lines.append("Temps \(Calc.fmtDur(w.endedAt - w.startedAt)) · Volume \(Calc.fmtVol(Calc.vol(w), unit)) kg · Records \(w.prs.count)")
        lines.append("")
        for ex in w.exercises {
            lines.append("\(exName(ex.name)) (\(ex.sets.count) séries)")
            for st in ex.sets {
                lines.append("  \(st.kg != nil ? Calc.fmtKg(st.kg, unit) + " kg" : "") × \(st.reps.map(String.init) ?? "—")")
            }
        }
        return lines.joined(separator: "\n")
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }
    func updateUIViewController(_ vc: UIActivityViewController, context: Context) {}
}
