using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using FieldTakHub.Builder.Models;

namespace FieldTakHub.Builder.Services;

public sealed record ProjectLayout(string RootDirectory, string SourceDirectory, string OutputDirectory, string ProjectFile);

public sealed class WorkspaceService
{
    public static readonly string[] SourceFolders = { "atak", "plugins", "maps", "overlays", "config", "data" };

    public string DefaultProjectsRoot => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments),
        "FieldTAKHub", "Projects");

    public ProjectLayout EnsureDefaultProject()
    {
        Directory.CreateDirectory(DefaultProjectsRoot);
        var root = Path.Combine(DefaultProjectsRoot, "Field-TAK-Package");
        return EnsureProject(root, "Field-TAK-Package");
    }

    public ProjectLayout EnsureProject(string projectRoot, string? projectName = null)
    {
        projectRoot = Path.GetFullPath(projectRoot);
        Directory.CreateDirectory(projectRoot);
        var source = Path.Combine(projectRoot, "source");
        var output = Path.Combine(projectRoot, "out");
        EnsureSourceTree(source, output);

        var safeName = Slug(string.IsNullOrWhiteSpace(projectName) ? Path.GetFileName(projectRoot) : projectName!);
        var projectFile = Path.Combine(projectRoot, safeName + ".fthproj");
        return new ProjectLayout(projectRoot, source, output, projectFile);
    }

    public void EnsureSourceTree(string sourceDirectory, string outputDirectory)
    {
        if (string.IsNullOrWhiteSpace(sourceDirectory)) throw new InvalidDataException("Source directory is empty.");
        if (string.IsNullOrWhiteSpace(outputDirectory)) throw new InvalidDataException("Output directory is empty.");

        sourceDirectory = Path.GetFullPath(sourceDirectory);
        outputDirectory = Path.GetFullPath(outputDirectory);
        Directory.CreateDirectory(sourceDirectory);
        Directory.CreateDirectory(outputDirectory);
        foreach (var folder in SourceFolders) Directory.CreateDirectory(Path.Combine(sourceDirectory, folder));

        var help = Path.Combine(sourceDirectory, "README-WRZUC-PLIKI-TUTAJ.txt");
        if (!File.Exists(help)) File.WriteAllText(help, HelpText, new UTF8Encoding(false));
    }

    public ProjectLayout CreateNewProject(string name)
    {
        Directory.CreateDirectory(DefaultProjectsRoot);
        var slug = Slug(name);
        var root = Path.Combine(DefaultProjectsRoot, slug);
        if (Directory.Exists(root) && Directory.EnumerateFileSystemEntries(root).Any())
        {
            var i = 2;
            string candidate;
            do candidate = Path.Combine(DefaultProjectsRoot, $"{slug}-{i++}");
            while (Directory.Exists(candidate));
            root = candidate;
        }
        return EnsureProject(root, name);
    }

    public static string FolderPath(string sourceDirectory, string folder)
    {
        if (!SourceFolders.Contains(folder, StringComparer.OrdinalIgnoreCase))
            throw new ArgumentOutOfRangeException(nameof(folder));
        return Path.Combine(sourceDirectory, folder.ToLowerInvariant());
    }

    public static string Slug(string value)
    {
        if (string.IsNullOrWhiteSpace(value)) return "Field-TAK-Package";
        var chars = value.Trim().Select(c => char.IsLetterOrDigit(c) ? c : '-').ToArray();
        var slug = Regex.Replace(new string(chars), "-+", "-").Trim('-');
        return string.IsNullOrWhiteSpace(slug) ? "Field-TAK-Package" : slug;
    }

    private const string HelpText = """
FIELD TAK HUB - PROJECT SOURCE / ZRODLA PROJEKTU
================================================

PL:
atak\\      - pliki do ATAK Mission Package (.pref, KML/KMZ i inne dane do importu)
plugins\\   - pluginy ATAK w formacie APK
maps\\      - mapy (MBTiles, GeoPackage, SQLite itd.)
overlays\\  - nakladki KML/KMZ
config\\    - dodatkowe pliki konfiguracyjne
data\\      - inne dane deploymentu

Nie umieszczaj tutaj prywatnego klucza Publishera ani hasel.
Brakujace katalogi sa automatycznie odtwarzane przy uruchomieniu i otwarciu projektu.

EN:
atak\\      - files for the ATAK Mission Package (.pref, KML/KMZ and other import data)
plugins\\   - ATAK plugin APK files
maps\\      - maps (MBTiles, GeoPackage, SQLite, etc.)
overlays\\  - KML/KMZ overlays
config\\    - additional configuration files
data\\      - other deployment data

Do not place the Publisher private key or passwords here.
Missing folders are automatically repaired when Builder starts or opens the project.
""";
}
