// Charts stubs (surface used by Liftbook) — PlottableValue pattern like the real API.
import Foundation
import SwiftUI

public protocol Plottable {}
extension Int: Plottable {}
extension Double: Plottable {}
extension String: Plottable {}
extension Date: Plottable {}

public struct PlottableValue<Value: Plottable> {
    public static func value(_ name: String, _ value: Value) -> PlottableValue<Value> { PlottableValue() }
    init() {}
}

public struct Chart<Data: RandomAccessCollection, ID: Hashable, Content: View>: View {
    public init(_ data: Data, id: KeyPath<Data.Element, ID>, @ViewBuilder content: @escaping (Data.Element) -> Content) { self.content = content; self.data = data }
    let data: Data
    let content: (Data.Element) -> Content
    public var body: some View { EmptyView() }
}

public struct AreaMark<X: Plottable, Y: Plottable>: View {
    public init(x: PlottableValue<X>, y: PlottableValue<Y>) {}
    public var body: some View { EmptyView() }
}
public struct LineMark<X: Plottable, Y: Plottable>: View {
    public init(x: PlottableValue<X>, y: PlottableValue<Y>) {}
    public var body: some View { EmptyView() }
}

extension View {
    public func interpolationMethod(_ m: InterpolationMethod) -> some View { self }
    public func lineStyle(_ s: StrokeStyle) -> some View { self }
    public func chartXAxis(_ visibility: Visibility) -> some View { self }
    public func chartYAxis(_ visibility: Visibility) -> some View { self }
}
public enum InterpolationMethod { case catmullRom, linear, monotone, stepCenter, stepStart, stepEnd }
