import SwiftUI

struct Keypad: View {
    let onDigit: (String) -> Void
    let onClear: () -> Void
    let onDelete: () -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 10), count: 3)

    var body: some View {
        LazyVGrid(columns: columns, spacing: 10) {
            ForEach(1...9, id: \.self) { n in
                key(label: "\(n)") { onDigit("\(n)") }
            }
            key(label: "clear", isSecondary: true, action: onClear)
            key(label: "0") { onDigit("0") }
            key(symbol: "delete.backward.fill", isSecondary: true, action: onDelete)
        }
    }

    private func key(label: String? = nil,
                     symbol: String? = nil,
                     isSecondary: Bool = false,
                     action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Group {
                if let symbol {
                    Image(systemName: symbol).font(.system(size: 20, weight: .semibold))
                } else if let label {
                    Text(label).font(isSecondary ? .ui(16, .semibold) : .display(27, .semibold))
                }
            }
            .foregroundStyle(isSecondary ? Palette.mute : Palette.chalk)
            .frame(maxWidth: .infinity, minHeight: 64)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(isSecondary ? Palette.surface.opacity(0.6) : Palette.surface)
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .strokeBorder(Palette.line, lineWidth: 1)
                    )
            )
        }
        .pressable(0.93)
        .accessibilityLabel(label ?? "delete")
    }
}
