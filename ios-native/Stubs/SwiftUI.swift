fileprivate func _c() -> SwiftUI.Color { fatalError() }
fileprivate func _ev() -> SwiftUI.EmptyView { SwiftUI.EmptyView() }
// (init nonisolated) 
// Comprehensive permissive SwiftUI stub (Linux) for full-app typechecking of Liftbook.
// Shapes mirror the real APIs used; bodies are dummies. Anything wrong in OUR code fails here.
import Foundation
import UIKit
import UniformTypeIdentifiers

@MainActor public protocol View {
    associatedtype Body: View
    var body: Body { get }
}
public struct AnyView: View {
    nonisolated public init(_ v: any View) {}
    public var body: some View { EmptyView() }
}
public struct EmptyView: View {
    nonisolated public init() {}
    public var body: some View { _ev() }
}

public struct Color: Hashable, Sendable, View {
    public init() {}
    public init(_ c: Color) {}
    public init(hex: UInt32) {}
    public func hash(into h: inout Hasher) {}
    public static func == (l: Color, r: Color) -> Bool { true }
    public static var white: Color { _c() }
    public static var black: Color { _c() }
    public static var red: Color { _c() }
    public static var green: Color { _c() }
    public static var blue: Color { _c() }
    public static var orange: Color { _c() }
    public static var yellow: Color { _c() }
    public static var purple: Color { _c() }
    public static var gray: Color { _c() }
    public static var clear: Color { _c() }
    public static var primary: Color { _c() }
    public static var secondary: Color { _c() }
    public static func sRGB(red: Double, green: Double, blue: Double, opacity: Double = 1) -> Color { _c() }
    public var body: some View { _ev() }
    public init(red: Double, green: Double, blue: Double, opacity: Double = 1) { self = _c() }
    public init(_ colorSpace: Color.RGBColorSpace, red: Double, green: Double, blue: Double, opacity: Double = 1) { self = _c() }
    public enum RGBColorSpace { case sRGB, sRGBLinear, extendedSRGB, displayP3, extendedLinearSRGB }
    public func opacity(_ d: Double) -> Color { _c() }
    public func frame(width: Double? = nil, height: Double? = nil, alignment: Alignment = .center) -> _ColorView { _ColorView() }
    public func ignoresSafeArea(_ r: SafeAreaRegions = .all, edges: Edge.Set = .all) -> _ColorView { _ColorView() }
}
public struct _ColorView: View {
    nonisolated public init() {}
    public var body: some View { EmptyView() }
}
public enum ColorScheme { case dark, light }

public struct Font: Hashable, Sendable {
    public init() {}
    public static func system(size: Double, weight: Font.Weight? = nil) -> Font { Font() }
    public static func custom(_ name: String, size: Double) -> Font { Font() }
    public enum Weight { case regular, medium, semibold, bold, heavy, black, light, thin, ultraLight }
}

public struct Text: View, Equatable {
    var _s: String
    public init(_ string: some StringProtocol) { _s = String(string) }
    public init(verbatim: String) { _s = verbatim }
    public init(_ key: String, tableName: String? = nil, bundle: Bundle? = nil, comment: StaticString? = nil) { _s = key }
    public var body: some View { EmptyView() }
    public static func == (l: Text, r: Text) -> Bool { true }
    public func foregroundColor(_ c: Color?) -> Text { self }
    public func bold() -> Text { self }
}

public struct Image: View {
    public init(_ name: String, bundle: Bundle? = nil) { body = EmptyView() }
    public init(systemName: String) { body = EmptyView() }
    public init(uiImage: UIKit.UIImage) { body = EmptyView() }
    public let body: EmptyView
}

