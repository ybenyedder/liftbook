import SwiftUI
import Foundation

/** Minimal SVG path-data parser (M L H V C S Q A Z, absolute/relative) → SwiftUI Path.
 *  Same command coverage as the Android app's pathNodesOf — the muscle figures render natively. */
enum SVGPath {
    static func path(from d: String) -> Path {
        var p = Path()
        let tokens = tokenize(d)
        var i = 0
        var cx: CGFloat = 0, cy: CGFloat = 0
        var lastCx: CGFloat = 0, lastCy: CGFloat = 0
        var startX: CGFloat = 0, startY: CGFloat = 0

        func num() -> CGFloat {
            guard i < tokens.count, case .number(let n) = tokens[i] else { i += 1; return 0 }
            i += 1
            return CGFloat(n)
        }
        func isCmd() -> Bool {
            guard i < tokens.count else { return false }
            if case .command = tokens[i] { return true }
            return false
        }

        while i < tokens.count {
            guard case .command(let c) = tokens[i] else { i += 1; continue }
            i += 1
            let rel = c == c.lowercased() && c != c.uppercased()
            switch c.uppercased() {
            case "M":
                var x = num(), y = num()
                if rel { x += cx; y += cy }
                p.move(to: CGPoint(x: x, y: y))
                cx = x; cy = y; startX = x; startY = y
                // implicit following pairs are lineTo
                while !isCmd() && i < tokens.count {
                    var lx = num(), ly = num()
                    if rel { lx += cx; ly += cy }
                    p.addLine(to: CGPoint(x: lx, y: ly))
                    cx = lx; cy = ly
                }
            case "L":
                var x = num(), y = num()
                if rel { x += cx; y += cy }
                p.addLine(to: CGPoint(x: x, y: y))
                cx = x; cy = y
                while !isCmd() && i < tokens.count {
                    var lx = num(), ly = num()
                    if rel { lx += cx; ly += cy }
                    p.addLine(to: CGPoint(x: lx, y: ly))
                    cx = lx; cy = ly
                }
            case "H":
                while !isCmd() && i < tokens.count {
                    var x = num()
                    if rel { x += cx }
                    p.addLine(to: CGPoint(x: x, y: cy))
                    cx = x
                }
            case "V":
                while !isCmd() && i < tokens.count {
                    var y = num()
                    if rel { y += cy }
                    p.addLine(to: CGPoint(x: cx, y: y))
                    cy = y
                }
            case "C":
                while !isCmd() && i < tokens.count {
                    func n(_ base: CGFloat) -> CGFloat { let v = num(); return rel ? v + base : v }
                    let x1 = n(cx), y1 = n(cy), x2 = n(cx), y2 = n(cy), x = n(cx), y = n(cy)
                    p.addCurve(to: CGPoint(x: x, y: y), control1: CGPoint(x: x1, y: y1), control2: CGPoint(x: x2, y: y2))
                    lastCx = x2; lastCy = y2; cx = x; cy = y
                }
            case "S":
                while !isCmd() && i < tokens.count {
                    func n(_ base: CGFloat) -> CGFloat { let v = num(); return rel ? v + base : v }
                    let x2 = n(cx), y2 = n(cy), x = n(cx), y = n(cy)
                    let x1 = 2 * cx - lastCx
                    let y1 = 2 * cy - lastCy
                    p.addCurve(to: CGPoint(x: x, y: y), control1: CGPoint(x: x1, y: y1), control2: CGPoint(x: x2, y: y2))
                    lastCx = x2; lastCy = y2; cx = x; cy = y
                }
            case "Q":
                while !isCmd() && i < tokens.count {
                    func n(_ base: CGFloat) -> CGFloat { let v = num(); return rel ? v + base : v }
                    let x1 = n(cx), y1 = n(cy), x = n(cx), y = n(cy)
                    p.addQuadCurve(to: CGPoint(x: x, y: y), control: CGPoint(x: x1, y: y1))
                    lastCx = x1; lastCy = y1; cx = x; cy = y
                }
            case "A":
                while !isCmd() && i < tokens.count {
                    _ = num(); _ = num(); _ = num()          // rx ry rotation (unused for these figures)
                    let _largeArc = num(), _sweep = num()
                    var x = num(), y = num()
                    if rel { x += cx; y += cy }
                    // circles in the figures are full circles via two arcs; approximate with a line —
                    // the heads are near-circles so approximate by quad curve through the midpoint
                    let mx = (cx + x) / 2, my = (cy + y) / 2
                    let nx = -(cy - y), ny = (cx - x)
                    p.addQuadCurve(to: CGPoint(x: x, y: y), control: CGPoint(x: mx + nx, y: my + ny))
                    cx = x; cy = y
                }
            case "Z":
                p.closeSubpath()
                cx = startX; cy = startY
            default:
                break
            }
        }
        return p
    }

    private enum Tok {
        case command(String)
        case number(Double)
    }

    private static func tokenize(_ d: String) -> [Tok] {
        var toks: [Tok] = []
        var num = ""
        var chars = Array(d)
        func flush() {
            if !num.isEmpty, let v = Double(num) {
                toks.append(.number(v))
            }
            num = ""
        }
        for ch in chars {
            if ch.isLetter {
                flush()
                toks.append(.command(String(ch)))
            } else if ch == "-" || ch == "+" {
                // start of a new number (may follow 'e' exponent)
                if num.hasSuffix("e") || num.hasSuffix("E") {
                    num.append(ch)
                } else {
                    flush()
                    num.append(ch)
                }
            } else if ch.isNumber || ch == "." {
                if ch == ".", num.contains(".") {
                    flush()
                    num.append(ch)
                } else {
                    num.append(ch)
                }
            } else if ch == "e" || ch == "E" {
                num.append(ch)
            } else {
                flush()
            }
        }
        flush()
        return toks
    }
}
