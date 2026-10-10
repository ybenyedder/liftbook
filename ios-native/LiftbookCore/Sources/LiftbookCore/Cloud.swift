/** GoTrue client + snapshot sync (LWW) + PKCE helpers — port of Cloud.kt's network layer.
 *  Session storage itself lives in the app (Keychain on iOS); this file is pure networking
 *  and compiles on Linux too (tests can hit the staging API). */
import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

public struct CloudSession: Codable, Equatable, Sendable {
    public var email: String
    public var userId: String
    public var access: String
    public var refresh: String
    public var expiresAt: Double
    public init(email: String, userId: String, access: String, refresh: String, expiresAt: Double) {
        self.email = email
        self.userId = userId
        self.access = access
        self.refresh = refresh
        self.expiresAt = expiresAt
    }
}

public enum CloudError: Error, Equatable {
    case network
    case http(Int, String)
}

public final class GoTrueClient: @unchecked Sendable {
    public static let base = "https://api.webtvmedia.net"
    public static let anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIiwiaWF0IjoxNzc2ODY2NDUyLCJleHAiOjIwOTIyMjY0NTJ9.jOfn90sK6YeY6LRRuwzdiZpiO-s8pN4Ozr418B8iRXE"

    public let redirect: String        // deep link Google bounces back to (liftbook://auth-callback)
    public let session: URLSession
    public let appVersion: String

    public init(redirect: String = "liftbook://auth-callback", appVersion: String = "1.0") {
        self.redirect = redirect
        self.appVersion = appVersion
        let cfg = URLSessionConfiguration.default
        cfg.timeoutIntervalForRequest = 20
        cfg.timeoutIntervalForResource = 40
        cfg.httpAdditionalHeaders = [
            "apikey": GoTrueClient.anonKey,
            "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) Liftbook/" + appVersion,
        ]
        self.session = URLSession(configuration: cfg)
    }

    // ---------------- http ----------------

    func http(_ method: String, _ path: String, body: Data? = nil, bearer: String? = nil, contentType: String? = "application/json") async throws -> (Int, Data) {
        guard let u = URL(string: GoTrueClient.base + path) else { throw CloudError.network }
        var req = URLRequest(url: u)
        req.httpMethod = method
        req.httpBody = body
        if let ct = contentType { req.setValue(ct, forHTTPHeaderField: "Content-Type") }
        if let bearer { req.setValue("Bearer " + bearer, forHTTPHeaderField: "Authorization") }
        let (data, resp): (Data, URLResponse)
        do {
            (data, resp) = try await session.data(for: req)
        } catch {
            throw CloudError.network
        }
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        return (code, data)
    }

    // ---------------- auth ----------------

    public func signIn(email: String, password: String) async throws -> CloudSession {
        try await authenticate("token?grant_type=password", email: email, password: password)
    }

    public func signUp(email: String, password: String) async throws -> CloudSession {
        try await authenticate("signup", email: email, password: password)
    }

    func authenticate(_ kind: String, email rawEmail: String, password: String) async throws -> CloudSession {
        let email = rawEmail.trimmingCharacters(in: .whitespaces).lowercased()
        let body = try JSONSerialization.data(withJSONObject: ["email": email, "password": password])
        let (code, data) = try await http("POST", "/auth/v1/\(kind)", body: body)
        guard (200...299).contains(code) else { throw CloudError.http(code, String(data: data, encoding: .utf8) ?? "") }
        return try Self.session(from: data)
    }

    static func session(from data: Data) throws -> CloudSession {
        guard let obj = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let token = obj["access_token"] as? String else {
            throw CloudError.network
        }
        let user = obj["user"] as? [String: Any]
        return CloudSession(
            email: user?["email"] as? String ?? "",
            userId: user?["id"] as? String ?? "",
            access: token,
            refresh: obj["refresh_token"] as? String ?? "",
            expiresAt: Date.now.timeIntervalSince1970 + (obj["expires_in"] as? Double ?? 3600)
        )
    }