// ---- layout ----
public struct HStack<Content: View>: View {
    public init(alignment: VerticalAlignment = .center, spacing: Double? = nil, @ViewBuilder content: () -> Content) { body = content() }
    public let body: Content
}
public struct VStack<Content: View>: View {
    public init(alignment: HorizontalAlignment = .center, spacing: Double? = nil, @ViewBuilder content: () -> Content) { body = content() }
    public let body: Content
}
public struct ZStack<Content: View>: View {
    public init(alignment: Alignment = .center, @ViewBuilder content: () -> Content) { body = content() }
    public let body: Content
}
public struct Group<Content: View>: View {
    public init(@ViewBuilder content: () -> Content) { body = content() }
    public let body: Content
}
public struct ScrollView<Content: View>: View {
    public init(_ axes: Axis.Set = .vertical, showsIndicators: Bool = true, @ViewBuilder content: () -> Content) { body = content() }
    public let body: Content
}
public struct LazyVStack<Content: View>: View {
    public init(alignment: HorizontalAlignment = .center, spacing: Double? = nil, pinnedViews: PinnedScrollableViews = [], @ViewBuilder content: () -> Content) { body = content() }
    public let body: Content
}
public struct LazyVGrid<Content: View>: View {
    public init(columns: [GridItem], alignment: HorizontalAlignment = .center, spacing: Double? = nil, pinnedViews: PinnedScrollableViews = [], @ViewBuilder content: () -> Content) { body = content() }
    public let body: Content
}
public struct GridItem: Hashable {
    public init(_ size: GridItem.Size = .flexible(), spacing: Double? = nil, alignment: HorizontalAlignment? = nil) {}
    public func hash(into h: inout Hasher) {}
    public static func == (l: GridItem, r: GridItem) -> Bool { true }
    public enum Size {
        case adaptive(minimum: Double, maximum: Double)
        case fixed(Double)
        case flexible(minimum: Double, maximum: Double)
        public static func flexible() -> GridItem.Size { .flexible(minimum: 10, maximum: 10_000) }
    }
}
public struct PinnedScrollableViews: OptionSet {
    public var rawValue: Int
    public init(rawValue: Int) { self.rawValue = rawValue }
    public static let sectionHeaders = PinnedScrollableViews(rawValue: 1)
    public static let sectionFooters = PinnedScrollableViews(rawValue: 2)
}
public enum Axis {
    case horizontal, vertical
    public struct Set: OptionSet {
        public var rawValue: Int
        public init(rawValue: Int) { self.rawValue = rawValue }
        public init() { self = .all }
        public static let all = Set()
        public static let horizontal = Set()
        public static let vertical = Set()
        public static let top = Set()
        public static let bottom = Set()
        public static let leading = Set()
        public static let trailing = Set()
    }
}
public typealias AxisSet = Axis

public struct Spacer: View {
    public init(minLength: Double? = nil) { body = EmptyView() }
    public let body: EmptyView
}
public struct Divider: View {
    public init() { body = EmptyView() }
    public let body: EmptyView
}

public struct EdgeInsets: Equatable, Sendable {
    public var top, leading, bottom, trailing: Double
    public init(top: Double, leading: Double, bottom: Double, trailing: Double) { self.top = top; self.leading = leading; self.bottom = bottom; self.trailing = trailing }
    public init() { top = 0; leading = 0; bottom = 0; trailing = 0 }
}

public struct Alignment { public init() {}; public static let center = Alignment(); public static let top = Alignment(); public static let bottom = Alignment(); public static let leading = Alignment(); public static let trailing = Alignment(); public static let topLeading = Alignment(); public static let topTrailing = Alignment(); public static let bottomLeading = Alignment(); public static let bottomTrailing = Alignment() }
public struct HorizontalAlignment { public init() {}; public static let leading = HorizontalAlignment(); public static let center = HorizontalAlignment(); public static let trailing = HorizontalAlignment() }
public struct VerticalAlignment { public init() {}; public static let top = VerticalAlignment(); public static let center = VerticalAlignment(); public static let bottom = VerticalAlignment() }
public enum TextAlignment { case leading, center, trailing }
public struct UnitPoint { public init() {}; public var x: Double = 0; public var y: Double = 0; public static let top = UnitPoint(); public static let bottom = UnitPoint(); public static let leading = UnitPoint(); public static let trailing = UnitPoint(); public static let center = UnitPoint() }

public struct GeometryReader<Content: View>: View {
    public init(@ViewBuilder content: @escaping (GeometryProxy) -> Content) { self.content = content }
    let content: (GeometryProxy) -> Content
    public var body: some View { content(GeometryProxy()) }
}
public struct GeometryProxy { public var size: CGSize { CGSize(width: 390, height: 844) } }

