import SwiftUI

/** Palette sampled from the user's Hevy screenshots (dark, blue accent) — port of Theme.kt. */
enum C {
    static let bg = Color(hex: 0x000000)
    static let navBar = Color(hex: 0x121214)
    static let card = Color(hex: 0x1C1C1E)
    static let card2 = Color(hex: 0x2A2A2D)
    static let line = Color(hex: 0x2C2C2F)
    static let line2 = Color(hex: 0x3A3A3E)
    static let text = Color(hex: 0xF4F8F8)
    static let mut = Color(hex: 0x8D9399)
    static let mut2 = Color(hex: 0x6B7076)
    static let accent = Color(hex: 0x028CFD)
    static let accPress = Color(hex: 0x0279DB)
    static let accText = Color.white
    static let red = Color(hex: 0xE5484D)
    static let gold = Color(hex: 0xF5C518)
    static let orange = Color(hex: 0xFFA03C)
    static let green = Color(hex: 0x34C759)
    static let greenBg = Color(hex: 0x1F3B2C)
}

func accentColor(_ name: String) -> Color {
    switch name {
    case "teal": return Color(hex: 0x20B49A)
    case "violet": return Color(hex: 0x7C5CFF)
    case "orange": return Color(hex: 0xFF7A45)
    default: return C.accent
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

extension Font {
    /// Inter static weights bundled in Fonts/ (UIAppFonts).
    static func inter(_ weight: Int, _ size: CGFloat) -> Font {
        let name: String
        switch weight {
        case ..<450: name = "Inter-Regular"
        case ..<550: name = "Inter-Medium"
        case ..<650: name = "Inter-SemiBold"
        case ..<750: name = "Inter-Bold"
        default: name = "Inter-ExtraBold"
        }
        return .custom(name, size: size)
    }
}

extension View {
    func cardStyle(corner: CGFloat = 14, border: Color = C.line) -> some View {
        background(RoundedRectangle(cornerRadius: corner, style: .continuous).fill(C.card))
            .overlay(RoundedRectangle(cornerRadius: corner, style: .continuous).strokeBorder(border, lineWidth: 1))
    }
}
