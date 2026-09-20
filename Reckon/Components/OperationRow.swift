import SwiftUI

struct OperationRow: View {
    let operation: Operation
    let stats: SaveData.OpStats?
    let worstAverage: TimeInterval?
    let isWorst: Bool

    private var fraction: Double {
        guard let avg = stats?.average, let worst = worstAverage, worst > 0 else { return 0 }
        return max(0.08, min(1, avg / worst))
    }

    private var tint: Color { isWorst ? Palette.bad : Palette.mute }

    var body: some View {
        HStack(spacing: 12) {
            Text(operation.symbol)
                .font(.display(17, .bold))
                .foregroundStyle(stats == nil ? Palette.mute.opacity(0.6) : Palette.chalk)
                .frame(width: 18, alignment: .leading)

            Text(stats?.average.map { Fmt.seconds($0) } ?? "—")
                .font(.display(14, .semibold))
                .foregroundStyle(stats == nil ? Palette.mute : tint)
                .frame(width: 52, alignment: .leading)

            Text(stats?.accuracy.map { Fmt.percent($0) } ?? "")
                .font(.ui(13))
                .foregroundStyle(Palette.mute)
                .frame(width: 42, alignment: .leading)

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Palette.surface2)
                    Capsule()
                        .fill(tint.opacity(isWorst ? 0.9 : 0.55))
                        .frame(width: max(0, geo.size.width * fraction))
                }
            }
            .frame(height: 7)
        }
        .padding(.vertical, 5)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(operation.title): \(stats?.average.map { Fmt.seconds($0) } ?? "no data yet")")
    }
}
