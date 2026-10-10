import SwiftUI
import Foundation
import UIKit
import PhotosUI
import Charts
import LiftbookCore

/** Progress photos: gallery + body-weight chart + viewer + before/after compare — port of Progress.kt (v1.47→v1.50).
    Split into small sub-views: Xcode's type-checker times out on one giant ViewBuilder body.

    NOTE gestes/pager : DragGesture, onLongPressGesture, TabView(.page) et UIGraphicsImageRenderer
    n'existent pas dans les stubs Linux — ces portions vivent sous `#if os(iOS)` (vrai côté Xcode,
    faux côté typecheck Linux qui vérifie alors la branche de repli). */

// ============================ dates (local, FR — Progress.kt fmtDay/fmtDayLong) ============================

fileprivate func fmtDay(_ ts: Double) -> String {
    let f = DateFormatter()
    f.locale = Locale(identifier: "fr_FR")
    f.dateFormat = "d MMM yy"
    return f.string(from: Date(timeIntervalSince1970: ts / 1000))
}

fileprivate func fmtDayLong(_ ts: Double) -> String {
    let f = DateFormatter()
    f.locale = Locale(identifier: "fr_FR")
    f.dateFormat = "EEEE d MMMM yyyy"
    let s = f.string(from: Date(timeIntervalSince1970: ts / 1000))
    return s.prefix(1).uppercased() + s.dropFirst()
}

/// Calendar-day gap between two timestamps (a 22h→9h overnight pair is 2 days apart, not "same day") — v1.50 fix.
fileprivate func calendarDaysBetween(_ fromTs: Double, _ toTs: Double) -> Int {
    let cal = Calendar(identifier: .gregorian)
    let from = cal.date(from: Calc.localDate(fromTs)) ?? Date(timeIntervalSince1970: fromTs / 1000)
    let to = cal.date(from: Calc.localDate(toTs)) ?? Date(timeIntervalSince1970: toTs / 1000)
    return cal.dateComponents([.day], from: from, to: to).day ?? 0
}

/// Downscale + JPEG-compress a picked image (long side ≤ maxSide, quality 0.86) — Android addPhotoFromUri.
enum PhotoDownscale {
    static func jpeg(_ img: UIImage, maxSide: CGFloat) -> Data? {
        #if os(iOS)
        let size = img.size
        let long = max(size.width, size.height)
        let scale = long > maxSide ? maxSide / long : 1
        let target = CGSize(width: floor(size.width * scale), height: floor(size.height * scale))
        let fmt = UIGraphicsImageRendererFormat.default()
        fmt.scale = 1
        return UIGraphicsImageRenderer(size: target, format: fmt).image { _ in
            img.draw(in: CGRect(x: 0, y: 0, width: target.width, height: target.height))
        }.jpegData(compressionQuality: 0.86)
        #else
        return nil // typecheck stub : UIImage n'a pas de pixels ici, le rendu réel passe par iOS
        #endif
    }
}

/// Square grid cell (1:1, overflowing fill clipped) — aspectRatio côté iOS, frame fixe pour le typecheck stub.
extension View {
    fileprivate func progressPhotoSquare() -> some View {
        #if os(iOS)
        return self.aspectRatio(1, contentMode: .fill).clipped()
        #else
        return self.frame(width: 110, height: 110)
        #endif
    }
}

// ============================ photo cell ============================

/// Photo tile; dark card + camera icon while loading/evicted (PhotoImg de Progress.kt).
fileprivate struct PhotoImg: View {
    @EnvironmentObject var repo: Repo
    let p: ProgressPhoto
    var fit = false   // viewer: ContentScale.Fit — ailleurs Crop
    var body: some View {
        ZStack {
            if let img = repo.photoImage(p.id) {
                Group {
                    if fit {
                        Image(uiImage: img).resizable().scaledToFit()
                    } else {
                        Image(uiImage: img).resizable().scaledToFill()
                    }
                }
            } else {
                Image(systemName: "camera.fill")
                    .font(.system(size: 26))
                    .foregroundColor(C.mut2)
            }
        }
        .background(C.card2)
    }
}

// ============================ gallery ============================

