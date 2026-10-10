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
    public var size: CGSize { CGSize(width: 100, height: 100) }
    public func draw(in rect: CGRect) {}
    public func jpegData(compressionQuality: Double) -> Data? { nil }
    public func pngData() -> Data? { nil }
}
public class UIGraphicsImageRendererFormat {
    public init() {}
    public var scale: Double = 1
    public var opaque = false
}
public class UIGraphicsImageRenderer {
    public init(size: CGSize, format: UIGraphicsImageRendererFormat = UIGraphicsImageRendererFormat()) {}
    public func image(_ actions: (UIGraphicsImageRendererContext) -> Void) -> UIImage { UIImage(named: "x")! }
}
public class UIGraphicsImageRendererContext {}
public class UIWindow {
    public init() {}
}
public class UIViewController {}
public class UIActivityViewController: UIViewController {
    public init(activityItems: [Any], applicationActivities: [Any?]?) {}
}
