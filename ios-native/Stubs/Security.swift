// Exact-shape Security stubs (Keychain constants + functions used by AppState).
import Foundation
public typealias CFDictionary = NSDictionary
public typealias CFTypeRef = AnyObject
public let kSecClass: String = "c"
public let kSecClassGenericPassword: String = "gp"
public let kSecAttrService: String = "s"
public let kSecAttrAccount: String = "a"
public let kSecValueData: String = "v"
public let kSecReturnAttributes: String = "ra"
public let kSecReturnData: String = "rd"
public let kSecMatchLimit: String = "ml"
public let kSecMatchLimitOne: String = "one"
public var errSecSuccess: Int { 0 }
public func SecItemDelete(_ query: CFDictionary) -> Int32 { 0 }
public func SecItemAdd(_ ai: CFDictionary, _ out: UnsafeMutablePointer<CFTypeRef?>?) -> Int32 { 0 }
public func SecItemCopyMatching(_ query: CFDictionary, _ result: UnsafeMutablePointer<CFTypeRef?>?) -> Int32 { 0 }
