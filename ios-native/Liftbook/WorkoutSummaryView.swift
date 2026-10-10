import SwiftUI
import Foundation
import LiftbookCore
import PhotosUI
import UIKit

/** Post-workout recap — port of WorkoutSummary.kt.
    Split into small sub-views: Xcode's type-checker times out on one giant ViewBuilder body. */
struct WorkoutSummaryView: View {
    static func computeStats(_ d: Draft) -> (Double, Int, Int) {
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
        return (vol, repsCount, setCount)
    }

    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    @State private var name = ""
    @State private var showSaveRoutine = false
    @State private var frozenDuration = ""   // figée à l'ouverture (WorkoutSummary.kt)
    @State private var addedPhotoId: Double?
    @State private var photoItem: PhotosPickerItem?

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
        VStack(spacing: 0) {
            topBar
            ScrollView {
                nameField
                statsRow(d: d)
                recordsSection(d: d)
                exercisesSection(d: d)
                notesSection(d: d)
                photoSection
                Spacer().frame(height: 16)
            }
            finishButton
        }
        .background(C.bg)
        .onAppear {
            name = d.name
            frozenDuration = Calc.fmtDur(Date.now.timeIntervalSince1970 * 1000 - d.startedAt!)
        }
        .overlay {
            if showSaveRoutine {
                saveAlert
            }
        }
    }

    var topBar: some View {
        HStack {
            Button { nav.pop() } label: {
                Image(systemName: "chevron.left").font(.system(size: 20)).foregroundColor(C.text).padding(8)
            }
            .buttonStyle(.plain)
            Txt(LS("Workout Summary", "Résumé de séance"), weight: 800, size: 17)
                .frame(maxWidth: .infinity)
            Color.clear.frame(width: 48)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
    }

    var nameField: some View {
        TextField("", text: $name)
            .font(.inter(700, 17))
            .foregroundColor(C.text)
            .tint(C.accent)
            .padding(.horizontal, 12)
            .frame(height: 52)
            .background(C.card, in: RoundedRectangle(cornerRadius: 11))
            .overlay(RoundedRectangle(cornerRadius: 11).strokeBorder(C.accent, lineWidth: 1))
            .padding(.horizontal, 16)
            .padding(.vertical, 6)
    }

    func statsRow(d: Draft) -> some View {
        let unit = repo.settings.unit
        let stats = WorkoutSummaryView.computeStats(d)
        let vol = stats.0
        let setCount = stats.2
        let volText = Calc.fmtVol(vol, unit) + Calc.unitLabel(unit)
        let muscles = Set(d.exercises.map { $0.muscle })
        return HStack(alignment: .top, spacing: 8) {
            SummaryStat(value: frozenDuration, label: LS("Duration", "Durée"), accent: true)
            SummaryStat(value: volText, label: LS("Volume", "Volume"))
            SummaryStat(value: "\(setCount)", label: LS("Sets", "Séries"))
            BodyMap(front: true, muscles: muscles)
            BodyMap(front: false, muscles: muscles)
        }
        .padding(.horizontal, 16)
    }

    func recordsSection(d: Draft) -> some View {
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
        return Group {
            if !prs.isEmpty {
                HStack(spacing: 6) {
                    Image(systemName: "trophy.fill").font(.system(size: 15)).foregroundColor(C.gold)
                    Txt(LS("RECORDS", "RECORDS"), weight: 800, size: 13, color: C.gold)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity, alignment: .leading)
                ForEach(Array(prs.enumerated()), id: \.offset) { _, p in
                    prRow(p)
                }
            }
        }
    }

    func prRow(_ p: PrRec) -> some View {
        let unit = repo.settings.unit
        let kindText = p.kind == "Weight" ? LS("Heaviest Weight", "Plus Gros Poids") : LS("Best Est. 1RM", "Meilleure Est. 1RM")
        let valueText = kindText + " · " + Calc.fmtKg(p.value, unit) + Calc.unitLabel(unit)
        return HStack(spacing: 8) {
            Image(systemName: "trophy.fill").font(.system(size: 12)).foregroundColor(C.gold)
            Txt(exName(p.ex), size: 13.5).lineLimit(1)
            Spacer()
            Txt(valueText, weight: 700, size: 12, color: C.orange)
                .lineLimit(1)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 9)
        .background(Color(hex: 0x1F3B2C), in: RoundedRectangle(cornerRadius: 10))
        .padding(.horizontal, 16)
        .padding(.vertical, 3)
    }

    func exercisesSection(d: Draft) -> some View {
        VStack(spacing: 0) {
            Txt(LS("Exercises", "Exercices").uppercased(), weight: 800, size: 13, color: C.mut)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity, alignment: .leading)
            ForEach(Array(d.exercises.enumerated()), id: \.offset) { _, ex in
                exerciseBlock(ex)
            }
        }
    }

    func exerciseBlock(_ ex: ExEntry) -> some View {
        let doneSets = ex.sets.filter { $0.done && ($0.kg != nil || $0.reps != nil || $0.mins != nil || $0.km != nil) }
        let cardio = ex.muscle == "Cardio" || Calc.isCardioName(ex.name)
        return Group {
            if !doneSets.isEmpty {
                VStack(alignment: .leading, spacing: 3) {
                    Txt(exName(ex.name), weight: 700, size: 15.5, color: C.accent)
                    ForEach(Array(doneSets.enumerated()), id: \.offset) { i, s in
                        setRow(i: i, s: s, cardio: cardio)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
        }
    }

    func setRow(i: Int, s: SetEntry, cardio: Bool) -> some View {
        let unit = repo.settings.unit
        let valueText = cardio
            ? Calc.fmtCardioSet(s.mins, s.km)
            : "\(s.kg != nil ? Calc.fmtKg(s.kg, unit) + Calc.unitLabel(unit) : "—") × \(s.reps.map(String.init) ?? "—")"
        let isPr = s.prW || s.prE
        return HStack(spacing: 6) {
            Txt("\(i + 1)", size: 13, color: C.mut).frame(width: 20, alignment: .leading)
            Txt(valueText, weight: 600, size: 13.5)
            Spacer()
            if isPr {
                Image(systemName: "trophy.fill").font(.system(size: 11)).foregroundColor(C.gold)
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(isPr ? Color(hex: 0x1F3B2C) : C.card, in: RoundedRectangle(cornerRadius: 8))
    }

    func notesSection(d: Draft) -> some View {
        Group {
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
        }
    }

    /* ---- PHOTO DE PROGRESSION (WorkoutSummary.kt:251-322) ---- */
    var photoSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                Image(systemName: "camera").font(.system(size: 12)).foregroundColor(C.mut)
                Txt(LS("PROGRESS PHOTO", "PHOTO DE PROGRESSION"), weight: 800, size: 13, color: C.mut)
            }
            if let pid = addedPhotoId, repo.photoById(pid) != nil {
                HStack(alignment: .top, spacing: 12) {
                    ZStack(alignment: .topTrailing) {
                        Group {
                            if let img = repo.photoImage(pid) {
                                Image(uiImage: img).resizable().scaledToFill()
                            } else {
                                RoundedRectangle(cornerRadius: 12).fill(C.card2)
                            }
                        }
                        .frame(width: 120, height: 120)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        Button {
                            repo.deletePhoto(id: pid)
                            addedPhotoId = nil
                        } label: {
                            Image(systemName: "xmark").font(.system(size: 9, weight: .bold)).foregroundColor(.white)
                                .frame(width: 22, height: 22)
                                .background(Circle().fill(Color.black.opacity(0.8)))
                        }
                        .buttonStyle(.plain)
                        .padding(5)
                    }
                    Txt(LS("Saved to your progress gallery — compare it later with your previous photos.",
                           "Ajoutée à ta galerie progression — compare-la plus tard avec tes anciennes photos."),
                        size: 12, color: C.mut)
                }
            } else {
                PhotosPicker(selection: $photoItem, matching: .images) {
                    HStack(spacing: 8) {
                        Image(systemName: "camera").font(.system(size: 15)).foregroundColor(C.accent)
                        Txt(LS("Add a photo of your physique", "Ajouter une photo de ton physique"), weight: 600, size: 13.5, color: C.accent)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 18)
                    .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(C.line, lineWidth: 1))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .onChange(of: photoItem) { item in
            guard let item else { return }
            Task {
                guard let data = try? await item.loadTransferable(type: Data.self),
                      let jpeg = downscaleJpeg(data) else {
                    repo.toast(LS("Could not read this image", "Impossible de lire cette image"))
                    photoItem = nil
                    return
                }
                addedPhotoId = repo.addPhoto(jpeg: jpeg, ts: Date.now.timeIntervalSince1970 * 1000)
                photoItem = nil
            }
        }
    }

    /** ≤1440 px de côté long, JPEG 0.86 (WorkoutSummary/Repo Android). */
    private func downscaleJpeg(_ data: Data) -> Data? {
        guard let src = UIImage(data: data) else { return nil }
        let maxDim: CGFloat = 1440
        let scale = min(1, maxDim / max(src.size.width, src.size.height))
        let size = CGSize(width: src.size.width * scale, height: src.size.height * scale)
        let fmt = UIGraphicsImageRendererFormat()
        fmt.scale = 1
        let renderer = UIGraphicsImageRenderer(size: size, format: fmt)
        let img = renderer.image { _ in src.draw(in: CGRect(origin: .zero, size: size)) }
        return img.jpegData(compressionQuality: 0.86)
    }

    var finishButton: some View {
        Button {
            if repo.draftDiffersFromRoutine() { showSaveRoutine = true } else { doFinish() }
        } label: {
            Txt(LS("TERMINER", "TERMINER"), weight: 700, size: 15, color: C.accText)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(C.accent, in: Capsule())
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 16)
        .padding(.bottom, 12)
    }

    var saveAlert: some View {
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
            onDismiss: {
                showSaveRoutine = false
                doFinish()
            },
            onDismissPress: {
                showSaveRoutine = false
                doFinish()
            }
        )
        .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { showSaveRoutine = false; doFinish() })
    }

    func doFinish() {
        let w = repo.finishWorkout(name)
        if let pid = addedPhotoId, let w {
            repo.linkPhotoToWorkout(photoId: pid, wId: Double(w.id))
        }
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
