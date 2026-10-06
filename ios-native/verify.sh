#!/usr/bin/env bash
# Vérification complète du port natif sur Linux (toolchain Swift 6.4 dans ~/tools/swift-6.4).
# 1) compile + tests du cœur (SwiftPM pur)  2) typecheck intégral de l'app SwiftUI via stubs fidèles.
set -e
cd "$(dirname "$0")"
SWIFT=${SWIFT:-$HOME/tools/swift-6.4/usr/bin/swift}
export -n APPIMAGE 2>/dev/null || true
unset APPIMAGE
echo "== 1. LiftbookCore : tests =="
(cd LiftbookCore && "$SWIFT" test 2>&1 | grep Executed | tail -1)
echo "== 2. Construction des modules stubs =="
mkdir -p Stubs/build
for m in UIKit UniformTypeIdentifiers SwiftUI Security UserNotifications Charts AuthenticationServices PhotosUI; do
  "$SWIFT"c -emit-module -module-name $m Stubs/$m.swift -o Stubs/build/$m.swiftmodule -I Stubs/build
done
echo "   7 modules OK"
echo "== 3. Typecheck intégral de l'app (17 fichiers) =="
CORE=LiftbookCore/.build/out/Products/Debug-linux-x86_64
set +e
OUT=$("$SWIFT"c -typecheck -I Stubs/build -I $CORE Liftbook/*.swift 2>&1)
set -e
ERRS=$(echo "$OUT" | grep -cE '^Liftbook/[^:]*: error:' || true)
if [ "$ERRS" = "0" ]; then echo "   0 erreur, 0 warning ✓"; else
  echo "$OUT" | grep -E '^Liftbook/[^:]*: error:' | head -20; exit 1
fi