// ---- shapes & graphics ----
public protocol Shape: View { func path(in rect: CGRect) -> Path }
extension Shape {
    public var body: some View { _sv() }
}
public struct Capsule: Shape { public init() {}; public func path(in rect: CGRect) -> Path { Path() } }
public struct Circle: Shape { public init() {}; public func path(in rect: CGRect) -> Path { Path() } }
public struct Rectangle: Shape { public init() {}; public func path(in rect: CGRect) -> Path { Path() } }
public struct RoundedRectangle: Shape {
    public init(cornerRadius: CGFloat, style: RoundedCornerStyle = .continuous) {}
    public init(cornerSize: CGSize, style: RoundedCornerStyle = .continuous) {}
    public func path(in rect: CGRect) -> Path { Path() }
}
public enum RoundedCornerStyle { case circular, continuous }
public struct LinearGradient: ShapeStyle {
    public init(colors: [Color], startPoint: UnitPoint, endPoint: UnitPoint) {}
}
public protocol ShapeStyle {}
extension Color: ShapeStyle {}
public struct StrokeStyle: Equatable {
    public init(lineWidth: CGFloat = 1, lineCap: CGLineCap = .butt, lineJoin: CGLineJoin = .miter, miterLimit: CGFloat = 10, dash: [CGFloat] = [], dashPhase: CGFloat = 0) {}
    public static func == (l: StrokeStyle, r: StrokeStyle) -> Bool { true }
}
public enum CGLineCap { case butt, round, square }
public enum CGLineJoin { case miter, round, bevel }

public struct Path: Shape, Equatable {
    nonisolated public init() {}
    nonisolated public init(_ rect: CGRect) {}
    nonisolated public init(ellipseIn rect: CGRect) {}
    nonisolated public init(roundedRect rect: CGRect, cornerRadius: CGFloat) {}
    nonisolated public init(roundedRect rect: CGRect, cornerSize: CGSize, style: RoundedCornerStyle = .circular) {}
    nonisolated public mutating func move(to point: CGPoint) {}
    nonisolated public mutating func addLine(to point: CGPoint) {}
    nonisolated public mutating func addCurve(to end: CGPoint, control1: CGPoint, control2: CGPoint) {}
    nonisolated public mutating func addQuadCurve(to end: CGPoint, control: CGPoint) {}
    nonisolated public mutating func addPath(_ path: Path, transform: CGAffineTransform = .identity) {}
    nonisolated public mutating func addArc(center: CGPoint, radius: CGFloat, startAngle: Angle, endAngle: Angle, clockwise: Bool) {}
    nonisolated public mutating func addRect(_ rect: CGRect) {}
    nonisolated public mutating func closeSubpath() {}
    nonisolated public func applying(_ t: CGAffineTransform) -> Path { Path() }
    nonisolated public var boundingRect: CGRect { CGRect(x: 0, y: 0, width: 1, height: 1) }
    public static func == (l: Path, r: Path) -> Bool { true }
    public func path(in rect: CGRect) -> Path { self }
    public var body: some View { EmptyView() }
}
public struct Angle {
    public init(degrees: Double) {}
    public init(radians: Double) {}
    public var degrees: Double { 0 }
}
public struct CGAffineTransform: Equatable, Sendable {
    public init() {}
    public init(scaleX sx: CGFloat, y sy: CGFloat) {}
    public static let identity = CGAffineTransform()
    public func scaledBy(x: CGFloat, y: CGFloat) -> CGAffineTransform { self }
    public static func == (l: CGAffineTransform, r: CGAffineTransform) -> Bool { true }
}

public struct GraphicsContext {
    public func fill(_ path: Path, with shading: Shading) {}
    public func stroke(_ path: Path, with shading: Shading, style: StrokeStyle = StrokeStyle()) {}
    public enum Shading {
        case color(Color)
        public func opacity(_ d: Double) -> GraphicsContext.Shading { .color(_c()) }
    }
}
public struct _CanvasView: View {
    nonisolated public init() {}
    public var body: some View { EmptyView() }
}
public func Canvas(_ render: @escaping (GraphicsContext, CGSize) -> Void) -> _CanvasView { _CanvasView() }
public enum ColorRenderingMode { case linear, nonLinear }

public struct TimelineView<Content: View>: View {
    // Les deux init prennent un builder non générique (some View) et fixent Content == AnyView :
    // c'est comme ça que l'inférence réelle se comporte à l'usage, sans ambiguïté.
    public init(from start: Date? = nil, by interval: TimeInterval, @ViewBuilder content: @escaping (Context) -> some View) where Content == AnyView {
        self.content = { ctx in AnyView(content(ctx)) }
    }
    public init(_ schedule: PeriodicTimelineSchedule, @ViewBuilder content: @escaping (Context) -> some View) where Content == AnyView {
        self.content = { ctx in AnyView(content(ctx)) }
    }
    let content: (Context) -> Content
    public var body: some View { content(Context()) }
    public struct Context { public var date: Date { Date() } }
}
public struct PeriodicTimelineSchedule {
    public init(from: Date? = nil, by: TimeInterval = 1) {}
    public static func periodic(from: Date? = nil, by: TimeInterval = 1) -> PeriodicTimelineSchedule { PeriodicTimelineSchedule() }
}