/** Photos grid most-recent-first, body-weight chart, long-press to pick two and compare. */
struct ProgressScreen: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    @State private var pickerItem: PhotosPickerItem? = nil
    @State private var selecting = false
    @State private var selected: [Double] = []

    var photos: [ProgressPhoto] { repo.photosDesc() }
    var weights: [ProgressPhoto] { repo.photos.filter { $0.kg != nil } }   // ts ASC (store trié)

    var body: some View {
        VStack(spacing: 0) {
            header
            content
        }
        .background(C.bg)
        .onChange(of: pickerItem) { item in
            guard let item else { return }
            pickerItem = nil
            Task { await importPicked(item) }
        }
    }

    var header: some View {
        HStack {
            Button { nav.pop() } label: {
                Image(systemName: "chevron.left").font(.system(size: 20)).foregroundColor(C.text).padding(8)
            }
            .buttonStyle(.plain)
            Txt(LS("Progress", "Progression"), weight: 800, size: 18)
                .frame(maxWidth: .infinity, alignment: .leading)
            PhotosPicker(selection: $pickerItem, matching: .images) {
                Image(systemName: "camera.fill")
                    .font(.system(size: 20))
                    .foregroundColor(accentCol(repo.settings.accent))
                    .padding(8)
            }
        }
        .padding(.horizontal, 8)
        .padding(.top, 6)
    }

    @ViewBuilder
    var content: some View {
        if photos.isEmpty {
            VStack {
                Spacer()
                EmptyState(text: LS(
                    "No progress photos yet.\nAdd one after a workout to track your physique.",
                    "Aucune photo de progression.\nAjoute-en une après une séance pour suivre ton physique."
                ))
                Spacer()
            }
            .padding(.horizontal, 32)
        } else {
            ScrollView {
                VStack(spacing: 0) {
                    if weights.count >= 2 { weightChartBlock }
                    gridHead
                    photoGrid
                    Spacer().frame(height: 24)
                }
            }
        }
    }

    // ---- body-weight chart (Swift Charts, AreaMark+LineMark dégradé comme TrainingView) ----

    var weightChartBlock: some View {
        let accent = accentCol(repo.settings.accent)
        let unit = repo.settings.unit
        let vals = weights.compactMap { $0.kg }
        let lo = vals.min() ?? 0
        let hi = vals.max() ?? 0
        return VStack(alignment: .leading, spacing: 0) {
            Txt(LS("Body weight", "Poids corporel"), weight: 800, size: 13, color: C.mut)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            ZStack(alignment: .trailing) {
                Chart(weights, id: \.id) { ph in
                    AreaMark(
                        x: .value("Date", Date(timeIntervalSince1970: ph.ts / 1000)),
                        y: .value("Poids", ph.kg ?? 0)
                    )
                    .foregroundStyle(LinearGradient(colors: [accent.opacity(0.35), .clear], startPoint: .top, endPoint: .bottom))
                    .interpolationMethod(.catmullRom)
                    LineMark(
                        x: .value("Date", Date(timeIntervalSince1970: ph.ts / 1000)),
                        y: .value("Poids", ph.kg ?? 0)
                    )
                    .foregroundStyle(accent)
                    .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round))
                    .interpolationMethod(.catmullRom)
                }
                .chartXAxis(.hidden)
                .chartYAxis(.hidden)
                .frame(height: 150)
                // rail Y max/milieu/min, comme LineChart.kt
                VStack(spacing: 0) {
                    Txt(Calc.fmtKg(hi, unit) + Calc.unitLabel(unit), size: 9.5, color: C.mut)
                    Spacer()
                    Txt(Calc.fmtKg((lo + hi) / 2, unit) + Calc.unitLabel(unit), size: 9.5, color: C.mut)
                    Spacer()
                    Txt(Calc.fmtKg(lo, unit) + Calc.unitLabel(unit), size: 9.5, color: C.mut)
                }
                .frame(height: 150)
                .padding(.trailing, 4)
                .allowsHitTesting(false)
            }
            .padding(.horizontal, 8)
        }
    }

    // ---- "N photos" + hint / bouton comparer ----

    var gridHead: some View {
        HStack {
            Txt("\(photos.count) \(LS("photos", "photos"))", weight: 800, size: 15)
                .frame(maxWidth: .infinity, alignment: .leading)
            if selecting {
                Button {
                    if selected.count == 2 {
                        let byAge = selected.sorted { (repo.photoById($0)?.ts ?? 0) < (repo.photoById($1)?.ts ?? 0) }
                        nav.push(.comparePhotos(byAge[0], byAge[1]))
                    }
                } label: {
                    Txt(selected.count == 2 ? LS("Compare →", "Comparer →") : LS("Select 2 photos", "Choisis 2 photos"),
                        weight: 600, size: 13, color: accentCol(repo.settings.accent))
                }
                .buttonStyle(.plain)
            } else if photos.count >= 2 {
                Txt(LS("Long-press to compare", "Appui long pour comparer"), size: 12, color: C.mut)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    // ---- grille 3 colonnes ----

    var photoGrid: some View {
        LazyVGrid(
            columns: [
                GridItem(.flexible(), spacing: 6),
                GridItem(.flexible(), spacing: 6),
                GridItem(.flexible(), spacing: 6),
            ],
            spacing: 6
        ) {
            ForEach(photos) { p in
                photoCell(p)
            }
        }
        .padding(.horizontal, 12)
    }

    func photoCell(_ p: ProgressPhoto) -> some View {
        let sel = selected.contains(p.id)
        return PhotoImg(p: p)
            .progressPhotoSquare()
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous)
                .strokeBorder(sel ? accentCol(repo.settings.accent) : Color.clear, lineWidth: 2.5))
            .overlay(alignment: .bottomLeading) { dateBadge(p.ts).padding(5) }
            .overlay(alignment: .topTrailing) { checkMark(sel).padding(6) }
            .onTapGesture { handleCellTap(p, sel) }
            #if os(iOS)
            .onLongPressGesture { handleCellLongPress(p, sel) }
            #endif
    }

    func dateBadge(_ ts: Double) -> some View {
        Txt(fmtDay(ts), weight: 600, size: 9.5, color: .white)
            .padding(.horizontal, 5)
            .padding(.vertical, 2)
            .background(Color.black.opacity(0.7), in: RoundedRectangle(cornerRadius: 5, style: .continuous))
    }

    @ViewBuilder
    func checkMark(_ sel: Bool) -> some View {
        if sel {
            ZStack {
                Circle().fill(accentCol(repo.settings.accent))
                Txt("✓", weight: 700, size: 13, color: .white)
            }
            .frame(width: 22, height: 22)
        } else if selecting {
            Circle()
                .strokeBorder(Color.white, lineWidth: 1.5)
                .frame(width: 22, height: 22)
        }
    }

    func handleCellTap(_ p: ProgressPhoto, _ sel: Bool) {
        if selecting {
            if sel { selected.removeAll { $0 == p.id } }
            else if selected.count < 2 { selected.append(p.id) }
        } else {
            nav.push(.photoViewer(p.id))
        }
    }

    func handleCellLongPress(_ p: ProgressPhoto, _ sel: Bool) {
        if !selecting { selecting = true }
        if !sel && selected.count < 2 { selected.append(p.id) }
    }

    // ---- import d'une photo piochée ----

    func importPicked(_ item: PhotosPickerItem) async {
        let bad = LS("Could not read this image", "Impossible de lire cette image")
        guard let data = try? await item.loadTransferable(type: Data.self),
              let img = UIImage(data: data) else {
            repo.toast(bad)
            return
        }
        guard let jpeg = PhotoDownscale.jpeg(img, maxSide: 1440) else {
            repo.toast(bad)
            return
        }
        _ = repo.addPhoto(jpeg: jpeg, ts: Date.now.timeIntervalSince1970)
    }
}

