import SwiftUI
import LiftbookCore

/** Muscle pictograms (extracted 1:1 from the Android drawables) rendered as native SwiftUI Paths. */

struct FigureView: View {
    let muscle: String
    var dark = false
    var size: CGFloat

    var body: some View {
        let entry = Figures.map[muscle] ?? Figures.map["Chest"]!
        let fig = dark ? entry.dark : entry.light
        let h = size * fig.vh / fig.vw
        Canvas { ctx, sz in
            let sx = sz.width / CGFloat(fig.vw)
            let sy = sz.height / CGFloat(fig.vh)
            for (fill, d) in fig.paths {
                let path = SVGPath.path(from: d)
                let scaled = path.applying(CGAffineTransform(scaleX: sx, y: sy))
                ctx.fill(Path(scaled), with: .color(Color(hexString: fill)))
            }
        }
        .frame(width: size, height: h)
    }
}

/// Hevy-style exercise badge: white circle + dark figure with the muscle highlighted.
struct ExCircle: View {
    let muscle: String
    var size: CGFloat = 46

    var body: some View {
        Circle()
            .fill(Color.white)
            .frame(width: size, height: size)
            .overlay(
                FigureView(muscle: muscle, dark: true, size: size * 0.62)
            )
    }
}

struct IllIcon: View {
    let muscle: String
    var size: CGFloat = 46

    var body: some View {
        FigureView(muscle: muscle, size: size)
    }
}

/** Highlight positions per muscle, (isFront, x, y) normalized. */
let bodySpots: [String: [(Bool, CGFloat, CGFloat)]] = [
    "Chest": [(true, 0.37, 0.28), (true, 0.63, 0.28)],
    "Shoulders": [(true, 0.30, 0.21), (true, 0.70, 0.21)],
    "Biceps": [(true, 0.23, 0.32), (true, 0.77, 0.32)],
    "Forearms": [(true, 0.17, 0.40), (true, 0.83, 0.40)],
    "Abs": [(true, 0.5, 0.38)],
    "Quads": [(true, 0.42, 0.60), (true, 0.58, 0.60)],
    "Adductors": [(true, 0.46, 0.52), (true, 0.54, 0.52)],
    "Calves": [(true, 0.41, 0.82), (true, 0.59, 0.82), (false, 0.41, 0.82), (false, 0.59, 0.82)],
    "Traps": [(true, 0.5, 0.17), (false, 0.5, 0.17)],
    "Lats": [(false, 0.37, 0.30), (false, 0.63, 0.30)],
    "Triceps": [(false, 0.23, 0.32), (false, 0.77, 0.32)],
    "Lower back": [(false, 0.5, 0.38)],
    "Glutes": [(false, 0.43, 0.47), (false, 0.57, 0.47)],
    "Hamstrings": [(false, 0.42, 0.60), (false, 0.58, 0.60)],
    "Abductors": [(false, 0.36, 0.52), (false, 0.64, 0.52)],
]

/** Hevy-style mini body pictogram (front/back) with the worked muscles highlighted in accent. */
struct BodyMap: View {
    var front: Bool
    var muscles: Set<String>
    @EnvironmentObject var repo: Repo

    var body: some View {
        let accent = accentColor(repo.settings.accent)
        let body = Color(hex: 0x4A4F56)
        Canvas { ctx, sz in
            let w = sz.width
            let h = sz.height
            func limb(_ x1: CGFloat, _ y1: CGFloat, _ x2: CGFloat, _ y2: CGFloat, _ t: CGFloat) {
                var p = Path()
                p.move(to: CGPoint(x: w * x1, y: h * y1))
                p.addLine(to: CGPoint(x: w * x2, y: h * y2))
                ctx.stroke(p, with: .color(body), style: StrokeStyle(lineWidth: w * t, lineCap: .round))
            }
            // head + neck
            ctx.fill(Path(ellipseIn: CGRect(x: w * 0.385, y: h * 0.085 - w * 0.115, width: w * 0.23, height: w * 0.23)), with: .color(body))
            limb(0.5, 0.14, 0.5, 0.185, 0.11)
            // torso — tapered V
            var torso = Path()
            torso.move(to: CGPoint(x: w * 0.36, y: h * 0.19))
            torso.addLine(to: CGPoint(x: w * 0.64, y: h * 0.19))
            torso.addLine(to: CGPoint(x: w * 0.585, y: h * 0.475))
            torso.addLine(to: CGPoint(x: w * 0.415, y: h * 0.475))
            torso.closeSubpath()
            ctx.fill(torso, with: .color(body))
            // pelvis
            let pelvis = CGRect(x: w * 0.415, y: h * 0.475, width: w * 0.17, height: h * 0.075)
            ctx.fill(Path(roundedRect: pelvis, cornerRadius: w * 0.09), with: .color(body))
            // arms
            limb(0.345, 0.205, 0.245, 0.31, 0.105)
            limb(0.245, 0.31, 0.275, 0.435, 0.09)
            limb(0.655, 0.205, 0.755, 0.31, 0.105)
            limb(0.755, 0.31, 0.725, 0.435, 0.09)
            // legs
            limb(0.455, 0.545, 0.43, 0.75, 0.125)
            limb(0.43, 0.75, 0.425, 0.925, 0.10)
            limb(0.545, 0.545, 0.57, 0.75, 0.125)
            limb(0.57, 0.75, 0.575, 0.925, 0.10)
            // highlights
            for m in muscles {
                for (f, x, y) in bodySpots[m] ?? [] where f == front {
                    ctx.fill(Path(ellipseIn: CGRect(x: w * x - w * 0.085, y: h * y - w * 0.085, width: w * 0.17, height: w * 0.17)), with: .color(accent))
                }
            }
        }
        .frame(width: 34, height: 56)
        .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
    }
}

extension Color {
    init(hexString: String) {
        var s = hexString.trimmingCharacters(in: .whitespaces)
        if s.hasPrefix("#") { s.removeFirst() }
        var v: UInt64 = 0
        Scanner(string: s).scanHexInt64(&v)
        if s.count == 6 {
            self.init(hex: UInt32(v))
        } else {
            self.init(hex: 0xFFFFFF)
        }
    }
}