public protocol UIViewControllerRepresentable: View {
    associatedtype UIViewControllerType
    func makeUIViewController(context: Self.Context) -> UIViewControllerType
    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Self.Context)
    typealias Context = UIViewControllerRepresentableContext<Self>
}
public struct UIViewControllerRepresentableContext<T> {}
extension UIViewControllerRepresentable {
    public var body: some View { EmptyView() }
}

// ---- controls ----
public struct Button<Label: View>: View {
    public init(action: @escaping () -> Void, @ViewBuilder label: () -> Label) { self.label = label(); self.action = action }
    public init(_ title: some StringProtocol, action: @escaping () -> Void) where Label == Text { label = Text(String(title)) as! Label; self.action = action }
    public init(_ title: some StringProtocol, role: ButtonRole? = nil, action: @escaping () -> Void) where Label == Text { label = Text(String(title)) as! Label; self.action = action }
    let label: Label
    let action: () -> Void
    public var body: some View { label }
}
public enum ButtonRole { case destructive, cancel }
public struct PlainButtonStyle: ButtonStyle {}
public protocol ButtonStyle {}
extension ButtonStyle where Self == PlainButtonStyle {
    public static var plain: PlainButtonStyle { PlainButtonStyle() }
}

public struct TextField: View {
    public init(_ title: String, text: Binding<String>, prompt: Text? = nil, axis: Axis.Set = .vertical) { body = EmptyView() }
    public let body: EmptyView
}
public struct SecureField: View {
    public init(_ title: String, text: Binding<String>, prompt: Text? = nil) { body = EmptyView() }
    public let body: EmptyView
}

public struct Menu<Label: View, Content: View>: View {
    public init(@ViewBuilder content: () -> Content, @ViewBuilder label: () -> Label) { self.label = label(); self.content = content() }
    let label: Label
    let content: Content
    public var body: some View { label }
}
public struct ProgressView: View {
    public init() { body = EmptyView() }
    public let body: EmptyView
}
public struct Picker<Label: View, SelectionValue: Hashable, Content: View>: View {
    public init(_ title: String = "", selection: Binding<SelectionValue>, @ViewBuilder content: () -> Content) where Label == EmptyView { body = content() }
    public init(selection: Binding<SelectionValue>, @ViewBuilder content: () -> Content) where Label == EmptyView { body = content() }
    public let body: Content
}
public struct WheelPickerStyle: PickerStyle {}
public protocol PickerStyle {}
extension PickerStyle where Self == WheelPickerStyle {
    public static var wheel: WheelPickerStyle { WheelPickerStyle() }
}

public struct ShareLink<Label: View>: View {
    public init(item: String, subject: Text? = nil, @ViewBuilder label: () -> Label) { self.label = label() }
    let label: Label
    public var body: some View { label }
}
public struct PHPickerFilter { public static var images: PHPickerFilter { PHPickerFilter() } }
public struct PhotosPicker<Label: View>: View {
    public init(selection: Binding<PhotosPickerItem?>, matching: PHPickerFilter? = nil, @ViewBuilder label: () -> Label) { self.label = label() }
    let label: Label
    public var body: some View { label }
}
public struct PhotosPickerItem: Hashable, Transferable {
    public init() {}
    public func hash(into h: inout Hasher) {}
    public static func == (l: PhotosPickerItem, r: PhotosPickerItem) -> Bool { true }
    public func loadTransferable<T: Transferable>(type: T.Type) async throws -> T? { nil }
}
public protocol Transferable {}
extension Data: Transferable {}

