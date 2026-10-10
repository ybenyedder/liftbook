/** Minimal ZIP reader — pure Swift, no Compression framework (Linux-compatible, unit-testable).
 *  Parses the End-of-Central-Directory record, walks the Central Directory for entry metadata
 *  (authoritative method/sizes — valid even when local headers defer to a data descriptor),
 *  then inflates each entry with a hand-written DEFLATE decoder (RFC 1951).
 *  Covers STORED (0) and DEFLATE (8), the two methods `zipfile`/Hevy exports use. No Zip64. */
import Foundation

public enum MiniZip {

    // ---------------- public API ----------------

    /// All non-directory entries of the archive; nil when `data` is not a ZIP we can read.
    public static func entries(data: Data) -> [(name: String, data: Data)]? {
        let bytes = [UInt8](data)
        guard let eocd = findEocd(bytes) else { return nil }
        var out: [(name: String, data: Data)] = []
        var off = Int(read32(bytes, eocd + 16))          // offset of central directory
        for _ in 0..<Int(read16(bytes, eocd + 10)) {     // total entries
            guard off + 46 <= bytes.count, sig(bytes, off, [0x50, 0x4B, 0x01, 0x02]) else { return nil }
            let flags = read16(bytes, off + 8)
            let method = Int(read16(bytes, off + 10))
            let compSize = Int(read32(bytes, off + 20))
            let uncompSize = Int(read32(bytes, off + 24))
            let nameLen = Int(read16(bytes, off + 28))
            let extraLen = Int(read16(bytes, off + 30))
            let commentLen = Int(read16(bytes, off + 32))
            let localOff = Int(read32(bytes, off + 42))
            let nameBytes = Array(bytes[(off + 46)..<(off + 46 + nameLen)])
            guard off + 46 + nameLen + extraLen + commentLen <= bytes.count else { return nil }
            let name = decodeName(nameBytes, utf8: flags & 0x800 != 0)
            off += 46 + nameLen + extraLen + commentLen
            guard !name.hasSuffix("/") else { continue }  // directory entry
            // local file header: 30 bytes + own name/extra lengths precede the data
            guard localOff + 30 <= bytes.count, sig(bytes, localOff, [0x50, 0x4B, 0x03, 0x04]) else { return nil }
            let lName = Int(read16(bytes, localOff + 26))
            let lExtra = Int(read16(bytes, localOff + 28))
            let start = localOff + 30 + lName + lExtra
            guard start + compSize <= bytes.count else { return nil }
            let comp = Array(bytes[start..<start + compSize])
            switch method {
            case 0:
                guard comp.count == uncompSize else { return nil }
                out.append((name: name, data: Data(comp)))
            case 8:
                guard let raw = Inflate.inflate(comp), raw.count == uncompSize else { return nil }
                out.append((name: name, data: Data(raw)))
            default:
                continue // unsupported compression: skip the entry (like ZipInputStream would throw; we stay lenient)
            }
        }
        return out
    }

    /// Convenience: entries whose name ends in ".csv" (case-insensitive), archive order kept.
    public static func csvEntries(data: Data) -> [(name: String, data: Data)]? {
        entries(data: data)?.filter { $0.name.lowercased().hasSuffix(".csv") }
    }

    /// Convenience: UTF-8 text of every .csv entry (bad UTF-8 entries skipped).
    public static func csvTexts(data: Data) -> [String]? {
        csvEntries(data: data)?.compactMap { String(data: $0.data, encoding: .utf8) }
    }

    // ---------------- zip structure ----------------

    private static func sig(_ b: [UInt8], _ i: Int, _ s: [UInt8]) -> Bool {
        guard i + s.count <= b.count else { return false }
        for (k, v) in s.enumerated() where b[i + k] != v { return false }
        return true
    }

    private static func read16(_ b: [UInt8], _ i: Int) -> Int {
        guard i + 2 <= b.count else { return 0 }
        return Int(b[i]) | Int(b[i + 1]) << 8
    }

    private static func read32(_ b: [UInt8], _ i: Int) -> Int {
        guard i + 4 <= b.count else { return 0 }
        return Int(b[i]) | Int(b[i + 1]) << 8 | Int(b[i + 2]) << 16 | Int(b[i + 3]) << 24
    }

