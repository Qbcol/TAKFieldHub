using System.IO;
using System.IO.Compression;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using FieldTakHub.Builder.Models;

namespace FieldTakHub.Builder.Services;

public sealed class FtakPackageBuilder
{
    private readonly SigningKeyService _signing = new();
    private readonly MissionPackageBuilder _missionBuilder = new();
    private readonly ServerTextConfigService _serverText = new();

    public string Build(FieldTakProject project, IReadOnlyList<ContentItem> items)
    {
        if (!Directory.Exists(project.SourceDirectory)) throw new DirectoryNotFoundException(project.SourceDirectory);
        Directory.CreateDirectory(project.OutputDirectory);

        var staging = Path.Combine(Path.GetTempPath(), "fieldtak-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(staging);
        try
        {
            var payload = Path.Combine(staging, "payload");
            Directory.CreateDirectory(payload);
            var atakRoot = Path.Combine(project.SourceDirectory, "atak");
            var missionOut = Path.Combine(payload, "atak", "mission-package.zip");
            var missionCreated = _missionBuilder.Build(atakRoot, missionOut, project.Name, project.Server);

            int pluginCount = 0;
            foreach (var item in items.Where(i => !i.RelativePath.StartsWith("atak/", StringComparison.OrdinalIgnoreCase)))
            {
                var destRel = DestinationPath(item);
                var dest = Path.Combine(payload, destRel.Replace('/', Path.DirectorySeparatorChar));
                Directory.CreateDirectory(Path.GetDirectoryName(dest)!);
                File.Copy(item.SourcePath, dest, true);
                if (destRel.StartsWith("plugins/", StringComparison.OrdinalIgnoreCase) && Path.GetExtension(destRel).Equals(".apk", StringComparison.OrdinalIgnoreCase)) pluginCount++;
            }

            var payloadBytes = Directory.EnumerateFiles(payload, "*", SearchOption.AllDirectories).Sum(f => new FileInfo(f).Length);
            var metaEstimate = 512L * 1024L;
            var uncompressedEstimate = payloadBytes + metaEstimate;
            // Keep enough room for the downloaded .ftak, extraction and temporary files.
            var recommendedFree = Math.Max(512L * 1024L * 1024L, payloadBytes * 2 + 256L * 1024L * 1024L);

            var preliminary = new FieldTakManifest
            {
                Package = new PackageInfo { Id = project.PackageId, Name = project.Name, Version = project.PackageVersion, Publisher = project.PublisherName, CreatedUtc = DateTime.UtcNow },
                Target = new TargetInfo { MinVersion = project.AtakMinVersion, MaxVersion = project.AtakMaxVersion },
                Server = project.Server,
                Components = new ComponentInfo { MissionPackage = missionCreated, Plugins = pluginCount, Files = Directory.EnumerateFiles(payload, "*", SearchOption.AllDirectories).Count() },
                Distribution = new DistributionInfo { ExpiresUtc = DateTime.UtcNow.AddHours(project.ExpiryHours), MaxDownloads = project.MaxDownloads },
                Size = new PackageSizeInfo { PayloadBytes = payloadBytes, UncompressedBytes = uncompressedEstimate, RecommendedFreeBytes = recommendedFree }
            };

            var meta = Path.Combine(staging, "META-INF");
            Directory.CreateDirectory(meta);
            var manifestPath = Path.Combine(meta, "fieldtak.json");
            // Signed, human-readable copy of the server profile for diagnostics/audit.
            _serverText.Save(Path.Combine(meta, "server.txt"), project.Server);
            // fingerprint depends only on pubkey; get via signing a temporary zero-length message
            var pubInfo = _signing.Sign(ReadOnlySpan<byte>.Empty);
            preliminary.Security.PublisherFingerprintSha256 = pubInfo.Fingerprint;
            File.WriteAllText(manifestPath, JsonSerializer.Serialize(preliminary, JsonDefaults.Options), new UTF8Encoding(false));

            var checksumLines = new List<string>();
            foreach (var file in Directory.EnumerateFiles(staging, "*", SearchOption.AllDirectories)
                         .Where(f => !f.EndsWith("checksums.sha256", StringComparison.OrdinalIgnoreCase) && !f.EndsWith("signature.ed25519", StringComparison.OrdinalIgnoreCase) && !f.EndsWith("publisher.pub", StringComparison.OrdinalIgnoreCase))
                         .OrderBy(f => Path.GetRelativePath(staging, f).Replace('\\','/'), StringComparer.Ordinal))
            {
                var rel = Path.GetRelativePath(staging, file).Replace('\\','/');
                using var hashStream = File.OpenRead(file);
                var hash = Convert.ToHexString(SHA256.HashData(hashStream)).ToLowerInvariant();
                checksumLines.Add($"{hash}  {rel}");
            }
            var checksumBytes = new UTF8Encoding(false).GetBytes(string.Join("\n", checksumLines) + "\n");
            File.WriteAllBytes(Path.Combine(meta, "checksums.sha256"), checksumBytes);
            var signed = _signing.Sign(checksumBytes);
            File.WriteAllText(Path.Combine(meta, "signature.ed25519"), Convert.ToBase64String(signed.Signature), new UTF8Encoding(false));
            File.WriteAllText(Path.Combine(meta, "publisher.pub"), Convert.ToBase64String(signed.PublicKey), new UTF8Encoding(false));

            var safeName = string.Concat(project.Name.Select(c => Path.GetInvalidFileNameChars().Contains(c) ? '_' : c)).Replace(' ', '-');
            var outPath = Path.Combine(project.OutputDirectory, $"{safeName}-{project.PackageVersion}.ftak");
            if (File.Exists(outPath)) File.Delete(outPath);
            ZipFile.CreateFromDirectory(staging, outPath, CompressionLevel.Optimal, false);
            // Also write an easy-to-edit sidecar next to the .ftak. Editing it does not alter an already-built bundle; load it into Builder and rebuild.
            _serverText.Save(ServerTextConfigService.SidecarPath(outPath), project.Server);
            return outPath;
        }
        finally
        {
            try { Directory.Delete(staging, true); } catch { }
        }
    }

    private static string DestinationPath(ContentItem item)
    {
        var rel = item.RelativePath.Replace('\\','/');
        if (rel.StartsWith("plugins/", StringComparison.OrdinalIgnoreCase) || rel.StartsWith("maps/", StringComparison.OrdinalIgnoreCase) ||
            rel.StartsWith("overlays/", StringComparison.OrdinalIgnoreCase) || rel.StartsWith("config/", StringComparison.OrdinalIgnoreCase) || rel.StartsWith("data/", StringComparison.OrdinalIgnoreCase)) return rel;
        if (Path.GetExtension(rel).Equals(".apk", StringComparison.OrdinalIgnoreCase)) return "plugins/" + Path.GetFileName(rel);
        return "data/" + rel;
    }
}
