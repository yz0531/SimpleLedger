import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

private struct SkinVariant {
    let source: String
    let output: String
    let tint: CGColor
    let tintAlpha: CGFloat
    let contrastOverlay: CGColor
    let contrastAlpha: CGFloat
    let scale: CGFloat
    let horizontalOffset: CGFloat
    let flipHorizontally: Bool
}

private let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
    .appendingPathComponent("app/src/main/res/drawable-nodpi")
private let width = 1080
private let height = 2400

private func color(_ red: CGFloat, _ green: CGFloat, _ blue: CGFloat) -> CGColor {
    CGColor(red: red, green: green, blue: blue, alpha: 1)
}

private let variants = [
    SkinVariant(
        source: "skin_jade.jpg",
        output: "skin_forest.jpg",
        tint: color(0.02, 0.30, 0.20),
        tintAlpha: 0.30,
        contrastOverlay: color(0.02, 0.08, 0.05),
        contrastAlpha: 0.12,
        scale: 1.13,
        horizontalOffset: -38,
        flipHorizontally: true,
    ),
    SkinVariant(
        source: "skin_sunset.jpg",
        output: "skin_amber.jpg",
        tint: color(0.90, 0.46, 0.08),
        tintAlpha: 0.34,
        contrastOverlay: color(0.22, 0.08, 0.01),
        contrastAlpha: 0.12,
        scale: 1.16,
        horizontalOffset: 44,
        flipHorizontally: true,
    ),
    SkinVariant(
        source: "skin_ocean.jpg",
        output: "skin_alpine.jpg",
        tint: color(0.12, 0.56, 0.72),
        tintAlpha: 0.26,
        contrastOverlay: color(0.02, 0.14, 0.22),
        contrastAlpha: 0.10,
        scale: 1.18,
        horizontalOffset: -54,
        flipHorizontally: true,
    ),
    SkinVariant(
        source: "skin_lavender.jpg",
        output: "skin_rose.jpg",
        tint: color(0.88, 0.24, 0.42),
        tintAlpha: 0.30,
        contrastOverlay: color(0.22, 0.02, 0.10),
        contrastAlpha: 0.08,
        scale: 1.12,
        horizontalOffset: 34,
        flipHorizontally: true,
    ),
    SkinVariant(
        source: "skin_ocean.jpg",
        output: "skin_night.jpg",
        tint: color(0.02, 0.12, 0.28),
        tintAlpha: 0.54,
        contrastOverlay: color(0.01, 0.02, 0.08),
        contrastAlpha: 0.25,
        scale: 1.24,
        horizontalOffset: 40,
        flipHorizontally: false,
    ),
    SkinVariant(
        source: "skin_lavender.jpg",
        output: "skin_aurora.jpg",
        tint: color(0.08, 0.66, 0.58),
        tintAlpha: 0.32,
        contrastOverlay: color(0.25, 0.06, 0.38),
        contrastAlpha: 0.16,
        scale: 1.20,
        horizontalOffset: -46,
        flipHorizontally: false,
    ),
]

for variant in variants {
    let inputURL = root.appendingPathComponent(variant.source) as CFURL
    guard
        let source = CGImageSourceCreateWithURL(inputURL, nil),
        let image = CGImageSourceCreateImageAtIndex(source, 0, nil),
        let context = CGContext(
            data: nil,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: 0,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        )
    else {
        fatalError("Unable to prepare \(variant.source)")
    }

    context.interpolationQuality = .high
    let drawWidth = CGFloat(width) * variant.scale
    let drawHeight = CGFloat(height) * variant.scale
    let drawX = (CGFloat(width) - drawWidth) / 2 + variant.horizontalOffset
    let drawY = (CGFloat(height) - drawHeight) / 2

    context.saveGState()
    if variant.flipHorizontally {
        context.translateBy(x: CGFloat(width), y: 0)
        context.scaleBy(x: -1, y: 1)
    }
    context.draw(image, in: CGRect(x: drawX, y: drawY, width: drawWidth, height: drawHeight))
    context.restoreGState()

    context.setBlendMode(.softLight)
    context.setFillColor(variant.tint.copy(alpha: variant.tintAlpha)!)
    context.fill(CGRect(x: 0, y: 0, width: width, height: height))
    context.setBlendMode(.multiply)
    context.setFillColor(variant.contrastOverlay.copy(alpha: variant.contrastAlpha)!)
    context.fill(CGRect(x: 0, y: 0, width: width, height: height))

    guard let outputImage = context.makeImage() else {
        fatalError("Unable to render \(variant.output)")
    }
    let outputURL = root.appendingPathComponent(variant.output) as CFURL
    guard let destination = CGImageDestinationCreateWithURL(
        outputURL,
        UTType.jpeg.identifier as CFString,
        1,
        nil
    ) else {
        fatalError("Unable to create \(variant.output)")
    }
    CGImageDestinationAddImage(
        destination,
        outputImage,
        [kCGImageDestinationLossyCompressionQuality: 0.84] as CFDictionary,
    )
    guard CGImageDestinationFinalize(destination) else {
        fatalError("Unable to save \(variant.output)")
    }
    print("Generated \(variant.output)")
}
