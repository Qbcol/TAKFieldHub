using System;
using System.IO;
using System.Linq;
using System.IO.Compression;
using FieldTakHub.Builder.Models;
using FieldTakHub.Builder.Services;
using Xunit;

namespace FieldTakHub.Builder.Tests;

public class BuilderCoreTests
{
    [Fact]
    public void ServerTxtRoundTrips()
    {
        var svc=new ServerTextConfigService(); var dir=Path.Combine(Path.GetTempPath(),Guid.NewGuid().ToString("N"));Directory.CreateDirectory(dir);
        var file=Path.Combine(dir,"server.txt"); var expected=new ServerProfile{Type="OpenTAK",Name="Test",Host="tak.example.org",CotPort=8089,ApiPort=8446,WebPort=8443};
        svc.Save(file,expected); var actual=svc.Load(file);
        Assert.Equal(expected.Host,actual.Host);Assert.Equal(expected.CotPort,actual.CotPort);Assert.Equal(expected.ApiPort,actual.ApiPort);Assert.Equal(expected.WebPort,actual.WebPort);
    }

    [Fact]
    public void ValidatorRejectsSchemeInHost()
    {
        Assert.Throws<InvalidDataException>(()=>ServerValidator.Validate(new ServerProfile{Host="https://tak.example.org"}));
    }

    [Fact]
    public void FtakContainsSignedMetadataAndMissionPackage()
    {
        var dir=Path.Combine(Path.GetTempPath(),"fth-test-"+Guid.NewGuid().ToString("N"));var source=Path.Combine(dir,"source");var output=Path.Combine(dir,"out");Directory.CreateDirectory(Path.Combine(source,"atak"));
        File.WriteAllText(Path.Combine(source,"atak","hello.txt"),"test");
        var p=new FieldTakProject{Name="Test",PackageId="test.package",PackageVersion="1.0.0",SourceDirectory=source,OutputDirectory=output,Server=new ServerProfile{Host="127.0.0.1"}};
        var items=new SourceAnalyzer().Analyze(source);var ftak=new FtakPackageBuilder().Build(p,items);
        using var zip=ZipFile.OpenRead(ftak);var names=zip.Entries.Select(x=>x.FullName).ToHashSet(StringComparer.OrdinalIgnoreCase);
        Assert.Contains("META-INF/fieldtak.json",names);Assert.Contains("META-INF/checksums.sha256",names);Assert.Contains("META-INF/signature.ed25519",names);Assert.Contains("payload/atak/mission-package.zip",names);
    }
    [Fact]
    public void WorkspaceCreatesAndRepairsProjectFolders()
    {
        var dir=Path.Combine(Path.GetTempPath(),"fth-workspace-"+Guid.NewGuid().ToString("N"));
        var source=Path.Combine(dir,"source"); var output=Path.Combine(dir,"out"); var svc=new WorkspaceService();
        svc.EnsureSourceTree(source,output);
        foreach(var folder in WorkspaceService.SourceFolders) Assert.True(Directory.Exists(Path.Combine(source,folder)));
        Assert.True(Directory.Exists(output)); Assert.True(File.Exists(Path.Combine(source,"README-WRZUC-PLIKI-TUTAJ.txt")));
        Directory.Delete(Path.Combine(source,"maps")); svc.EnsureSourceTree(source,output);
        Assert.True(Directory.Exists(Path.Combine(source,"maps")));
    }

    [Fact]
    public void WorkspaceSlugIsFilesystemFriendly()
    {
        Assert.Equal("GGZS-STANDARD",WorkspaceService.Slug("GGZS STANDARD"));
        Assert.Equal("Field-TAK-Package",WorkspaceService.Slug("   "));
    }

    [Fact]
    public void SourceAnalyzerIgnoresWorkspaceHelpFile()
    {
        var dir=Path.Combine(Path.GetTempPath(),"fth-analyze-"+Guid.NewGuid().ToString("N")); var source=Path.Combine(dir,"source"); var output=Path.Combine(dir,"out");
        new WorkspaceService().EnsureSourceTree(source,output); File.WriteAllText(Path.Combine(source,"plugins","plugin.apk"),"x");
        var items=new SourceAnalyzer().Analyze(source);
        Assert.Single(items); Assert.Equal("plugins/plugin.apk",items[0].RelativePath);
    }

    [Fact]
    public void CloudQrBindsHttpsUrlToLocalPackageHash()
    {
        var dir=Path.Combine(Path.GetTempPath(),"fth-cloud-"+Guid.NewGuid().ToString("N")); Directory.CreateDirectory(dir);
        var ftak=Path.Combine(dir,"test.ftak"); File.WriteAllBytes(ftak,new byte[]{1,2,3,4,5});
        var deep=CloudDistributionService.CreateDeepLink("https://example.org/test.ftak",ftak,DateTimeOffset.Parse("2030-01-01T00:00:00Z"),"Test Package");
        Assert.StartsWith("fieldtak://provision?",deep);
        Assert.Contains("packageUrl=https%3A%2F%2Fexample.org%2Ftest.ftak",deep);
        Assert.Contains("sha256=",deep); Assert.Contains("packageBytes=5",deep); Assert.Contains("expiresUtc=",deep);
    }

    [Fact]
    public void CloudQrRejectsPlainHttp()
    {
        var dir=Path.Combine(Path.GetTempPath(),"fth-cloud-"+Guid.NewGuid().ToString("N")); Directory.CreateDirectory(dir);
        var ftak=Path.Combine(dir,"test.ftak"); File.WriteAllText(ftak,"x");
        Assert.Throws<InvalidDataException>(()=>CloudDistributionService.CreateDeepLink("http://example.org/test.ftak",ftak,DateTimeOffset.UtcNow.AddHours(1),"Test"));
    }

    [Fact]
    public void GoogleDriveShareLinkIsNormalizedForCloudQr()
    {
        var resolved=CloudDistributionService.ResolvePackageUrl("https://drive.google.com/file/d/1AbC_def-123/view?usp=sharing");
        Assert.Equal("Google Drive",resolved.Provider);
        Assert.True(resolved.WasNormalized);
        Assert.Equal("1AbC_def-123",resolved.FileId);
        Assert.Equal("https://drive.usercontent.google.com/download?id=1AbC_def-123&export=download&confirm=t",resolved.DownloadUrl);
    }

    [Fact]
    public void GoogleDriveOpenLinkIsNormalizedAndKeepsResourceKey()
    {
        var resolved=CloudDistributionService.ResolvePackageUrl("https://drive.google.com/open?id=FILE123&resourcekey=RK456");
        Assert.Contains("id=FILE123",resolved.DownloadUrl);
        Assert.Contains("resourcekey=RK456",resolved.DownloadUrl);
    }

    [Fact]
    public void GoogleDriveFolderLinkIsRejected()
    {
        Assert.Throws<InvalidDataException>(()=>CloudDistributionService.ResolvePackageUrl("https://drive.google.com/drive/folders/FOLDER123?usp=sharing"));
    }

}
