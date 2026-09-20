import Charts
import SwiftUI
import WidgetKit

struct ReckonEntry: TimelineEntry {
    let date: Date
    let save: SaveData
}

struct ReckonProvider: TimelineProvider {
    func placeholder(in context: Context) -> ReckonEntry {
        ReckonEntry(date: .now, save: .widgetPlaceholder)
    }

    func getSnapshot(in context: Context, completion: @escaping (ReckonEntry) -> Void) {
        let save = context.isPreview ? .widgetPlaceholder : ReckonDefaults.load()
        completion(ReckonEntry(date: .now, save: save))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ReckonEntry>) -> Void) {
        let entry = ReckonEntry(date: .now, save: ReckonDefaults.load())
        // Nothing changes on its own except the date, so sit still until just
        // after midnight, when the Daily resets and the streak can lapse.
        let tomorrow = Calendar.current.startOfDay(for: Date().addingTimeInterval(86_400))
        completion(Timeline(entries: [entry], policy: .after(tomorrow)))
    }
}

struct ReckonWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "ReckonWidget", provider: ReckonProvider()) { entry in
            ReckonWidgetView(save: entry.save)
                .containerBackground(for: .widget) {
                    ZStack {
                        Palette.ink
                        LinearGradient(colors: [Palette.amber.opacity(0.16), .clear],
                                       startPoint: .topLeading, endPoint: .bottomTrailing)
                    }
                }
        }
        .configurationDisplayName("Reckon")
        .description("Your median solve time, daily streak and whether today's Daily is done.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct ReckonWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let save: SaveData

    var body: some View {
        Group {
            switch family {
            case .systemSmall: SmallLayout(save: save)
            default: MediumLayout(save: save)
            }
        }
        // Tapping opens straight into a sprint rather than the home screen.
        .widgetURL(URL(string: "reckon://sprint"))
    }
}

// MARK: - Pieces

private struct PaceHeadline: View {
    let save: SaveData
    var size: CGFloat = 34

    var body: some View {
        if let median = save.medianPace {
            HStack(alignment: .firstTextBaseline, spacing: 1) {
                Text(String(format: "%.2f", median))
                    .font(.system(size: size, weight: .black, design: .rounded))
                    .monospacedDigit()
                Text("s")
                    .font(.system(size: size * 0.42, weight: .heavy, design: .rounded))
                    .foregroundStyle(Palette.amber.opacity(0.75))
            }
            .foregroundStyle(Palette.amber)
            .minimumScaleFactor(0.6)
            .lineLimit(1)
        } else {
            Text("Ready")
                .font(.system(size: size * 0.8, weight: .black, design: .rounded))
                .foregroundStyle(Palette.amber)
        }
    }
}

private struct StreakBadge: View {
    let streak: Int

    var body: some View {
        if streak > 0 {
            HStack(spacing: 3) {
                Image(systemName: "flame.fill").font(.system(size: 9, weight: .bold))
                Text("\(streak)").font(.system(size: 11, weight: .bold, design: .rounded))
            }
            .foregroundStyle(Palette.amber)
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(Capsule().fill(Palette.amber.opacity(0.16)))
        }
    }
}

private struct DailyLine: View {
    let save: SaveData

    var body: some View {
        HStack(spacing: 5) {
            Image(systemName: save.playedDailyToday ? "checkmark.circle.fill" : "circle.dashed")
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(save.playedDailyToday ? Palette.good : Palette.mute)
            Text(save.playedDailyToday ? "Daily \(save.dailyScore)/15" : "Daily waiting")
                .font(.system(size: 12, weight: .medium, design: .rounded))
                .foregroundStyle(save.playedDailyToday ? Palette.chalk : Palette.mute)
        }
    }
}

