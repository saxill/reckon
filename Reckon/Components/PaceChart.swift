import Charts
import SwiftUI

/// Sparkline of the last rounds' average solve time. Lower is better, so the
/// scale is flipped: a rising line means you're getting faster.
struct PaceChart: View {
    let pace: [TimeInterval]

    private var points: [(index: Int, seconds: Double)] {
        pace.enumerated().map { (index: $0.offset, seconds: $0.element) }
    }

    private var bounds: (low: Double, high: Double) {
        let lo = pace.min() ?? 0, hi = pace.max() ?? 1
        let pad = max(0.15, (hi - lo) * 0.35)
        return (max(0, lo - pad), hi + pad)
    }

    /// Mirror the value inside the domain so faster rounds sit higher.
    private func flip(_ seconds: Double) -> Double { bounds.low + bounds.high - seconds }

    var body: some View {
        Chart {
            ForEach(points, id: \.index) { p in
                AreaMark(x: .value("Round", p.index), y: .value("Speed", flip(p.seconds)))
                    .interpolationMethod(.monotone)
                    .foregroundStyle(
                        .linearGradient(colors: [Palette.amber.opacity(0.28), Palette.amber.opacity(0.01)],
                                        startPoint: .bottom, endPoint: .top)
                    )
                LineMark(x: .value("Round", p.index), y: .value("Speed", flip(p.seconds)))
                    .interpolationMethod(.monotone)
                    .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round))
                    .foregroundStyle(Palette.amber)
            }
            if let best = pace.min(), let i = pace.firstIndex(of: best) {
                PointMark(x: .value("Round", i), y: .value("Speed", flip(best)))
                    .symbolSize(58)
                    .foregroundStyle(Palette.good)
            }
        }
        // Reversed: fewer seconds is better, so a rising line reads as progress.
        .chartYScale(domain: bounds.low...bounds.high, type: .linear)
        // Breathing room so the marker on the newest round isn't half cut off.
        .chartXScale(domain: -0.35...(Double(max(pace.count - 1, 1)) + 0.35))
        .chartXAxis(.hidden)
        .chartYAxis(.hidden)
        .chartLegend(.hidden)
        .chartPlotStyle { $0.clipped() }
        .frame(height: 56)
        .clipped()
    }
}