    public func refresh(_ s: CloudSession) async throws -> CloudSession {
        let body = try JSONSerialization.data(withJSONObject: ["refresh_token": s.refresh])
        let (code, data) = try await http("POST", "/auth/v1/token?grant_type=refresh_token", body: body)
        guard (200...299).contains(code) else { throw CloudError.http(code, String(data: data, encoding: .utf8) ?? "") }
        var new = try Self.session(from: data)
        if new.refresh.isEmpty { new.refresh = s.refresh }
        return new
    }

    public func logout(_ s: CloudSession) async {
        _ = try? await http("POST", "/auth/v1/logout", body: Data("{}".utf8), bearer: s.access)
    }

    // ---------------- Google (PKCE, no `state` — GoTrue v2.186 breaks with one) ----------------

    public func googleAuthorizeUrl(verifier: String, challenge: String) -> String {
        "\(GoTrueClient.base)/auth/v1/authorize?provider=google&redirect_to=\(redirect.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? redirect)&flow_type=pkce&code_challenge=\(challenge)&code_challenge_method=s256"
    }

    public func exchangePkce(code: String, verifier: String) async throws -> CloudSession {
        let body = try JSONSerialization.data(withJSONObject: ["auth_code": code, "code_verifier": verifier])
        let (code2, data) = try await http("POST", "/auth/v1/token?grant_type=pkce", body: body)
        guard (200...299).contains(code2) else { throw CloudError.http(code2, String(data: data, encoding: .utf8) ?? "") }
        return try Self.session(from: data)
    }

    // ---------------- snapshot sync ----------------

    /// GET own snapshot row; returns (payload?, client_ts).
    public func pull(_ s: CloudSession) async throws -> (SyncPayload?, Double) {
        let (code, data) = try await http("GET", "/rest/v1/hevy_snapshots?select=data,client_ts", bearer: s.access)
        guard (200...299).contains(code) else { throw CloudError.http(code, String(data: data, encoding: .utf8) ?? "") }
        guard let arr = try JSONSerialization.jsonObject(with: data) as? [[String: Any]], let row = arr.first else {
            return (nil, 0)
        }
        let ts = row["client_ts"] as? Double ?? 0
        guard let raw = row["data"] else { return (nil, ts) }
        do {
            let payload = try JSONDecoder().decode(SyncPayload.self, from: JSONSerialization.data(withJSONObject: raw))
            return (payload, ts)
        } catch {
            return (nil, ts)
        }
    }

    /// Push full snapshot with LWW guard; returns the ts now stored server-side.
    public func push(_ s: CloudSession, payload: SyncPayload, ts: Double) async throws -> Double {
        let enc = JSONEncoder()
        let body = try JSONSerialization.jsonObject(with: enc.encode(payload))
        let reqBody = try JSONSerialization.data(withJSONObject: ["p_data": body, "p_client_ts": ts])
        let (code, data) = try await http("POST", "/rest/v1/rpc/hevy_push_snapshot", body: reqBody, bearer: s.access)
        guard (200...299).contains(code) else { throw CloudError.http(code, String(data: data, encoding: .utf8) ?? "") }
        if let v = try? JSONSerialization.jsonObject(with: data) as? Double, v > 0 { return v }
        if let i = try? JSONSerialization.jsonObject(with: data) as? Int { return Double(i) }
        return ts
    }

    // ---------------- avatar storage ----------------

    public func uploadAvatar(_ s: CloudSession, jpeg: Data) async throws -> String {
        guard let url = URL(string: "\(GoTrueClient.base)/storage/v1/object/avatars/\(s.userId).jpg") else { throw CloudError.network }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("image/jpeg", forHTTPHeaderField: "Content-Type")
        req.setValue("true", forHTTPHeaderField: "x-upsert")
        req.setValue(GoTrueClient.anonKey, forHTTPHeaderField: "apikey")
        req.setValue("Bearer " + s.access, forHTTPHeaderField: "Authorization")
        req.httpBody = jpeg
        let (data, resp) = try await session.data(for: req)
        guard let code = (resp as? HTTPURLResponse)?.statusCode, (200...299).contains(code) else {
            throw CloudError.http((resp as? HTTPURLResponse)?.statusCode ?? 0, String(data: data, encoding: .utf8) ?? "")
        }
        return "\(GoTrueClient.base)/storage/v1/object/public/avatars/\(s.userId).jpg"
    }

