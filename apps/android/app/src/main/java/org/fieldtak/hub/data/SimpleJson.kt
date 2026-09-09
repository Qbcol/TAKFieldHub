package org.fieldtak.hub.data

import org.fieldtak.hub.model.*
import org.json.JSONObject

object SimpleJson {
  fun descriptor(s:String):ProvisionDescriptor {
    val o=JSONObject(s)
    return ProvisionDescriptor(
      packageUrl=o.getString("packageUrl"),
      expiresUtc=o.optString("expiresUtc").ifBlank{null},
      packageSha256=o.optString("packageSha256").ifBlank{null},
      label=o.optString("label").ifBlank{null},
      packageBytes=o.optLong("packageBytes",-1L).takeIf{it>=0},
      recommendedFreeBytes=o.optLong("recommendedFreeBytes",-1L).takeIf{it>=0}
    )
  }

  fun manifest(s:String):PackageManifest {
    val o=JSONObject(s); val p=o.getJSONObject("package"); val t=o.getJSONObject("target"); val srv=o.getJSONObject("server"); val c=o.getJSONObject("components"); val sec=o.getJSONObject("security"); val d=o.getJSONObject("distribution"); val size=o.optJSONObject("size")
    return PackageManifest(o.optString("schema"),o.optInt("schemaVersion"),
      PackageInfo(p.optString("id"),p.optString("name"),p.optString("version"),p.optString("publisher"),p.optString("createdUtc")),
      TargetInfo(t.optString("platform"),t.optString("minVersion"),t.optString("maxVersion")),
      ServerInfo(srv.optString("type"),srv.optString("name"),srv.optString("host"),srv.optInt("cotPort"),srv.optInt("apiPort"),srv.optInt("webPort")),
      ComponentInfo(c.optBoolean("missionPackage"),c.optInt("plugins"),c.optInt("files")),
      SecurityInfo(sec.optString("hash"),sec.optString("signature"),sec.optString("publisherFingerprintSha256")),
      DistributionInfo(d.optString("expiresUtc")),
      PackageSizeInfo(size?.optLong("payloadBytes") ?: 0L, size?.optLong("uncompressedBytes") ?: 0L, size?.optLong("recommendedFreeBytes") ?: 0L))
  }
}
