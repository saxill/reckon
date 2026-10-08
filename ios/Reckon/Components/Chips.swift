import SwiftUI

struct OperationChips: View {
    @Environment(Store.self) private var store

    var body: some View {
        HStack(spacing: 8) {
            ForEach(Operation.allCases) { op in
                let on = store.data.operations.contains(op)
                Button {
                    Haptics.pop()
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) { store.toggle(op) }
                } label: {
                    Text(op.symbol)
                        .font(.display(20, .bold))
                        .frame(maxWidth: .infinity, minHeight: 46)
                        .foregroundStyle(on ? Palette.ink : Palette.mute)
                        .background(
                            RoundedRectangle(cornerRadius: 13, style: .continuous)
                                .fill(on ? Palette.chalk : Palette.surface)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 13, style: .continuous)
                                        .strokeBorder(on ? .clear : Palette.line, lineWidth: 1)
                                )
                        )
                }
                .pressable(0.94)
                .accessibilityLabel(op.title)
                .accessibilityAddTraits(on ? [.isSelected] : [])
            }
        }
    }
}

struct DifficultyChips: View {
    @Environment(Store.self) private var store
    @Namespace private var ns

    var body: some View {
        HStack(spacing: 8) {
            ForEach(Difficulty.range, id: \.self) { level in
                let on = store.data.level == level
                Button {
                    Haptics.pop()
                    withAnimation(.spring(response: 0.32, dampingFraction: 0.72)) { store.setLevel(level) }
                } label: {
                    Text("\(level)")
                        .font(.display(17, .semibold))
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .foregroundStyle(on ? Palette.ink : Palette.mute)
                        .background {
                            if on {
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(Palette.chalk)
                                    .matchedGeometryEffect(id: "lvl", in: ns)
                            } else {
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(Palette.surface)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                                            .strokeBorder(Palette.line, lineWidth: 1)
                                    )
                            }
                        }
                }
                .pressable(0.94)
            }
        }
    }
}
