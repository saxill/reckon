import SwiftUI

struct HomeView: View {
    @Environment(Store.self) private var store
    let start: (GameMode, [Operation]?) -> Void

    @State private var confirmReset = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                header
                hero
                modes
                profile
                settings
                footer
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 36)
            .frame(maxWidth: Layout.maxContent)
            .frame(maxWidth: .infinity)
        }
        .scrollIndicators(.hidden)
        .background(background)
        // Cards sliding under the clock and island need something to sit on.
        .overlay(alignment: .top) {
            Rectangle()
                .fill(.ultraThinMaterial)
                .mask(LinearGradient(colors: [.black, .black, .clear], startPoint: .top, endPoint: .bottom))
                .frame(height: 38)
                .ignoresSafeArea(edges: .top)
                .allowsHitTesting(false)
        }
    }

    private var background: some View {
        ZStack(alignment: .top) {
            Palette.ink
            LinearGradient(colors: [Palette.amber.opacity(0.10), .clear],
                           startPoint: .top, endPoint: .bottom)
                .frame(height: 280)
        }
        .ignoresSafeArea()
    }

    // MARK: header

    private var header: some View {
        HStack(alignment: .firstTextBaseline) {
            HStack(spacing: 0) {
                Text("Reckon").font(.ui(27, .heavy)).foregroundStyle(Palette.chalk)
                Text(".").font(.ui(27, .heavy)).foregroundStyle(Palette.amber)
            }
            Spacer()
            if store.data.dailyStreak > 0 {
                HStack(spacing: 5) {
                    Image(systemName: "flame.fill").font(.system(size: 11, weight: .bold))
                    Text("\(store.data.dailyStreak)").font(.display(13, .bold))
                }
                .foregroundStyle(Palette.amber)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Capsule().fill(Palette.amber.opacity(0.14)))
                .accessibilityLabel("Daily streak \(store.data.dailyStreak) days")
            }
        }
        .padding(.top, 6)
    }

    // MARK: hero

    private var hero: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let median = store.medianPace {
                HStack(alignment: .firstTextBaseline, spacing: 2) {
                    Text(String(format: "%.2f", median))
                        .font(.display(60, .black))
                        .foregroundStyle(Palette.amber)
                    Text("s")
                        .font(.display(24, .heavy))
                        .foregroundStyle(Palette.amber.opacity(0.75))
                }
                .contentTransition(.numericText())

                Text(heroSubtitle)
                    .font(.ui(13))
                    .foregroundStyle(Palette.mute)
                    .fixedSize(horizontal: false, vertical: true)

                if store.data.pace.count > 1 {
                    PaceChart(pace: store.data.pace)
                        .padding(.top, 2)
                }
            } else {
                Text("No rounds yet")
                    .font(.display(34, .black))
                    .foregroundStyle(Palette.chalk)
                Text("A sprint takes 60 seconds and gives you a baseline to beat.")
                    .font(.ui(13))
                    .foregroundStyle(Palette.mute)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .card(padding: 20, radius: 24)
    }

    private var heroSubtitle: String {
        let rounds = store.data.pace.count
        var text = "median per solve · last \(rounds) round\(rounds == 1 ? "" : "s")"
        if rounds > 2, let median = store.medianPace, let last = store.data.pace.last {
            let delta = last - median
            if abs(delta) < 0.06 {
                text += " · holding steady"
            } else if delta < 0 {
                text += " · last round \(Fmt.seconds(-delta)) faster"
            } else {
                text += " · last round \(Fmt.seconds(delta)) slower"
            }
        }
        return text
    }

    // MARK: modes

    private var modes: some View {
        VStack(spacing: 10) {
            ModeCard(mode: .sprint,
                     value: store.data.bestSprint > 0 ? "\(store.data.bestSprint)" : "—",
                     caption: "best",
                     isLocked: false) { start(.sprint, nil) }

            ModeCard(mode: .marathon,
                     value: store.data.bestMarathon.map { Fmt.seconds($0, decimals: 1) } ?? "—",
                     caption: "best",
                     isLocked: false) { start(.marathon, nil) }

            ModeCard(mode: .daily,
                     value: store.playedDailyToday ? "\(store.data.dailyScore)/15" : "—",
                     caption: "today",
                     isLocked: store.playedDailyToday) { start(.daily, nil) }
        }
    }

    // MARK: profile

    private var profile: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Where your time goes")
                .font(.ui(13, .medium))
                .foregroundStyle(Palette.mute)
                .padding(.bottom, 6)

            ForEach(Operation.allCases) { op in
                OperationRow(operation: op,
                             stats: store.stats(for: op),
                             worstAverage: store.weakestOperation?.average,
                             isWorst: store.weakestOperation?.operation == op)
            }

            if let weak = store.weakestOperation {
                Button {
                    start(.sprint, [weak.operation])
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "target").font(.system(size: 13, weight: .bold))
                        Text("Drill \(weak.operation.symbol) only — slowest at \(Fmt.seconds(weak.average))")
                            .font(.ui(13, .semibold))
                    }
                    .foregroundStyle(Palette.chalk)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 13)
                    .background(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .strokeBorder(style: StrokeStyle(lineWidth: 1, dash: [5, 4]))
                            .foregroundStyle(Palette.line)
                    )
                }
                .pressable()
                .padding(.top, 10)
            }
        }
        .card(padding: 18, radius: 22)
    }

    // MARK: settings

    private var settings: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 8) {
                Text("Operations").font(.ui(13, .medium)).foregroundStyle(Palette.mute)
                OperationChips()
            }
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Difficulty").font(.ui(13, .medium)).foregroundStyle(Palette.mute)
                    Spacer()
                    Text(Difficulty.label(store.data.level))
                        .font(.ui(13, .semibold))
                        .foregroundStyle(Palette.chalk)
                }
                DifficultyChips()
            }
        }
        .card(padding: 18, radius: 22)
    }

    // MARK: footer

    private var footer: some View {
        VStack(spacing: 8) {
            Text("Runs fully offline. Scores stay on this device.")
                .font(.ui(11.5))
                .foregroundStyle(Palette.mute)
            Button("Clear all scores") { confirmReset = true }
                .font(.ui(11.5, .semibold))
                .foregroundStyle(Palette.mute)
                .underline()
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 4)
        .confirmationDialog("Clear every score and streak on this device?",
                            isPresented: $confirmReset, titleVisibility: .visible) {
            Button("Clear everything", role: .destructive) { store.reset() }
            Button("Keep them", role: .cancel) {}
        }
    }
}