// ---- data views ----
public struct ForEach<Data: RandomAccessCollection, ID: Hashable, Content: View>: View {
    public init(_ data: Data, id: KeyPath<Data.Element, ID>, @ViewBuilder content: @escaping (Data.Element) -> Content) { self.content = content; self.data = data }
    public init(_ data: Data, @ViewBuilder content: @escaping (Data.Element) -> Content) where ID == Data.Element.ID, Data.Element: Identifiable { self.content = content; self.data = data }
    public init(_ range: Range<Int>, id: KeyPath<Int, Int>, @ViewBuilder content: @escaping (Int) -> Content) where Data == Range<Int>, ID == Int, Data.Element == Int { self.content = content; self.data = range }
    let data: Data
    let content: (Data.Element) -> Content
    public var body: some View { EmptyView() }
}
public struct Section<Parent: View, Content: View, Footer: View>: View {
    public init(@ViewBuilder content: () -> Content) where Parent == EmptyView, Footer == EmptyView { self.content = content() }
    public init(@ViewBuilder header: () -> Parent, @ViewBuilder content: () -> Content) where Footer == EmptyView { self.content = content() }
    let content: Content
    public var body: some View { content }
}
public struct List<Content: View>: View {
    public init(@ViewBuilder content: () -> Content) { self.content = content() }
    let content: Content
    public var body: some View { content }
}

// ---- reactive ----
public protocol ObservableObject: AnyObject {
    var objectWillChange: ObservableObjectPublisher { get }
}
public final class ObservableObjectPublisher { public func send() {} }
extension ObservableObject {
    public var objectWillChange: ObservableObjectPublisher { ObservableObjectPublisher() }
}
@propertyWrapper public final class Published<Value> {
    var storage: Value
    public init(wrappedValue initialValue: Value) { storage = initialValue }
    public var wrappedValue: Value { get { storage } set { storage = newValue } }
    public var projectedValue: PublishedPublisher<Value> { PublishedPublisher() }
}
public struct PublishedPublisher<T> {}
@propertyWrapper public final class EnvironmentObject<ObjectType: ObservableObject> {
    public init() {}
    public var wrappedValue: ObjectType { fatalError() }
}
@propertyWrapper public final class ObservedObject<ObjectType: ObservableObject> {
    var obj: ObjectType
    public init(wrappedValue: ObjectType) { obj = wrappedValue }
    public var wrappedValue: ObjectType { obj }
}
@propertyWrapper public final class StateObject<ObjectType: ObservableObject> {
    var obj: ObjectType
    public init(wrappedValue: @autoclosure @escaping () -> ObjectType) { obj = wrappedValue() }
    public var wrappedValue: ObjectType { obj }
}
@propertyWrapper public final class State<Value> {
    var storage: Value
    public init(wrappedValue initialValue: Value) { storage = initialValue }
    public init(initialValue: Value) { storage = initialValue }
    public var wrappedValue: Value { get { storage } set { storage = newValue } }
    public var projectedValue: Binding<Value> { Binding(get: { self.storage }, set: { self.storage = $0 }) }
}
@propertyWrapper public final class Binding<Value> {
    var v: Value
    public init(get: @escaping () -> Value, set: @escaping (Value) -> Void) { v = (Optional<Any>.none as! Value) }
    public static func constant(_ value: Value) -> Binding<Value> { Binding(get: { fatalError() }, set: { _ in }) }
    public var wrappedValue: Value { get { v } set { v = newValue } }
}
@propertyWrapper public struct Environment<Value> {
    public init(_ keyPath: KeyPath<EnvironmentValues, Value>) {}
    public var wrappedValue: Value { fatalError() }
}
public struct EnvironmentValues {
    public var dismiss: DismissAction { DismissAction() }
    public var editMode: Binding<EditMode> = Binding.constant(.inactive)
    public init() {}
}
public struct DismissAction { public func callAsFunction() {} }
public enum EditMode: Equatable { case active, inactive, transient }
public func withAnimation<Result>(_ animation: Animation? = nil, _ body: () throws -> Result) rethrows -> Result { try body() }
public struct Animation {
    public static func easeInOut(duration: Double) -> Animation { Animation() }
    public static func spring(response: Double = 0.5, dampingFraction: Double = 0.825, blendDuration: Double = 0) -> Animation { Animation() }
    public func repeatForever(autoreverses: Bool = true) -> Animation { Animation() }
}
public struct AnyTransition {
    public static var opacity: AnyTransition { AnyTransition() }
    public static func move(edge: Edge) -> AnyTransition { AnyTransition() }
    public func combined(with other: AnyTransition) -> AnyTransition { AnyTransition() }
}
public enum Edge: Hashable {
    case top, bottom, leading, trailing
    public struct Set: OptionSet {
        public var rawValue: Int
        public init(rawValue: Int) { self.rawValue = rawValue }
        public init() { self = .all }
        public static let all = Set()
        public static let horizontal = Set()
        public static let vertical = Set()
        public static let top = Set()
        public static let bottom = Set()
        public static let leading = Set()
        public static let trailing = Set()
    }
}
public enum Visibility { case visible, hidden, automatic }
public struct PresentationDetent: Hashable, Sendable {
    public static let large = PresentationDetent()
    public static let medium = PresentationDetent()
    public init() {}
}
public enum SubmitLabel { case done, next, go, search, `return`, join, route, send }
public enum TextCase { case never, characters, words, sentences }
public enum KeyboardDismissMode { case automatic, interactive, immediately, onDrag, interactively }
public struct FillStyle { public init() {} }
public struct SafeAreaRegions: OptionSet {
    public var rawValue: Int
    public init(rawValue: Int) { self.rawValue = rawValue }
    public static let all = SafeAreaRegions(rawValue: 1)
    public static let container = SafeAreaRegions(rawValue: 2)
    public static let keyboard = SafeAreaRegions(rawValue: 4)
}

