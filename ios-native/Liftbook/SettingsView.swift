import SwiftUI
import PhotosUI
import UniformTypeIdentifiers
import LiftbookCore

/** Settings — port of Settings.kt (account, profile, units, accent, data import/export, avatar). */
struct SettingsView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    @State private var editName = false
    @State private var editHandle = false
    @State private var confirmWipe = false
    @State private var photoItem: PhotosPickerItem? = nil
    @State private var hevyPicker = false
    @State private var csvPicker = false
    @State private var restorePicker = false
    @State private var exportedFileUrl: URL? = nil

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Button { nav.pop() } label: {
                        Image(systemName: "chevron.left").font(.system(size: 20)).foregroundColor(C.text).padding(8)
                    }
                    .buttonStyle(.plain)
                    Txt(LS("Settings", "Réglages"), weight: 700, size: 18)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 8)

                SectionHeader(LS("ACCOUNT", "COMPTE"))
                if let s = repo.session {
                    PhotosPicker(selection: $photoItem, matching: .images) {
                        HStack(spacing: 14) {
                            AvatarView(letter: String(repo.settings.profileName.prefix(1)).uppercased(), size: 56)
                            VStack(alignment: .leading, spacing: 1) {
                                Txt(LS("Profile photo", "Photo de profil"), size: 15.5)
                                Txt(LS("Tap to change", "Touche pour changer"), size: 12.5, color: C.mut)
                            }
                            Spacer()
                            Image(systemName: "chevron.right").font(.system(size: 14)).foregroundColor(C.mut)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                    }
                    .buttonStyle(.plain)
                    RowDivider()
                    SettingsRow(title: LS("Sync now", "Synchroniser maintenant"), value: repo.syncStatus ?? "") {
                        repo.requestSync(debounce: 0)
                        repo.toast(LS("Syncing…", "Synchronisation…"))
                    }
                    RowDivider()
                    SettingsRow(title: s.email, value: "") {}
                    RowDivider()
                    SettingsRow(title: LS("Sign out", "Se déconnecter"), red: true) {
                        repo.signOut()
                        nav.toTab(2)
                    }
                } else {
                    SettingsRow(title: LS("Sign in or create an account", "Se connecter ou créer un compte"), blue: true) {
                        repo.skipped = false
                        repo.persistMeta()
                    }
                }

                SectionHeader("PROFIL")
                SettingsRow(title: LS("Name", "Nom"), value: repo.settings.profileName) { editName = true }
                RowDivider()
                SettingsRow(title: LS("Username", "Pseudo"), value: "@\(repo.settings.handle)") { editHandle = true }

                SectionHeader(LS("GENERAL", "GÉNÉRAL"))
                SettingsRow(title: LS("Units", "Unités"), value: repo.settings.unit) {
                    repo.setUnit(repo.settings.unit == "kg" ? "lb" : "kg")
                }
                RowDivider()
                SettingsRow(title: LS("Rest timer", "Minuteur de repos"), value: restLabel(repo.settings.restSec)) {
                    let next = [60, 90, 120, 180].first { $0 > repo.settings.restSec } ?? 60
                    repo.setRest(next)
                }
                RowDivider()
                HStack {
                    Txt(LS("Accent color", "Couleur d'accent"), size: 15.5)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    HStack(spacing: 10) {
                        ForEach([("blue", Color(hex: 0x028CFD)), ("teal", Color(hex: 0x20B49A)), ("violet", Color(hex: 0x7C5CFF)), ("orange", Color(hex: 0xFF7A45))], id: \.0) { key, color in
                            Button {
                                repo.setAccent(key)
                            } label: {
                                Circle()
                                    .fill(color)
                                    .frame(width: 26, height: 26)
                                    .overlay(Circle().strokeBorder(repo.settings.accent == key ? C.text : Color.clear, lineWidth: 2.5))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)

                SectionHeader(LS("DATA", "DONNÉES"))
                SettingsRow(title: LS("Import from Hevy (account export)", "Importer depuis Hevy (export du compte)")) { hevyPicker = true }
                RowDivider()
                SettingsRow(title: LS("Import workouts (CSV)", "Importer des séances (CSV)")) { csvPicker = true }
                RowDivider()
                SettingsRow(title: LS("Export workouts (CSV)", "Exporter les séances (CSV)"), action: exportCsv)
                RowDivider()
                SettingsRow(title: LS("Backup data (JSON)", "Sauvegarder les données (JSON)"), action: backupJson)
                RowDivider()
                SettingsRow(title: LS("Restore backup", "Restaurer une sauvegarde")) { restorePicker = true }
                RowDivider()
                SettingsRow(title: LS("Erase all data", "Tout effacer"), red: true) { confirmWipe = true }

                Txt("Liftbook iOS 1.0" + (repo.session != nil ? " · \(LS("synced as", "synchronisé en tant que")) \(repo.session!.email)" : ""),
                    size: 11.5, color: C.mut)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 26)
                    .padding(.bottom, 30)
            }
        }
        .background(C.bg)
        .overlay {
            if editName {
                PromptAlert(initial: repo.settings.profileName, title: LS("Name", "Nom"), confirmLabel: LS("Save", "Enregistrer"), maxLength: 30) { v in
                    if !v.trimmingCharacters(in: .whitespaces).isEmpty { repo.setProfile(v.trimmingCharacters(in: .whitespaces), repo.settings.handle) }
                    editName = false
                } onDismiss: { editName = false }
                .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { editName = false })
            }
            if editHandle {
                PromptAlert(initial: repo.settings.handle, title: LS("Username", "Pseudo"), confirmLabel: LS("Save", "Enregistrer"), prefix: "@", maxLength: 20, filter: { c in c.isLetter || c.isNumber || c == "." || c == "_" }) { v in
                    if !v.trimmingCharacters(in: .whitespaces).isEmpty { repo.setProfile(repo.settings.profileName, v.trimmingCharacters(in: .whitespaces)) }
                    editHandle = false
                } onDismiss: { editHandle = false }
                .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { editHandle = false })
            }
            if confirmWipe {
                AlertView(
                    title: LS("Erase all data?", "Tout effacer ?"),
                    message: LS("All workouts, routines and records will be permanently deleted from this device and your account.",
                                "Toutes les séances, routines et records seront définitivement supprimés de cet appareil et de ton compte."),
                    confirmLabel: LS("Erase", "Effacer"),
                    dismissLabel: LS("Cancel", "Annuler"),
                    confirmColor: C.red,
                    onConfirm: {
                        confirmWipe = false
                        repo.wipe()
                        nav.toTab(0)
                    },
                    onDismiss: { confirmWipe = false }
                )
                .background(Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { confirmWipe = false })
            }
        }
        .onChange(of: photoItem) { item in
            guard let item else { return }
            Task {
                guard let data = try? await item.loadTransferable(type: Data.self) else { return }
                AvatarStore.save(data)
                repo.avatarVersion += 1
                repo.touchPublic()
                if let s = repo.session {
                    if let url = try? await repo.client.uploadAvatar(s, jpeg: data) {
                        repo.setAvatar(url)
                    }
                }
                photoItem = nil
            }
        }
        .fileImporter(isPresented: $hevyPicker, allowedContentTypes: [.plainText, .commaSeparatedText, .text, .data]) { result in
            if case .success(let url) = result { importHevyExport(url: url) }
        }
        .fileImporter(isPresented: $csvPicker, allowedContentTypes: [.plainText, .commaSeparatedText, .text, .data]) { result in
            if case .success(let url) = result { importCsv(url: url) }
        }
        .fileImporter(isPresented: $restorePicker, allowedContentTypes: [.json, .data]) { result in
            if case .success(let url) = result {
                if let text = readText(url), repo.restoreBackup(text) {
                    repo.toast(LS("Backup restored", "Sauvegarde restaurée"))
                } else {
                    repo.toast(LS("Invalid backup file", "Fichier de sauvegarde invalide"))
                }
            }
        }
        .sheet(isPresented: Binding(get: { exportedFileUrl != nil }, set: { if !$0 { exportedFileUrl = nil } })) {
            if let url = exportedFileUrl {
                ShareSheet(items: [url])
            }
        }
    }

    func readText(_ url: URL) -> String? {
        let accessing = url.startAccessingSecurityScopedResource()
        defer { if accessing { url.stopAccessingSecurityScopedResource() } }
        return try? String(contentsOf: url, encoding: .utf8)
    }

    func importHevyExport(url: URL) {
        guard let text = readText(url) else {
            repo.toast(LS("Import failed (check format)", "Import échoué (vérifie le format)"))
            return
        }
        let (ws, rs) = Calc.parseHevyCsv(text)
        if ws.isEmpty && rs.isEmpty {
            let head = text.split(separator: "\n").first.map(String.init) ?? ""
            repo.toast(LS("No Hevy data found — header:", "Aucune donnée Hevy trouvée — en-tête :") + " " + String(head.prefix(90)))
        } else {
            let n = repo.importHevy(ws, rs)
            repo.toast("\(n) \(LS("workouts imported from Hevy (routines rebuilt)", "séances importées (routines reconstruites)"))")
        }
    }

    func importCsv(url: URL) {
        guard let text = readText(url) else {
            repo.toast(LS("Import failed (check format)", "Import échoué (vérifie le format)"))
            return
        }
        var n = repo.importCsv(text)
        if n == 0 {
            let (ws, rs) = Calc.parseHevyCsv(text)
            if !ws.isEmpty || !rs.isEmpty { n = repo.importHevy(ws, rs) }
        }
        repo.toast(n > 0 ? "\(n) \(LS("workouts imported", "séances importées"))" : LS("Nothing imported (check format)", "Rien d'importé (vérifie le format)"))
    }

    func exportCsv() {
        let unit = repo.settings.unit
        var lines = ["Date;Heure;Exercice;Serie;KG;Reps"]
        for w in repo.workouts.sorted(by: { $0.startedAt < $1.startedAt }) {
            let date = Calc.fmtDateShort(w.startedAt)
            let time = Calc.fmtTime(w.startedAt)
            for ex in w.exercises {
                for (i, st) in ex.sets.enumerated() {
                    lines.append("\(date);\(time);\(exName(ex.name).replacingOccurrences(of: ";", with: ","));\(i + 1);\(st.kg != nil ? Calc.fmtKg(st.kg, unit) : "");\(st.reps.map(String.init) ?? "")")
                }
            }
        }
        writeShareFile(name: "hevy-seances.csv", content: lines.joined(separator: "\n"))
    }

    func backupJson() {
        writeShareFile(name: "hevy-sauvegarde.json", content: repo.backupJson())
    }

    func writeShareFile(name: String, content: String) {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(name)
        try? content.write(to: url, atomically: true, encoding: .utf8)
        exportedFileUrl = url
    }

    func restLabel(_ sec: Int) -> String {
        let m = sec / 60
        let r = sec % 60
        return "\(m > 0 ? "\(m)min " : "")\(r)s"
    }

    func SectionHeader(_ text: String) -> some View {
        Txt(text, weight: 700, size: 11, color: C.mut)
            .padding(.horizontal, 16)
            .padding(.top, 24)
            .padding(.bottom, 4)
    }

    func RowDivider() -> some View {
        Rectangle().fill(C.card).frame(height: 0.5).padding(.leading, 16)
    }

    struct SettingsRow: View {
        @EnvironmentObject var repo: Repo
        let title: String
        var value: String = ""
        var red = false
        var blue = false
        var action: (() -> Void)? = nil
        var body: some View {
            Button(action: { action?() }) {
                HStack(spacing: 8) {
                    Txt(title, size: 15.5, color: red ? C.red : blue ? C.accent : C.text)
                        .lineLimit(1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if !value.trimmingCharacters(in: .whitespaces).isEmpty {
                        Txt(value, size: 13.5, color: C.mut)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 15)
            }
            .buttonStyle(.plain)
        }
    }
}
