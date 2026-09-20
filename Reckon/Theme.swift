import SwiftUI

enum Layout {
    /// Phone-sized column, centred on iPad and in wide windows: a keypad
    /// stretched across 1000pt is unusable and card rows become unreadable.
    static let maxContent: CGFloat = 620
    static let maxKeypad: CGFloat = 460
    /// Landscape iPad / Stage Manager: question and keypad sit side by side.
    static func isWide(_ size: CGSize) -> Bool { size.width > size.height && size.width >= 700 }
}

extension Font {
    /// Rounded, tight and tabular — numbers should never jitter as they change.
    static func display(_ size: CGFloat, _ weight: Font.Weight = .bold) -> Font {
        .system(size: size, weight: weight, design: .rounded).monospacedDigit()
    }
    static func ui(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight, design: .rounded)
    }
}

/// Card chrome used by every surface in the app.
struct CardBackground: ViewModifier {
    var padding: CGFloat = 18
    var radius: CGFloat = 20
    var fill: Color = Palette.surface

    func body(content: Content) -> some View {
        content
            .padding(padding)
            .background(
                RoundedRectangle(cornerRadius: radius, style: .continuous).fill(fill)
            )
            // Charts draw past their frame, so every card clips to its own shape.
            .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .strokeBorder(Palette.line, lineWidth: 1)
            )
    }
}

extension View {
    func card(padding: CGFloat = 18, radius: CGFloat = 20, fill: Color = Palette.surface) -> some View {
        modifier(CardBackground(padding: padding, radius: radius, fill: fill))
    }

    /// Shrinks slightly while pressed — the whole app should feel physical.
    func pressable(_ scale: CGFloat = 0.97) -> some View {
        buttonStyle(PressStyle(scale: scale))
    }
}

struct PressStyle: ButtonStyle {
    var scale: CGFloat = 0.97
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? scale : 1)
            .animation(.spring(response: 0.22, dampingFraction: 0.6), value: configuration.isPressed)
    }
}
