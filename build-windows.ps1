[CmdletBinding()]
param(
    [string]$SdkRoot = $env:ANDROID_SDK_ROOT,
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$BuildToolsVersion = '37.0.0',
    [string]$Platform = 'android-35',
    [switch]$TestsOnly
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
if (-not $SdkRoot) { $SdkRoot = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
if (-not $JdkHome) { throw 'Set JAVA_HOME or pass -JdkHome pointing to JDK 17 or newer.' }
$java = Join-Path $JdkHome 'bin/java.exe'
$javac = Join-Path $JdkHome 'bin/javac.exe'
$jar = Join-Path $JdkHome 'bin/jar.exe'
$keytool = Join-Path $JdkHome 'bin/keytool.exe'
$buildTools = Join-Path $SdkRoot "build-tools/$BuildToolsVersion"
$androidJar = Join-Path $SdkRoot "platforms/$Platform/android.jar"
$buildRoot = Join-Path $projectRoot 'build'
$work = Join-Path $buildRoot ('work-' + [guid]::NewGuid().ToString('N'))
$testClasses = Join-Path $work 'test-classes'
$classes = Join-Path $work 'classes'
$dex = Join-Path $work 'dex'
$deps = Join-Path $projectRoot 'deps'
$apiJar = Join-Path $deps 'api-82.jar'
$expectedApiHash = 'F48C635F1C7469FDEC0E00AD2EA0B7A6B2F5B55065784A35B7CA3A84615E8E25'

function Invoke-Checked([string]$Tool, [string[]]$Arguments) {
    & $Tool @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Tool failed with exit code $LASTEXITCODE" }
}

foreach ($path in @($java, $javac)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing required tool: $path" }
}
New-Item -ItemType Directory -Force -Path $testClasses, $classes, $dex, $deps | Out-Null

# JVM tests are intentionally independent of Android and the Xposed framework.
$pure = @(
    (Join-Path $projectRoot 'src/com/m0e_n00b/questpinyinprobe/FixedPinyinSession.java'),
    (Join-Path $projectRoot 'src/com/m0e_n00b/questpinyinprobe/PinyinSession.java'),
    (Join-Path $projectRoot 'src/com/m0e_n00b/questpinyinprobe/PinyinBackend.java'),
    (Join-Path $projectRoot 'src/com/m0e_n00b/questpinyinprobe/PinyinCandidate.java'),
    (Join-Path $projectRoot 'src/com/m0e_n00b/questpinyinprobe/PinyinCandidates.java'),
    (Join-Path $projectRoot 'src/com/m0e_n00b/questpinyinprobe/AsyncPinyinSession.java'),
    (Join-Path $projectRoot 'src/com/m0e_n00b/questpinyinprobe/InputPolicy.java'),
    (Join-Path $projectRoot 'src/com/m0e_n00b/questpinyinprobe/EditorActions.java'),
    (Join-Path $projectRoot 'src/com/m0e_n00b/questpinyinprobe/CompositionRange.java'),
    (Join-Path $projectRoot 'src/com/m0e_n00b/questpinyinprobe/CandidateFit.java')
)
$testSources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'tests') -Filter '*.java' -Recurse | ForEach-Object FullName)
Invoke-Checked $javac (@('-encoding', 'UTF-8', '-source', '8', '-target', '8', '-Xlint:-options', '-d', $testClasses) + $pure + $testSources)
foreach ($test in $testSources) {
    $className = 'com.m0e_n00b.questpinyinprobe.' + [IO.Path]::GetFileNameWithoutExtension($test)
    Invoke-Checked $java @('-ea', '-cp', $testClasses, $className)
}
if ($TestsOnly) { Write-Output 'RESULT=tests_passed'; return }

