// UIKit stubs (surface used by Liftbook).
import Foundation
public class UIApplication {
    public static let shared = UIApplication()
    public var connectedScenes: Set<UIScene> { [] }
}
public class UIScene: Hashable {
    public init() {}
    public func hash(into h: inout Hasher) {}
    public static func == (l: UIScene, r: UIScene) -> Bool { true }
}
public class UIWindowScene: UIScene {
    public var keyWindow: UIWindow? { nil }
}
public class UIImage {
    public init?(named: String) { nil }
    public init?(data: Data) { nil }
}
public class UIWindow {
    public init() {}
}
public class UIViewController {}
public class UIActivityViewController: UIViewController {
    public init(activityItems: [Any], applicationActivities: [Any?]?) {}
}
