import SwiftUI

struct ResultView: View {
    let result: RoundResult
    let onAgain: () -> Void
    let onHome: () -> Void

    @State private var appeared = false

    private var headline: String {
        switch result.mode {
        case .sprint: "\(result.correct)"
        case .marathon: result.ranOutOfLives ? "OUT" : Fmt.seconds(result.elapsed, decimals: 1)
        case .daily: "\(result.correct)/\(result.mode.total ?? 15)"
        }
    }

    private var caption: String {
        switch result.mode {
        case .sprint: "problems solved in 60 seconds"
        case .marathon: result.ranOutOfLives ? "three misses — the run ends there" : "to clear 20 problems"
        case .daily: "on today's set"
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Short rounds shouldn't leave a void: centre the card stack, and
            // let it scroll normally once the missed list makes it tall.
            GeometryReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        score
                        tiles
                        if !result.misses.isEmpty { missed }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 24)
                    .frame(maxWidth: Layout.maxContent)
                    .frame(maxWidth: .infinity)
                    .frame(minHeight: proxy.size.height, alignment: .center)
                }
                .scrollIndicators(.hidden)
            }

            actions
        }
        .background(Palette.ink.ignoresSafeArea())
        .onAppear {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.68)) { appeared = true }
            if result.isPersonalBest { Haptics.success() }
        }
    }

    private var score: some View {
        VStack(alignment: .leading, spacing: 8) {
            if result.isPersonalBest {
                HStack(spacing: 5) {
                    Image(systemName: "trophy.fill").font(.system(size: 10, weight: .bold))
                    Text("PERSONAL BEST").font(.ui(11, .heavy)).tracking(0.8)
                }
                .foregroundStyle(Palette.good)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Capsule().fill(Palette.good.opacity(0.15)))
            }

            Text(headline)
                .font(.display(84, .black))
                .foregroundStyle(Palette.amber)
                .minimumScaleFactor(0.5)
                .lineLimit(1)
                .scaleEffect(appeared ? 1 : 0.6, anchor: .leading)
                .opacity(appeared ? 1 : 0)

            Text(caption)
                .font(.ui(14))
                .foregroundStyle(Palette.mute)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var tiles: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
            StatTile(icon: "scope",
                     value: result.accuracy.map { Fmt.percent($0) } ?? "—",
                     label: "accuracy")
            StatTile(icon: "timer",
                     value: result.averageSolve.map { Fmt.seconds($0) } ?? "—",
                     label: "average solve")
            StatTile(icon: "bolt.fill",
                     value: result.fastestSolve.map { Fmt.seconds($0) } ?? "—",
                     label: "fastest solve")
            StatTile(icon: "number",
                     value: "\(result.seen)",
                     label: "problems seen")
        }
    }

    private var missed: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Missed \(result.misses.count)")
                .font(.ui(13, .semibold))
                .foregroundStyle(Palette.bad)

            ForEach(result.misses) { miss in
                HStack {
                    Text("\(miss.question) = \(miss.answer)")
                        .font(.display(14, .semibold))
                        .foregroundStyle(Palette.chalk)
                    Spacer()
                    Text("you: \(miss.typed)")
                        .font(.display(13))
                        .foregroundStyle(Palette.mute)
                }
                if miss.id != result.misses.last?.id {
                    Divider().overlay(Palette.line)
                }
            }
        }
        .card(padding: 16, radius: 20)
    }

    private var actions: some View {
        HStack(spacing: 10) {
            Button(action: onHome) {
                Text("Home")
                    .font(.ui(16, .semibold))
                    .foregroundStyle(Palette.chalk)
                    .frame(maxWidth: .infinity, minHeight: 54)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(Palette.surface)
                            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .strokeBorder(Palette.line, lineWidth: 1))
                    )
            }
            .pressable()

            Button(action: onAgain) {
                Text(result.mode == .daily ? "Sprint" : "Play again")
                    .font(.ui(16, .bold))
                    .foregroundStyle(Color(UIColor(rgb: 0x0B0F12)))
                    .frame(maxWidth: .infinity, minHeight: 54)
                    .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(Palette.amber))
            }
            .pressable()
        }
        .frame(maxWidth: Layout.maxContent)
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .padding(.bottom, 6)
        .background(
            Palette.ink.opacity(0.96).ignoresSafeArea(edges: .bottom)
        )
    }
}

struct StatTile: View {
    let icon: String
    let value: String
    let label: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(Palette.amber)
            Text(value)
                .font(.display(22, .bold))
                .foregroundStyle(Palette.chalk)
                .minimumScaleFactor(0.6)
                .lineLimit(1)
            Text(label)
                .font(.ui(11.5))
                .foregroundStyle(Palette.mute)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .card(padding: 14, radius: 18)
    }
}
