using System.IO;
using System.Diagnostics;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media.Imaging;
using FieldTakHub.Builder.Models;
using FieldTakHub.Builder.Services;
using Microsoft.Win32;

namespace FieldTakHub.Builder;

public partial class MainWindow : Window
{
    private readonly ProjectService _projects = new();
    private readonly SourceAnalyzer _analyzer = new();
    private readonly FtakPackageBuilder _builder = new();
    private readonly DistributionServer _server = new();
    private readonly QrService _qr = new();
    private readonly ServerTextConfigService _serverText = new();
    private readonly ServerDiagnosticsService _diagnostics = new();
    private readonly SigningKeyService _signing = new();
    private readonly UpdateService _updates = new();
    private readonly WorkspaceService _workspace = new();
    private readonly LegacyPackageImporter _legacy = new();
    private readonly CloudDistributionService _cloud = new();
    private IReadOnlyList<ContentItem> _items = Array.Empty<ContentItem>();
    private string? _lastPackage;
    private byte[]? _lastQrPng;
    private string? _lastQrDeepLink;
    private bool _languageReady;
    private string? _currentProjectPath;

    public MainWindow()
    {
        InitializeComponent();
        PublisherBox.Text = Environment.UserName;
        var layout = _workspace.EnsureDefaultProject();
        SourceBox.Text = layout.SourceDirectory;
        OutputBox.Text = layout.OutputDirectory;
        _currentProjectPath = File.Exists(layout.ProjectFile) ? layout.ProjectFile : null;
        FingerprintRun.Text = ShortFingerprint(_signing.Fingerprint());
        _languageReady = true;
        RefreshFolderCounts();
        Loaded += async (_, _) =>
        {
            EnsureCurrentStructure(log: true);
            await CheckUpdatesAsync(silent: true);
        };
    }

    private FieldTakProject FromUi() => new()
    {
        PackageId = IdBox.Text.Trim(), Name = NameBox.Text.Trim(), PackageVersion = VersionBox.Text.Trim(), PublisherName = PublisherBox.Text.Trim(),
        SourceDirectory = SourceBox.Text.Trim(), OutputDirectory = OutputBox.Text.Trim(), AtakMinVersion = AtakMinBox.Text.Trim(), AtakMaxVersion = AtakMaxBox.Text.Trim(),
        ExpiryHours = int.TryParse(ExpiryBox.Text, out var h) ? Math.Max(1, h) : 6,
        MaxDownloads = int.TryParse(MaxDownloadsBox.Text, out var md) ? Math.Max(1, md) : 50,
        Server = ServerFromUi()
    };

    private ServerProfile ServerFromUi() => new()
    {
        Type = string.IsNullOrWhiteSpace(ServerTypeBox.Text) ? "OpenTAK" : ServerTypeBox.Text.Trim(),
        Name = string.IsNullOrWhiteSpace(ServerNameBox.Text) ? "TAK Server" : ServerNameBox.Text.Trim(),
        Host = HostBox.Text.Trim(), CotPort = Int(CotBox, 8089), ApiPort = Int(ApiBox, 8446), WebPort = Int(WebBox, 8443)
    };

    private static int Int(TextBox b, int d) => int.TryParse(b.Text, out var v) && v is > 0 and <= 65535 ? v : d;

    private void ToUi(FieldTakProject p)
    {
        IdBox.Text=p.PackageId; NameBox.Text=p.Name; VersionBox.Text=p.PackageVersion; PublisherBox.Text=p.PublisherName; SourceBox.Text=p.SourceDirectory; OutputBox.Text=p.OutputDirectory;
        AtakMinBox.Text=p.AtakMinVersion; AtakMaxBox.Text=p.AtakMaxVersion; ExpiryBox.Text=p.ExpiryHours.ToString(); MaxDownloadsBox.Text=p.MaxDownloads.ToString(); ServerToUi(p.Server);
    }

