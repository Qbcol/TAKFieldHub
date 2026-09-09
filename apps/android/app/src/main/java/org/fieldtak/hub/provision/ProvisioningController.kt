package org.fieldtak.hub.provision

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.provider.Settings
import androidx.core.content.FileProvider
import org.fieldtak.hub.R
import org.fieldtak.hub.model.*
import org.fieldtak.hub.util.VersionUtil
import java.io.File

class ProvisioningController(private val context:Context) {
  private val pm get() = context.packageManager
  private val atakPackage = "com.atakmap.app.civ"
  private fun s(id:Int,vararg args:Any)=context.getString(id,*args)

  @Suppress("DEPRECATION")
  fun detectAtak(target:TargetInfo?=null):AtakStatus = try {
    val info = if(Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(atakPackage, PackageManager.PackageInfoFlags.of(0)) else pm.getPackageInfo(atakPackage,0)
    val version=info.versionName
    val compatible=target?.let { VersionUtil.inRange(version,it.minVersion,it.maxVersion) } ?: true
    AtakStatus(true,version,info.longVersionCode, if(compatible) CheckState.READY else CheckState.ACTION_REQUIRED,
      if(compatible) s(R.string.pc_version_compatible) else s(R.string.pc_version_required,target?.minVersion.orEmpty(),target?.maxVersion.orEmpty(),version ?: "?"))
  } catch(_:Exception) { AtakStatus(false,null,null,CheckState.ACTION_REQUIRED,s(R.string.pc_atak_missing)) }

  fun apkFiles(root:File)=File(root,"payload/plugins").takeIf{it.isDirectory}?.walkTopDown()?.filter{it.isFile&&it.extension.equals("apk",true)}?.toList().orEmpty()
  fun atakApk(root:File)=File(root,"payload/atak/atak.apk").takeIf{it.isFile}
  fun missionPackage(root:File)=File(root,"payload/atak/mission-package.zip").takeIf{it.isFile}

  @Suppress("DEPRECATION")
  fun pluginStatus(apk:File):PluginStatus {
    return try {
      val archive = if(Build.VERSION.SDK_INT>=33) pm.getPackageArchiveInfo(apk.absolutePath,PackageManager.PackageInfoFlags.of(0)) else pm.getPackageArchiveInfo(apk.absolutePath,0)
      if(archive==null) return PluginStatus(apk,null,apk.name,null,null,null,null,PluginInstallState.UNKNOWN)
      archive.applicationInfo?.sourceDir=apk.absolutePath; archive.applicationInfo?.publicSourceDir=apk.absolutePath
      val packageName=archive.packageName
      val srcVersion=archive.versionName; val srcCode=archive.longVersionCode
      val label=runCatching { archive.applicationInfo?.loadLabel(pm)?.toString() }.getOrNull().orEmpty().ifBlank{packageName}
      val installed=runCatching { if(Build.VERSION.SDK_INT>=33) pm.getPackageInfo(packageName,PackageManager.PackageInfoFlags.of(0)) else pm.getPackageInfo(packageName,0) }.getOrNull()
      val state=when {
        installed==null -> PluginInstallState.NOT_INSTALLED
        installed.longVersionCode < srcCode -> PluginInstallState.UPDATE_REQUIRED
        else -> PluginInstallState.CURRENT
      }
      PluginStatus(apk,packageName,label,srcVersion,srcCode,installed?.versionName,installed?.longVersionCode,state)
    } catch(_:Exception){ PluginStatus(apk,null,apk.name,null,null,null,null,PluginInstallState.UNKNOWN) }
  }

  fun pluginStatuses(root:File)=apkFiles(root).map(::pluginStatus)
  fun pendingPlugins(root:File)=pluginStatuses(root).filter { it.state==PluginInstallState.NOT_INSTALLED || it.state==PluginInstallState.UPDATE_REQUIRED }

  fun canInstallPackages() = if(Build.VERSION.SDK_INT>=26) pm.canRequestPackageInstalls() else true
  fun openUnknownSourcesSettings(){ if(Build.VERSION.SDK_INT>=26) context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

  fun installApk(file:File){
    val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
      .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  fun openMissionPackage(file:File){
    val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
    val base=Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/zip").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    try { context.startActivity(Intent(base).setPackage(atakPackage)) }
    catch(_:ActivityNotFoundException) { context.startActivity(Intent.createChooser(base,"Import ATAK Mission Package").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
  }

  fun openAtak():Boolean = try {
    val launch=pm.getLaunchIntentForPackage(atakPackage) ?: return false
    context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
  } catch(_:Exception){ false }

  fun hasUsableNetwork():Boolean {
    val cm=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network=cm.activeNetwork ?: return false
    val caps=cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) || caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
  }

  fun freeStorageBytes():Long = StatFs(context.filesDir.absolutePath).availableBytes

  fun computeReadiness(pkg:VerifiedPackage, trusted:Boolean):PhoneReadiness {
    val atak=detectAtak(pkg.manifest.target)
    val plugins=pluginStatuses(pkg.root)
    val pending=plugins.count { it.state==PluginInstallState.NOT_INSTALLED || it.state==PluginInstallState.UPDATE_REQUIRED }
    val unknown=plugins.count { it.state==PluginInstallState.UNKNOWN }
    val mission=missionPackage(pkg.root)
    val items=mutableListOf<ReadinessItem>()
    items += ReadinessItem("signature",s(R.string.pc_signature_label),if(pkg.signatureValid)CheckState.READY else CheckState.PROBLEM,if(pkg.signatureValid)s(R.string.pc_signature_ok) else s(R.string.pc_signature_bad))
    items += ReadinessItem("hashes",s(R.string.pc_integrity_label),if(pkg.hashesValid)CheckState.READY else CheckState.PROBLEM,if(pkg.hashesValid)s(R.string.pc_hash_ok) else s(R.string.pc_hash_bad))
    items += ReadinessItem("publisher",s(R.string.pc_publisher_label),if(trusted)CheckState.READY else CheckState.ACTION_REQUIRED,if(trusted)s(R.string.pc_trusted) else s(R.string.pc_requires_trust))
    items += ReadinessItem("network",s(R.string.pc_network_label),if(hasUsableNetwork())CheckState.READY else CheckState.ACTION_REQUIRED,if(hasUsableNetwork())s(R.string.pc_network_available) else s(R.string.pc_network_missing))
    val free=freeStorageBytes(); val recommended=pkg.manifest.size.recommendedFreeBytes.takeIf{it>0} ?: 256L*1024*1024
    items += ReadinessItem("storage",s(R.string.pc_storage_label),if(free>=recommended)CheckState.READY else CheckState.PROBLEM,
      if(free>=recommended)s(R.string.disk_space_ok,humanBytes(free),humanBytes(recommended)) else s(R.string.disk_space_problem,humanBytes(recommended),humanBytes(free)))
    items += ReadinessItem("atak",s(R.string.atak),atak.compatibility,atak.compatibilityMessage.ifBlank{atak.versionName.orEmpty()})
    val pluginState=when{pending>0->CheckState.ACTION_REQUIRED;unknown>0->CheckState.UNKNOWN;else->CheckState.READY}
    val pendingText=if(pending>0)s(R.string.pc_plugins_pending,pending) else ""
    val unknownText=if(unknown>0)s(R.string.pc_plugins_unknown,unknown) else ""
    items += ReadinessItem("plugins",s(R.string.pc_plugins_label),pluginState,s(R.string.pc_plugins_summary,plugins.size-pending-unknown,plugins.size,pendingText,unknownText))
    items += ReadinessItem("mission",s(R.string.pc_mission_label),if(mission!=null)CheckState.ACTION_REQUIRED else CheckState.READY,if(mission!=null)s(R.string.pc_mission_waiting) else s(R.string.pc_mission_none))
    val score = items.sumOf { when(it.state){CheckState.READY->100;CheckState.UNKNOWN->55;CheckState.ACTION_REQUIRED->25;CheckState.PROBLEM->0} } / maxOf(1,items.size)
    val fatal=items.any{it.state==CheckState.PROBLEM}
    val ready=!fatal && atak.installed && atak.compatibility==CheckState.READY && pending==0 && trusted && pkg.signatureValid && pkg.hashesValid
    return PhoneReadiness(score,items,ready)
  }

  fun shareText(activity:Activity,title:String,text:String){
    val i=Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT,title).putExtra(Intent.EXTRA_TEXT,text)
    activity.startActivity(Intent.createChooser(i,title))
  }

  private fun humanBytes(bytes:Long):String = when {
    bytes >= 1024L*1024*1024 -> "%.1f GB".format(bytes/1024.0/1024.0/1024.0)
    bytes >= 1024L*1024 -> "%.1f MB".format(bytes/1024.0/1024.0)
    else -> "$bytes B"
  }
}
