// AuthenticationServices stubs (ASWebAuthenticationSession flow).
import Foundation
import UIKit
public typealias ASPresentationAnchor = UIKit.UIWindow
public protocol ASWebAuthenticationPresentationContextProviding: AnyObject {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor
}
public class ASWebAuthenticationSession {
    public init(url: URL, callbackURLScheme: String, completionHandler: @escaping (URL?, Error?) -> Void) {}
    @discardableResult public func start() -> Bool { true }
    public var presentationContextProvider: (any ASWebAuthenticationPresentationContextProviding)?
    public var prefersEphemeralWebBrowserSession: Bool = false
}