// App lifecycle
@MainActor public protocol App {
    associatedtype Body: Scene
    var body: Body { get }
    static func main()
}
extension App {
    public static func main() {}
}
@MainActor public protocol Scene {}
public struct WindowGroup<Content: View>: Scene { public init(@ViewBuilder content: () -> Content) {} }

// ---- View modifiers (all permissive, return self) ----
extension View {
    public func padding(_ edges: Edge.Set = .all, _ length: Double? = nil) -> _Mod { _Mod() }
    public func padding(_ length: Double) -> _Mod { _Mod() }
    public func padding(_ insets: EdgeInsets) -> _Mod { _Mod() }
    public func frame(width: Double? = nil, height: Double? = nil, alignment: Alignment = .center) -> _Mod { _Mod() }
    public func frame(minWidth: Double? = nil, idealWidth: Double? = nil, maxWidth: Double? = .infinity, minHeight: Double? = nil, idealHeight: Double? = nil, maxHeight: Double? = .infinity, alignment: Alignment = .center) -> _Mod { _Mod() }
    public func background<B: View>(_ background: B, alignment: Alignment = .center) -> some View { background }
    public func background<B: View>(alignment: Alignment = .center, @ViewBuilder content: () -> B) -> some View { content() }
    public func overlay<B: View>(_ overlay: B, alignment: Alignment = .center) -> some View { overlay }
    public func overlay<B: View>(alignment: Alignment = .center, @ViewBuilder content: () -> B) -> some View { content() }
    public func foregroundColor(_ color: Color?) -> _Mod { _Mod() }
    public func foregroundStyle<S: ShapeStyle>(_ style: S) -> _Mod { _Mod() }
    public func font(_ font: Font?) -> _Mod { _Mod() }
    public func lineLimit(_ n: Int?) -> _Mod { _Mod() }
    public func multilineTextAlignment(_ a: TextAlignment) -> _Mod { _Mod() }
    public func lineSpacing(_ s: Double) -> _Mod { _Mod() }
    public func minimumScaleFactor(_ f: Double) -> _Mod { _Mod() }
    public func buttonStyle<S: ButtonStyle>(_ style: S) -> _Mod { _Mod() }
    public func disabled(_ b: Bool) -> _Mod { _Mod() }
    public func onAppear(perform action: (() -> Void)? = nil) -> _Mod { _Mod() }
    public func onDisappear(perform action: (() -> Void)? = nil) -> _Mod { _Mod() }
    public func onChange<V: Equatable>(of value: V, perform action: @escaping (V) -> Void) -> _Mod { _Mod() }
    public func onChange<V>(of value: V, initial: Bool = false, _ action: @escaping (V, V) -> Void) -> _Mod { _Mod() }
    public func onTapGesture(count: Int = 1, perform action: @escaping () -> Void) -> _Mod { _Mod() }
    public func sheet<C: View>(isPresented: Binding<Bool>, onDismiss: (() -> Void)? = nil, @ViewBuilder content: () -> C) -> _Mod { _Mod() }
    public func fileImporter(isPresented: Binding<Bool>, allowedContentTypes: [UTType], onCompletion: @escaping (Result<URL, Error>) -> Void) -> _Mod { _Mod() }
    public func presentationDetents(_ d: Set<PresentationDetent>) -> _Mod { _Mod() }
    public func scrollContentBackground(_ v: Visibility) -> _Mod { _Mod() }
    public func listStyle<S: ListStyle>(_ s: S) -> _Mod { _Mod() }
    public func listRowBackground<V: View>(_ v: V) -> _Mod { _Mod() }
    public func listRowInsets(_ i: EdgeInsets?) -> _Mod { _Mod() }
    public func listRowSeparator(_ v: Visibility) -> _Mod { _Mod() }
    public func listSectionSeparator(_ v: Visibility) -> _Mod { _Mod() }
    public func scrollDismissesKeyboard(_ m: KeyboardDismissMode) -> _Mod { _Mod() }
    public func preferredColorScheme(_ c: ColorScheme?) -> _Mod { _Mod() }
    public func ignoresSafeArea(_ r: SafeAreaRegions = .all, edges: Edge.Set = .all) -> _Mod { _Mod() }
    public func transition(_ t: AnyTransition) -> _Mod { _Mod() }
    public func animation<V: Equatable>(_ a: Animation?, value v: V) -> _Mod { _Mod() }
    public func animation(_ a: Animation?) -> _Mod { _Mod() }
    public func scaleEffect(_ s: CGFloat) -> _Mod { _Mod() }
    public func offset(x: Double = 0, y: Double = 0) -> _Mod { _Mod() }
    public func keyboardType(_ t: UIKeyboardType) -> _Mod { _Mod() }
    public func submitLabel(_ l: SubmitLabel) -> _Mod { _Mod() }
    public func autocorrectionDisabled(_ b: Bool = true) -> _Mod { _Mod() }
    public func textInputAutocapitalization(_ m: TextCase) -> _Mod { _Mod() }
    public func tint(_ c: Color?) -> _Mod { _Mod() }
    public func tag<V: Hashable>(_ v: V) -> _Mod { _Mod() }
    public func pickerStyle<S: PickerStyle>(_ s: S) -> _Mod { _Mod() }
    public func colorMultiply(_ c: Color) -> _Mod { _Mod() }
    public func onOpenURL(perform action: @escaping (URL) -> Void) -> _Mod { _Mod() }
    public func allowsHitTesting(_ b: Bool) -> _Mod { _Mod() }
    public func clipShape<S: Shape>(_ s: S, style: FillStyle = FillStyle()) -> _Mod { _Mod() }
    public func opacity(_ d: Double) -> _Mod { _Mod() }
    public func onMove(perform action: @escaping (IndexSet, Int) -> Void) -> _Mod { _Mod() }
    public func environmentObject(_ o: some ObservableObject) -> _Mod { _Mod() }
    public func environment<V>(_ k: WritableKeyPath<EnvironmentValues, V>, _ v: V) -> _Mod { _Mod() }
    public func monospacedDigit() -> _Mod { _Mod() }
    public func confirmationDialog<C: View>(_ title: String, isPresented: Binding<Bool>, titleVisibility: Visibility = .automatic, @ViewBuilder actions: () -> C) -> _Mod { _Mod() }
    public func task(priority: Int = 0, _ a: @escaping () async -> Void) -> _Mod { _Mod() }
    public func resizable() -> _Mod { _Mod() }
    public func scaledToFill() -> _Mod { _Mod() }
    public func scaledToFit() -> _Mod { _Mod() }
    public func hidden() -> _Mod { _Mod() }
}
public protocol ListStyle {}
public struct PlainListStyle: ListStyle {}
extension ListStyle where Self == PlainListStyle { public static var plain: PlainListStyle { PlainListStyle() } }

