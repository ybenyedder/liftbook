// Exact-shape UserNotifications stubs (surface used by RestTimer.swift).
import Foundation
public final class UNUserNotificationCenter {
    public static func current() -> UNUserNotificationCenter { UNUserNotificationCenter() }
    public func requestAuthorization(options: UNAuthorizationOptions) async throws -> Bool { true }
    public func setNotificationCategories(_ categories: Set<UNNotificationCategory>) async {}
    public func add(_ request: UNNotificationRequest) {}
    public func removeDeliveredNotifications(withIdentifiers ids: [String]) {}
    public func removePendingNotificationRequests(withIdentifiers ids: [String]) {}
    public weak var delegate: UNUserNotificationCenterDelegate?
}
public struct UNAuthorizationOptions: OptionSet {
    public var rawValue: UInt
    public init(rawValue: UInt) { self.rawValue = rawValue }
    public static let alert = UNAuthorizationOptions(rawValue: 4)
    public static let sound = UNAuthorizationOptions(rawValue: 2)
    public static let badge = UNAuthorizationOptions(rawValue: 1)

}
public final class UNNotificationAction {
    public var _x: Int = 0
    public init(identifier: String, title: String, options: UNNotificationActionOptions = []) {}
}
public struct UNNotificationActionOptions: OptionSet {
    public var rawValue: UInt
    public init(rawValue: UInt) { self.rawValue = rawValue }
    public static let foreground = UNNotificationActionOptions(rawValue: 1)
}
public final class UNNotificationCategory: Hashable {
    public var _x: Int = 0
    public func hash(into hasher: inout Hasher) {}
    public static func == (l: UNNotificationCategory, r: UNNotificationCategory) -> Bool { true }
    public init(identifier: String, actions: [UNNotificationAction], intentIdentifiers: [String], options: UNNotificationCategoryOptions = []) {}
}
public struct UNNotificationCategoryOptions: OptionSet { public var rawValue: UInt; public init(rawValue: UInt) { self.rawValue = rawValue } }
public class UNNotificationContent {
    public var title: String { "" }
    public var body: String { "" }
    public var sound: UNNotificationSound? { nil }
    public var categoryIdentifier: String { "" }
    public var _x: Int = 0
    init() {}
}
public final class UNMutableNotificationContent: UNNotificationContent {
    override public var title: String { get { _t } set { _t = newValue } }
    var _t: String = ""
    override public var body: String { get { _b } set { _b = newValue } }
    var _b: String = ""
    override public var sound: UNNotificationSound? { get { _s } set { _s = newValue } }
    var _s: UNNotificationSound?
    override public var categoryIdentifier: String { get { _c } set { _c = newValue } }
    var _c: String = ""
    public override init() { super.init() }
}
public final class UNNotificationSound {
    public static var `default`: UNNotificationSound { UNNotificationSound() }
}
public final class UNNotificationRequest {
    public init(identifier: String, content: UNNotificationContent, trigger: UNNotificationTrigger?) {}
}
public protocol UNNotificationTrigger {}
public final class UNTimeIntervalNotificationTrigger: UNNotificationTrigger {
    public init(timeInterval: TimeInterval, repeats: Bool) {}
}
@MainActor public protocol UNUserNotificationCenterDelegate: AnyObject {
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async
}
public final class UNNotification {
    public var request: UNNotificationRequest { fatalError() }
}
public struct UNNotificationPresentationOptions: OptionSet {
    public var rawValue: UInt
    public init(rawValue: UInt) { self.rawValue = rawValue }
    public static let banner = UNNotificationPresentationOptions(rawValue: 8)
    public static let list = UNNotificationPresentationOptions(rawValue: 16)
}
public final class UNNotificationResponse {
    public var actionIdentifier: String { "" }
}
