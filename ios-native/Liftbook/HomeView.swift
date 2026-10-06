import SwiftUI
import LiftbookCore

/** Home feed — port of Home.kt. */
struct HomeView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                HStack(spacing: 4) {
                    Txt(LS("Home", "Accueil"), weight: 800, size: 27)
                        .padding(.leading, 16)
                    Spacer()
                    Button { nav.push(.exercises) } label: {
                        Image(systemName: "magnifyingglass").font(.system(size: 20)).foregroundColor(C.text).padding(8)
                    }
                    .buttonStyle(.plain)
                    Button { repo.toast(LS("No new notifications", "Pas de nouvelles notifications")) } label: {
                        Image(systemName: "bell").font(.system(size: 20)).foregroundColor(C.text).padding(8)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.top, 14)
                let posts = Array(repo.workoutsDesc().prefix(30))
                if posts.isEmpty {
                    EmptyState(text: LS("No workouts yet.\nHead to Training to get started!",
                                         "Aucune séance pour le moment.\nVa dans Entraînement pour démarrer !"))
                } else {
                    ForEach(posts) { w in
                        FeedPost(workout: w)
                        Rectangle().fill(C.bg).frame(height: 8)
                    }
                }
                Color.clear.frame(height: 20)
            }
        }
        .background(C.bg)
    }
}

private struct FeedPost: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    let w: Workout
    @State private var expanded = false

    var body: some View {
        let shown = expanded ? w.exercises.count : min(3, w.exercises.count)
        let hidden = w.exercises.count - shown
        return VStack(alignment: .leading, spacing: 0) {
            Button { nav.push(.workoutDetail(w.id)) } label: { postBody(shown: shown, hidden: hidden) }
                .buttonStyle(.plain)
        }
        .padding(.vertical, 10)
    }

    @ViewBuilder
    func postBody(shown: Int, hidden: Int) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 12) {
                AvatarView(letter: String(repo.settings.profileName.trimmingCharacters(in: .whitespaces).prefix(1)).uppercased())
                VStack(alignment: .leading, spacing: 1) {
                    Txt(repo.settings.profileName, weight: 700, size: 16)
                    Txt(L10n.relativeTime(w.startedAt, nowMs: Date.now.timeIntervalSince1970), size: 13, color: C.mut)
                }
                Spacer()
                Image(systemName: "ellipsis").font(.system(size: 16)).foregroundColor(C.mut)
            }
            .padding(.horizontal, 16)
            Txt(Calc.dayName(w.startedAt, locale: L10n.lang == "fr" ? "fr_FR" : "en_US"), weight: 800, size: 21)
                .padding(.leading, 16)
                .padding(.top, 10)
                .padding(.bottom, 8)
            HStack(spacing: 8) {
                FeedStat(label: LS("Time", "Temps"), value: Calc.fmtDur(w.endedAt - w.startedAt), flex: 1.2)
                FeedStat(label: LS("Volume", "Volume"), value: "\(Calc.fmtVol(Calc.vol(w), repo.settings.unit)) kg", flex: 1.2)
                HStack(spacing: 5) {
                    Text("🏅").font(.system(size: 15))
                    Txt("\(w.prs.count)", weight: 600, size: 15)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 16)
            Rectangle().fill(C.line).frame(height: 1).padding(.horizontal, 16).padding(.vertical, 12)
            ForEach(0..<shown, id: \.self) { i in
                HStack(spacing: 12) {
                    Txt("\(w.exercises[i].sets.count)", weight: 700, size: 13)
                        .padding(.horizontal, 9).padding(.vertical, 5)
                        .background(RoundedRectangle(cornerRadius: 8).fill(C.card2))
                    Txt(exName(w.exercises[i].name), weight: 500, size: 16)
                        .lineLimit(2)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 7)
            }
            if hidden > 0 {
                Button { expanded = true } label: {
                    Txt(L10n.seeMoreExercises(hidden), size: 14.5, color: C.mut)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                }
                .buttonStyle(.plain)
            }
            Rectangle().fill(C.line).frame(height: 1).padding(.horizontal, 16).padding(.vertical, 6)
            HStack(spacing: 6) {
                postAction(icon: "hand.thumbsup", msg: LS("Added to favorites", "Ajouté aux favoris"))
                postAction(icon: "bubble", msg: LS("Comments coming soon", "Les commentaires arrivent bientôt"))
                postAction(icon: "square.and.arrow.up", msg: LS("Shared!", "Partagé !"))
                Spacer()
            }
            .padding(.leading, 8)
        }
    }

    func postAction(icon: String, msg: String) -> some View {
        Button { repo.toast(msg) } label: {
            Image(systemName: icon).font(.system(size: 18)).foregroundColor(C.mut).padding(8)
        }
        .buttonStyle(.plain)
    }
}

private struct FeedStat: View {
    let label: String
    let value: String
    var flex: CGFloat = 1
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Txt(label, size: 13, color: C.mut)
            Txt(value, weight: 600, size: 15)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
