#!/usr/bin/env python3
from pathlib import Path
import json,re,sys,xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
errors=[]
def fail(msg): errors.append(msg)
def read(p): return (ROOT/p).read_text(encoding='utf-8')

def xml_keys(path):
    tree=ET.parse(ROOT/path); return {e.attrib.get('{http://schemas.microsoft.com/winfx/2006/xaml}Key') for e in tree.getroot() if e.attrib.get('{http://schemas.microsoft.com/winfx/2006/xaml}Key')}

def android_keys(path):
    tree=ET.parse(ROOT/path); return {e.attrib['name'] for e in tree.getroot().findall('string')}

v=json.loads(read('version.json'))
if v.get('releaseChannel') not in {'rc','stable'}: fail('version.json releaseChannel must be rc or stable')
gradle=read('apps/android/app/build.gradle.kts')
csproj=read('apps/builder/FieldTakHub.Builder/FieldTakHub.Builder.csproj')
if f'versionCode = {v["androidVersionCode"]}' not in gradle: fail('Android versionCode != version.json')
if f'versionName = "{v["hubVersion"]}"' not in gradle: fail('Android versionName != version.json')
if f'<Version>{v["builderVersion"]}</Version>' not in csproj: fail('Builder Version != version.json')
# releaseChannel has a single canonical value in version.json; implementation may embed it for runtime, but gate does not demand it in csproj.
if f'RELEASE_CHANNEL", "\\"{v["releaseChannel"]}\\""' not in gradle: fail('Android runtime release channel does not match version.json')

en=android_keys('apps/android/app/src/main/res/values/strings.xml'); pl=android_keys('apps/android/app/src/main/res/values-pl/strings.xml')
if en!=pl: fail(f'Android PL/EN resource mismatch: only-en={sorted(en-pl)}, only-pl={sorted(pl-en)}')
wen=xml_keys('apps/builder/FieldTakHub.Builder/Resources/Strings.en.xaml'); wpl=xml_keys('apps/builder/FieldTakHub.Builder/Resources/Strings.pl.xaml')
if wen!=wpl: fail(f'Builder PL/EN resource mismatch: only-en={sorted(wen-wpl)}, only-pl={sorted(wpl-wen)}')


# Every Android R.string reference in production Kotlin must exist in the base English resources.
android_refs=set()
for kp in (ROOT/'apps/android/app/src/main/java').rglob('*.kt'):
    android_refs.update(re.findall(r'R\.string\.([A-Za-z0-9_]+)', kp.read_text(encoding='utf-8')))
missing_android=android_refs-en
if missing_android: fail(f'Android missing string resources referenced from Kotlin: {sorted(missing_android)}')

# Every WPF DynamicResource reference must exist in the English resource dictionary.
wpf_refs=set()
for xp in (ROOT/'apps/builder/FieldTakHub.Builder').rglob('*.xaml'):
    if 'Resources' in xp.parts: continue
    wpf_refs.update(re.findall(r'\{DynamicResource\s+([A-Za-z0-9_.-]+)\}', xp.read_text(encoding='utf-8')))
missing_wpf=wpf_refs-wen
if missing_wpf: fail(f'Builder missing DynamicResource keys referenced from XAML: {sorted(missing_wpf)}')

# Package integrity contract: Builder signs META-INF/server.txt and Android must compare all signed files,
# excluding only the cryptographic metadata that cannot sign itself.
repo=read('apps/android/app/src/main/java/org/fieldtak/hub/data/PackageRepository.kt')
builder=read('apps/builder/FieldTakHub.Builder/Services/FtakPackageBuilder.cs')
if 'META-INF/server.txt' not in builder and 'Path.Combine(meta, "server.txt")' not in builder:
    fail('Builder no longer emits signed META-INF/server.txt')
for required_unsigned in ('META-INF/checksums.sha256','META-INF/signature.ed25519','META-INF/publisher.pub'):
    if required_unsigned not in repo: fail(f'Android verifier unsigned-meta contract missing {required_unsigned}')
