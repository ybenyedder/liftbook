import SwiftUI
import LiftbookCore

/** Post-workout recap — port of WorkoutSummary.kt. */
struct WorkoutSummaryView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    @State private var name = ""
    @State private var showSaveRoutine = false

    var body: some View {
        Group {
            if let d = repo.draft, d.mode == "workout", d.startedAt != nil {
                content(d: d)
            } else {
                Color.clear.onAppear { nav.pop() }
            }
        }
    }

    func content(d: Draft) -> some View {
        let unit = repo.settings.unit
        var vol = 0.0
        var repsCount = 0
        var setCount = 0
        for ex in d.exercises {
            for s in ex.sets where s.done && s.kg != nil && s.reps != nil {
                vol += s.kg! * Double(s.reps!)
                repsCount += s.reps!
                setCount += 1
            }
        }
        // records are evaluated against history strictly before this session
        var prs: [PrRec] = []
        for ex in d.exercises {
            let done = ex.sets.filter { ($0.kg ?? 0) > 0 && ($0.reps ?? 0) > 0 && $0.done }
            guard !done.isEmpty else { continue }
            let prev = repo.prFor(ex.name)
            let pw = prev?.weight ?? 0
            let pe = prev?.e1rm ?? 0
            let bw = done.map { $0.kg! }.max()!
            let be = done.map { Calc.e1rm($0.kg!, $0.reps!) }.max()!
            if bw > pw { prs.append(PrRec(ex: ex.name, kind: "Weight", value: bw)) }
            if be > pe { prs.append(PrRec(ex: ex.name, kind: "Est. 1RM", value: be)) }
        }
        let muscles = Set(d.exercises.map { $0.muscle })
        return VStack(spacing: 0) {
            HStack {
                Button { nav.pop() } label: {
                    Image(systemName: "chevron.left").font(.system(size: 20)).foregroundColor(C.text).padding(8)
                }
                .buttonStyle(.plain)
                Txt(LS("Workout Summary", "Résumé de la séance"), weight: 800, size: 17)
                    .frame(maxWidth: .infinity)
                Color.clear.frame(width: 48)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
            ScrollView {
                TextField("", text: $name)
                    .font(.inter(700, 17))
                    .foregroundColor(C.text)
                    .tint(C.accent)
                    .padding(.horizontal, 12)
                    .frame(height: 52)
                    .background(RoundedRectangle(cornerRadius: 11).fill(C.card))
                    .overlay(RoundedRectangle(cornerRadius: 11).strokeBorder(C.accent, lineWidth: 1))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 6)
                HStack(alignment: .top, spacing: 8) {
                    SummaryStat(value: Calc.fmtDur(Date.now.timeIntervalSince1970 - d.startedAt!), label: LS("Duration", "Durée"), accent: true)
                    SummaryStat(value: "\(Calc.fmtVol(vol, unit))\(Calc.unitLabel(unit))", label: LS("Volume", "Volume"))
                    SummaryStat(value: "\(setCount)", label: LS("Sets", "Séries"))
                    BodyMap(front: true, muscles: muscles)
                    BodyMap(front: false, muscles: muscles)
                }
                .padding(.horizontal, 16)
                if !prs.isEmpty {
                    HStack(spacing: 6) {
                        Image(systemName: "trophy.fill").font(.system(size: 15)).foregroundColor(C.gold)
                        Txt(LS("RECORDS", "RECORDS"), weight: 800, size: 13, color: C.gold)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                ForEach(Array(prs.enumerated()), id: \.offset) { _, p in
                    HStack(spacing: 8) {
                        Image(systemName: "trophy.fill").font(.system(size: 12)).foregroundColor(C.gold)
                        Txt(exName(p.ex), size: 13.5).lineLimit(1)
                        Spacer()
                        Txt("\(p.kind == "Weight" ? LS("Heaviest Weight", "Plus Gros Poids") : LS("Best Est. 1RM", "Meilleure Est. 1RM")) · \(Calc.fmtKg(p.value, unit))\(Calc.unitLabel(unit))",
                            weight: 700, size: 12, color: C.orange)
                            .lineLimit(1)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 9)
                    .background(RoundedRectangle(cornerRadius: 10).fill(Color(hex: 0x1F3B2C)))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 3)
                }
                Txt(LS("Exercises", "Exercices").uppercased(), weight: 800, size: 13, color: C.mut)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .frame(maxWidth: .infinity, alignment: .leading)
                ForEach(Array(d.exercises.enumerated()), id: \.offset) { _, ex in
                    let doneSets = ex.sets.filter { $0.done && ($0.kg != nil || $0.reps != nil) }
                    if !doneSets.isEmpty {
                        VStack(alignment: .leading, spacing: 3) {
                            Txt(exName(ex.name), weight: 700, size: 15.5, color: C.accent)
                            ForEach(Array(doneSets.enumerated()), id: \.offset) { i, s in
                                HStack(spacing: 6) {
                                    Txt("\(i + 1)", size: 13, color: C.mut).frame(width: 20, alignment: .leading)
                                    Txt("\(s.kg != nil ? Calc.fmtKg(s.kg, unit) + Calc.unitLabel(unit) : "—") × \(s.reps.map(String.init) ?? "—")",
                                        weight: 600, size: 13.5)
                                    Spacer()
                                    if s.prW || s.prE {
                                        Image(systemName: "trophy.fill").font(.system(size: 11)).foregroundColor(C.gold)
                                    }
                                }
                                .padding(.horizontal, 10)
                                .padding(.vertical, 8)
                                .background(RoundedRectangle(cornerRadius: 8).fill(s.prW || s.prE ? Color(hex: 0x1F3B2C) : C.card))
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                    }
                }
                if !d.notes.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        HStack(spacing: 6) {
                            Image(systemName: "note.text").font(.system(size: 12)).foregroundColor(C.mut)
                            Txt(LS("Notes", "Notes").uppercased(), weight: 800, size: 13, color: C.mut)
                        }
                        Txt(d.notes, size: 13.5)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                Spacer().frame(height: 16)
            }
            Button {
                if repo.draftDiffersFromRoutine() { showSaveRoutine = true } else { doFinish() }
            } label: {
                Txt(LS("TERMINER", "TERMINER"), weight: 700, size: 15, color: C.accText)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Capsule().fill(C.accent))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 16)
            .padding(.bottom, 12)
        }
        .background(C.bg)
        .onAppear { name = d.name }
        .overlay {
            if showSaveRoutine {
                AlertView(
                    title: LS("Routine modified", "Routine modifiée"),
                    message: LS("Save these changes for your next sessions?",
                                "Enregistrer les modifications pour les prochaines séances ?"),
                    confirmLabel: LS("Save", "Enregistrer"),
                    dismissLabel: LS("Skip", "Ignorer"),
                    onConfirm: {
                        showSaveRoutine = false
                        repo.updateRoutineFromDraft()
                        doFinish()
                    },
                    onDismissPress: {
                        showSaveRoutine = false
                        doFinish()
                    },
                    onDismiss: {
                        showSaveRoutine = false
                        doFinish()
                    }
                )
                .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { showSaveRoutine = false; doFinish() })
            }
        }
    }

    func doFinish() {
        let w = repo.finishWorkout(name)
        RestTimerModel.shared.clear()
        RestTimerModel.shared.cancelWorkout()
        nav.toTab(0)
        if let w, !w.prs.isEmpty {
            repo.toast("\(LS("Workout saved", "Séance enregistrée")) · \(w.prs.count) \(LS("PRs", "records")) !")
        } else {
            repo.toast(LS("Workout saved", "Séance enregistrée"))
        }
    }
}

struct SummaryStat: View {
    let value: String
    let label: String
    var accent = false
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Txt(label, weight: 500, size: 13, color: C.mut)
            Txt(value, weight: 800, size: 21, color: accent ? C.accent : C.text)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
