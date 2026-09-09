using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Security.Cryptography;

namespace FieldTakHub.Builder.Services;

public sealed record CloudLinkTestResult(bool Success, int StatusCode, string FinalUrl, string ContentType, long? ContentLength, string Detail);

/// <summary>
/// Creates direct-cloud provisioning QR links. Builder never uploads the package itself:
/// the operator uploads .ftak to an HTTPS origin, pastes the direct file URL, tests it,
/// and Builder binds that URL to the local package SHA-256 inside the QR deep-link.
/// </summary>
public sealed class CloudDistributionService : IDisposable
{
    private readonly HttpClient _http;

    public CloudDistributionService()
    {
        var handler = new HttpClientHandler { AllowAutoRedirect = true, MaxAutomaticRedirections = 6 };
        _http = new HttpClient(handler) { Timeout = TimeSpan.FromSeconds(20) };
        _http.DefaultRequestHeaders.UserAgent.Add(new ProductInfoHeaderValue("FieldTAKHubBuilder", "2.1.0-rc5"));
    }

    public static Uri RequireHttps(string value)
    {
        if (!Uri.TryCreate(value?.Trim(), UriKind.Absolute, out var uri) ||
            !uri.Scheme.Equals(Uri.UriSchemeHttps, StringComparison.OrdinalIgnoreCase) ||
            string.IsNullOrWhiteSpace(uri.Host))
            throw new InvalidDataException("FTH-CLOUD-001: external/cloud package URL must be an absolute HTTPS URL.");
        return uri;
    }

    public async Task<CloudLinkTestResult> TestAsync(string url, CancellationToken cancellationToken = default)
    {
        var uri = RequireHttps(url);
        HttpResponseMessage? response = null;
        try
        {
            using (var head = new HttpRequestMessage(HttpMethod.Head, uri))
            {
                response = await _http.SendAsync(head, HttpCompletionOption.ResponseHeadersRead, cancellationToken);
            }

            // Some object stores/CDNs reject HEAD. Probe one byte instead without downloading the package.
            if (!response.IsSuccessStatusCode)
            {
                response.Dispose();
                using var get = new HttpRequestMessage(HttpMethod.Get, uri);
                get.Headers.Range = new RangeHeaderValue(0, 0);
                response = await _http.SendAsync(get, HttpCompletionOption.ResponseHeadersRead, cancellationToken);
            }

            var finalUrl = response.RequestMessage?.RequestUri?.ToString() ?? uri.ToString();
            var mediaType = response.Content.Headers.ContentType?.MediaType ?? string.Empty;
            var len = response.Content.Headers.ContentRange?.Length ?? response.Content.Headers.ContentLength;
            var html = mediaType.Contains("text/html", StringComparison.OrdinalIgnoreCase);
            var ok = response.IsSuccessStatusCode && !html && Uri.TryCreate(finalUrl, UriKind.Absolute, out var finalUri) && finalUri.Scheme.Equals(Uri.UriSchemeHttps, StringComparison.OrdinalIgnoreCase);
            var detail = html
                ? "The URL returned HTML instead of package bytes. Use a direct/download link to the .ftak file."
                : $"HTTP {(int)response.StatusCode} {response.ReasonPhrase}; type={(string.IsNullOrWhiteSpace(mediaType) ? "unknown" : mediaType)}; final={finalUrl}";
            return new CloudLinkTestResult(ok, (int)response.StatusCode, finalUrl, mediaType, len, detail);
        }
        finally { response?.Dispose(); }
    }

    public static string Sha256File(string file)
    {
        if (!File.Exists(file)) throw new FileNotFoundException("Local .ftak file not found.", file);
        using var input = File.OpenRead(file);
        return Convert.ToHexString(SHA256.HashData(input)).ToLowerInvariant();
    }

    public static string CreateDeepLink(string packageUrl, string localPackage, DateTimeOffset expiresUtc, string label)
    {
        var uri = RequireHttps(packageUrl);
        if (!File.Exists(localPackage)) throw new FileNotFoundException("Local .ftak file not found.", localPackage);
        if (!Path.GetExtension(localPackage).Equals(".ftak", StringComparison.OrdinalIgnoreCase))
            throw new InvalidDataException("FTH-CLOUD-002: select the exact local .ftak file that was uploaded to the cloud.");

        var sha = Sha256File(localPackage);
        var bytes = new FileInfo(localPackage).Length;
        var query = string.Join("&", new[]
        {
            "packageUrl=" + Uri.EscapeDataString(uri.ToString()),
            "sha256=" + Uri.EscapeDataString(sha),
            "packageBytes=" + bytes,
            "expiresUtc=" + Uri.EscapeDataString(expiresUtc.UtcDateTime.ToString("O")),
            "label=" + Uri.EscapeDataString(label ?? string.Empty)
        });
        return "fieldtak://provision?" + query;
    }

    public void Dispose() => _http.Dispose();
}
