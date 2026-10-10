import SwiftUI
import Foundation
import LiftbookCore

/** Profile — port of Profile.kt (heatmap Canvas, monthly bars, muscle distribution). */
struct ProfileView: View {
    @EnvironmentObject var repo: Repo
    @EnvironmentObject var nav: Nav
    @State private var heatYear = Calendar.current.component(.year, from: Date.now)

    var body: some View {
        let unit = repo.settings.unit
        let dayVolumes = Dictionary(grouping: repo.workouts, by: { Calc.dayKey($0.startedAt) })
            .mapValues { $0.reduce(0) { $0 + Calc.vol($1) } }
        let dist = Calc.muscleDist(repo.workouts, 15)
        let maxVol = dist.first?.1 ?? 1
        let monthStart = Calendar.current.dateInterval(of: .month, for: Date.now)?.start.timeIntervalSince1970 ?? 0
        let month = repo.workouts.filter { $0.startedAt >= monthStart }
        let now = Date.now.timeIntervalSince1970
        let last7 = repo.workouts.filter { $0.startedAt >= (now - 7 * 86400) * 1000 }
        let prev7 = repo.workouts.filter { $0.startedAt >= (now - 14 * 86400) * 1000 && $0.startedAt < (now - 7 * 86400) * 1000 }
        let v1 = last7.reduce(0) { $0 + Calc.vol($1) }
        let v2 = prev7.reduce(0) { $0 + Calc.vol($1) }
        let delta = v2 > 0 ? Int((v1 - v2) / v2 * 100) : nil
        return ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Group {
                    HStack(alignment: .top, spacing: 8) {
                    VStack(alignment: .leading, spacing: 0) {
                        HStack(spacing: 14) {
                            AvatarView(letter: String(repo.settings.profileName.trimmingCharacters(in: .whitespaces).prefix(1)).uppercased(), size: 52)
                            VStack(alignment: .leading, spacing: 1) {
                                Txt(repo.settings.profileName, weight: 700, size: 23)
                                Txt("@\(repo.settings.handle)", size: 13, color: C.mut)
                            }
                        }
                        Spacer().frame(height: 14)
                        HStack(spacing: 26) {
                            stat("\(repo.workouts.count)", LS("Workouts", "Entraînements"))
                            stat("0", LS("Followers", "Abonnés"))
                            stat("0", LS("Following", "Abonnements"))
                        }
                    }
                    Button { nav.push(.settings) } label: {
                        Image(systemName: "gearshape").font(.system(size: 20)).foregroundColor(C.text).padding(8)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)
                // heatmap
                HStack {
                    Txt(LS("Volume", "Volume"), weight: 700, size: 16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        let y = Calendar.current.component(.year, from: Date.now)
                        heatYear = heatYear > y - 5 ? heatYear - 1 : y
                    } label: {
                        HStack(spacing: 3) {
                            Txt("\(heatYear)", weight: 600, size: 14, color: accentCol(repo.settings.accent))
                            Txt("˅", weight: 600, size: 12, color: accentCol(repo.settings.accent))
                        }
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
                .padding(.bottom, 8)
                HeatmapYear(dayVolumes: dayVolumes, year: heatYear, accent: accentCol(repo.settings.accent))
                // streak (Profile.kt : pilule dès 2 jours consécutifs)
                let streak = Calc.streak(repo.workouts, nowMs: Date.now.timeIntervalSince1970 * 1000)
                if streak >= 2 {
                    HStack(spacing: 6) {
                        Txt("🔥", size: 14)
                        Txt(LS("%1$d-day streak", "Série de %1$d jours").replacingOccurrences(of: "%1$d", with: String(streak)), weight: 600, size: 13)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 7)
                    .background(C.card2, in: RoundedRectangle(cornerRadius: 10))
                    .padding(.leading, 16)
                    .padding(.top, 12)
                }
                Txt("\(Calc.fmtVol(Calc.totalVol(repo.workouts), unit)) \(Calc.unitLabel(unit)) · \(repo.workouts.reduce(0) { $0 + Calc.setsDone($1) }) \(LS("series", "séries")) · \(repo.workouts.reduce(0) { $0 + Calc.reps($1) }) \(LS("reps", "réps"))",
                    size: 13, color: C.mut)
                    .padding(.horizontal, 16)
                    .padding(.top, 6)
                if !month.isEmpty {
                    Txt("\(LS("This month", "Ce mois-ci")) : \(month.count) \(LS("workouts", "séances")) · \(Calc.fmtVol(month.reduce(0) { $0 + Calc.vol($1) }, unit)) \(Calc.unitLabel(unit)) · \(month.reduce(0) { $0 + $1.prs.count }) \(LS("PRs", "records"))",
                        size: 13, color: C.mut)
                        .padding(.horizontal, 16)
                        .padding(.top, 10)
                }
                if !last7.isEmpty || !prev7.isEmpty {
                    Txt("\(LS("Last 7 days", "7 derniers jours")) : \(Calc.fmtVol(v1, unit)) \(Calc.unitLabel(unit)) · \(last7.count) \(LS("workouts", "séances"))"
                        + (delta != nil ? "  (\(delta! >= 0 ? "+" : "")\(delta!)% \(LS("vs previous week", "vs semaine précédente")))" : "")
                        + "\n\(LS("Previous 7 days", "7 jours précédents")) : \(Calc.fmtVol(v2, unit)) \(Calc.unitLabel(unit)) · \(prev7.count) \(LS("workouts", "séances"))",
                        size: 13, color: C.mut)
                        .lineSpacing(6)
                        .padding(.horizontal, 16)
                        .padding(.top, 10)
                }
                monthlyBars
                }
                Spacer().frame(height: 14)
                AppCard {
                    Button { nav.push(.history) } label: {
                        HStack(spacing: 12) {
                            Image(systemName: "calendar").font(.system(size: 17)).foregroundColor(accentCol(repo.settings.accent))
                            Txt(LS("Workout history", "Historique des séances"), weight: 500, size: 15)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Image(systemName: "chevron.right").font(.system(size: 13)).foregroundColor(C.mut)
                        }
                        .padding(.vertical, 13)
                    }
                    .buttonStyle(.plain)
                }
                // carte Photos de progression (Profile.kt : compteur + 5 vignettes récentes)
                Spacer().frame(height: 8)
                AppCard {
                    Button { nav.push(.progress) } label: {
                        VStack(spacing: 0) {
                            HStack(spacing: 12) {
                                Image(systemName: "camera").font(.system(size: 17)).foregroundColor(accentCol(repo.settings.accent))
                                Txt(LS("Progress photos", "Photos de progression"), weight: 500, size: 15)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                Txt(repo.photos.isEmpty ? "" : String(repo.photos.count), size: 13, color: C.mut)
                                Image(systemName: "chevron.right").font(.system(size: 13)).foregroundColor(C.mut)
                            }
                            let recent = repo.photosDesc().prefix(5)
                            if !recent.isEmpty {
                                Spacer().frame(height: 10)
                                HStack(spacing: 6) {
                                    ForEach(Array(recent), id: \.id) { p in
                                        PhotoThumb(id: p.id)
                                            .frame(maxWidth: .infinity)
                                            .aspectRatio(1, contentMode: .fit)
                                            .clipShape(RoundedRectangle(cornerRadius: 6))
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 13)
                    }
                    .buttonStyle(.plain)
                }
                Txt(LS("Volume by muscle group", "Volume par groupe musculaire"), weight: 700, size: 16)
                    .padding(.horizontal, 16)
                    .padding(.top, 18)
                    .padding(.bottom, 6)
                if dist.isEmpty {
                    EmptyState(text: LS("No data yet.\nYour stats will appear after your first workout.",
                                         "Aucune donnée.\nTes stats apparaîtront après ta première séance."))
                } else {
                    ForEach(dist, id: \.0) { m, v in
                        HStack(spacing: 12) {
                            IllIcon(muscle: m, size: 34)
                            VStack(spacing: 5) {
                                HStack {
                                    Txt(muscleName(m), weight: 500, size: 13)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                    Txt("\(Calc.fmtVol(v, unit)) \(Calc.unitLabel(unit))", size: 12, color: C.mut)
                                }
                                GeometryReader { g in
                                    ZStack(alignment: .leading) {
                                        Capsule().fill(C.card2)
                                        Capsule().fill(accentCol(repo.settings.accent))
                                            .frame(width: g.size.width * CGFloat(min(1, max(0.02, v / maxVol))))
                                    }
                                }
                                .frame(height: 7)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 7)
                    }
                }
                Txt(repo.session != nil
                    ? "\(LS("Synced to your account", "Synchronisé sur ton compte")) · \(repo.session?.email ?? "")"
                    : LS("Local app · your data stays on this device.", "Application locale · tes données restent sur cet appareil."),
                    size: 11.5, color: C.mut)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 18)
                    .padding(.bottom, 24)
            }
        }
        .background(C.bg)
    }

    func stat(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 1) {
            Txt(value, weight: 800, size: 17)
            Txt(label, size: 11.5, color: C.mut)
        }
    }

    var monthlyBars: some View {
        let cal = Calendar.current
        let thisMonth = cal.dateInterval(of: .month, for: Date.now)!.start
        let monthly: [(String, Double)] = (0..<6).reversed().map { back in
            let m = cal.date(byAdding: .month, value: -back, to: thisMonth)!
            let start = m.timeIntervalSince1970
            let end = cal.date(byAdding: .month, value: 1, to: m)!.timeIntervalSince1970
            let vol = repo.workouts.filter { $0.startedAt >= start && $0.startedAt < end }.reduce(0) { $0 + Calc.vol($1) }
            return (Calc.monthShort(start), vol)
        }
        let maxV = max(1, monthly.map { $0.1 }.max() ?? 1)
        return HStack(spacing: 10) {
            ForEach(Array(monthly.enumerated()), id: \.offset) { _, mv in
                VStack(spacing: 4) {
                    GeometryReader { g in
                        VStack {
                            Spacer()
                            RoundedRectangle(cornerRadius: 6)
                                .fill(accentCol(repo.settings.accent))
                                .frame(height: g.size.height * CGFloat(min(1, mv.1 / maxV)) * 0.92)
                        }
                    }
                    Txt(String(mv.0.prefix(3)), size: 10, color: C.mut)
                }
            }
        }
        .frame(height: 90)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}

/** Vignette photo locale (pietons de la carte Photos de progression). */
private struct PhotoThumb: View {
    @EnvironmentObject var repo: Repo
    let id: Double

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 6).fill(C.card2)
            if let img = repo.photoImage(id) {
                Image(uiImage: img).resizable().scaledToFill()
            }
        }
    }
}

/** GitHub-style year heatmap of daily volume (native Canvas). */
struct HeatmapYear: View {
    let dayVolumes: [String: Double]
    let year: Int
    let accent: Color