    // ---------------- progress photo storage (private `progress` bucket, bearer-auth) ----------------

    /// Storage object path of a progress photo inside the account's folder: "{uid}/ph_<id>.jpg".
    /// Double ids render like Android's Longs ("ph_3.jpg"); stored in `ProgressPhoto.remote`.
    public static func progressObjectPath(_ s: CloudSession, photoId: Double) -> String {
        let id = photoId == photoId.rounded() && abs(photoId) < 1e15 ? String(Int(photoId)) : String(photoId)
        return "\(s.userId)/ph_\(id).jpg"
    }

    /// Upload a progress photo JPEG into the private `progress` bucket (x-upsert).
    /// Returns false on any transport/HTTP failure (no throw — upload is best-effort in the sync cycle).
    public func uploadProgressPhoto(_ s: CloudSession, photoId: Double, jpeg: Data) async -> Bool {
        let path = GoTrueClient.progressObjectPath(s, photoId: photoId)
        guard let url = URL(string: "\(GoTrueClient.base)/storage/v1/object/progress/\(path)") else { return false }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("image/jpeg", forHTTPHeaderField: "Content-Type")
        req.setValue("true", forHTTPHeaderField: "x-upsert")
        req.setValue(GoTrueClient.anonKey, forHTTPHeaderField: "apikey")
        req.setValue("Bearer " + s.access, forHTTPHeaderField: "Authorization")
        req.httpBody = jpeg
        guard let (_, resp) = try? await session.data(for: req) else { return false }
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        return (200...299).contains(code)
    }

    /// Download a progress photo from the private bucket by object path (bearer required).
    /// `path` is the `ProgressPhoto.remote` value ("{uid}/ph_<id>.jpg"); a "progress/" prefix is tolerated.
    public func downloadProgressPhoto(_ s: CloudSession, path: String) async -> Data? {
        let p = path.hasPrefix("progress/") ? String(path.dropFirst("progress/".count)) : path
        guard !p.isEmpty, let url = URL(string: "\(GoTrueClient.base)/storage/v1/object/progress/\(p)") else { return nil }
        var req = URLRequest(url: url)
        req.httpMethod = "GET"
        req.setValue(GoTrueClient.anonKey, forHTTPHeaderField: "apikey")
        req.setValue("Bearer " + s.access, forHTTPHeaderField: "Authorization")
        guard let (data, resp) = try? await session.data(for: req) else { return nil }
        guard let code = (resp as? HTTPURLResponse)?.statusCode, (200...299).contains(code) else { return nil }
        return data
    }

    /// Download a progress photo by id (path derived from the session's user id).
    public func downloadProgressPhoto(_ s: CloudSession, photoId: Double) async -> Data? {
        await downloadProgressPhoto(s, path: GoTrueClient.progressObjectPath(s, photoId: photoId))
    }

    /// DELETE a storage object; 404 counts as success (already gone).
    /// ⚠️ Never send a Content-Type on this bodiless DELETE: the storage-api's Fastify
    /// answers 400 to empty-bodied DELETEs declared as application/json.
    public func deleteStorageObject(_ s: CloudSession, path: String) async -> Bool {
        guard !path.isEmpty, let url = URL(string: "\(GoTrueClient.base)/storage/v1/object/\(path)") else { return false }
        var req = URLRequest(url: url)
        req.httpMethod = "DELETE"
        req.setValue(GoTrueClient.anonKey, forHTTPHeaderField: "apikey")
        req.setValue("Bearer " + s.access, forHTTPHeaderField: "Authorization")
        guard let (_, resp) = try? await session.data(for: req) else { return false }
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        return (200...299).contains(code) || code == 404
    }

    // ---------------- PKCE helpers ----------------

