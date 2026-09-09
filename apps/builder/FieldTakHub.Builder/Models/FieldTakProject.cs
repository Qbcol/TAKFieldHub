namespace FieldTakHub.Builder.Models;

public sealed class FieldTakProject
{
    public int SchemaVersion { get; set; } = 2;
    public string PackageId { get; set; } = "local.fieldtak.package";
    public string Name { get; set; } = "Field TAK Package";
    public string PackageVersion { get; set; } = "2.0.0";
    public string PublisherName { get; set; } = Environment.UserName;
    public string SourceDirectory { get; set; } = string.Empty;
    public string OutputDirectory { get; set; } = string.Empty;
    public string AtakMinVersion { get; set; } = "5.6";
    public string AtakMaxVersion { get; set; } = "5.8";
    public int ExpiryHours { get; set; } = 6;
    public int MaxDownloads { get; set; } = 50;
    public ServerProfile Server { get; set; } = new();
}

public sealed class ServerProfile
{
    public string Type { get; set; } = "OpenTAK";
    public string Name { get; set; } = "GGZS OpenTAK";
    public string Host { get; set; } = "ggzstak.duckdns.org";
    public int CotPort { get; set; } = 8089;
    public int ApiPort { get; set; } = 8446;
    public int WebPort { get; set; } = 8443;
}