extension Shape {
    public func fill(_ color: Color, style: FillStyle = FillStyle()) -> _Mod { _Mod() }
    public func fill(_ fillStyle: ShapeStyle, style: FillStyle = FillStyle()) -> _Mod { _Mod() }
    public func strokeBorder(_ color: Color, lineWidth: CGFloat = 1) -> _Mod { _Mod() }
    public func stroke(_ color: Color, lineWidth: CGFloat = 1) -> _Mod { _Mod() }
    public func stroke(_ style: StrokeStyle) -> _Mod { _Mod() }
}
public struct _Mod: View {
    nonisolated public init() {}
    public var body: some View { EmptyView() }
}
public struct _ShapeView: View {
    nonisolated public init() {}
    public var body: some View { EmptyView() }
}
fileprivate func _sv() -> _ShapeView { _ShapeView() }

extension Array {
    public mutating func move(fromOffsets source: IndexSet, toOffset destination: Int) {}
    public mutating func remove(atOffsets member: IndexSet) {}
}

public enum UIKeyboardType { case `default`, asciiCapable, emailAddress, decimalPad, numberPad, phonePad }

// Canvas in real SwiftUI: Canvas { ctx, size in } — the closure is a ViewBuilder-free renderer.
// We model it as a View whose init takes the renderer directly.
public struct Canvas2<Content: View>: View {
    public init(renderer: @escaping (GraphicsContext, CGSize) -> Void) { self.renderer = renderer }
    let renderer: (GraphicsContext, CGSize) -> Void
    public var body: some View { EmptyView() }
}


