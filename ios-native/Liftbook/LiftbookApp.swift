import SwiftUI
import Foundation

@main
struct LiftbookApp: App {
    @StateObject private var repo = Repo()
    @StateObject private var nav = Nav()

    init() {
        RestTimerModel.wireActions()
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if repo.session == nil && !repo.skipped {
                    AuthView()
                } else {
                    RootView()
                }
            }
            .environmentObject(repo)
            .environmentObject(nav)
        }
    }
}
