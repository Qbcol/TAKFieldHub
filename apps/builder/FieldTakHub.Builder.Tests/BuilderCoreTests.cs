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
}
