import SwiftUI
import Foundation
import LiftbookCore

/** Navigation stack + tab bar + global undo bar + toast — port of App.kt / MainActivity. */

enum Screen: Hashable {
    case settings
    case workoutDetail(Int)
    case exerciseDetail(String)
    case routineDetail(Int)
    case history
    case exercises
    case logger
    case workoutSummary
}

@MainActor
final class Nav: ObservableObject {
    @Published var tab = 0   // 0 Accueil, 1 Entraînement, 2 Profil
    @Published var stack: [Screen] = []
    @Published var deletedUndo: Workout?
    var pendingStartEmpty = false

    var atTab: Bool { stack.isEmpty }
    func push(_ s: Screen) { stack.append(s) }
    func pop() { if !stack.isEmpty { stack.removeLast() } }
    func toTab(_ i: Int) { tab = i; stack = [] }
}

struct RootView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav

    var body: some View {
        ZStack(alignment: .bottom) {
            currentScreen
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            if nav.atTab { tabBar }
            undoBar
            toastHost
        }
        .background(C.bg.ignoresSafeArea())
        .preferredColorScheme(.dark)
        .onAppear(perform: onAppear)
        .onChange(of: nav.stack) { _ in
            if nav.current != .logger, repo.draft == nil || repo.draft?.startedAt == nil {
                RestTimerModel.shared.cancelWorkout()
            }
        }
        .onOpenURL { url in handleUrl(url) }
    }

    func handleUrl(_ url: URL) {
        // Google OAuth deep link: liftbook://auth-callback?code=…
        guard url.host == "auth-callback" else { return }
        let comps = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let items = comps?.queryItems ?? []
        if let err = items.first(where: { $0.name == "error_description" || $0.name == "error" })?.value {
            repo.googlePending = false
            repo.authError = String(err.prefix(120))
            return
        }
        guard let code = items.first(where: { $0.name == "code" })?.value else {
            repo.googlePending = false
            repo.authError = LS("Connexion Google incomplète — réessaie.", "Connexion Google incomplète — réessaie.")
            return
        }
        let verifier = UserDefaults.standard.string(forKey: "pkce_verifier") ?? ""
        let issuedAt = UserDefaults.standard.double(forKey: "pkce_ts")
        UserDefaults.standard.removeObject(forKey: "pkce_verifier")
        UserDefaults.standard.removeObject(forKey: "pkce_ts")
        repo.googlePending = false
        guard !verifier.isEmpty, Date.now.timeIntervalSince1970 - issuedAt < 600 else {
            repo.authError = "Session de connexion expirée — réessaie."
            return
        }
        Task {
            do {
                let s = try await repo.client.exchangePkce(code: code, verifier: verifier)
                await repo.completeGoogleSession(s)
            } catch {
                repo.authError = LS("Connexion Google échouée — réessaie.", "Connexion Google échouée — réessaie.")
            }
        }
    }

    func onAppear() {
        if nav.pendingStartEmpty {
            nav.pendingStartEmpty = false
            if repo.draft == nil { repo.startWorkout(nil) }
            nav.push(.logger)
        }
    }

    @ViewBuilder
    var currentScreen: some View {
        if !nav.atTab, let top = nav.stack.last {
            switch top {
            case .settings: SettingsView()
            case .workoutDetail(let id): WorkoutDetailView(id: id)
            case .exerciseDetail(let name): ExerciseDetailView(name: name)
            case .routineDetail(let id): RoutineDetailView(id: id)
            case .history: HistoryView()
            case .exercises: ExercisesView()
            case .logger: LoggerView()
            case .workoutSummary: WorkoutSummaryView()
            }
        } else {
            switch nav.tab {
            case 0: HomeView()
            case 1: TrainingView()
            default: ProfileView()
            }
        }
    }

    var tabBar: some View {
        VStack(spacing: 0) {
            Rectangle().fill(C.line).frame(height: 0.5)
            HStack {
                tabButton(0, label: LS("Home", "Accueil"), icon: "house.fill")
                tabButton(1, label: LS("Training", "Entraînement"), icon: "dumbbell.fill")
                tabButton(2, label: LS("Profile", "Profil"), icon: "person.fill")
            }
            .frame(height: 52)
        }
        .background(C.navBar.ignoresSafeArea(edges: .bottom))
    }

    func tabButton(_ idx: Int, label: String, icon: String) -> some View {
        Button {
            nav.toTab(idx)
        } label: {
            VStack(spacing: 3) {
                Image(systemName: icon).font(.system(size: 21))
                Text(label).font(.inter(500, 11))
            }
            .foregroundColor(nav.tab == idx ? accentCol(repo.settings.accent) : C.mut)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    var undoBar: some View {
        if let victim = nav.deletedUndo {
            VStack {
                Spacer()
                HStack {
                    Txt(LS("Workout deleted", "Séance supprimée"), size: 14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        repo.restoreWorkout(victim)
                        nav.deletedUndo = nil
                    } label: {
                        Txt(LS("UNDO", "ANNULER"), weight: 700, size: 14, color: accentCol(repo.settings.accent))
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(C.card2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                .padding(.horizontal, 16)
                .padding(.bottom, 76)
            }
            .onAppear {
                Task {
                    try? await Task.sleep(nanoseconds: 5_000_000_000)
                    if nav.deletedUndo?.id == victim.id { nav.deletedUndo = nil }
                }
            }
        }
    }

    @ViewBuilder
    var toastHost: some View {
        if let msg = repo.toastMessage {
            VStack {
                Spacer()
                Text(msg)
                    .font(.inter(400, 13.5))
                    .foregroundColor(C.text)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(C.card2, in: Capsule())
                    .padding(.bottom, 90)
            }
            .allowsHitTesting(false)
        }
    }
}

extension Nav {
    var current: Screen? { stack.last }
}
