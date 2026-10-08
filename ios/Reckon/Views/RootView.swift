import SwiftUI

struct RootView: View {
    @Environment(Store.self) private var store

    private enum Route: Equatable { case home, play, result }

    @Environment(\.scenePhase) private var scenePhase

    @State private var route: Route = .home
    @State private var engine: GameEngine?
    @State private var result: RoundResult?
    @State private var lastMode: GameMode = .sprint
    @State private var lastDrill: [Operation]?
    @FocusState private var keyboardFocused: Bool

    var body: some View {
        ZStack {
            Palette.ink.ignoresSafeArea()

            switch route {
            case .home:
                HomeView(start: begin)
                    .transition(.opacity.combined(with: .scale(scale: 0.98)))
            case .play:
                if let engine {
                    GameView(engine: engine, onQuit: quit)
                        .transition(.opacity.combined(with: .scale(scale: 1.02)))
                }
            case .result:
                if let result {
                    ResultView(result: result,
                               onAgain: { begin(result.mode == .daily ? .sprint : result.mode, lastDrill) },
                               onHome: { go(.home) })
                        .transition(.opacity.combined(with: .move(edge: .bottom)))
                }
            }
        }
        .animation(.spring(response: 0.38, dampingFraction: 0.86), value: route)
        // Hardware keyboard: digits answer, Return submits or starts, Esc backs
        // out. One handler at the root so screens never fight over focus.
        .focusable()
        .focusEffectDisabled()
        .focused($keyboardFocused)
        .onKeyPress(phases: .down, action: handleKey)
        .onAppear {
            Haptics.warmUp()
            // A frame later: focus set during the first layout pass doesn't
            // stick, which left the keyboard dead until the first tap.
            DispatchQueue.main.async { keyboardFocused = true }
        }
        .onChange(of: route) { keyboardFocused = true }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { keyboardFocused = true }
        }
        // reckon://sprint and reckon://daily — what the widget taps into.
        .onOpenURL { url in
            guard route != .play else { return }
            switch url.host() {
            case "sprint": begin(.sprint, nil)
            case "daily" where !store.playedDailyToday: begin(.daily, nil)
            case "daily": go(.home)
            default: break
            }
        }
    }

    private func handleKey(_ press: KeyPress) -> KeyPress.Result {
        switch route {
        case .home:
            guard press.key == .return else { return .ignored }
            begin(.sprint, nil)
            return .handled

        case .play:
            guard let engine else { return .ignored }
            if let character = press.characters.first, character.isNumber {
                engine.type(String(character))
                return .handled
            }
            switch press.key {
            case .delete, .deleteForward: engine.backspace()
            case .escape: quit()
            case .return: engine.commit()
            default: return .ignored
            }
            return .handled

        case .result:
            switch press.key {
            case .return:
                guard let result else { return .ignored }
                begin(result.mode == .daily ? .sprint : result.mode, lastDrill)
            case .escape: go(.home)
            default: return .ignored
            }
            return .handled
        }
    }

    private func begin(_ mode: GameMode, _ drill: [Operation]?) {
        lastMode = mode
        lastDrill = drill
        let game = GameEngine(mode: mode,
                              operations: drill ?? store.data.operations,
                              level: store.data.level)
        game.start { finished in
            let recorded = store.record(finished)
            result = recorded
            go(.result)
        }
        engine = game
        go(.play)
    }

    private func quit() {
        engine?.abandon()
        engine = nil
        go(.home)
    }

    private func go(_ next: Route) {
        withAnimation(.spring(response: 0.38, dampingFraction: 0.86)) { route = next }
    }
}