    /// Scan backwards for the End-of-Central-Directory signature "PK\x05\x06"
    /// (a trailing comment of up to 64 KiB may follow it).
    private static func findEocd(_ b: [UInt8]) -> Int? {
        guard b.count >= 22 else { return nil }
        let floor = max(0, b.count - 22 - 65_536)
        var i = b.count - 22
        while i >= floor {
            if sig(b, i, [0x50, 0x4B, 0x05, 0x06]) { return i }
            i -= 1
        }
        return nil
    }

    private static func decodeName(_ b: [UInt8], utf8: Bool) -> String {
        if utf8 || b.allSatisfy({ $0 < 0x80 }) {
            return String(decoding: b, as: UTF8.self)
        }
        // CP437 fallback: map the high bytes onto Latin-1 (close enough for real-world names)
        return String(decoding: b.map { $0 < 0x80 ? $0 : codepage437[Int($0)] }, as: UTF8.self)
    }

    private static let codepage437: [UInt8] = {
        var t = [UInt8](0...127)
        let high: [UInt8] = [
            0xC7, 0xFC, 0xE9, 0xE2, 0xE4, 0xE0, 0xE5, 0xE7, 0xEA, 0xEB, 0xE8, 0xEF, 0xEE, 0xEC, 0xC4, 0xC5,
            0xC9, 0xE6, 0xC6, 0xF4, 0xF6, 0xF2, 0xFB, 0xF9, 0xFF, 0xD6, 0xDC, 0xA2, 0xA3, 0xA5, 0x20, 0xE1,
            0xED, 0xF3, 0xFA, 0xF1, 0xD1, 0xAA, 0xBA, 0xBF, 0x20, 0xAC, 0xBD, 0x20, 0xAB, 0xBB, 0x61, 0x20,
            0x61, 0x6F, 0x20, 0x20, 0x20, 0x20, 0x61, 0xDF, 0x20, 0x20, 0x20, 0x20, 0xB5, 0x20, 0x20, 0x20,
            0x20, 0x20, 0x20, 0x20, 0x20, 0x70, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0xE3, 0x83, 0x20, 0x20,
            0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0xB7, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
        ]
        t.append(contentsOf: high)
        return t
    }()
}

// ---------------- DEFLATE (RFC 1951) ----------------

enum Inflate {
    static func inflate(_ input: [UInt8]) -> [UInt8]? {
        var br = BitReader(data: input)
        var out = [UInt8]()
        out.reserveCapacity(input.count * 4)
        repeat {
            guard let last = br.bit(), let type = br.bits(2) else { return nil }
            switch type {
            case 0:
                br.alignToByte()
                guard let len = br.u16(), let nlen = br.u16(), len | nlen == 0xFFFF else { return nil }
                for _ in 0..<len {
                    guard let b = br.byte() else { return nil }
                    out.append(b)
                }
            case 1:
                let (lit, dist) = fixedTrees()
                guard decodeBlock(&br, &out, lit, dist) else { return nil }
            case 2:
                guard let (lit, dist) = dynamicTrees(&br) else { return nil }
                guard decodeBlock(&br, &out, lit, dist) else { return nil }
            default:
                return nil
            }
            if last == 1 { break }
        } while true
        return out
    }

    private static func decodeBlock(_ br: inout BitReader, _ out: inout [UInt8], _ lit: Huffman, _ dist: Huffman) -> Bool {
        while true {
            guard let sym = lit.decode(&br) else { return false }
            if sym < 256 {
                out.append(UInt8(sym))
            } else if sym == 256 {
                return true
            } else {
                guard sym <= 285, let len = lengthOf(sym, &br) else { return false }
                guard let dsym = dist.decode(&br), dsym <= 29 else { return false }
                guard let back = distanceOf(dsym, &br), back > 0, back <= out.count else { return false }
                for _ in 0..<len {
                    out.append(out[out.count - back])
                }
            }
        }
    }

    // ---- Huffman trees (zlib "puff"-style canonical decoding) ----

    struct Huffman {
        var count = [Int](repeating: 0, count: 16)
        var symbol: [Int] = []

