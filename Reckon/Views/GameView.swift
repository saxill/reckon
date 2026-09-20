import SwiftUI

struct GameView: View {
    let engine: GameEngine
    let onQuit: () -> Void

    @State private var shake: CGFloat = 0
    @State private var caretOn = true   // blinks the input line while empty

    private var answerTint: Color {
        switch engine.feedback {
        case .idle: Palette.amber
        case .correct: Palette.good
        case .wrong: Palette.bad
        }
    }

    var body: some View {
        GeometryReader { geo in
            let wide = Layout.isWide(geo.size)
            VStack(spacing: 0) {
                hud
                progress.padding(.top, 12)

                if wide {
                    // Landscape iPad / Stage Manager: problem and keypad share
                    // the width, so the keys stay in reach instead of stretching
                    // across the whole screen.
                    HStack(alignment: .center, spacing: 36) {
                        stageView(big: true).frame(maxWidth: .infinity)
                        keypad.frame(maxWidth: 400)
                    }
                    .frame(maxHeight: .infinity)
                    .padding(.vertical, 18)
                } else {
                    Spacer(minLength: 16)
                    stageView(big: geo.size.width > 500)
                    Spacer(minLength: 8).frame(maxHeight: 56)
                    keypad
                        .frame(maxWidth: Layout.maxKeypad)
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 6)
                }
            }
            .frame(maxWidth: wide ? .infinity : Layout.maxContent)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 20)
        }
        .background(Palette.ink.ignoresSafeArea())
        .onChange(of: engine.feedback) { _, new in
            guard new == .wrong else { return }
            withAnimation(.linear(duration: 0.4)) { shake += 1 }
        }
    }

    private var keypad: some View {
        Keypad(onDigit: engine.type,
               onClear: engine.clear,
               onDelete: engine.backspace)
    }

    // MARK: hud

    private var hud: some View {
        HStack(spacing: 14) {
            Button(action: onQuit) {
                Image(systemName: "xmark")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Palette.mute)
                    .frame(width: 34, height: 34)
                    .background(Circle().fill(Palette.surface))
            }
            .pressable(0.9)
            .accessibilityLabel("End round")

            Text(engine.clockText)
                .font(.display(22, .heavy))
                .foregroundStyle(isUrgent ? Palette.bad : Palette.chalk)
                .contentTransition(.numericText(countsDown: engine.mode == .sprint))
                .animation(.snappy(duration: 0.2), value: engine.clockText)

            Spacer()

            if let total = engine.total {
                HStack(spacing: 10) {
                    lives
                    Text("\(min(engine.answered + 1, total))/\(total)")
                        .font(.display(15, .semibold))
                        .foregroundStyle(Palette.mute)
                }
            } else {
                HStack(spacing: 5) {
                    Text("\(engine.correct)")
                        .font(.display(17, .heavy))
                        .foregroundStyle(Palette.chalk)
                        .contentTransition(.numericText())
                    Text("solved").font(.ui(13)).foregroundStyle(Palette.mute)
                }
                .animation(.snappy, value: engine.correct)
            }
        }
        .padding(.top, 6)
    }

    private var isUrgent: Bool {
        guard let remaining = engine.remaining else { return false }
        return remaining <= 10
    }

    private var lives: some View {
        HStack(spacing: 4) {
            ForEach(0..<3, id: \.self) { i in
                Image(systemName: i < engine.livesLeft ? "heart.fill" : "heart")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(i < engine.livesLeft ? Palette.bad : Palette.surface2)
                    .scaleEffect(i == engine.livesLeft ? 1.0 : 1.0)
            }
        }
        .animation(.spring(response: 0.3, dampingFraction: 0.5), value: engine.livesLeft)
    }

    private var progress: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Palette.surface2)
                Capsule()
                    .fill(LinearGradient(colors: [Palette.amber.opacity(0.75), Palette.amber],
                                         startPoint: .leading, endPoint: .trailing))
                    .frame(width: max(0, geo.size.width * engine.progress))
            }
        }
        .frame(height: 5)
        .animation(.linear(duration: 0.06), value: engine.progress)
    }

    // MARK: stage

    private func stageView(big: Bool) -> some View {
        VStack(spacing: big ? 26 : 20) {
            Text(engine.problem.question)
                .font(.display(big ? 74 : 56, .heavy))
                .foregroundStyle(Palette.chalk)
                .minimumScaleFactor(0.55)
                .lineLimit(1)
                .modifier(Shake(animatableData: shake))
                .id(engine.problem.question)
                .transition(.asymmetric(insertion: .opacity.combined(with: .offset(y: 14)),
                                        removal: .opacity))
                .animation(.spring(response: 0.3, dampingFraction: 0.8), value: engine.problem.question)

            ZStack {
                if !engine.typed.isEmpty {
                    Text(engine.typed)
                        .font(.display(big ? 64 : 50, .heavy))
                        .foregroundStyle(answerTint)
                        .contentTransition(.numericText())
                }
            }
            .frame(height: 56)
            .animation(.snappy(duration: 0.18), value: engine.typed)
            .overlay(alignment: .bottom) {
                Capsule()
                    .fill(answerTint.opacity(engine.typed.isEmpty ? (caretOn ? 0.55 : 0.18) : 0.45))
                    .frame(width: engine.typed.isEmpty ? 54 : 150, height: 4)
                    .offset(y: 6)
                    .animation(.easeInOut(duration: 0.7).repeatForever(autoreverses: true), value: caretOn)
                    .animation(.spring(response: 0.3, dampingFraction: 0.7), value: engine.typed.isEmpty)
                    .onAppear { caretOn = false }
            }

            Text(engine.feedback == .wrong ? "\(engine.problem.answer)" : " ")
                .font(.display(15, .semibold))
                .foregroundStyle(Palette.bad)
                .opacity(engine.feedback == .wrong ? 1 : 0)
                .animation(.easeOut(duration: 0.15), value: engine.feedback)
        }
        .frame(maxWidth: .infinity)
    }
}

/// Horizontal wobble for a wrong answer.
struct Shake: GeometryEffect {
    var amount: CGFloat = 8
    var shakes: CGFloat = 3
    var animatableData: CGFloat

    func effectValue(size: CGSize) -> ProjectionTransform {
        ProjectionTransform(CGAffineTransform(translationX: amount * sin(animatableData * .pi * shakes), y: 0))
    }
}
