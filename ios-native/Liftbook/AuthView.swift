import SwiftUI
import Foundation
import Foundation
import UIKit
import AuthenticationServices
import LiftbookCore

/** Login / signup gate — port of Auth.kt (Liftbook wordmark, Google via ASWebAuthenticationSession, skip). */
struct AuthView: View {
    @EnvironmentObject var repo: Repo
    @State private var mode = 0 // 0 sign in, 1 create
    @State private var email = ""
    @State private var pw = ""
    @State private var showPw = false

    var canSubmit: Bool { !email.trimmingCharacters(in: .whitespaces).isEmpty && !pw.isEmpty && !repo.busy }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Color.clear.frame(height: 46)
                HStack(spacing: 14) {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(C.accent)
                        .frame(width: 58, height: 58)
                        .overlay(Image(systemName: "dumbbell.fill").font(.system(size: 27)).foregroundColor(.white))
                    VStack(alignment: .leading, spacing: 2) {
                        Txt("Liftbook", weight: 800, size: 24)
                        Txt(LS("Workout tracker", "Carnet de musculation"), weight: 500, size: 12.5, color: C.mut)
                    }
                    Spacer()
                }
                Spacer().frame(height: 34)
                Txt(mode == 0 ? LS("Welcome back", "Content de te revoir") : LS("Create an account", "Créer un compte"), weight: 800, size: 26)
                Spacer().frame(height: 8)
                Txt(LS("Your workouts and routines, synced on all your devices.",
                       "Tes séances et tes routines, synchronisées sur tous tes appareils."), size: 14, color: C.mut)
                    .lineSpacing(5)
                Group {
                    Spacer().frame(height: 26)
                    VStack(spacing: 12) {
                        field(text: $email, placeholder: LS("Email", "Email"), keyboard: .emailAddress, next: true)
                        HStack(spacing: 6) {
                            field(text: $pw, placeholder: LS("Password", "Mot de passe"), keyboard: .default, next: false, secure: !showPw)
                            Button { showPw.toggle() } label: {
                                Image(systemName: showPw ? "eye.slash" : "eye")
                                    .font(.system(size: 17))
                                    .foregroundColor(C.mut)
                                    .padding(8)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    if let err = repo.authError {
                        Txt(err, size: 13.5, color: C.red).padding(.top, 10)
                    }
                    Spacer().frame(height: 20)
                    Button(action: submit) {
                        Group {
                            if repo.busy {
                                ProgressView().tint(C.mut).frame(height: 52)
                            } else {
                                Txt(mode == 0 ? LS("Sign in", "Se connecter") : LS("Create account", "Créer un compte"),
                                    weight: 700, size: 16, color: canSubmit ? C.accText : C.mut)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 52)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .background(canSubmit ? C.accent : C.card, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .disabled(!canSubmit)
                }
                Group {
                    Spacer().frame(height: 18)
                    HStack(spacing: 14) {
                        Rectangle().fill(C.line).frame(height: 1)
                        Txt(LS("or", "ou"), size: 13, color: C.mut)
                        Rectangle().fill(C.line).frame(height: 1)
                    }
                    Spacer().frame(height: 18)
                    Button(action: startGoogle) {
                        HStack(spacing: 12) {
                            GoogleG(size: 22)
                            Txt(LS("Continue with Google", "Continuer avec Google"), weight: 600, size: 15.5, color: Color(hex: 0x1F1F1F))
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .disabled(repo.busy)
                    if repo.googlePending {
                        HStack(spacing: 10) {
                            ProgressView().scaleEffect(0.8)
                            Txt(LS("Finishing Google sign-in…", "Connexion Google en cours…"), size: 13.5, color: C.mut)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.top, 12)
                    }
                    Spacer().frame(height: 22)
                    HStack(spacing: 0) {
                        Txt(mode == 0 ? LS("No account yet? ", "Pas encore de compte ? ") : LS("Already have an account? ", "Déjà un compte ? "), size: 14, color: C.mut)
                        Button {
                            mode = 1 - mode
                            repo.authError = nil
                        } label: {
                            Txt(mode == 0 ? LS("Sign up", "S'inscrire") : LS("Sign in", "Se connecter"), weight: 600, size: 14, color: C.accent)
                        }
                        .buttonStyle(.plain)
                    }
                    .frame(maxWidth: .infinity)
                }
                Group {
                    Spacer().frame(height: 40)
                    Button {
                        repo.skipped = true
                        repo.persistMeta()
                    } label: {
                        Txt(LS("Continue without an account", "Continuer sans compte"), weight: 500, size: 14, color: C.mut)
                    }
                    .buttonStyle(.plain)
                    .frame(maxWidth: .infinity)
                    Spacer().frame(height: 24)
                }
            }
            .padding(.horizontal, 24)
        }
        .background(C.bg.ignoresSafeArea())
        .scrollDismissesKeyboard(.interactively)
    }

    func submit() {
        let mail = email.trimmingCharacters(in: .whitespaces)
        guard mail.contains("@"), mail.count >= 5 else {
            repo.authError = LS("Enter a valid email address", "Entre une adresse email valide")
            return
        }
        guard pw.count >= 6 else {
            repo.authError = LS("Password must be 6+ characters", "Mot de passe de 6 caractères minimum")
            return
        }
        repo.authError = nil
        Task {
            if mode == 0 { await repo.signIn(email: mail, password: pw) }
            else { await repo.signUp(email: mail, password: pw) }
        }
    }

    func field(text: Binding<String>, placeholder: String, keyboard: UIKeyboardType, next: Bool, secure: Bool = false) -> some View {
        Group {
            if secure {
                SecureField("", text: text, prompt: Text(placeholder).foregroundColor(C.mut))
            } else {
                TextField("", text: text, prompt: Text(placeholder).foregroundColor(C.mut))
            }
        }
        .font(.inter(500, 16))
        .foregroundColor(C.text)
        .keyboardType(keyboard)
        .submitLabel(next ? .next : .done)
        .autocorrectionDisabled()
        .textInputAutocapitalization(.never)
        .padding(.horizontal, 12)
        .frame(height: 56)
        .background(C.card2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    func startGoogle() {
        let verifier = GoTrueClient.randomPkceVerifier()
        let challenge = GoTrueClient.pkceChallenge(verifier)
        UserDefaults.standard.set(verifier, forKey: "pkce_verifier")
        UserDefaults.standard.set(Date.now.timeIntervalSince1970, forKey: "pkce_ts")
        repo.googlePending = true
        repo.authError = nil
        let url = URL(string: repo.client.googleAuthorizeUrl(verifier: verifier, challenge: challenge))!
        let ctx = WebAuthContext()
        let session = ASWebAuthenticationSession(url: url, callbackURLScheme: "liftbook") { callbackUrl, _ in
            Task { @MainActor in
                if let callbackUrl {
                    RootView.handleCallbackForSessionRestore(url: callbackUrl, repo: repo)
                } else {
                    repo.googlePending = false
                }
                ctx.release()
            }
        }
        session.presentationContextProvider = ctx
        session.prefersEphemeralWebBrowserSession = false
        if !session.start() {
            repo.googlePending = false
            repo.authError = "Connexion Google impossible sur cet appareil."
        }
    }
}

extension RootView {
    /// Google callback via ASWebAuthenticationSession (in-app Safari tab — not the onOpenURL path).
    @MainActor
    static func handleCallbackForSessionRestore(url: URL, repo: Repo) {
        let comps = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let items = comps?.queryItems ?? []
        guard url.host == "auth-callback" else { repo.googlePending = false; return }
        if let err = items.first(where: { $0.name == "error_description" || $0.name == "error" })?.value {
            repo.googlePending = false
            repo.authError = String(err.prefix(120))
            return
        }
        guard let code = items.first(where: { $0.name == "code" })?.value else {
            repo.googlePending = false
            repo.authError = "Connexion Google incomplète — réessaie."
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
                repo.authError = "Connexion Google échouée — réessaie."
            }
        }
    }
}

/** Presentation anchor for ASWebAuthenticationSession. */
@MainActor
final class WebAuthContext: NSObject, @preconcurrency ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.first?.keyWindow ?? ASPresentationAnchor()
    }

    func release() {}
}

struct GoogleG: View {
    var size: CGFloat = 22
    var body: some View {
        // official 4-color G paths, drawn natively
        ZStack {
            GoogleGCanvas()
                .frame(width: size, height: size)
        }
    }
}

struct GoogleGCanvas: View {
    var body: some View {
        Canvas { ctx, sz in
            let s = sz.width / 48
            func draw(_ d: String, _ color: Color) {
                var p = SVGPath.path(from: d)
                p = p.applying(CGAffineTransform(scaleX: s, y: s))
                ctx.fill(p, with: .color(color))
            }
            draw("M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z", Color(hex: 0xEA4335))
            draw("M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z", Color(hex: 0x4285F4))
            draw("M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z", Color(hex: 0xFBBC05))
            draw("M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z", Color(hex: 0x34A853))
        }
    }
}
