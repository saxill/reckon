import SwiftUI

@main
struct ReckonApp: App {
    @State private var store = Store()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(store)
                .tint(Palette.amber)
        }
    }
}