        init(lengths: [Int]) {
            for l in lengths where l > 0 { count[l] += 1 }
            var offs = [Int](repeating: 0, count: 16)
            for l in 1..<15 { offs[l + 1] = offs[l] + count[l] }
            symbol = [Int](repeating: 0, count: lengths.filter { $0 > 0 }.count)
            for (sym, l) in lengths.enumerated() where l > 0 {
                symbol[offs[l]] = sym
                offs[l] += 1
            }
        }

        func decode(_ br: inout BitReader) -> Int? {
            var code = 0, first = 0, index = 0
            for len in 1...15 {
                guard let b = br.bit() else { return nil }
                code |= b
                if code - first < count[len] { return symbol[index + (code - first)] }
                index += count[len]
                first = (first + count[len]) << 1
                code <<= 1
            }
            return nil
        }
    }

    private static func fixedTrees() -> (Huffman, Huffman) {
        var litLengths = [Int](repeating: 0, count: 288)
        for i in 0...143 { litLengths[i] = 8 }
        for i in 144...255 { litLengths[i] = 9 }
        for i in 256...279 { litLengths[i] = 7 }
        for i in 280...287 { litLengths[i] = 8 }
        return (Huffman(lengths: litLengths), Huffman(lengths: [Int](repeating: 5, count: 30)))
    }

    private static let clOrder = [16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15]

    private static func dynamicTrees(_ br: inout BitReader) -> (Huffman, Huffman)? {
        guard let hlit = br.bits(5), let hdist = br.bits(5), let hclen = br.bits(4) else { return nil }
        let nLit = hlit + 257, nDist = hdist + 1, nCl = hclen + 4
        guard nLit <= 286, nDist <= 30 else { return nil }
        var clLengths = [Int](repeating: 0, count: 19)
        for i in 0..<nCl {
            guard let v = br.bits(3) else { return nil }
            clLengths[clOrder[i]] = v
        }
        let clTree = Huffman(lengths: clLengths)
        var lengths = [Int]()
        lengths.reserveCapacity(nLit + nDist)
        while lengths.count < nLit + nDist {
            guard let sym = clTree.decode(&br) else { return nil }
            switch sym {
            case 0...15:
                lengths.append(sym)
            case 16:
                guard let n = br.bits(2), let prev = lengths.last else { return nil }
                lengths.append(contentsOf: [Int](repeating: prev, count: n + 3))
            case 17:
                guard let n = br.bits(3) else { return nil }
                lengths.append(contentsOf: [Int](repeating: 0, count: n + 3))
            case 18:
                guard let n = br.bits(7) else { return nil }
                lengths.append(contentsOf: [Int](repeating: 0, count: n + 11))
            default:
                return nil
            }
        }
        guard lengths.count == nLit + nDist else { return nil }
        return (Huffman(lengths: Array(lengths[0..<nLit])), Huffman(lengths: Array(lengths[nLit...])))
    }

    // ---- length / distance codes ----

    private static let lenBase = [3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258]
    private static let lenExtra = [0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0]
    private static let distBase = [1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577]
    private static let distExtra = [0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13]

    private static func lengthOf(_ sym: Int, _ br: inout BitReader) -> Int? {
        guard let extra = br.bits(lenExtra[sym - 257]) else { return nil }
        return lenBase[sym - 257] + extra
    }

    private static func distanceOf(_ sym: Int, _ br: inout BitReader) -> Int? {
        guard let extra = br.bits(distExtra[sym]) else { return nil }
        return distBase[sym] + extra
    }
}

/// LSB-first bit reader over a byte array.
struct BitReader {
    let data: [UInt8]
    var pos = 0
    var bitIdx = 0

    mutating func bit() -> Int? {
        guard pos < data.count else { return nil }
        let v = Int((data[pos] >> bitIdx) & 1)
        advance()
        return v
    }

    mutating func bits(_ n: Int) -> Int? {
        var v = 0
        for i in 0..<n {
            guard let b = bit() else { return nil }
            v |= b << i
        }
        return v
    }

    mutating func byte() -> UInt8? {
        bits(8).map(UInt8.init)
    }

    mutating func u16() -> Int? {
        guard let a = byte(), let b = byte() else { return nil }
        return Int(a) | Int(b) << 8
    }

    mutating func alignToByte() {
        if bitIdx > 0 { bitIdx = 0; pos += 1 }
    }

    private mutating func advance() {
        bitIdx += 1
        if bitIdx == 8 { bitIdx = 0; pos += 1 }
    }
}
