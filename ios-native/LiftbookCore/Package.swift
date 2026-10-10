// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "LiftbookCore",
    platforms: [.iOS(.v17), .macOS(.v13)],
    products: [
        .library(name: "LiftbookCore", targets: ["LiftbookCore"]),
    ],
    targets: [
        .target(name: "LiftbookCore"),
        .testTarget(
            name: "LiftbookCoreTests",
            dependencies: ["LiftbookCore"],
            resources: [.copy("fixtures")]
        ),
    ]
)