    public static func randomPkceVerifier() -> String {
        var bytes = [UInt8](repeating: 0, count: 48)
        for i in bytes.indices { bytes[i] = UInt8.random(in: 0...255) }
        return Data(bytes).base64URLEncodedString()
    }

    public static func pkceChallenge(_ verifier: String) -> String {
        let digest = Data(Data(verifier.utf8).sha256())
        return digest.base64URLEncodedString()
    }

    public static func mapAuthError(_ code: Int, _ body: String) -> String {
        let b = body.lowercased()
        if code == 429 { return "Trop d'essais — réessaie dans un instant." }
        if b.contains("already") || b.contains("registered") { return "Cet email a déjà un compte. Connecte-toi." }
        if b.contains("invalid_grant") || b.contains("invalid login") { return "Email ou mot de passe incorrect." }
        if b.contains("at least") && b.contains("character") { return "Mot de passe trop court (6 caractères minimum)." }
        if b.contains("validation") && b.contains("email") { return "Adresse email invalide." }
        if code >= 500 { return "Serveur indisponible — réessaie plus tard." }
        return "Erreur inattendue (\(code))."
    }
}

// ---------------- small crypto/base64 helpers (no CryptoKit: Linux-compilable) ----------------

extension Data {
    public func sha256() -> [UInt8] {
        // SHA-256 implementation (FIPS 180-4) — small, dependency-free, works on Linux and iOS.
        let k: [UInt32] = [
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
        ]
        var h: [UInt32] = [0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19]
        let msg = [UInt8](self)
        var len = msg.count
        var data = msg + [0x80] + [UInt8](repeating: 0, count: ((55 - len) % 64 + 64) % 64)
        len &= 0xffffffff
        var bitLen = UInt64(msg.count) * 8
        var be = [UInt8](repeating: 0, count: 8)
        for i in 0..<8 { be[i] = UInt8((bitLen >> UInt64(8 * (7 - i))) & 0xff) }
        data.append(contentsOf: be)
        var w = [UInt32](repeating: 0, count: 64)
        var off = 0
        while off < data.count {
            for i in 0..<16 {
                let j = off + i * 4
                w[i] = (UInt32(data[j]) << 24) | (UInt32(data[j + 1]) << 16) | (UInt32(data[j + 2]) << 8) | UInt32(data[j + 3])
            }
            for i in 16..<64 {
                let s0 = rotr(w[i - 15], 7) ^ rotr(w[i - 15], 18) ^ (w[i - 15] >> 3)
                let s1 = rotr(w[i - 2], 17) ^ rotr(w[i - 2], 19) ^ (w[i - 2] >> 10)
                w[i] = w[i - 16] &+ s0 &+ w[i - 7] &+ s1
            }
            var a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], hh = h[7]
            for i in 0..<64 {
                let s1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25)
                let ch = (e & f) ^ (~e & g)
                let t1 = hh &+ s1 &+ ch &+ k[i] &+ w[i]
                let s0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22)
                let maj = (a & b) ^ (a & c) ^ (b & c)
                let t2 = s0 &+ maj
                hh = g; g = f; f = e; e = d &+ t1; d = c; c = b; b = a; a = t1 &+ t2
            }
            h[0] = h[0] &+ a; h[1] = h[1] &+ b; h[2] = h[2] &+ c; h[3] = h[3] &+ d
            h[4] = h[4] &+ e; h[5] = h[5] &+ f; h[6] = h[6] &+ g; h[7] = h[7] &+ hh
            off += 64
        }
        var out = [UInt8]()
        out.reserveCapacity(32)
        for v in h {
            out.append(UInt8((v >> 24) & 0xff))
            out.append(UInt8((v >> 16) & 0xff))
            out.append(UInt8((v >> 8) & 0xff))
            out.append(UInt8(v & 0xff))
        }
        return out
    }

    public func base64URLEncodedString() -> String {
        base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}

private func rotr(_ x: UInt32, _ n: UInt32) -> UInt32 {
    (x >> n) | (x << (32 - n))
}
