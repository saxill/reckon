import SwiftUI

/// One palette, two schemes. Dark is the house style; light is a proper
/// paper-and-ink variant rather than an inversion.
enum Palette {
    static let ink      = adaptive(light: 0xF3F1EC, dark: 0x0B0F12)
    static let surface  = adaptive(light: 0xFFFFFF, dark: 0x151C22)
    static let surface2 = adaptive(light: 0xEBE8E1, dark: 0x1D262E)
    static let line     = adaptive(light: 0xDBD6CC, dark: 0x2A343C)
    static let chalk    = adaptive(light: 0x14181B, dark: 0xE9EFF2)
    static let mute     = adaptive(light: 0x6B7580, dark: 0x8797A2)
    static let amber    = adaptive(light: 0xB86E00, dark: 0xFFB020)
    static let good     = adaptive(light: 0x0E7A55, dark: 0x3DDC97)
    static let bad      = adaptive(light: 0xC22B2B, dark: 0xFF5D5D)

    static func adaptive(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(rgb: dark) : UIColor(rgb: light) })
    }
}

extension UIColor {
    convenience init(rgb: UInt32) {
        self.init(red: CGFloat((rgb >> 16) & 0xFF) / 255,
                  green: CGFloat((rgb >> 8) & 0xFF) / 255,
                  blue: CGFloat(rgb & 0xFF) / 255,
                  alpha: 1)
    }
}

enum Fmt {
    static func seconds(_ t: TimeInterval, decimals: Int = 2) -> String {
        String(format: "%.\(decimals)fs", t)
    }
    static func percent(_ v: Double) -> String { "\(Int((v * 100).rounded()))%" }
}
