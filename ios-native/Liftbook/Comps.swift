import SwiftUI
import Foundation
import UIKit
import LiftbookCore

/** Shared components — port of Comps.kt. */

struct Txt: View {
    var text: String
    var weight: Int = 400
    var size: CGFloat = 14
    var color: Color = C.text
    init(_ text: String, weight: Int = 400, size: CGFloat = 14, color: Color = C.text) {
        self.text = text
        self.weight = weight
        self.size = size
        self.color = color
    }
    var body: some View {
        Text(text)
            .font(.inter(weight, size))
            .foregroundColor(color)
    }
}

func LS(_ en: String, _ fr: String, _ es: String? = nil, _ de: String? = nil) -> String {
    L10n.s(en, fr, es, de)
}

struct AppCard<Content: View>: View {
    @ViewBuilder var content: Content
    var corner: CGFloat = 14
    var body: some View {
        content
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .cardStyle(corner: corner)
            .padding(.horizontal, 16)
            .padding(.vertical, 5)
    }
}

struct Chip: View {
    @EnvironmentObject var repo: Repo
    let label: String
    let selected: Bool
    let onClick: () -> Void
    var body: some View {
        Button(action: onClick) {
            Text(label)
                .font(.inter(700, 12.5))
                .foregroundColor(selected ? C.accText : C.mut)
                .padding(.horizontal, 13)
                .padding(.vertical, 7)
                .background(selected ? accentCol(repo.settings.accent) : C.card, in: Capsule())
                .overlay(Capsule().strokeBorder(selected ? accentCol(repo.settings.accent) : C.line, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

struct PrimaryButton: View {
    @EnvironmentObject var repo: Repo
    let text: String
    let onClick: () -> Void
    var body: some View {
        Button(action: onClick) {
            Text(text)
                .font(.inter(700, 15))
                .foregroundColor(C.accText)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(accentCol(repo.settings.accent), in: Capsule())
        }
        .buttonStyle(.plain)
    }
}

struct EmptyState: View {
    let text: String
    var slim = false
    var body: some View {
        Text(text)
            .font(.inter(400, 13.5))
            .foregroundColor(C.mut)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 20)
            .padding(.vertical, slim ? 8 : 30)
            .frame(maxWidth: .infinity)
    }
}

struct AvatarView: View {
    @EnvironmentObject var repo: Repo
    let letter: String
    var size: CGFloat = 44
    var body: some View {
        Group {
            if let data = AvatarStore.load(), let img = UIImage(data: data) {
                Image(uiImage: img).resizable().scaledToFill()
            } else {
                ZStack {
                    Circle().fill(accentCol(repo.settings.accent))
                    Text(letter)
                        .font(.inter(800, size * 0.42))
                        .foregroundColor(C.accText)
                }
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
    }
}

/** One-shot "record battu" pill shown at the top of the workout screen. */
struct PrBadgeView: View {
    let badge: (ex: String, muscle: String, kind: String, value: String)
    let onClose: () -> Void
    var body: some View {
        HStack(spacing: 10) {
            ExCircle(muscle: badge.muscle, size: 40)
            VStack(alignment: .leading, spacing: 1) {
                Text(exName(badge.ex))
                    .font(.inter(700, 13.5))
                    .foregroundColor(C.text)
                    .lineLimit(1)
                Text("\(badge.kind) - \(badge.value)")
                    .font(.inter(700, 12.5))
                    .foregroundColor(C.orange)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(C.mut)
                    .padding(6)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 8)
        .background(C.card2, in: Capsule())
        .padding(.horizontal, 20)
    }
}

/** Workout-in-progress guard dialog. */
struct WorkoutInProgressDialog: View {
    let onDismiss: () -> Void
    let onResume: () -> Void
    let onRestart: () -> Void
    var body: some View {
        VStack(spacing: 14) {
            Txt(LS("Workout in progress", "Une séance est déjà en cours"), weight: 800, size: 20)
                .multilineTextAlignment(.center)
            Txt(LS("Resume it, or discard it and start a new one.", "Reprends-la, ou supprime-la pour en commencer une nouvelle."), size: 14.5)
                .foregroundColor(C.text)
                .multilineTextAlignment(.center)
            HStack(spacing: 8) {
                Button(action: onRestart) {
                    Txt(LS("Discard & restart", "Supprimer et recommencer"), weight: 600, size: 14, color: C.red)
                        .padding(.horizontal, 14).padding(.vertical, 8)
                }
                .buttonStyle(.plain)
                Button(action: onResume) {
                    Txt(LS("Resume", "Reprendre"), weight: 600, size: 14, color: C.accent)
                        .padding(.horizontal, 14).padding(.vertical, 8)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(22)
        .frame(maxWidth: 340)
        .background(C.card2, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .padding(40)
    }
}

struct AlertView: View {
    let title: String
    var message: String? = nil
    let confirmLabel: String
    var dismissLabel: String? = nil
    var confirmColor: Color = C.accent
    var dismissColor: Color = C.mut
    var customBody: AnyView? = nil
    let onConfirm: () -> Void
    var onDismiss: (() -> Void)? = nil
    var onDismissPress: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(.inter(800, 20))
                .foregroundColor(C.text)
                .padding(.bottom, message != nil || customBody != nil ? 14 : 16)
            if let message {
                Text(message)
                    .font(.inter(400, 14.5))
                    .foregroundColor(C.text)
                    .lineSpacing(4)
                    .padding(.bottom, 18)
            }
            if let customBody { customBody.padding(.bottom, 14) }
            HStack(spacing: 6) {
                Spacer()
                if let dismissLabel {
                    Button(action: { (onDismissPress ?? onDismiss)?() }) {
                        Text(dismissLabel).font(.inter(600, 14)).foregroundColor(dismissColor)
                            .padding(.horizontal, 14).padding(.vertical, 8)
                    }
                    .buttonStyle(.plain)
                }
                Button(action: onConfirm) {
                    Text(confirmLabel).font(.inter(600, 14)).foregroundColor(confirmColor)
                        .padding(.horizontal, 14).padding(.vertical, 8)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(22)
        .frame(maxWidth: 360)
        .background(C.card2, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

struct PromptAlert: View {
    @State var value: String
    let title: String
    let confirmLabel: String
    var prefix: String? = nil
    var maxLength: Int = 40
    var filter: ((Character) -> Bool)? = nil
    let onConfirm: (String) -> Void
    let onDismiss: () -> Void

    init(initial: String, title: String, confirmLabel: String, prefix: String? = nil, maxLength: Int = 40, filter: ((Character) -> Bool)? = nil, onConfirm: @escaping (String) -> Void, onDismiss: @escaping () -> Void) {
        _value = State(initialValue: initial)
        self.title = title
        self.confirmLabel = confirmLabel
        self.prefix = prefix
        self.maxLength = maxLength
        self.filter = filter
        self.onConfirm = onConfirm
        self.onDismiss = onDismiss
    }

    var body: some View {
        AlertView(
            title: title,
            confirmLabel: confirmLabel,
            dismissLabel: LS("Cancel", "Annuler"),
            customBody: AnyView(
                HStack {
                    if let prefix { Text(prefix).font(.inter(400, 16)).foregroundColor(C.text) }
                    TextField("", text: $value)
                        .font(.inter(400, 16))
                        .foregroundColor(C.text)
                        .tint(C.accent)
                        .onChange(of: value) { v in
                            let filtered = filter != nil ? String(v.filter(filter!)) : v
                            value = String(filtered.prefix(maxLength))
                        }
                }
                .padding(12)
                .background(C.bg, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).strokeBorder(C.line2, lineWidth: 1))
            ),
            onConfirm: { onConfirm(value) },
            onDismiss: onDismiss
        )
    }
}

/** Hevy rest bar: blue progress on top, −15 / big countdown / +15, blue Passer. */
struct RestBar: View {
    @EnvironmentObject var repo: Repo
    @ObservedObject private var timer = RestTimerModel.shared
    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.25)) { tl in
            let now = tl.date.timeIntervalSince1970
            let remaining = max(0, timer.endAt - now)
            let over = timer.endAt - now <= 0
            let shown = Int(remaining / 1000)
            let fraction = timer.totalMs > 0 ? min(1, max(0, remaining / timer.totalMs)) : 0
            VStack(spacing: 0) {
                GeometryReader { g in
                    ZStack(alignment: .leading) {
                        Rectangle().fill(C.line)
                        Rectangle().fill(accentCol(repo.settings.accent)).frame(width: g.size.width * fraction)
                    }
                }
                .frame(height: 4)
                HStack {
                    Button { timer.minus15() } label: {
                        Txt("−15", weight: 600, size: 15)
                            .padding(.horizontal, 12).padding(.vertical, 9)
                            .background(C.card2, in: RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                    Spacer()
                    Text(String(format: "%02d:%02d", shown / 60, shown % 60))
                        .font(.inter(800, 32))
                        .foregroundColor(over ? C.red : C.text)
                        .monospacedDigit()
                    Spacer()
                    Button { timer.plus15() } label: {
                        Txt("+15", weight: 600, size: 15)
                            .padding(.horizontal, 12).padding().background(C.card2, in: RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                    Button { timer.clear() } label: {
                        Txt(LS("Skip", "Passer"), weight: 600, size: 15, color: C.accText)
                            .padding(.horizontal, 18).padding(.vertical, 11)
                            .background(accentCol(repo.settings.accent), in: RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                }
                .padding(10)
            }
            .background(C.card, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
    }
}

func fmtRestLabel(_ sec: Int) -> String {
    sec >= 60 ? "\(sec / 60)min \(sec % 60)s" : "\(sec)s"
}