// ============================ viewer ============================

/** Full-screen viewer with swipe, editable note/body weight, and delete. */
struct PhotoViewerScreen: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    let id: Double
    @State private var page: Int? = nil
    @State private var confirmDelete = false
    @State private var edit = false

    var photos: [ProgressPhoto] { repo.photosDesc() }
    var startIdx: Int { photos.firstIndex(where: { $0.id == id }) ?? 0 }
    var curIdx: Int { min(max(page ?? startIdx, 0), max(photos.count - 1, 0)) }
    var currentPhoto: ProgressPhoto? { photos.indices.contains(curIdx) ? photos[curIdx] : nil }

    /// La photo d'origine a été supprimée (ou plus aucune photo) → retour, comme Progress.kt.
    var originGone: Bool { photos.isEmpty || repo.photoById(id) == nil }

    var body: some View {
        Group {
            if originGone {
                Color.clear.onAppear { nav.pop() }
            } else {
                viewerContent
            }
        }
    }

    var viewerContent: some View {
        VStack(spacing: 0) {
            viewerTopBar
            pagerBody
            if let p = currentPhoto { viewerFooter(p) }
        }
        .background(Color.black)
        .overlay {
            if confirmDelete, let p = currentPhoto { deleteDialog(p) }
        }
        .overlay {
            if edit, let p = currentPhoto {
                EditPhotoDialog(p: p, unit: repo.settings.unit, onDone: { edit = false })
            }
        }
        // supprimer la dernière page ramène le pager dans les bornes
        .onChange(of: photos.count) { _ in
            if let pg = page, pg >= photos.count, !photos.isEmpty { page = photos.count - 1 }
        }
    }

    var viewerTopBar: some View {
        HStack {
            Button { nav.pop() } label: {
                Image(systemName: "chevron.left").font(.system(size: 20)).foregroundColor(.white).padding(8)
            }
            .buttonStyle(.plain)
            Txt(photos.count > 1 ? "\(min(curIdx + 1, photos.count))/\(photos.count)" : "",
                weight: 600, size: 14, color: C.mut)
                .frame(maxWidth: .infinity)
            Button { edit = true } label: {
                Image(systemName: "pencil").font(.system(size: 18)).foregroundColor(.white).padding(8)
            }
            .buttonStyle(.plain)
            Button { confirmDelete = true } label: {
                Image(systemName: "trash").font(.system(size: 18)).foregroundColor(.white).padding(8)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 8)
    }

    var pageBinding: Binding<Int> {
        Binding(get: { curIdx }, set: { page = $0 })
    }

    var pagerBody: some View {
        #if os(iOS)
        return TabView(selection: pageBinding) {
            ForEach(Array(photos.enumerated()), id: \.element.id) { i, p in
                PhotoImg(p: p, fit: true).tag(i)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        #else
        // repli typecheck stub : image courante + zones tap gauche/droite
        return ZStack {
            if let p = currentPhoto { PhotoImg(p: p, fit: true) }
            HStack(spacing: 0) {
                Color.clear.frame(maxWidth: .infinity)
                    .onTapGesture { page = max(0, curIdx - 1) }
                Color.clear.frame(maxWidth: .infinity)
                    .onTapGesture { page = min(photos.count - 1, curIdx + 1) }
            }
        }
        #endif
    }

    func viewerFooter(_ p: ProgressPhoto) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Txt(fmtDayLong(p.ts), weight: 700, size: 15, color: .white)
            HStack(spacing: 12) {
                if let kg = p.kg {
                    Txt(LS("Body weight", "Poids corporel") + " : " + Calc.fmtKg(kg, repo.settings.unit) + Calc.unitLabel(repo.settings.unit),
                        size: 13, color: C.mut)
                }
                if let w = p.wId, repo.workoutById(Int(w)) != nil {
                    Txt("· " + LS("linked to a workout", "liée à une séance"), size: 12, color: C.mut2)
                }
            }
            .padding(.top, 2)
            if !p.note.isEmpty {
                Txt(p.note, size: 13, color: C.mut)
                    .lineSpacing(5)
                    .padding(.top, 6)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
    }

    func deleteDialog(_ p: ProgressPhoto) -> some View {
        AlertView(
            title: LS("Delete this photo?", "Supprimer cette photo ?"),
            message: LS("It will be removed from all your devices.", "Elle sera supprimée de tous tes appareils."),
            confirmLabel: LS("Delete", "Supprimer"),
            dismissLabel: LS("Cancel", "Annuler"),
            confirmColor: C.red,
            onConfirm: {
                confirmDelete = false
                repo.deletePhoto(id: p.id)
                if repo.photos.isEmpty { nav.pop() }
            },
            onDismiss: { confirmDelete = false },
            onDismissPress: { confirmDelete = false }
        )
        .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { confirmDelete = false })
    }
}

// ============================ edit dialog ============================

fileprivate struct EditPhotoDialog: View {
    @EnvironmentObject var repo: Repo
    let p: ProgressPhoto
    let onDone: () -> Void
    @State private var note: String
    @State private var kg: String

    init(p: ProgressPhoto, unit: String, onDone: @escaping () -> Void) {
        self.p = p
        self.onDone = onDone
        _note = State(initialValue: p.note)
        // prérempli dans l'UNITÉ D'AFFICHAGE (lb en mode lb) — v1.50 : sauver brut multipliait par 2,2
        _kg = State(initialValue: p.kg.map { Calc.fmtKg($0, unit) } ?? "")
    }

    var body: some View {
        AlertView(
            title: LS("Edit photo", "Modifier la photo"),
            confirmLabel: LS("Save", "Enregistrer"),
            dismissLabel: LS("Cancel", "Annuler"),
            customBody: AnyView(fields),
            onConfirm: save,
            onDismiss: onDone,
            onDismissPress: onDone
        )
        .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { onDone() })
    }

    var fields: some View {
        VStack(alignment: .leading, spacing: 10) {
            Txt(LS("Note", "Note"), size: 13, color: C.mut)
            TextField("", text: $note, axis: .vertical)
                .font(.inter(400, 15))
                .foregroundColor(C.text)
                .tint(C.accent)
                .lineLimit(3)
                .padding(10)
                .background(C.bg, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).strokeBorder(C.line2, lineWidth: 1))
            Txt(LS("Body weight (optional)", "Poids corporel (optionnel)"), size: 13, color: C.mut)
            TextField("", text: $kg)
                .font(.inter(400, 15))
                .foregroundColor(C.text)
                .tint(C.accent)
                .keyboardType(.decimalPad)
                .padding(10)
                .background(C.bg, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).strokeBorder(C.line2, lineWidth: 1))
                .onChange(of: kg) { v in
                    kg = v.filter { $0.isNumber || $0 == "." || $0 == "," }.replacingOccurrences(of: ",", with: ".")
                }
        }
    }

    func save() {
        // resaisi dans l'unité d'affichage → reconversion vers kg
        let parsed = Calc.toKg(kg, repo.settings.unit)
        repo.updatePhoto(id: p.id, note: note, kg: parsed, kgSet: !kg.trimmingCharacters(in: .whitespaces).isEmpty)
        onDone()
    }
}