if 'actualSignedFiles != listed.toSet()' not in repo:
    fail('Android verifier does not enforce exact signed-file list equality')

# Provision descriptor must carry pre-download storage hints end-to-end.
dist=read('apps/builder/FieldTakHub.Builder/Services/DistributionServer.cs')
models=read('apps/android/app/src/main/java/org/fieldtak/hub/model/Models.kt')
vm=read('apps/android/app/src/main/java/org/fieldtak/hub/MainViewModel.kt')
for token in ('packageBytes','recommendedFreeBytes'):
    if token not in dist: fail(f'Builder provision descriptor missing {token}')
    if token not in models: fail(f'Android provision descriptor model missing {token}')
    if token not in vm: fail(f'Android pre-download storage check missing {token}')

# Parse source JSON/XML/XAML.
for p in ROOT.rglob('*.json'):
    if any(x in p.parts for x in ('.git','build','bin','obj')): continue
    try: json.loads(p.read_text(encoding='utf-8'))
    except Exception as e: fail(f'JSON parse {p.relative_to(ROOT)}: {e}')
for ext in ('*.xml','*.xaml'):
    for p in ROOT.rglob(ext):
        if any(x in p.parts for x in ('.git','build','bin','obj')): continue
        try: ET.parse(p)
        except Exception as e: fail(f'XML/XAML parse {p.relative_to(ROOT)}: {e}')

# Large packages/maps must be hashed as streams, never loaded wholly into RAM.
for cp in (ROOT/'apps/builder').rglob('*.cs'):
    txt=cp.read_text(encoding='utf-8')
    if re.search(r'SHA256\.HashData\(\s*(?:await\s+)?File\.ReadAllBytes',txt):
        fail(f'Non-streaming SHA-256 in Builder: {cp.relative_to(ROOT)}')

# No production secrets/certificate material belongs in source tree.
for p in ROOT.rglob('*'):
    if not p.is_file() or '.git' in p.parts: continue
    if p.suffix.lower() in {'.p12','.pfx','.jks','.keystore','.key'} and not p.name.endswith('.example'):
        fail(f'Potential secret/certificate file committed: {p.relative_to(ROOT)}')

required=[
 'apps/android/app/src/main/java/org/fieldtak/hub/update/UpdateService.kt',
 'apps/android/app/src/main/java/org/fieldtak/hub/security/UrlPolicy.kt',
 'apps/android/app/src/main/java/org/fieldtak/hub/storage/StorageMaintenance.kt',
 'apps/builder/FieldTakHub.Builder/Services/UpdateService.cs',
 'apps/builder/FieldTakHub.Builder/Services/ServerDiagnosticsService.cs',
 'apps/builder/FieldTakHub.Builder/Services/ServerValidator.cs',
 'docs/ADMIN_GUIDE.md','docs/BUILD_AND_SIGN.md','docs/UPDATES.md','docs/ACCESSIBILITY.md','docs/RELEASE_CHECKLIST.md'
]
for r in required:
    if not (ROOT/r).exists(): fail(f'Missing required release file: {r}')

release=read('.github/workflows/release.yml') if (ROOT/'.github/workflows/release.yml').exists() else ''
if 'assembleRelease' not in release: fail('Release workflow must build assembleRelease')
if 'fieldtak-release.json' not in release: fail('Release workflow must publish fieldtak-release.json')
if 'assembleDebug' in release: fail('Release workflow must not publish a debug APK')

if errors:
    print('SOURCE GATE: FAIL')
    for e in errors: print(' -',e)
    sys.exit(1)
print('SOURCE GATE: PASS')
print(f'Hub={v["hubVersion"]} Builder={v["builderVersion"]} channel={v["releaseChannel"]}')
print(f'Android strings={len(en)} Builder strings={len(wen)}')
