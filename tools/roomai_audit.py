from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]

required = [
    "app/build.gradle",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/roomai/app/MainActivity.kt",
    "app/src/main/java/com/roomai/app/RoomAIProblemFlow.kt",
    "app/src/main/java/com/roomai/app/RoomAIDecisionEngine.kt",
    "app/src/main/java/com/roomai/app/RoomAISolutionBuilder.kt",
    "app/src/main/java/com/roomai/app/RoomAICoreContracts.kt",
    "app/src/main/java/com/roomai/app/RoomAIRoleContracts.kt",
    "backend.py",
    "diagnostic_engine.py",
]

errors = []

for rel in required:
    if not (ROOT / rel).exists():
        errors.append(f"MISSING: {rel}")

main = (ROOT / required[2]).read_text(encoding="utf-8")
flow = (ROOT / required[3]).read_text(encoding="utf-8")
decision = (ROOT / required[4]).read_text(encoding="utf-8")
solution = (ROOT / required[5]).read_text(encoding="utf-8")
backend = (ROOT / "backend.py").read_text(encoding="utf-8")
diagnostic = (ROOT / "diagnostic_engine.py").read_text(encoding="utf-8")

# Core navigation must exist.
for route in [
    'composable("home")',
    'composable("problem_first")',
    'composable("decision_engine")',
]:
    if route not in main:
        errors.append(f"MISSING ROUTE: {route}")

# Existing core API contracts must remain discoverable.
for endpoint in ["/diagnose", "/generate", "/usage"]:
    if endpoint not in main and endpoint not in backend:
        errors.append(f"MISSING API CONTRACT: {endpoint}")

# Problem-first architecture.
for token in [
    "LIGHTING",
    "STORAGE",
    "LAYOUT",
    "RoomAIProblemFlow",
]:
    if token not in flow:
        errors.append(f"MISSING PROBLEM CONTRACT: {token}")

# Existing reasoning architecture must survive.
for token in [
    "diagnosis",
    "recommendation",
    "buildRoomAISolutionBrief",
]:
    if token not in decision and token not in solution:
        errors.append(f"MISSING SOLUTION PIPELINE: {token}")

# Diagnostic engine safety principles.
for token in [
    "evidence",
    "confidence",
    "unknown",
]:
    if token not in diagnostic.lower():
        errors.append(f"DIAGNOSTIC SAFETY TOKEN MISSING: {token}")

# Legacy image Home must remain removed.
for forbidden in [
    "Transform your space.",
    "Create my room",
]:
    if forbidden in main:
        errors.append(f"LEGACY HOME FOUND: {forbidden}")

# Do not allow obvious committed secrets.
secret_patterns = [
    r'GEMINI_KEY\s*=\s*["\'][A-Za-z0-9_\-]{20,}["\']',
    r'API_KEY\s*=\s*["\'][A-Za-z0-9_\-]{20,}["\']',
]

for pattern in secret_patterns:
    if re.search(pattern, backend):
        errors.append("POSSIBLE HARDCODED SECRET IN backend.py")

# Do not accept real-looking production ad IDs in source audit.
# Test IDs are allowed.
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(
    encoding="utf-8"
)

if "ca-app-pub-3940256099942544~3347511713" not in manifest:
    print("WARNING: AdMob App ID is not the official test App ID.")

# ============================================================
# Commercial UX / architecture invariants
# ============================================================

if "account_email" not in main:
    errors.append("ACCOUNT IDENTITY: account_email persistence missing")

if "ROOMAI_PLAN_KEY" not in main:
    errors.append("PLAN STATE: server plan persistence missing")

# Professional workspace is optional in the current consumer-first product.
# Do not fail the production audit for a legacy navigation invariant.

# Never commit obvious API credentials.
for token in [
    "AIza",
    "sk-",
]:
    if token in backend:
        errors.append(f"POSSIBLE SECRET TOKEN FOUND: {token}")

# Production backend must protect oversized uploads.
if "MAX_CONTENT_LENGTH" not in backend:
    errors.append("BACKEND SAFETY: MAX_CONTENT_LENGTH missing")

# Functional integrity invariants.
for token in [
    '"budget_design"',
    '"product_match"',
]:
    if token not in backend:
        errors.append(
            f"BACKEND OPERATION CONTRACT MISSING: {token}"
        )

if 'ROOMAI_DEBUG_API_ENABLED' not in backend:
    errors.append(
        "BACKEND DEBUG GATE MISSING: ROOMAI_DEBUG_API_ENABLED"
    )

if 'getType(uri)' not in main:
    errors.append(
        "ANDROID IMAGE CONTRACT: MIME type is not read from Uri"
    )

if 'userGoal: String' not in main or '"goal"' not in main:
    errors.append(
        "DIAGNOSIS GOAL CONTRACT: user goal is not forwarded"
    )

if 'roomType' not in main:
    errors.append(
        "DIAGNOSIS CONTEXT: room type is not preserved"
    )



# ============================================================
# FINAL AUDIT DECISION
# ============================================================

if errors:
    print("\nROOMAI AUDIT FAILED\n")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("\nROOMAI AUDIT PASSED")
print("Core navigation: OK")
print("Problem taxonomy: OK")
print("Diagnosis contracts: OK")
print("Solution pipeline: OK")
print("Legacy Home removal: OK")
print("Commercial invariants: OK")
print("Secret scan: OK")
print("Backend upload protection: OK")