// ============================ before/after compare ============================

/** Draggable slider (default) or side-by-side, dates, days apart and body-weight delta. */
struct ComparePhotosScreen: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    let aId: Double
    let bId: Double
    @State private var flipped = false
    @State private var mode = 0        // 0 = curseur, 1 = côte à côte
    @State private var fraction: Double = 0.5

    var a: ProgressPhoto? { repo.photoById(aId) }
    var b: ProgressPhoto? { repo.photoById(bId) }

    var body: some View {
        Group {
            if a == nil || b == nil {
                Color.clear.onAppear { nav.pop() }
            } else {
                compareContent
            }
        }
    }

    var older: ProgressPhoto { a!.ts <= b!.ts ? a! : b! }
    var newer: ProgressPhoto { a!.ts <= b!.ts ? b! : a! }
    var before: ProgressPhoto { flipped ? newer : older }
    var after: ProgressPhoto { flipped ? older : newer }

    /// Écart en JOURS CALENDAIRES (diff des minuits locaux) — fix v1.50.
    var days: Int { calendarDaysBetween(before.ts, after.ts) }
    var kgDelta: Double? {
        if let bk = before.kg, let ak = after.kg { return ak - bk }
        return nil
    }

    var compareContent: some View {
        VStack(spacing: 0) {
            compareTopBar
            chipsRow
            GeometryReader { g in
                let w = g.size.width
                Group {
                    if mode == 0 { sliderBox(w) } else { sideBySideBox(w) }
                }
                .frame(width: w, height: g.size.height, alignment: .center)
            }
            .padding(.horizontal, 16)
            infoBandeau
        }
        .background(Color.black)
    }

    var compareTopBar: some View {
        HStack {
            Button { nav.pop() } label: {
                Image(systemName: "chevron.left").font(.system(size: 20)).foregroundColor(.white).padding(8)
            }
            .buttonStyle(.plain)
            Txt(LS("Comparison", "Comparaison"), weight: 800, size: 17, color: .white)
                .frame(maxWidth: .infinity)
            Button {
                flipped.toggle()
                fraction = 1 - fraction
            } label: {
                Image(systemName: "arrow.left.arrow.right").font(.system(size: 18)).foregroundColor(.white).padding(8)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 8)
    }

    var chipsRow: some View {
        HStack(spacing: 8) {
            CompareChip(label: LS("Slider", "Curseur"), active: mode == 0) { mode = 0 }
            CompareChip(label: LS("Side by side", "Côte à côte"), active: mode == 1) { mode = 1 }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }

    // ---- mode curseur : APRÈS plein cadre, AVANT clippé à gauche de la poignée ----

    func sliderBox(_ w: CGFloat) -> some View {
        let h = w / 0.75
        return ZStack(alignment: .topLeading) {
            PhotoImg(p: after)
                .frame(width: w, height: h)
            PhotoImg(p: before)
                .frame(width: w, height: h)
                .frame(width: w * fraction, height: h, alignment: .leading)
                .clipShape(Rectangle())
            // ombre + ligne du curseur
            Rectangle().fill(Color.black.opacity(0.4))
                .frame(width: 1, height: h)
                .offset(x: w * fraction - 4)
            Rectangle().fill(Color.white)
                .frame(width: 2, height: h)
                .offset(x: w * fraction - 1)
            // poignée ronde ⇄
            Txt("⇄", weight: 700, size: 20, color: .white)
                .frame(width: 44, height: 44)
                .background(Color.black.opacity(0.8), in: Circle())
                .offset(x: w * fraction - 22, y: h / 2 - 22)
            // étiquettes AVANT / APRÈS
            compareLabel(LS("BEFORE", "AVANT") + " · " + fmtDay(before.ts))
                .offset(x: 8, y: 8)
            compareLabel(LS("AFTER", "APRÈS") + " · " + fmtDay(after.ts))
                .padding(8)
                .frame(width: w, height: h, alignment: .topTrailing)
        }
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        #if os(iOS)
        .gesture(
            DragGesture(minimumDistance: 0).onChanged { v in
                guard w > 0 else { return }
                fraction = min(0.97, max(0.03, Double(v.location.x / w)))
            }
        )
        #else
        .onTapGesture {
            fraction = min(0.97, max(0.03, fraction > 0.5 ? fraction - 0.15 : fraction + 0.15))
        }
        #endif
    }

    func compareLabel(_ text: String) -> some View {
        Txt(text, weight: 700, size: 11, color: .white)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(Color.black.opacity(0.6), in: RoundedRectangle(cornerRadius: 6, style: .continuous))
    }

    // ---- mode côte à côte ----

    func sideBySideBox(_ w: CGFloat) -> some View {
        let colW = (w - 8) / 2
        return HStack(spacing: 8) {
            VStack(spacing: 6) {
                PhotoImg(p: before)
                    .frame(width: colW, height: colW / 0.75)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                Txt(LS("BEFORE", "AVANT"), weight: 800, size: 11, color: C.mut)
                Txt(fmtDay(before.ts), weight: 600, size: 12, color: .white)
            }
            VStack(spacing: 6) {
                PhotoImg(p: after)
                    .frame(width: colW, height: colW / 0.75)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                Txt(LS("AFTER", "APRÈS"), weight: 800, size: 11, color: C.mut)
                Txt(fmtDay(after.ts), weight: 600, size: 12, color: .white)
            }
        }
        .frame(width: w)
    }

    // ---- bandeau infos : écart + delta poids ----

    var infoBandeau: some View {
        HStack(spacing: 14) {
            Txt(days > 0 ? "\(days) \(LS("days apart", "jours d'écart"))" : LS("Same day", "Le même jour"),
                size: 13, color: C.mut)
            if let d = kgDelta {
                Txt((d >= 0 ? "+" : "−") + Calc.fmtKg(abs(d), repo.settings.unit) + Calc.unitLabel(repo.settings.unit),
                    weight: 700, size: 13, color: d > 0 ? C.red : C.green)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
    }
}

/// Segment "Curseur" / "Côte à côte" — CompareChip de Progress.kt (actif = accent + texte noir).
fileprivate struct CompareChip: View {
    @EnvironmentObject var repo: Repo
    let label: String
    let active: Bool
    let onClick: () -> Void
    var body: some View {
        Button(action: onClick) {
            Txt(label, weight: 600, size: 12.5, color: active ? Color.black : .white)
                .padding(.horizontal, 14)
                .padding(.vertical, 7)
                .background(active ? accentCol(repo.settings.accent) : C.card2, in: Capsule())
        }
        .buttonStyle(.plain)
    }
}