    var body: some View {
        let cal = Calendar.current
        let start = cal.date(from: DateComponents(year: year, month: 1, day: 1))!
        let count = cal.range(of: .day, in: .year, for: start)?.count ?? 365
        let days: [Date] = (0..<count).compactMap { cal.date(byAdding: .day, value: $0, to: start) }
        let lead = (cal.component(.weekday, from: start) + 5) % 7   // Monday-first
        let weeks = Int(ceil(Double(lead + count) / 7))
        let maxDay = max(1, dayVolumes.values.max() ?? 1)
        let today = Date.now
        return VStack(spacing: 4) {
            Canvas { ctx, sz in
                let gap: CGFloat = 2
                let cellW = (sz.width - gap * CGFloat(weeks - 1)) / CGFloat(weeks)
                let cellH = (sz.height - gap * 6) / 7
                let cell = min(cellW, cellH)
                for (i, d) in days.enumerated() {
                    let idx = i + lead
                    let w = idx / 7
                    let dow = idx % 7
                    let key = Calc.dayKey(d.timeIntervalSince1970 * 1000)
                    let v = dayVolumes[key] ?? 0
                    let future = d > today
                    let rect = CGRect(x: CGFloat(w) * (cell + gap), y: CGFloat(dow) * (cell + gap), width: cell, height: cell)
                    let path = Path(roundedRect: rect, cornerRadius: cell * 0.22)
                    if future {
                        ctx.fill(path, with: .color(C.card2.opacity(0.35)))
                    } else if v <= 0 {
                        ctx.fill(path, with: .color(C.card2))
                    } else {
                        ctx.fill(path, with: .color(accent.opacity(0.30 + 0.70 * min(1, v / maxDay))))
                    }
                }
            }
            .frame(height: 96)
            HStack {
                Txt(Calc.monthShort(start.timeIntervalSince1970), size: 10, color: C.mut)
                Spacer()
                Txt(Calc.monthShort(days.last!.timeIntervalSince1970), size: 10, color: C.mut)
            }
        }
        .padding(.horizontal, 16)
    }

}
