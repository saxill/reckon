import SwiftUI

struct ModeCard: View {
    let mode: GameMode
    let value: String
    let caption: String
    let isLocked: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(Palette.amber.opacity(0.15))
                    Image(systemName: isLocked ? "checkmark" : mode.icon)
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(Palette.amber)
                }
                .frame(width: 46, height: 46)

                VStack(alignment: .leading, spacing: 3) {
                    Text(mode.title)
                        .font(.ui(19, .semibold))
                        .foregroundStyle(Palette.chalk)
                    Text(isLocked ? "Played. A fresh set lands tomorrow." : mode.blurb)
                        .font(.ui(13))
                        .foregroundStyle(Palette.mute)
                        .fixedSize(horizontal: false, vertical: true)
                        .multilineTextAlignment(.leading)
                }

                Spacer(minLength: 8)

                VStack(alignment: .trailing, spacing: 1) {
                    Text(value)
                        .font(.display(21, .heavy))
                        .foregroundStyle(Palette.amber)
                    Text(caption)
                        .font(.ui(10.5, .medium))
                        .foregroundStyle(Palette.mute)
                }
            }
            .card()
        }
        .pressable()
        .disabled(isLocked)
        .opacity(isLocked ? 0.6 : 1)
    }
}
