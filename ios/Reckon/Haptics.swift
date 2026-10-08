import UIKit

/// Prepared generators: an unprepared one can lag the first tap by ~100ms,
/// which is very noticeable in a timed drill.
enum Haptics {
    private static let light = UIImpactFeedbackGenerator(style: .light)
    private static let rigid = UIImpactFeedbackGenerator(style: .rigid)
    private static let notice = UINotificationFeedbackGenerator()

    static func warmUp() {
        light.prepare()
        rigid.prepare()
        notice.prepare()
    }

    static func tap() { light.impactOccurred(intensity: 0.7); light.prepare() }
    static func success() { rigid.impactOccurred(intensity: 0.9); rigid.prepare() }
    static func error() { notice.notificationOccurred(.error); notice.prepare() }
    static func pop() { rigid.impactOccurred(intensity: 0.6); rigid.prepare() }
}
