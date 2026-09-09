package org.fieldtak.hub.model

import java.io.File

data class ProvisionDescriptor(
  val packageUrl: String,
  val expiresUtc: String?,
  val packageSha256: String? = null,
  val label: String? = null,
  val packageBytes: Long? = null,
  val recommendedFreeBytes: Long? = null
)

data class PackageManifest(
  val schema: String = "",
  val schemaVersion: Int = 0,
  val packageInfo: PackageInfo = PackageInfo(),
  val target: TargetInfo = TargetInfo(),
  val server: ServerInfo = ServerInfo(),
  val components: ComponentInfo = ComponentInfo(),
  val security: SecurityInfo = SecurityInfo(),
  val distribution: DistributionInfo = DistributionInfo(),
  val size: PackageSizeInfo = PackageSizeInfo()
)

data class PackageInfo(
  val id:String="",
  val name:String="",
  val version:String="",
  val publisher:String="",
  val createdUtc:String=""
)

data class TargetInfo(
  val platform:String="",
  val minVersion:String="",
  val maxVersion:String=""
)

data class ServerInfo(
  val type:String="",
  val name:String="",
  val host:String="",
  val cotPort:Int=0,
  val apiPort:Int=0,
  val webPort:Int=0
)

data class ComponentInfo(
  val missionPackage:Boolean=false,
  val plugins:Int=0,
  val files:Int=0
)

data class SecurityInfo(
  val hash:String="",
  val signature:String="",
  val publisherFingerprintSha256:String=""
)

data class DistributionInfo(val expiresUtc:String="")

data class PackageSizeInfo(
  val payloadBytes:Long=0,
  val uncompressedBytes:Long=0,
  val recommendedFreeBytes:Long=0
)

data class VerifiedPackage(
  val root: File,
  val sourceFile: File,
  val manifest: PackageManifest,
  val publisherFingerprint:String,
  val signatureValid:Boolean,
  val hashesValid:Boolean
)

enum class CheckState { READY, ACTION_REQUIRED, PROBLEM, UNKNOWN }

data class AtakStatus(
  val installed:Boolean,
  val versionName:String?,
  val versionCode:Long? = null,
  val compatibility:CheckState = CheckState.UNKNOWN,
  val compatibilityMessage:String = ""
)

enum class PluginInstallState { CURRENT, UPDATE_REQUIRED, NOT_INSTALLED, UNKNOWN }

data class PluginStatus(
  val file: File,
  val packageName:String?,
  val label:String,
  val sourceVersion:String?,
  val sourceVersionCode:Long?,
  val installedVersion:String?,
  val installedVersionCode:Long?,
  val state:PluginInstallState
)

data class ReadinessItem(
  val id:String,
  val label:String,
  val state:CheckState,
  val detail:String = ""
)

data class PhoneReadiness(
  val percent:Int,
  val items:List<ReadinessItem>,
  val ready:Boolean
)