private struct SmallLayout: View {
    let save: SaveData

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("Reckon.")
                    .font(.system(size: 13, weight: .heavy, design: .rounded))
                    .foregroundStyle(Palette.chalk)
                Spacer()
                StreakBadge(streak: save.dailyStreak)
            }
            Spacer(minLength: 4)
            PaceHeadline(save: save)
            Text(save.medianPace == nil ? "no rounds yet" : "median per solve")
                .font(.system(size: 10.5, design: .rounded))
                .foregroundStyle(Palette.mute)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Spacer(minLength: 6)
            DailyLine(save: save)
        }
    }
}

private struct MediumLayout: View {
    let save: SaveData

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 6) {
                    Text("Reckon.")
                        .font(.system(size: 13, weight: .heavy, design: .rounded))
                        .foregroundStyle(Palette.chalk)
                    StreakBadge(streak: save.dailyStreak)
                }
                Spacer(minLength: 4)
                PaceHeadline(save: save, size: 38)
                Text(save.medianPace == nil ? "run a sprint to start" : "median per solve")
                    .font(.system(size: 11, design: .rounded))
                    .foregroundStyle(Palette.mute)
                Spacer(minLength: 6)
                DailyLine(save: save)
            }

            VStack(alignment: .trailing, spacing: 8) {
                if save.pace.count > 1 {
                    WidgetSparkline(pace: save.pace)
                        .frame(height: 46)
                } else {
                    Spacer(minLength: 0)
                }
                HStack(spacing: 10) {
                    bestChip(value: save.bestSprint > 0 ? "\(save.bestSprint)" : "—", label: "sprint")
                    bestChip(value: save.bestMarathon.map { Fmt.seconds($0, decimals: 1) } ?? "—",
                             label: "marathon")
                }
            }
            .frame(maxWidth: .infinity, alignment: .trailing)
        }
    }

    private func bestChip(value: String, label: String) -> some View {
        VStack(alignment: .trailing, spacing: 0) {
            Text(value)
                .font(.system(size: 15, weight: .heavy, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(Palette.chalk)
            Text(label)
                .font(.system(size: 9.5, design: .rounded))
                .foregroundStyle(Palette.mute)
        }
    }
}

/// Same idea as the in-app chart: higher means faster.
private struct WidgetSparkline: View {
    let pace: [TimeInterval]

    private var bounds: (low: Double, high: Double) {
        let lo = pace.min() ?? 0, hi = pace.max() ?? 1
        let pad = max(0.12, (hi - lo) * 0.3)
        return (max(0, lo - pad), hi + pad)
    }

    private func flip(_ seconds: Double) -> Double { bounds.low + bounds.high - seconds }

    var body: some View {
        Chart {
            ForEach(Array(pace.enumerated()), id: \.offset) { index, seconds in
                AreaMark(x: .value("Round", index), y: .value("Speed", flip(seconds)))
                    .interpolationMethod(.monotone)
                    .foregroundStyle(.linearGradient(colors: [Palette.amber.opacity(0.3), Palette.amber.opacity(0.02)],
                                                     startPoint: .bottom, endPoint: .top))
                LineMark(x: .value("Round", index), y: .value("Speed", flip(seconds)))
                    .interpolationMethod(.monotone)
                    .lineStyle(StrokeStyle(lineWidth: 2, lineCap: .round))
                    .foregroundStyle(Palette.amber)
            }
        }
        .chartYScale(domain: bounds.low...bounds.high)
        .chartXScale(domain: -0.3...(Double(max(pace.count - 1, 1)) + 0.3))
        .chartXAxis(.hidden)
        .chartYAxis(.hidden)
        .chartLegend(.hidden)
        .chartPlotStyle { $0.clipped() }
    }
}

extension SaveData {
    /// Shown in the widget gallery, where reading real data isn't allowed.
    static var widgetPlaceholder: SaveData {
        var d = SaveData()
        d.bestSprint = 24
        d.bestMarathon = 41.2
        d.dailyStreak = 3
        d.pace = [2.41, 2.28, 2.35, 2.12, 2.19, 1.96, 2.04, 1.88]
        return d
    }
}