// ---- result builder ----
@resultBuilder
public struct ViewBuilder {
    public static func buildBlock() -> EmptyView { _ev() }
    public static func buildBlock<C0: View>(_ c0: C0) -> C0 { c0 }
    public static func buildBlock<C0: View, C1: View>(_ c0: C0, _ c1: C1) -> TupleView<(C0, C1)> { TupleView() }
    public static func buildBlock<C0: View, C1: View, C2: View>(_ c0: C0, _ c1: C1, _ c2: C2) -> TupleView<(C0, C1, C2)> { TupleView() }
    public static func buildBlock<C0: View, C1: View, C2: View, C3: View>(_ c0: C0, _ c1: C1, _ c2: C2, _ c3: C3) -> TupleView<(C0, C1, C2, C3)> { TupleView() }
    public static func buildBlock<C0: View, C1: View, C2: View, C3: View, C4: View>(_ c0: C0, _ c1: C1, _ c2: C2, _ c3: C3, _ c4: C4) -> TupleView<(C0, C1, C2, C3, C4)> { TupleView() }
    public static func buildBlock<C0: View, C1: View, C2: View, C3: View, C4: View, C5: View>(_ c0: C0, _ c1: C1, _ c2: C2, _ c3: C3, _ c4: C4, _ c5: C5) -> TupleView<(C0, C1, C2, C3, C4, C5)> { TupleView() }
    public static func buildBlock<C0: View, C1: View, C2: View, C3: View, C4: View, C5: View, C6: View>(_ c0: C0, _ c1: C1, _ c2: C2, _ c3: C3, _ c4: C4, _ c5: C5, _ c6: C6) -> TupleView<(C0, C1, C2, C3, C4, C5, C6)> { TupleView() }
    public static func buildBlock<C0: View, C1: View, C2: View, C3: View, C4: View, C5: View, C6: View, C7: View>(_ c0: C0, _ c1: C1, _ c2: C2, _ c3: C3, _ c4: C4, _ c5: C5, _ c6: C6, _ c7: C7) -> TupleView<(C0, C1, C2, C3, C4, C5, C6, C7)> { TupleView() }
    public static func buildBlock<C0: View, C1: View, C2: View, C3: View, C4: View, C5: View, C6: View, C7: View, C8: View>(_ c0: C0, _ c1: C1, _ c2: C2, _ c3: C3, _ c4: C4, _ c5: C5, _ c6: C6, _ c7: C7, _ c8: C8) -> TupleView<(C0, C1, C2, C3, C4, C5, C6, C7, C8)> { TupleView() }
    public static func buildBlock<C0: View, C1: View, C2: View, C3: View, C4: View, C5: View, C6: View, C7: View, C8: View, C9: View>(_ c0: C0, _ c1: C1, _ c2: C2, _ c3: C3, _ c4: C4, _ c5: C5, _ c6: C6, _ c7: C7, _ c8: C8, _ c9: C9) -> TupleView<(C0, C1, C2, C3, C4, C5, C6, C7, C8, C9)> { TupleView() }
    public static func buildOptional<C: View>(_ c: C?) -> OptionalContent<C> { OptionalContent() }
    public static func buildEither<F: View, T: View>(first: F) -> _ConditionalContent<F, T> { _ConditionalContent() }
    public static func buildEither<F: View, T: View>(second: T) -> _ConditionalContent<F, T> { _ConditionalContent() }
    public static func buildLimitedAvailability<C: View>(_ c: C) -> C { c }
    public static func buildExpression<C: View>(_ c: C) -> C { c }
}
public struct TupleView<T>: View {
    nonisolated public init() {}
    public var body: some View { EmptyView() }
}
public struct OptionalContent<C: View>: View {
    nonisolated public init() {}
    public var body: some View { EmptyView() }
}
public struct _ConditionalContent<F: View, T: View>: View {
    nonisolated public init() {}
    public var body: some View { EmptyView() }
}
