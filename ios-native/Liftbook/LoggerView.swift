import SwiftUI
import LiftbookCore

/** Séance screen — port of Logger.kt (workout logger + routine editor, PR badge, rest bar, picker). */
struct LoggerView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    @ObservedObject private var rest = RestTimerModel.shared
    @State private var showPicker = false
    @State private var replaceIndex: Int? = nil
    @State private var showMenu = false
    @State private var showDiscard = false
    @State private var showNoSets = false
    @State private var showDeleteRoutine = false
    @State private var showNotes = false
    @State private var notesText = ""
    @State private var prBadge: (ex: String, muscle: String, kind: String, value: String)? = nil
    @State private var nameText = ""
    @State private var draftStamp = ""

    var body: some View {
        Group {
            if let d = repo.draft {
                content(d: d)
            } else {
                Color.clear.onAppear { nav.toTab(0) }
            }
        }
    }

    func content(d: Draft) -> some View {
        let isWorkout = d.mode == "workout"
        return VStack(spacing: 0) {
            if isWorkout { workoutHeader(d: d) } else { editorHeader(d: d) }
            exercisesList(d: d, isWorkout: isWorkout)
            VStack(spacing: 10) {
                if rest.endAt > 0 { RestBar() }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .background(C.bg)
        .onAppear {
            if isWorkout, let startedAt = d.startedAt {
                RestTimerModel.shared.postWorkout(startedAt: startedAt)
            }
        }
        .onDisappear {
            if repo.draft == nil { RestTimerModel.shared.cancelWorkout() }
        }
        .overlay(alignment: .top) {
            if let b = prBadge {
                PrBadgeView(badge: b) { prBadge = nil }
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .onAppear {
                        Task {
                            try? await Task.sleep(nanoseconds: 4_200_000_000)
                            withAnimation { prBadge = nil }
                        }
                    }
            }
        }
        .sheet(isPresented: $showPicker) {
            ExercisePicker { name in
                repo.addExToDraft(name)
                showPicker = false
                repo.toast("\(exName(name)) ajouté")
            }
            .environmentObject(repo)
            .presentationDetents([.large])
        }
        .sheet(isPresented: Binding(get: { replaceIndex != nil }, set: { if !$0 { replaceIndex = nil } })) {
            ExercisePicker { name in
                if let ei = replaceIndex, var dd = repo.draft, ei < dd.exercises.count {
                    dd.exercises[ei].name = name
                    dd.exercises[ei].muscle = ExData.byName[name]?.muscle ?? ""
                    repo.draft = dd
                    repo.touchPublic()
                }
                replaceIndex = nil
            }
            .environmentObject(repo)
        }
        .overlay {
            if showDiscard {
                AlertView(
                    title: d.mode == "workout" ? LS("Discard workout?", "Supprimer la séance ?") : LS("Discard changes?", "Ignorer les modifications ?"),
                    message: d.mode == "workout" ? "Your logged sets from this session will be lost." : "Changes to this routine will be lost.",
                    confirmLabel: "Discard",
                    dismissLabel: LS("Cancel", "Annuler"),
                    confirmColor: C.red,
                    onConfirm: discardDraft,
                    onDismiss: { showDiscard = false }
                )
                .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { showDiscard = false })
            }
            if showNoSets {
                AlertView(
                    title: LS("No sets completed", "Aucune série terminée"),
                    message: "There is nothing to save yet. Discard this workout?",
                    confirmLabel: "Discard",
                    dismissLabel: LS("Keep editing", "Continuer"),
                    confirmColor: C.red,
                    onConfirm: discardDraft,
                    onDismiss: { showNoSets = false }
                )
                .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { showNoSets = false })
            }
            if showNotes {
                AlertView(
                    title: LS("Workout notes", "Notes de la séance"),
                    confirmLabel: LS("Save", "Enregistrer"),
                    dismissLabel: LS("Cancel", "Annuler"),
                    customBody: AnyView(
                        TextField("", text: $notesText, prompt: Text(LS("How did it feel?", "Comment ça s'est passé ?")).foregroundColor(C.mut), axis: .vertical)
                            .font(.inter(400, 15))
                            .foregroundColor(C.text)
                            .tint(C.accent)
                            .frame(height: 80, alignment: .top)
                            .padding(12)
                            .background(RoundedRectangle(cornerRadius: 10).fill(C.bg))
                            .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(C.line2, lineWidth: 1))
                    ),
                    onConfirm: {
                        if var dd = repo.draft { dd.notes = notesText; repo.draft = dd }
                        showNotes = false
                    },
                    onDismiss: { showNotes = false }
                )
                .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { showNotes = false })
            }
            if showDeleteRoutine {
                AlertView(
                    title: "Delete routine?",
                    message: "This routine will be removed from your list.",
                    confirmLabel: LS("Discard", "Supprimer"),
                    dismissLabel: "Annuler",
                    confirmColor: C.red,
                    onConfirm: {
                        showDeleteRoutine = false
                        if let rid = repo.draft?.routineId { repo.deleteRoutine(rid) }
                        discardDraftSilent()
                    },
                    onDismiss: { showDeleteRoutine = false }
                )
                .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { showDeleteRoutine = false })
            }
        }
    }

    func discardDraft() {
        showDiscard = false
        showNoSets = false
        RestTimerModel.shared.clear()
        repo.discardDraft()
        nav.pop()
        repo.toast("Supprimée")
    }

    func discardDraftSilent() {
        RestTimerModel.shared.clear()
        repo.discardDraft()
        nav.pop()
    }

    func hasData() -> Bool {
        repo.draft?.exercises.contains { ex in ex.sets.contains { $0.kg != nil || $0.reps != nil } } ?? false
    }

    func anyDone() -> Bool {
        repo.draft?.exercises.contains { ex in ex.sets.contains { $0.done && ($0.kg != nil || $0.reps != nil) } } ?? false
    }

    // ---- workout top bar: ∨ Entraînement | ⏱ | Terminer ----
    func workoutHeader(d: Draft) -> some View {
        VStack(spacing: 4) {
            HStack(spacing: 0) {
                Menu {
                    Button(LS("Workout notes", "Notes de la séance")) {
                        notesText = d.notes
                        showNotes = true
                    }
                    Button(LS("Resume later", "Reprendre plus tard")) { nav.pop() }
                    Button(LS("Discard workout", "Supprimer la séance"), role: .destructive) {
                        if hasData() { showDiscard = true } else { discardDraftSilent(); repo.toast("Supprimée") }
                    }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "chevron.down").font(.system(size: 18, weight: .semibold))
                        Txt(LS("Training", "Entraînement"), weight: 800, size: 21).lineLimit(1)
                    }
                    .foregroundColor(C.text)
                    .padding(.horizontal, 4)
                    .padding(.vertical, 6)
                }
                Spacer()
                Image(systemName: "stopwatch").font(.system(size: 17))
                Spacer().frame(width: 12)
                Button {
                    if anyDone() { nav.push(.workoutSummary) } else { showNoSets = true }
                } label: {
                    Txt(LS("Finish", "Terminer"), weight: 600, size: 15, color: C.accText)
                        .padding(.horizontal, 18).padding(.vertical, 10)
                        .background(RoundedRectangle(cornerRadius: 12).fill(accentColor(repo.settings.accent)))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            liveStats(d: d)
        }
    }

    func liveStats(d: Draft) -> some View {
        TimelineView(.periodic(from: .now, by: 1)) { tl in
            var liveVol = 0.0
            var liveSets = 0
            for ex in d.exercises {
                for s in ex.sets where s.done && s.kg != nil && s.reps != nil {
                    liveVol += s.kg! * Double(s.reps!)
                    liveSets += 1
                }
            }
            let muscles = Set(d.exercises.map { $0.muscle })
            return HStack(alignment: .top, spacing: 8) {
                LiveStat(value: Calc.fmtClock(max(0, (tl.date.timeIntervalSince1970 - (d.startedAt ?? tl.date.timeIntervalSince1970)) * 1000)),
                         label: LS("Duration", "Durée"), accent: true)
                LiveStat(value: "\(Calc.fmtVol(liveVol, repo.settings.unit)) \(Calc.unitLabel(repo.settings.unit))", label: LS("Volume", "Volume"))
                LiveStat(value: "\(liveSets)", label: LS("Sets", "Séries"))
                BodyMap(front: true, muscles: muscles)
                BodyMap(front: false, muscles: muscles)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 4)
        }
    }

    // ---- routine editor top bar ----
    func editorHeader(d: Draft) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Button {
                    if hasData() { showDiscard = true } else { discardDraftSilent() }
                } label: {
                    Txt(LS("Cancel", "Annuler"), weight: 600, size: 15, color: accentColor(repo.settings.accent))
                        .padding(.horizontal, 6).padding(.vertical, 10)
                }
                .buttonStyle(.plain)
                Txt(d.routineId != nil ? LS("Edit Routine", "Modifier la Routine") : LS("Create Routine", "Créer une Routine"),
                    weight: 800, size: 17)
                    .frame(maxWidth: .infinity)
                Button(action: trySaveRoutine) {
                    Txt(LS("Save", "Enregistrer"), weight: 600, size: 15, color: C.accText)
                        .padding(.horizontal, 16).padding(.vertical, 10)
                        .background(RoundedRectangle(cornerRadius: 12).fill(accentColor(repo.settings.accent)))
                }
                .buttonStyle(.plain)
                Menu {
                    if d.routineId != nil {
                        Button(LS("Delete routine", "Supprimer la routine"), role: .destructive) { showDeleteRoutine = true }
                    }
                    Button(LS("Discard changes", "Ignorer les modifications"), role: .destructive) {
                        if hasData() { showDiscard = true } else { discardDraftSilent(); repo.toast("Supprimée") }
                    }
                } label: {
                    Image(systemName: "ellipsis").font(.system(size: 15)).foregroundColor(C.mut).padding(6)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            VStack(spacing: 0) {
                ZStack(alignment: .leading) {
                    if nameText.isEmpty {
                        Txt(LS("Routine title", "Titre de la routine"), weight: 700, size: 22, color: C.mut)
                    }
                    TextField("", text: $nameText)
                        .font(.inter(700, 24))
                        .foregroundColor(C.text)
                        .tint(C.accent)
                        .onChange(of: nameText) { v in
                            if var dd = repo.draft { dd.name = v; repo.draft = dd }
                        }
                }
                .frame(height: 38)
                Rectangle().fill(C.line).frame(height: 1)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 4)
        }
        .onAppear { nameText = d.name }
    }

    func trySaveRoutine() {
        let n = nameText.trimmingCharacters(in: .whitespaces)
        if n.isEmpty { repo.toast(LS("Name your routine first", "Donne un nom à ta routine")); return }
        if repo.draft?.exercises.isEmpty ?? true { repo.toast(LS("Add at least one exercise", "Ajoute au moins un exercice")); return }
        repo.saveRoutine(n)
        nav.pop()
        repo.toast(LS("Routine saved", "Routine enregistrée"))
    }

    // ---- exercise sections (native List + drag reorder) ----
    func exercisesList(d: Draft, isWorkout: Bool) -> some View {
        List {
            if d.exercises.isEmpty {
                EmptyState(text: isWorkout
                    ? "Aucun exercice.\nTouche « Ajouter un Exercice » pour commencer."
                    : "Cette routine est vide.\nTouche « Ajouter un Exercice » pour la construire.")
                    .listRowBackground(C.bg)
                    .listRowSeparator(.hidden)
            }
            ForEach(Array(d.exercises.enumerated()), id: \.offset) { ei, ex in
                ExSection(ei: ei, ex: ex, isWorkout: isWorkout,
                          onReplace: { replaceIndex = ei },
                          prBadge: $prBadge)
                    .listRowBackground(C.bg)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
            }
            .onMove { from, to in
                guard var dd = repo.draft else { return }
                dd.exercises.move(fromOffsets: from, toOffset: to)
                repo.draft = dd
            }
            Button { showPicker = true } label: {
                HStack(spacing: 10) {
                    Image(systemName: "plus").font(.system(size: 18))
                    Txt(LS("Add Exercise", "Ajouter un Exercice"), weight: 600, size: 15, color: C.accText)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(RoundedRectangle(cornerRadius: 12).fill(accentColor(repo.settings.accent)))
            }
            .buttonStyle(.plain)
            .listRowBackground(C.bg)
            .listRowSeparator(.hidden)
            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
            Color.clear.frame(height: 60)
                .listRowBackground(C.bg)
                .listRowSeparator(.hidden)
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .environment(\.editMode, .constant(.active))
    }
}

extension ExEntry {
    var id: String { name }
}

struct LiveStat: View {
    @EnvironmentObject var repo: Repo
    let value: String
    let label: String
    var accent = false
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Txt(label, weight: 500, size: 13, color: C.mut)
            Txt(value, weight: 800, size: 21, color: accent ? accentColor(repo.settings.accent) : C.text)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/** One exercise section (Hevy: no card box, directly on black). */
struct ExSection: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    let ei: Int
    @State var ex: ExEntry
    let isWorkout: Bool
    let onReplace: () -> Void
    @Binding var prBadge: (ex: String, muscle: String, kind: String, value: String)?
    @State private var showExNotes = false
    @State private var exNotes = ""
    @State private var showRest = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // title row
            HStack(spacing: 10) {
                if ex.superset {
                    RoundedRectangle(cornerRadius: 2).fill(accentColor(repo.settings.accent)).frame(width: 3, height: 34)
                }
                ExCircle(muscle: ex.muscle, size: 46)
                Button { nav.push(.exerciseDetail(ex.name)) } label: {
                    Txt(exName(ex.name), weight: 700, size: 17, color: accentColor(repo.settings.accent))
                        .multilineTextAlignment(.leading)
                        .lineLimit(2)
                }
                .buttonStyle(.plain)
                Spacer(minLength: 0)
                Menu {
                    Button(LS("Replace exercise", "Remplacer l'exercice"), action: onReplace)
                    Button(LS("Duplicate exercise", "Dupliquer l'exercice")) { duplicateEx() }
                    Button(LS("Rest timer", "Minuteur de repos") + (ex.restSec != nil ? " : \(ex.restSec!)s" : "")) { showRest = true }
                    if !isLast {
                        Button(ex.superset ? LS("Remove superset", "Retirer le superset") : LS("Superset with next", "Superset avec le suivant")) { toggleSuperset() }
                    }
                    Button(LS("Delete exercise", "Supprimer l'exercice"), role: .destructive) { deleteEx() }
                } label: {
                    Image(systemName: "ellipsis").font(.system(size: 16)).foregroundColor(C.mut).padding(6)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            // notes
            Button {
                exNotes = ex.notes
                showExNotes = true
            } label: {
                Group {
                    if ex.notes.isEmpty {
                        Txt(LS("Add notes here…", "Ajouter des notes ici…"), size: 15, color: C.mut)
                    } else {
                        HStack(alignment: .top, spacing: 6) {
                            Image(systemName: "note.text").font(.system(size: 12)).foregroundColor(C.mut)
                            Txt(ex.notes, size: 13.5, color: C.mut)
                                .lineLimit(4)
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 16)
            .padding(.vertical, 6)
            // blue rest line
            Button { showRest = true } label: {
                HStack(spacing: 6) {
                    Image(systemName: "timer").font(.system(size: 13)).foregroundColor(accentColor(repo.settings.accent))
                    Txt("\(LS("Rest", "Repos")): \(fmtRestLabel(ex.restSec ?? repo.settings.restSec))",
                        weight: 500, size: 15, color: accentColor(repo.settings.accent))
                }
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 16)
            .padding(.vertical, 2)
            Spacer().frame(height: 8)
            // column headers
            HStack(spacing: 0) {
                Txt(LS("SET", "SÉRIE"), size: 12, color: C.mut).frame(width: 38)
                if isWorkout {
                    Txt(LS("PREVIOUS", "PRÉCÉDENT"), size: 12, color: C.mut).frame(maxWidth: .infinity)
                }
                HStack(spacing: 3) {
                    Image(systemName: "dumbbell.fill").font(.system(size: 9)).foregroundColor(C.mut)
                    Txt(repo.settings.unit.uppercased(), size: 12, color: C.mut)
                }
                .frame(maxWidth: .infinity)
                Txt(LS("REPS", "RÉPS"), size: 12, color: C.mut).frame(maxWidth: .infinity)
                Color.clear.frame(width: 44)
            }
            .padding(.horizontal, 12)
            Spacer().frame(height: 4)
            let prev = isWorkout ? repo.prevDisplay(ex.name) : nil
            ForEach(Array(ex.sets.enumerated()), id: \.offset) { si, _ in
                SetRow(ei: ei, si: si, ex: $ex, isWorkout: isWorkout, prevText: prev != nil && si < prev!.count ? prev![si] : nil, prBadge: $prBadge)
            }
            // add set
            Button { addSet() } label: {
                HStack(spacing: 8) {
                    Image(systemName: "plus").font(.system(size: 15))
                    Txt(LS("Add Set", "Ajouter une Série"), weight: 500, size: 15)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(RoundedRectangle(cornerRadius: 10).fill(C.card2))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .sheet(isPresented: $showRest) {
            RestSheet(initialSec: ex.restSec ?? repo.settings.restSec) { sec in
                ex.restSec = sec
                writeBack()
                showRest = false
            } onReset: {
                ex.restSec = nil
                writeBack()
                showRest = false
            }
            .environmentObject(repo)
            .presentationDetents([.medium])
        }
        .overlay {
            if showExNotes {
                AlertView(
                    title: exName(ex.name),
                    confirmLabel: LS("Save", "Enregistrer"),
                    dismissLabel: LS("Cancel", "Annuler"),
                    customBody: AnyView(
                        TextField("", text: $exNotes, prompt: Text(LS("Exercise notes…", "Notes de l'exercice…")).foregroundColor(C.mut), axis: .vertical)
                            .font(.inter(400, 15))
                            .foregroundColor(C.text)
                            .frame(height: 80, alignment: .top)
                            .padding(12)
                            .background(RoundedRectangle(cornerRadius: 10).fill(C.bg))
                            .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(C.line2, lineWidth: 1))
                    ),
                    onConfirm: {
                        ex.notes = exNotes
                        writeBack()
                        showExNotes = false
                    },
                    onDismiss: { showExNotes = false }
                )
                .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { showExNotes = false })
            }
        }
    }

    var isLast: Bool {
        guard let d = repo.draft, ei < d.exercises.count else { return true }
        return ei >= d.exercises.count - 1
    }

    func writeBack() {
        guard var d = repo.draft, ei < d.exercises.count else { return }
        d.exercises[ei] = ex
        repo.draft = d
        repo.touchPublic()
    }

    func duplicateEx() {
        guard var d = repo.draft, ei < d.exercises.count else { return }
        d.exercises.insert(ex, at: ei + 1)
        repo.draft = d
        repo.touchPublic()
    }

    func toggleSuperset() {
        ex.superset.toggle()
        writeBack()
    }

    func deleteEx() {
        guard var d = repo.draft, ei < d.exercises.count else { return }
        d.exercises.remove(at: ei)
        repo.draft = d
        repo.touchPublic()
    }

    func addSet() {
        let last = ex.sets.last
        ex.sets.append(SetEntry(kg: last?.kg, reps: last?.reps, done: false))
        writeBack()
    }
}

/** One set row: chip / previous / kg / reps / check. */
struct SetRow: View {
    @EnvironmentObject var repo: Repo
    let ei: Int
    let si: Int
    @Binding var ex: ExEntry
    let isWorkout: Bool
    let prevText: String?
    @Binding var prBadge: (ex: String, muscle: String, kind: String, value: String)?

    var body: some View {
        let s = ex.sets[si]
        let isPr = isWorkout && (s.prW || s.prE)
        return HStack(spacing: 6) {
            Menu {
                Button(LS("Copy set", "Copier la série")) {
                    ex.sets.insert(SetEntry(kg: s.kg, reps: s.reps, done: false), at: si + 1)
                    writeBack()
                }
                Button(LS("Delete set", "Supprimer la série"), role: .destructive) { deleteSet() }
            } label: {
                Group {
                    if isPr {
                        Image(systemName: "trophy.fill").font(.system(size: 17)).foregroundColor(C.gold)
                            .frame(width: 38, height: 38)
                    } else {
                        Txt("\(si + 1)", weight: 600, size: 15)
                            .frame(width: 38, height: 38)
                            .background(RoundedRectangle(cornerRadius: 8).fill(C.card2))
                    }
                }
            }
            if isWorkout {
                Txt(prevText ?? "—", size: 13.5, color: C.mut)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity)
                    .minimumScaleFactor(0.6)
            }
            SetField(text: kgText(s), keyboardType: .decimalPad) { v in
                setKg(v)
            }
            .frame(maxWidth: .infinity)
            SetField(text: s.reps.map(String.init) ?? "", keyboardType: .numberPad) { v in
                let digits = v.filter { $0.isNumber }.prefix(4)
                ex.sets[si].reps = digits.isEmpty ? nil : Int(digits)
                writeBackLight()
            }
            .frame(maxWidth: .infinity)
            Spacer().frame(width: 4)
            if isWorkout {
                Button { toggleDone() } label: {
                    ZStack {
                        if s.done {
                            RoundedRectangle(cornerRadius: 8).fill(C.green).frame(width: 34, height: 34)
                        }
                        Image(systemName: "checkmark").font(.system(size: 16, weight: .semibold))
                            .foregroundColor(s.done ? .white : C.mut)
                    }
                    .frame(width: 44, height: 44)
                }
                .buttonStyle(.plain)
            } else {
                Button { deleteSet() } label: {
                    Image(systemName: "trash").font(.system(size: 15)).foregroundColor(C.mut)
                        .frame(width: 40, height: 40)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 3)
        .background(RoundedRectangle(cornerRadius: 10).fill(isPr ? C.greenBg : Color.clear))
    }

    func kgText(_ s: SetEntry) -> String {
        Calc.fmtKg(s.kg, repo.settings.unit)
    }

    func setKg(_ v: String) {
        ex.sets[si].kg = Calc.toKg(v, repo.settings.unit)
        writeBackLight()
    }

    func writeBack() {
        guard var d = repo.draft, ei < d.exercises.count else { return }
        d.exercises[ei] = ex
        repo.draft = d
        repo.touchPublic()
    }

    func writeBackLight() {
        guard var d = repo.draft, ei < d.exercises.count else { return }
        d.exercises[ei] = ex
        repo.draft = d
    }

    func deleteSet() {
        guard var d = repo.draft, ei < d.exercises.count else { return }
        if d.exercises[ei].sets.count == 1 { d.exercises.remove(at: ei) }
        else { d.exercises[ei].sets.remove(at: si) }
        repo.draft = d
        repo.touchPublic()
    }

    func toggleDone() {
        ex.sets[si].done.toggle()
        if ex.sets[si].done {
            evaluatePr()
            RestTimerModel.shared.setExercise(ex.name, ex.muscle)
            RestTimerModel.shared.start(ex.restSec ?? repo.settings.restSec)
        }
        writeBack()
    }

    /** PR evaluation at check-time, against history strictly before this workout. */
    func evaluatePr() {
        let s = ex.sets[si]
        guard let kg = s.kg, let reps = s.reps, kg > 0, reps > 0 else { return }
        let pr = repo.prFor(ex.name)
        let bestW = pr?.weight ?? 0
        let bestE = pr?.e1rm ?? 0
        let e = Calc.e1rm(kg, reps)
        if kg > bestW && !s.prW {
            ex.sets[si].prW = true
            prBadge = (ex.name, ex.muscle, LS("Heaviest Weight", "Plus Gros Poids"),
                       "\(Calc.fmtKg(kg, repo.settings.unit)) \(Calc.unitLabel(repo.settings.unit))")
        } else if e > bestE && !s.prE {
            ex.sets[si].prE = true
            prBadge = (ex.name, ex.muscle, LS("Best Est. 1RM", "Meilleure Est. 1RM"),
                       "\(Calc.fmtKg(e, repo.settings.unit)) \(Calc.unitLabel(repo.settings.unit))")
        }
    }
}

struct SetField: View {
    @State var text: String
    let hint = "-"
    var keyboardType: UIKeyboardType = .decimalPad
    let onChange: (String) -> Void

    var body: some View {
        ZStack {
            if text.isEmpty {
                Txt(hint, size: 15, color: C.mut)
            }
            TextField("", text: $text)
                .font(.inter(600, 16))
                .foregroundColor(C.text)
                .keyboardType(keyboardType)
                .multilineTextAlignment(.center)
                .tint(C.accent)
                .onChange(of: text) { v in onChange(v) }
                .frame(height: 44)
        }
        .background(RoundedRectangle(cornerRadius: 10).fill(C.card2))
        .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(C.line2, lineWidth: 1))
    }
}

/** Hevy rest picker: native wheel + Terminé. */
struct RestSheet: View {
    @EnvironmentObject var repo: Repo
    let initialSec: Int
    let onDone: (Int) -> Void
    let onReset: () -> Void

    static let values: [Int] = Array(stride(from: 15, through: 600, by: 5))

    var body: some View {
        VStack(spacing: 8) {
            Txt(LS("Rest Timer", "Minuteur de Repos"), weight: 800, size: 17)
                .padding(.top, 12)
            Txt("\(LS("Rest", "Repos")): \(fmtRestLabel(initialSec.clamped(15, 600)))", weight: 800, size: 26, color: accentColor(repo.settings.accent))
            Picker("", selection: Binding(get: { initialSec }, set: { onDone($0) })) {
                ForEach(Self.values, id: \.self) { v in
                    Text(fmtRestLabel(v)).tag(v)
                        .font(.inter(400, 15))
                        .foregroundColor(C.mut)
                }
            }
            .pickerStyle(.wheel)
            .frame(height: 220)
            .colorMultiply(.white)
            HStack {
                Button(action: onReset) {
                    Txt(LS("Use default", "Par défaut"), weight: 600, size: 14, color: C.mut)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)
                Button { onDone(initialSec) } label: {
                    Txt(LS("Done", "Terminé"), weight: 700, size: 15, color: C.accText)
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(Capsule().fill(accentColor(repo.settings.accent)))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 18)
        }
        .background(C.card)
    }
}

extension Int {
    func clamped(_ lo: Int, _ hi: Int) -> Int { Swift.min(hi, Swift.max(lo, self)) }
}

/** Exercise picker sheet. */
struct ExercisePicker: View {
    @EnvironmentObject var repo: Repo
    let onPick: (String) -> Void
    @State private var q = ""
    @State private var mus = "All"
    @Environment(\.dismiss) private var dismiss

    var filtered: [ExerciseDef] {
        ExData.all
            .filter { (mus == "All" || $0.muscle == mus) && matches(q, $0.name) }
            .sorted { exName($0.name) < exName($1.name) }
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Txt(LS("Select exercises", "Choisir des exercices"), weight: 700, size: 16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button("OK") { dismiss() }
                    .font(.inter(700, 14))
                    .foregroundColor(accentColor(repo.settings.accent))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
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
            .background(RoundedRectangle(cornerRadius: 12).fill(C.card2))
            .padding(.horizontal, 16)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(["All"] + ExData.muscles, id: \.self) { m in
                        Chip(label: m == "All" ? LS("All", "Tous") : muscleName(m), selected: mus == m) { mus = m }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
            ScrollView {
                LazyVStack(spacing: 0) {
                    if filtered.isEmpty {
                        EmptyState(text: LS("No exercises found.", "Aucun exercice trouvé."), slim: true)
                    }
                    ForEach(filtered, id: \.name) { e in
                        Button { onPick(e.name) } label: {
                            HStack(spacing: 14) {
                                IllIcon(muscle: e.muscle, size: 46)
                                VStack(alignment: .leading, spacing: 1) {
                                    Txt(exName(e.name), weight: 700, size: 15)
                                        .multilineTextAlignment(.leading)
                                    Txt("\(muscleName(e.muscle)) · \(equipName(e.equip))", size: 12.5, color: C.mut)
                                }
                                Spacer()
                                Circle().fill(accentColor(repo.settings.accent)).frame(width: 30, height: 30)
                                    .overlay(Image(systemName: "plus").font(.system(size: 14)).foregroundColor(C.accText))
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .background(C.card)
    }
}