    private void ServerToUi(ServerProfile s)
    {
        ServerTypeBox.Text=s.Type; ServerNameBox.Text=s.Name; HostBox.Text=s.Host; CotBox.Text=s.CotPort.ToString(); ApiBox.Text=s.ApiPort.ToString(); WebBox.Text=s.WebPort.ToString();
    }

    private void BrowseSource_Click(object sender, RoutedEventArgs e) { var p=Folder(SourceBox.Text); if(p!=null){ SourceBox.Text=p; EnsureCurrentStructure(log:false); RefreshFolderCounts(); } }
    private void BrowseOutput_Click(object sender, RoutedEventArgs e) { var p=Folder(OutputBox.Text); if(p!=null){ OutputBox.Text=p; EnsureCurrentStructure(log:false); } }
    private static string? Folder(string initial) { var d = new OpenFolderDialog { InitialDirectory = Directory.Exists(initial) ? initial : string.Empty, Multiselect = false }; return d.ShowDialog() == true ? d.FolderName : null; }

    private void Analyze_Click(object sender, RoutedEventArgs e)
    {
        try { EnsureCurrentStructure(log:false); _items=_analyzer.Analyze(SourceBox.Text); ContentList.ItemsSource=_items; Log(string.Format(T("LogAnalyzed","Analyzed {0} files. ATAK source: {1}; APKs: {2}; total {3}."),_items.Count,_items.Count(x=>x.Category=="ATAK Mission Package"),_items.Count(x=>x.Category=="Plugin APK"),HumanBytes(_items.Sum(x=>x.Size)))); }
        catch(Exception ex){ Error(ex); }
    }