$dictionary = Join-Path $projectRoot 'assets/ime/google_pinyin.db'
$dictionarySha = '920f621c690355d33ed6b9732bb7fa3e297b36c379d6b352ef3643be43b83635'
if (-not (Test-Path -LiteralPath $dictionary) -or (Get-FileHash -LiteralPath $dictionary -Algorithm SHA256).Hash.ToLowerInvariant() -ne $dictionarySha) {
    throw 'Pinned Pinyin database missing or checksum mismatch.'
}
foreach ($path in @($jar, $keytool, $androidJar, (Join-Path $buildTools 'd8.bat'),
    (Join-Path $buildTools 'aapt2.exe'), (Join-Path $buildTools 'apksigner.bat'),
    (Join-Path $buildTools 'zipalign.exe'), (Join-Path $buildTools 'dexdump.exe'))) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing required Android build tool: $path" }
}
if (-not (Test-Path -LiteralPath $apiJar -PathType Leaf)) {
    $download = Join-Path $deps 'api-82.download'
    Invoke-WebRequest -Uri 'https://api.xposed.info/de/robv/android/xposed/api/82/api-82.jar' -OutFile $download -TimeoutSec 30
    if ((Get-FileHash -LiteralPath $download -Algorithm SHA256).Hash -ne $expectedApiHash) {
        throw 'Official API 82 download hash does not match the pinned artifact.'
    }
    Move-Item -LiteralPath $download -Destination $apiJar
}
if ((Get-FileHash -LiteralPath $apiJar -Algorithm SHA256).Hash -ne $expectedApiHash) {
    throw 'API 82 stub hash mismatch; refuse to compile against an unknown API.'
}

$oldJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $JdkHome
    $sources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Filter '*.java' -Recurse | ForEach-Object FullName)
    Invoke-Checked $javac (@('-encoding', 'UTF-8', '-source', '8', '-target', '8', '-Xlint:-options',
        '-cp', "$androidJar;$apiJar", '-d', $classes) + $sources)
    $classesJar = Join-Path $work 'module-classes.jar'
    Invoke-Checked $jar @('cf', $classesJar, '-C', $classes, '.')
    # The API jar is a classpath stub, not a program input; it never enters classes.dex.
    Invoke-Checked (Join-Path $buildTools 'd8.bat') @('--min-api', '29', '--lib', $androidJar,
        '--classpath', $apiJar, '--output', $dex, $classesJar)
    $resources = Join-Path $work 'resources.zip'
    Invoke-Checked (Join-Path $buildTools 'aapt2.exe') @('compile', '--dir', (Join-Path $projectRoot 'res'), '-o', $resources)
    $unsigned = Join-Path $work 'unsigned.apk'
    Invoke-Checked (Join-Path $buildTools 'aapt2.exe') @('link', '-o', $unsigned, '-I', $androidJar,
        '--manifest', (Join-Path $projectRoot 'AndroidManifest.xml'), '--min-sdk-version', '29',
        '--target-sdk-version', '29', '-A', (Join-Path $projectRoot 'assets'), $resources)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::Open($unsigned, [IO.Compression.ZipArchiveMode]::Update)
    try {
        # Windows aapt2 may emit backslashes for nested assets. Java ZipFile lookup uses
        # exact forward-slash names; normalize before alignment/signing, never afterwards.
        foreach ($assetEntry in @($archive.Entries)) {
            if ($assetEntry.FullName.Contains('\')) {
                $normalizedName = $assetEntry.FullName.Replace('\', '/')
                $buffer = [IO.MemoryStream]::new()
                $assetStream = $assetEntry.Open()
                try { $assetStream.CopyTo($buffer) } finally { $assetStream.Dispose() }
                $assetEntry.Delete()
                $normalized = $archive.CreateEntry($normalizedName, [IO.Compression.CompressionLevel]::Optimal)
                $destination = $normalized.Open()
                try { $buffer.Position = 0; $buffer.CopyTo($destination) }
                finally { $destination.Dispose(); $buffer.Dispose() }
            }
        }
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, (Join-Path $dex 'classes.dex'),
            'classes.dex', [IO.Compression.CompressionLevel]::Optimal) | Out-Null
    } finally { $archive.Dispose() }
    $aligned = Join-Path $work 'aligned.apk'
    Invoke-Checked (Join-Path $buildTools 'zipalign.exe') @('-f', '-p', '4', $unsigned, $aligned)
    $keystore = Join-Path $buildRoot 'debug.keystore'
    if (-not (Test-Path -LiteralPath $keystore)) {
        Invoke-Checked $keytool @('-genkeypair', '-keystore', $keystore, '-alias', 'androiddebugkey',
            '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000', '-storepass', 'android',
            '-keypass', 'android', '-dname', 'CN=Android Debug,O=Android,C=US')
    }
    $apk = Join-Path $buildRoot 'quest-pinyin-probe.apk'
    Invoke-Checked (Join-Path $buildTools 'apksigner.bat') @('sign', '--ks', $keystore,
        '--ks-key-alias', 'androiddebugkey', '--ks-pass', 'pass:android', '--key-pass', 'pass:android',
        '--v1-signing-enabled', 'true', '--v2-signing-enabled', 'true', '--out', $apk, $aligned)
    $signatureInfo = & (Join-Path $buildTools 'apksigner.bat') verify --verbose --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw 'Signature verification failed.' }
    $signatureInfo | Write-Output
    Invoke-Checked (Join-Path $buildTools 'zipalign.exe') @('-c', '-p', '4', $apk)
    $dexDump = & (Join-Path $buildTools 'dexdump.exe') (Join-Path $dex 'classes.dex')
    if ($LASTEXITCODE -ne 0) { throw 'Dex inspection failed.' }
    if ($dexDump -match "Class descriptor\s*:\s*'Lde/robv/android/xposed/") {
        throw 'Xposed framework classes must never be packaged into the module.'
    }
    $manifestDump = & (Join-Path $buildTools 'aapt2.exe') dump badging $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK manifest inspection failed.' }
    $manifestDump | Set-Content -LiteralPath (Join-Path $buildRoot 'apk-badging.txt') -Encoding UTF8
    $hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  quest-pinyin-probe.apk" | Set-Content -LiteralPath (Join-Path $buildRoot 'SHA256SUMS.txt') -Encoding UTF8
    $packed = [IO.Compression.ZipFile]::OpenRead($apk)
    try {
        $entry = $packed.GetEntry('assets/ime/google_pinyin.db')
        if (-not $entry) { throw 'Packaged dictionary is missing.' }
        $stream = $entry.Open()
        $digest = [Security.Cryptography.SHA256]::Create()
        try { $packedSha = [BitConverter]::ToString($digest.ComputeHash($stream)).Replace('-', '').ToLowerInvariant() }
        finally { $stream.Dispose(); $digest.Dispose() }
        if ($packedSha -ne $dictionarySha) { throw 'Packaged dictionary digest mismatch.' }
    } finally { $packed.Dispose() }
    [xml]$manifest = Get-Content -LiteralPath (Join-Path $projectRoot 'AndroidManifest.xml') -Raw
    $androidNamespace = 'http://schemas.android.com/apk/res/android'
    $revision = & git -C $projectRoot rev-parse HEAD
    $dirty = @(& git -C $projectRoot status --porcelain).Count -gt 0
    $certificate = ($signatureInfo | Select-String 'certificate SHA-256 digest:' | Select-Object -First 1).ToString() -replace '^.*certificate SHA-256 digest:\s*', ''
    [ordered]@{
        sourceCommit = "$revision".Trim(); sourceDirty = $dirty
        versionName = $manifest.manifest.GetAttribute('versionName', $androidNamespace)
        versionCode = $manifest.manifest.GetAttribute('versionCode', $androidNamespace)
        apk = [IO.Path]::GetFileName($apk); apkSha256 = $hash
        dictionarySha256 = $dictionarySha; signingCertificateSha256 = $certificate.Trim()
        hostPackage = 'com.oculus.vrshell'; hostVersionCode = 1045683657
        hostApkSha256 = '9c2f1c503031f3adb21964463c8c89c13b790b45c5375ccbd3ed01c5b5c17ed4'
        buildTools = $BuildToolsVersion; compilePlatform = $Platform
        builtAtUtc = [DateTime]::UtcNow.ToString('o')
        runtimeVerified = $false
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $buildRoot 'build-info.json') -Encoding utf8
    Write-Output "APK=$apk"
    Write-Output "SHA256=$hash"
    Write-Output 'RESULT=build_verified_runtime_unverified'
} finally {
    $env:JAVA_HOME = $oldJavaHome
}
