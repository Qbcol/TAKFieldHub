using System.IO;
using System.Text.Json;
using FieldTakHub.Builder.Models;
namespace FieldTakHub.Builder.Services;

public sealed class ProjectService
{
    public FieldTakProject Load(string path)
    {
        var p = JsonSerializer.Deserialize<FieldTakProject>(File.ReadAllText(path), JsonDefaults.Options)
            ?? throw new InvalidDataException("Invalid project file.");
        if (p.SchemaVersion != 2) throw new InvalidDataException($"Unsupported project schema {p.SchemaVersion}.");
        var baseDir = Path.GetDirectoryName(Path.GetFullPath(path))!;
        p.SourceDirectory = Resolve(baseDir, p.SourceDirectory);
        p.OutputDirectory = Resolve(baseDir, p.OutputDirectory);
        return p;
    }

    public void Save(string path, FieldTakProject project)
    {
        var full = Path.GetFullPath(path);
        var baseDir = Path.GetDirectoryName(full)!;
        Directory.CreateDirectory(baseDir);
        // Keep project files portable: paths are serialized relative to the .fthproj location when possible.
        var portable = new FieldTakProject
        {
            SchemaVersion = project.SchemaVersion,
            PackageId = project.PackageId,
            Name = project.Name,
            PackageVersion = project.PackageVersion,
            PublisherName = project.PublisherName,
            SourceDirectory = PortablePath(baseDir, project.SourceDirectory),
            OutputDirectory = PortablePath(baseDir, project.OutputDirectory),
            AtakMinVersion = project.AtakMinVersion,
            AtakMaxVersion = project.AtakMaxVersion,
            ExpiryHours = project.ExpiryHours,
            MaxDownloads = project.MaxDownloads,
            Server = new ServerProfile
            {
                Type=project.Server.Type, Name=project.Server.Name, Host=project.Server.Host,
                CotPort=project.Server.CotPort, ApiPort=project.Server.ApiPort, WebPort=project.Server.WebPort
            }
        };
        File.WriteAllText(full, JsonSerializer.Serialize(portable, JsonDefaults.Options));
    }

    private static string Resolve(string baseDir, string path) =>
        string.IsNullOrWhiteSpace(path) ? baseDir : Path.GetFullPath(Path.IsPathRooted(path) ? path : Path.Combine(baseDir, path));

    private static string PortablePath(string baseDir,string value)
    {
        if (string.IsNullOrWhiteSpace(value)) return ".";
        try
        {
            var full=Path.GetFullPath(value);
            var rel=Path.GetRelativePath(baseDir,full);
            return rel.Replace('\\','/');
        }
        catch { return value; }
    }
}