    private void Build_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            EnsureCurrentStructure(log:false);
            var project=FromUi(); ServerValidator.ValidateProject(project);
            if(_items.Count==0) _items=_analyzer.Analyze(project.SourceDirectory);
            var preview = BuildPreview(project,_items);
            var caption = LocalizationService.Text("BuildPreviewTitle","Build preview");
            if(MessageBox.Show(preview+"\n\n"+LocalizationService.Text("BuildPreviewConfirm","Build this package?"),caption,MessageBoxButton.YesNo,MessageBoxImage.Question)!=MessageBoxResult.Yes) return;
            _lastPackage=_builder.Build(project,_items);
            CloudPackageBox.Text=_lastPackage;
            var serverTxt=ServerTextConfigService.SidecarPath(_lastPackage);
            Log(string.Format(T("LogBuilt","Built and signed:\r\n{0}\r\n\r\nserver.txt:\r\n{1}\r\n\r\nATAK server.pref is generated inside the Mission Package unless source/atak contains a custom .pref."),_lastPackage,serverTxt));
        }
        catch(Exception ex){ Error(ex); }
    }

    private string BuildPreview(FieldTakProject p,IReadOnlyList<ContentItem> items)
    {
        var total=items.Sum(x=>x.Size); var plugins=items.Count(x=>x.Category=="Plugin APK"); var maps=items.Count(x=>x.Category=="Map");
        return string.Format(T("BuildPreviewBody","{0}  v{1}\n\nATAK: {2} – {3}\nServer: {4}\nPorts: {5} / {6} / {7}\nPlugins: {8}\nMaps: {9}\nFiles: {10}\nSource size: {11}\nQR validity: {12} h\nMax downloads: {13}\nPublisher: {14}"),p.Name,p.PackageVersion,p.AtakMinVersion,p.AtakMaxVersion,p.Server.Host,p.Server.CotPort,p.Server.ApiPort,p.Server.WebPort,plugins,maps,items.Count,HumanBytes(total),p.ExpiryHours,p.MaxDownloads,p.PublisherName);
    }

    private async void TestServer_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            var server=ServerFromUi(); ServerValidator.Validate(server); Log(string.Format(T("LogTestingServer","Testing {0}…"),server.Host));
            var results=await _diagnostics.TestAsync(server);
            Log(T("LogServerTest","SERVER TEST")+"\r\n"+string.Join("\r\n",results.Select(r=>$"{(r.Success?"OK":"FAIL"),-4} {r.Name,-16} {r.Detail}")));
        }
        catch(Exception ex){ Error(ex); }
    }

    private void StartServer_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            if(string.IsNullOrWhiteSpace(_lastPackage) || !File.Exists(_lastPackage)) { MessageBox.Show(T("BuildFirst","Build a package first."),T("WindowTitle","Field TAK Hub Builder")); return; }
            var project=FromUi(); ServerValidator.Validate(project.Server);
            var url=_server.Start(_lastPackage, TimeSpan.FromHours(project.ExpiryHours), project.MaxDownloads); UrlText.Text=url;
            var deepLink=$"fieldtak://provision?url={Uri.EscapeDataString(url)}";
            ShowQr(deepLink,_qr.CreatePng(deepLink));
            Log(string.Format(T("LogLanStarted","LAN distribution started. HTTP Range/206 resume is enabled. Plain HTTP is intended only for private LAN; Hub rejects public cleartext URLs.\r\nDescriptor: {0}\r\nQR deep-link: {1}"),url,deepLink));
        }
        catch(Exception ex){ Error(ex); }
    }
    private void StopServer_Click(object sender, RoutedEventArgs e){ _server.Stop(); UrlText.Text=""; QrImage.Source=null; _lastQrPng=null; _lastQrDeepLink=null; Log(T("LogDistributionStopped","Distribution stopped.")); }

    private void SelectCloudPackage_Click(object sender, RoutedEventArgs e)
    {
        var d=new OpenFileDialog{Filter="Field TAK package (*.ftak)|*.ftak|All files (*.*)|*.*",InitialDirectory=Directory.Exists(OutputBox.Text)?OutputBox.Text:string.Empty};
        if(d.ShowDialog()==true){CloudPackageBox.Text=d.FileName;_lastPackage=d.FileName;CloudStatusText.Text=T("CloudPackageSelected","Selected local package for cloud QR.");}
    }

    private async void TestCloudLink_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            CloudStatusText.Text=T("CloudTesting","Testing external HTTPS link…");
            var result=await _cloud.TestAsync(CloudUrlBox.Text);
            if(result.Success && result.WasNormalized) CloudUrlBox.Text=result.DownloadUrl;
            CloudStatusText.Text=(result.Success?"OK: ":"FAIL: ")+result.Detail;
            Log(string.Format(T("LogCloudTest","CLOUD LINK TEST\r\n{0}"),CloudStatusText.Text));
        }
        catch(Exception ex){CloudStatusText.Text="FAIL: "+ex.Message;Error(ex);}
    }

    private void GenerateCloudQr_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            var local=CloudPackageBox.Text.Trim();
            if(string.IsNullOrWhiteSpace(local) || !File.Exists(local)){MessageBox.Show(T("SelectCloudPackageFirst","Select the exact local .ftak file that you uploaded to the cloud."),T("WindowTitle","Field TAK Hub Builder"));return;}
            var project=FromUi();
            var resolved=CloudDistributionService.ResolvePackageUrl(CloudUrlBox.Text);
            var deepLink=CloudDistributionService.CreateDeepLink(resolved.DownloadUrl,local,DateTimeOffset.UtcNow.AddHours(project.ExpiryHours),project.Name);
            if(resolved.WasNormalized) CloudUrlBox.Text=resolved.DownloadUrl;
            var png=_qr.CreatePng(deepLink); ShowQr(deepLink,png);
            Directory.CreateDirectory(project.OutputDirectory);
            var stem=Path.GetFileNameWithoutExtension(local);
            var qrPath=Path.Combine(project.OutputDirectory,stem+"-cloud-QR.png");
            var txtPath=Path.Combine(project.OutputDirectory,stem+"-cloud-QR.txt");
            File.WriteAllBytes(qrPath,png); File.WriteAllText(txtPath,deepLink);
            CloudStatusText.Text=string.Format(T("CloudQrReady","Cloud QR ready. Saved: {0}"),qrPath);
            Log(string.Format(T("LogCloudQr","Generated direct-cloud QR. SHA-256 is bound to the local .ftak.\r\nPackage: {0}\r\nCloud URL: {1}\r\nQR: {2}"),local,CloudUrlBox.Text.Trim(),qrPath));
        }
        catch(Exception ex){Error(ex);}
    }

    private void CopyCloudDeepLink_Click(object sender, RoutedEventArgs e)
    {
        if(string.IsNullOrWhiteSpace(_lastQrDeepLink)){MessageBox.Show(T("GenerateQrFirst","Generate a QR first."),T("WindowTitle","Field TAK Hub Builder"));return;}
        Clipboard.SetText(_lastQrDeepLink); CloudStatusText.Text=T("DeepLinkCopied","Provisioning deep-link copied to clipboard.");
    }

    private void SaveQr_Click(object sender, RoutedEventArgs e)
    {
        if(_lastQrPng==null){MessageBox.Show(T("GenerateQrFirst","Generate a QR first."),T("WindowTitle","Field TAK Hub Builder"));return;}
        var d=new SaveFileDialog{Filter="PNG image (*.png)|*.png",FileName="FieldTAK-provision-QR.png",InitialDirectory=Directory.Exists(OutputBox.Text)?OutputBox.Text:string.Empty};
        if(d.ShowDialog()==true){File.WriteAllBytes(d.FileName,_lastQrPng);Log(string.Format(T("LogQrSaved","Saved QR PNG: {0}"),d.FileName));}
    }

    private void ShowQr(string deepLink,byte[] png)
    {
        _lastQrDeepLink=deepLink; _lastQrPng=png;
        using var ms=new MemoryStream(png); var bi=new BitmapImage(); bi.BeginInit(); bi.CacheOption=BitmapCacheOption.OnLoad; bi.StreamSource=ms; bi.EndInit(); bi.Freeze(); QrImage.Source=bi;
    }

    private void SaveProject_Click(object sender, RoutedEventArgs e)
    {
        var initialName = WorkspaceService.Slug(NameBox.Text) + ".fthproj";
        var d=new SaveFileDialog{Filter="Field TAK project (*.fthproj)|*.fthproj",FileName=initialName,InitialDirectory=CurrentProjectRoot()};
        if(d.ShowDialog()==true)
        {
            try
            {
                var p=FromUi(); ServerValidator.Validate(p.Server); EnsureCurrentStructure(log:false); _projects.Save(d.FileName,p); _currentProjectPath=d.FileName;
                Log(string.Format(T("LogSavedProject","Saved {0}"),d.FileName));
            }
            catch(Exception ex){Error(ex);}
        }
    }

    private void LoadProject_Click(object sender, RoutedEventArgs e)
    {
        var d=new OpenFileDialog{Filter="Field TAK project (*.fthproj)|*.fthproj",InitialDirectory=_workspace.DefaultProjectsRoot};
        if(d.ShowDialog()==true)
        {
            try
            {
                var p=_projects.Load(d.FileName); _workspace.EnsureSourceTree(p.SourceDirectory,p.OutputDirectory); ToUi(p); _currentProjectPath=d.FileName;
                _items=Array.Empty<ContentItem>(); ContentList.ItemsSource=_items; RefreshFolderCounts();
                Log(string.Format(T("LogLoadedProject","Loaded {0}"),d.FileName));
                Log(T("LogFoldersRepaired","Project folders checked and missing folders created."));
            }
            catch(Exception ex){Error(ex);}
        }
    }

    private void ImportLegacy_Click(object sender, RoutedEventArgs e)
    {
        var d = new OpenFileDialog
        {
            Filter = T("LegacyZipFilter", "TAK Field Hub 1.x ZIP (*.zip)|*.zip|All files (*.*)|*.*"),
            InitialDirectory = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments)
        };
        if (d.ShowDialog() != true) return;
        try
        {
            var publisher = string.IsNullOrWhiteSpace(PublisherBox.Text) ? Environment.UserName : PublisherBox.Text.Trim();
            var expiry = int.TryParse(ExpiryBox.Text, out var h) ? Math.Max(1, h) : 72;
            var maxDownloads = int.TryParse(MaxDownloadsBox.Text, out var md) ? Math.Max(1, md) : 50;
            var result = _legacy.Import(d.FileName, _workspace, publisher, expiry, maxDownloads);
            _projects.Save(result.ProjectFile, result.Project);
            _currentProjectPath = result.ProjectFile;
            ToUi(result.Project);
            EnsureCurrentStructure(log:false);
            _items = _analyzer.Analyze(result.Project.SourceDirectory);
            ContentList.ItemsSource = _items;
            RefreshFolderCounts();
            var sig = result.SignatureVerified ? T("LegacyVerified", "verified") : T("LegacyNotPresent", "not present");
            Log(string.Format(T("LogLegacyImported", "Imported legacy 1.x ZIP into a new 2.x project. Files: {0}; RSA signature: {1}; SHA-256: OK. Project: {2}"), result.ImportedFiles, sig, result.ProjectFile));
            MessageBox.Show(string.Format(T("LegacyImportSummary", "Legacy package imported successfully.\n\nFiles: {0}\nRSA signature: {1}\nSHA-256: OK\n\nClick BUILD PACKAGE to create a new Ed25519-signed .ftak v2."), result.ImportedFiles, sig), T("ImportLegacy", "Import legacy 1.x ZIP"), MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch(Exception ex){ Error(ex); }
    }

    private void NewProject_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new NewProjectDialog { Owner=this };
        if(dialog.ShowDialog()!=true) return;
        try
        {
            var layout=_workspace.CreateNewProject(dialog.ProjectName);
            NameBox.Text=dialog.ProjectName; IdBox.Text=dialog.PackageId; VersionBox.Text=string.IsNullOrWhiteSpace(dialog.PackageVersion)?"1.0.0":dialog.PackageVersion;
            SourceBox.Text=layout.SourceDirectory; OutputBox.Text=layout.OutputDirectory;
            var p=FromUi(); _projects.Save(layout.ProjectFile,p); _currentProjectPath=layout.ProjectFile;
            _items=Array.Empty<ContentItem>(); ContentList.ItemsSource=_items; RefreshFolderCounts();
            Log(string.Format(T("LogProjectCreated","Created project: {0}"),layout.RootDirectory));
            OpenPath(layout.RootDirectory);
        }
        catch(Exception ex){Error(ex);}
    }

    private void RepairFolders_Click(object sender, RoutedEventArgs e)
    {
        try { EnsureCurrentStructure(log:true); RefreshFolderCounts(); }
        catch(Exception ex){ Error(ex); }
    }

    private void OpenProjectFolder_Click(object sender, RoutedEventArgs e)
    {
        try { EnsureCurrentStructure(log:false); OpenPath(CurrentProjectRoot()); }
        catch(Exception ex){ Error(ex); }
    }

    private void OpenSourceFolder_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            if(sender is not FrameworkElement element || element.Tag is not string folder) return;
            EnsureCurrentStructure(log:false); OpenPath(WorkspaceService.FolderPath(SourceBox.Text,folder));
        }
        catch(Exception ex){ Error(ex); }
    }

    private void ProjectFolder_DragOver(object sender, DragEventArgs e)
    {
        e.Effects = e.Data.GetDataPresent(DataFormats.FileDrop) ? DragDropEffects.Copy : DragDropEffects.None;
        e.Handled = true;
    }

    private void ProjectFolder_Drop(object sender, DragEventArgs e)
    {
        try
        {
            if(sender is not FrameworkElement element || element.Tag is not string folder || !e.Data.GetDataPresent(DataFormats.FileDrop)) return;
            EnsureCurrentStructure(log:false);
            var target=WorkspaceService.FolderPath(SourceBox.Text,folder); var paths=(string[])e.Data.GetData(DataFormats.FileDrop)!; var copied=0;
            foreach(var path in paths)
            {
                if(File.Exists(path)) { CopyUnique(path,target); copied++; }
                else if(Directory.Exists(path))
                {
                    foreach(var file in Directory.EnumerateFiles(path,"*",SearchOption.AllDirectories)) { CopyUnique(file,target); copied++; }
                }
            }
            RefreshFolderCounts(); _items=Array.Empty<ContentItem>(); ContentList.ItemsSource=_items;
            Log(string.Format(T("LogFilesDropped","Copied {0} file(s) to {1}."),copied,folder));
        }
        catch(Exception ex){ Error(ex); }
    }

    private void EnsureCurrentStructure(bool log)
    {
        _workspace.EnsureSourceTree(SourceBox.Text,OutputBox.Text);
        RefreshFolderCounts();
        if(log) Log(T("LogFoldersRepaired","Project folders checked and missing folders created."));
    }

    private void RefreshFolderCounts()
    {
        if(string.IsNullOrWhiteSpace(SourceBox.Text)) return;
        static string Count(string root,string folder)
        {
            var path=Path.Combine(root,folder); return Directory.Exists(path) ? Directory.EnumerateFiles(path,"*",SearchOption.AllDirectories).Count().ToString() : "0";
        }
        AtakCountText.Text=Count(SourceBox.Text,"atak"); PluginsCountText.Text=Count(SourceBox.Text,"plugins"); MapsCountText.Text=Count(SourceBox.Text,"maps");
        OverlaysCountText.Text=Count(SourceBox.Text,"overlays"); ConfigCountText.Text=Count(SourceBox.Text,"config"); DataCountText.Text=Count(SourceBox.Text,"data");
    }

    private string CurrentProjectRoot()
    {
        if(!string.IsNullOrWhiteSpace(_currentProjectPath)) return Path.GetDirectoryName(Path.GetFullPath(_currentProjectPath))!;
        if(!string.IsNullOrWhiteSpace(SourceBox.Text))
        {
            var full=Path.GetFullPath(SourceBox.Text); var di=new DirectoryInfo(full); if(di.Name.Equals("source",StringComparison.OrdinalIgnoreCase) && di.Parent!=null) return di.Parent.FullName;
            return full;
        }
        return _workspace.DefaultProjectsRoot;
    }

    private static void OpenPath(string path)
    {
        Directory.CreateDirectory(path);
        Process.Start(new ProcessStartInfo(path){UseShellExecute=true});
    }

    private static void CopyUnique(string file,string target)
    {
        Directory.CreateDirectory(target); var name=Path.GetFileName(file); var dest=Path.Combine(target,name);
        if(File.Exists(dest))
        {
            var stem=Path.GetFileNameWithoutExtension(name); var ext=Path.GetExtension(name); var i=2;
            do dest=Path.Combine(target,$"{stem} ({i++}){ext}"); while(File.Exists(dest));
        }
        File.Copy(file,dest,false);
    }

    private void LoadServerTxt_Click(object sender, RoutedEventArgs e)
    {
        var d=new OpenFileDialog{Filter="TAK server profile (server.txt;*.txt)|server.txt;*.txt|Text files (*.txt)|*.txt|All files (*.*)|*.*"};
        if(d.ShowDialog()==true){ try{var s=_serverText.Load(d.FileName);ServerValidator.Validate(s);ServerToUi(s);Log(string.Format(T("LogLoadedServer","Loaded server configuration: {0}"),d.FileName));}catch(Exception ex){Error(ex);} }
    }

    private void SaveServerTxt_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            var server=ServerFromUi(); ServerValidator.Validate(server);
            var d=new SaveFileDialog{Filter="TAK server profile (*.txt)|*.txt",FileName="server.txt"};
            if(d.ShowDialog()==true){_serverText.Save(d.FileName,server);Log(string.Format(T("LogSavedServer","Saved server configuration: {0}"),d.FileName));}
        }
        catch(Exception ex){Error(ex);}
    }

    private void ExportKey_Click(object sender,RoutedEventArgs e)
    {
        var d=new SaveFileDialog{Filter="Field TAK publisher key (*.fthkey)|*.fthkey",FileName="FieldTAK-Publisher.fthkey"}; if(d.ShowDialog()!=true)return;
        var prompt=new PasswordPrompt{Owner=this}; if(prompt.ShowDialog()!=true)return;
        try{_signing.ExportEncryptedBackup(d.FileName,prompt.Password);Log(string.Format(T("LogKeyExported","Encrypted publisher key backup exported: {0}"),d.FileName));}catch(Exception ex){Error(ex);}
    }

    private void ImportKey_Click(object sender,RoutedEventArgs e)
    {
        var d=new OpenFileDialog{Filter="Field TAK publisher key (*.fthkey)|*.fthkey"}; if(d.ShowDialog()!=true)return;
        var prompt=new PasswordPrompt{Owner=this}; if(prompt.ShowDialog()!=true)return;
        try
        {
            var fp=_signing.ImportEncryptedBackup(d.FileName,prompt.Password); FingerprintRun.Text=ShortFingerprint(fp);
            Log(string.Format(T("LogKeyRestored","Publisher key restored. Fingerprint: {0}"),fp)); MessageBox.Show(T("KeyRestoredMessage","Publisher key restored. Verify the fingerprint before building production packages."),T("WindowTitle","Field TAK Hub"),MessageBoxButton.OK,MessageBoxImage.Information);
        }
        catch(Exception ex){Error(ex);}
    }

    private async void CheckUpdates_Click(object sender,RoutedEventArgs e)=>await CheckUpdatesAsync(silent:false);
    private async Task CheckUpdatesAsync(bool silent)
    {
        try
        {
            var update=await _updates.CheckAsync();
            if(update==null){if(!silent)Log(T("NoBuilderUpdate","No newer Builder release on the configured channel."));return;}
            Log(string.Format(T("BuilderUpdateAvailable","Builder update available: {0} → {1}"),"2.1.0-rc7",update.Version));
            if(silent)return;
            if(MessageBox.Show(string.Format(T("BuilderUpdatePrompt","Builder {0} is available. Download the verified ZIP now?"),update.Version),T("UpdateTitle","Field TAK Hub Update"),MessageBoxButton.YesNo,MessageBoxImage.Information)!=MessageBoxResult.Yes)return;
            var path=await _updates.DownloadAsync(update); Log(string.Format(T("UpdateDownloaded","Update downloaded and SHA-256 verified: {0}"),path)); UpdateService.ShowInExplorer(path);
        }
        catch(Exception ex){if(!silent)Error(ex);else Log(string.Format(T("UpdateSkipped","Update check skipped: {0}"),ex.Message));}
    }

    private void LanguageBox_SelectionChanged(object sender,SelectionChangedEventArgs e)
    {
        if(!_languageReady || LanguageBox.SelectedItem is not ComboBoxItem item || item.Tag is not string tag)return;
        LocalizationService.Apply(tag);
    }

    private static string T(string key,string fallback)=>LocalizationService.Text(key,fallback).Replace("\\r\\n","\r\n").Replace("\\n","\n");
    private void Log(string s)=>StatusBox.Text=$"[{DateTime.Now:HH:mm:ss}] {s}\r\n\r\n"+StatusBox.Text;
    private void Error(Exception ex){ Log(T("ErrorPrefix","ERROR")+": "+ex.Message); MessageBox.Show(ex.Message,T("WindowTitle","Field TAK Hub"),MessageBoxButton.OK,MessageBoxImage.Error); }
    private static string HumanBytes(long bytes)=>bytes>=1024L*1024*1024?$"{bytes/1024d/1024d/1024d:F2} GB":bytes>=1024L*1024?$"{bytes/1024d/1024d:F1} MB":$"{bytes} B";
    private static string ShortFingerprint(string fp)=>fp.Length<=24?fp:$"{fp[..12]}…{fp[^12..]}";
    protected override void OnClosed(EventArgs e){ _server.Dispose(); _cloud.Dispose(); base.OnClosed(e); }
}
