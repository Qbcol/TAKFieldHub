package org.fieldtak.hub

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fieldtak.hub.data.PackageRepository
import org.fieldtak.hub.data.PublisherTrustStore
import org.fieldtak.hub.data.SimpleJson
import org.fieldtak.hub.deployment.*
import org.fieldtak.hub.diagnostics.ServerDiagnostics
import org.fieldtak.hub.diagnostics.ServiceReport
import org.fieldtak.hub.model.*
import org.fieldtak.hub.provision.ProvisioningController
import org.fieldtak.hub.util.VersionUtil
import org.fieldtak.hub.storage.StorageMaintenance
import org.fieldtak.hub.update.AppUpdateInfo
import org.fieldtak.hub.update.UpdateService
import java.io.File
import java.net.URLDecoder
import java.security.MessageDigest
import java.time.Instant


data class AppUiState(
  val busyMessage:String? = null,
  val error:String? = null,
  val pkg:VerifiedPackage? = null,
  val trusted:Boolean = false,
  val readiness:PhoneReadiness? = null,
  val session:DeploymentSession? = null,
  val history:List<DeploymentSession> = emptyList(),
  val report:ServiceReport? = null,
  val downloadCurrent:Long = 0,
  val downloadTotal:Long? = null,
  val updateNotice:String? = null,
  val appUpdate:AppUpdateInfo? = null,
  val updateMessage:String? = null,
  val storageBytes:Long = 0
)

class MainViewModel(app:Application):AndroidViewModel(app){
  private val repo=PackageRepository(app)
  private val trust=PublisherTrustStore(app)
  val provisioning=ProvisioningController(app)
  private val store=DeploymentStore(app)
  private val engine=DeploymentEngine(store)
  private val diagnostics=ServerDiagnostics(app)
  private val updates=UpdateService(app)
  private val storage=StorageMaintenance(app)
  private val _state=MutableStateFlow(AppUiState(history=historySnapshot(),session=store.active(),storageBytes=storage.totalBytes()))
  val state=_state.asStateFlow()

  init {
    storage.cleanup(store.retainedPaths())
    _state.value=_state.value.copy(storageBytes=storage.totalBytes())
    resumeActive()
    checkForUpdates(silent=true)
  }

  private fun text(id:Int,vararg args:Any)=getApplication<Application>().getString(id,*args)
  private fun humanBytes(bytes:Long):String {
    val unit=1024.0; if(bytes<1024) return "$bytes B"
    val exp=(kotlin.math.ln(bytes.toDouble())/kotlin.math.ln(unit)).toInt().coerceIn(1,4)
    val prefix="KMGT"[exp-1]
    return String.format(java.util.Locale.getDefault(),"%.1f %sB",bytes/Math.pow(unit,exp.toDouble()),prefix)
  }

  fun fromDescriptor(value:String)=viewModelScope.launch(Dispatchers.IO){
    runCatching {
      val descriptorUrl=normalize(value)
      var session=engine.start(descriptorUrl,null)
      session=engine.transition(session,DeploymentStage.QR_SCANNED,StepResult.READY,text(R.string.vm_qr_accepted))
      update(session=session,busy=text(R.string.vm_downloading_descriptor))
      val descriptor=SimpleJson.descriptor(repo.fetchDescriptor(descriptorUrl))
      descriptor.expiresUtc?.let { require(Instant.parse(it).isAfter(Instant.now())) { text(R.string.vm_descriptor_expired) } }
      val freeBytes=provisioning.freeStorageBytes()
      val requiredBytes=descriptor.recommendedFreeBytes?.takeIf{it>0}
        ?: descriptor.packageBytes?.takeIf{it>0}?.let{maxOf(512L*1024*1024,it*3)}
      if(requiredBytes!=null) require(freeBytes>=requiredBytes){text(R.string.disk_space_problem,humanBytes(requiredBytes),humanBytes(freeBytes))}
      session=session.copy(packageUrl=descriptor.packageUrl,updatedUtc=Instant.now().toString()).also(store::saveActive)
      session=engine.transition(session,DeploymentStage.DESCRIPTOR_VERIFIED,StepResult.READY,text(R.string.vm_descriptor_valid))
      session=engine.transition(session,DeploymentStage.DOWNLOADING,StepResult.RUNNING,text(R.string.vm_downloading_resume))
      update(session=session,busy=text(R.string.vm_downloading_package),progress=0 to null)
      val key=stableKey(descriptor.packageUrl)
      val f=repo.downloadResumable(descriptor.packageUrl,key,descriptor.packageSha256){cur,total->_state.value=_state.value.copy(downloadCurrent=cur,downloadTotal=total)}
      session=engine.transition(session,DeploymentStage.BUNDLE_DOWNLOADED,StepResult.READY,text(R.string.vm_package_downloaded))
      loadPackage(f,session)
    }.onFailure { fail(it) }
  }

  fun fromFile(uri:Uri)=viewModelScope.launch(Dispatchers.IO){
    runCatching {
      var session=engine.start(null,null)
      update(session=session,busy=text(R.string.vm_importing))
      val f=repo.copyFromUri(uri)
      session=engine.transition(session,DeploymentStage.BUNDLE_DOWNLOADED,StepResult.READY,text(R.string.vm_local_imported))
      loadPackage(f,session)
    }.onFailure { fail(it) }
  }

  private suspend fun loadPackage(f:File,starting:DeploymentSession){
    var session=starting
    update(session=session,busy=text(R.string.vm_verifying))
    val p=repo.verify(f)
    require(p.manifest.schema=="fieldtak.package" && p.manifest.schemaVersion==2){text(R.string.vm_unsupported_schema)}
    if(p.manifest.distribution.expiresUtc.isNotBlank()) require(Instant.parse(p.manifest.distribution.expiresUtc).isAfter(Instant.now())){text(R.string.vm_package_expired)}
    require(p.signatureValid){text(R.string.vm_bad_signature)}; require(p.hashesValid){text(R.string.vm_bad_hash)}
    session=engine.attachPackage(session,p.manifest.packageInfo.id,p.manifest.packageInfo.name,p.manifest.packageInfo.version,f.absolutePath,p.root.absolutePath)
    session=engine.transition(session,DeploymentStage.BUNDLE_VERIFIED,StepResult.READY,text(R.string.vm_bundle_verified))
    val trusted=trust.isTrusted(p.publisherFingerprint)
    val archived=store.history()
    val history=historySnapshot()
    val previous=archived.firstOrNull{it.packageId==p.manifest.packageInfo.id && it.packageVersion.isNotBlank()}
    val notice=previous?.takeIf{VersionUtil.compare(p.manifest.packageInfo.version,it.packageVersion)>0}?.let{text(R.string.vm_newer_config,it.packageVersion,p.manifest.packageInfo.version)}
    _state.value=_state.value.copy(pkg=p,trusted=trusted,session=session,busyMessage=null,error=null,history=history,updateNotice=notice,storageBytes=storage.totalBytes())
    preflight()
  }

  fun trustPublisher(){
    val p=_state.value.pkg ?: return
    trust.trust(p.publisherFingerprint)
    _state.value=_state.value.copy(trusted=true)
    preflight()
  }

  fun preflight()=viewModelScope.launch(Dispatchers.IO){
    val p=_state.value.pkg ?: return@launch
    var s=_state.value.session ?: return@launch
    if(s.stage==DeploymentStage.COMPLETE){
      val readiness=applySessionReadiness(withServerReadiness(provisioning.computeReadiness(p,_state.value.trusted),_state.value.report),s)
      update(session=s,readiness=readiness,busy=null); return@launch
    }
    s=engine.transition(s,DeploymentStage.PREFLIGHT,StepResult.RUNNING,text(R.string.vm_preflight_phone))
    update(session=s,busy=text(R.string.vm_preflight_busy))
    val report=diagnostics.run(p.manifest.server)
    val readiness=provisioning.computeReadiness(p,_state.value.trusted)
    val atak=provisioning.detectAtak(p.manifest.target)
    s=if(atak.installed && atak.compatibility==CheckState.READY) engine.transition(s,DeploymentStage.ATAK_READY,StepResult.READY,"ATAK ${atak.versionName ?: "?"}")
      else engine.transition(s,DeploymentStage.ATAK_READY,StepResult.ACTION_REQUIRED,atak.compatibilityMessage)
    val pending=provisioning.pendingPlugins(p.root)
    if(pending.isEmpty()) s=engine.transition(s,DeploymentStage.PLUGINS_READY,StepResult.READY,text(R.string.vm_plugins_current))
    update(session=s,report=report,readiness=applySessionReadiness(withServerReadiness(readiness,report),s),busy=null)
  }

  fun preparePhone()=viewModelScope.launch(Dispatchers.IO){
    val p=_state.value.pkg ?: return@launch
    var s=_state.value.session ?: return@launch
    try {
      if(s.stage==DeploymentStage.COMPLETE){
        val report=diagnostics.run(p.manifest.server)
        val readiness=applySessionReadiness(withServerReadiness(provisioning.computeReadiness(p,_state.value.trusted),report),s)
        update(session=s,report=report,readiness=readiness,busy=null); return@launch
      }
      if(!_state.value.trusted){ s=engine.transition(s,DeploymentStage.WAITING_FOR_USER,StepResult.ACTION_REQUIRED,text(R.string.vm_trust_publisher)); update(session=s); return@launch }
      val atak=provisioning.detectAtak(p.manifest.target)
      if(!atak.installed || atak.compatibility!=CheckState.READY){
        val apk=provisioning.atakApk(p.root)
        if(apk!=null && provisioning.canInstallPackages()) { s=engine.transition(s,DeploymentStage.ATAK_READY,StepResult.ACTION_REQUIRED,text(R.string.vm_atak_installer_started)); update(session=s); withContext(Dispatchers.Main){provisioning.installApk(apk)} }
        else { s=engine.transition(s,DeploymentStage.WAITING_FOR_USER,StepResult.ACTION_REQUIRED,atak.compatibilityMessage); update(session=s) }
        return@launch
      }
      val pending=provisioning.pendingPlugins(p.root)
      if(pending.isNotEmpty()){
        if(!provisioning.canInstallPackages()){ s=engine.transition(s,DeploymentStage.WAITING_FOR_USER,StepResult.ACTION_REQUIRED,text(R.string.vm_allow_unknown_sources)); update(session=s); withContext(Dispatchers.Main){provisioning.openUnknownSourcesSettings()}; return@launch }
        val next=pending.first(); s=engine.transition(s,DeploymentStage.PLUGINS_INSTALLING,StepResult.ACTION_REQUIRED,"${next.label}: ${next.sourceVersion ?: next.file.name}"); update(session=s)
        withContext(Dispatchers.Main){provisioning.installApk(next.file)}; return@launch
      }
      s=engine.transition(s,DeploymentStage.PLUGINS_READY,StepResult.READY,text(R.string.vm_plugins_ready))
      val mission=provisioning.missionPackage(p.root)
      val alreadyHanded=s.steps.any{it.stage==DeploymentStage.MAPS_READY && (it.result==StepResult.READY || it.result==StepResult.WARNING)}
      if(mission!=null && !alreadyHanded){ s=engine.transition(s,DeploymentStage.MAPS_IMPORTING,StepResult.ACTION_REQUIRED,text(R.string.vm_handoff_mission)); update(session=s); withContext(Dispatchers.Main){provisioning.openMissionPackage(mission)}; return@launch }
      verifyOtsAndFinish(s)
    } catch(t:Throwable){ fail(t) }
  }

  fun onExternalReturn(){
    viewModelScope.launch(Dispatchers.IO){
      val p=_state.value.pkg ?: return@launch
      var s=_state.value.session ?: return@launch
      if(s.stage==DeploymentStage.COMPLETE){
        val readiness=applySessionReadiness(withServerReadiness(provisioning.computeReadiness(p,_state.value.trusted),_state.value.report),s)
        update(session=s,readiness=readiness,busy=null); return@launch
      }
      when(s.stage){
        DeploymentStage.PLUGINS_INSTALLING -> if(provisioning.pendingPlugins(p.root).isEmpty()) s=engine.transition(s,DeploymentStage.PLUGINS_READY,StepResult.READY,text(R.string.vm_plugin_install_complete))
        DeploymentStage.MAPS_IMPORTING -> s=engine.transition(s,DeploymentStage.MAPS_READY,StepResult.WARNING,text(R.string.vm_mission_handed))
        DeploymentStage.ATAK_READY, DeploymentStage.WAITING_FOR_USER -> Unit
        else -> Unit
      }
      update(session=s)
      preflight()
    }
  }

  fun onPackageChanged(){ onExternalReturn() }

  fun runDiagnostics()=viewModelScope.launch(Dispatchers.IO){
    val p=_state.value.pkg ?: return@launch
    update(busy=text(R.string.vm_ots_diagnostics))
    val r=diagnostics.run(p.manifest.server)
    val base=provisioning.computeReadiness(p,_state.value.trusted)
    update(report=r,readiness=applySessionReadiness(withServerReadiness(base,r),_state.value.session),busy=null)
  }

  private suspend fun verifyOtsAndFinish(start:DeploymentSession){
    val p=_state.value.pkg ?: return
    var s=engine.transition(start,DeploymentStage.OTS_VERIFYING,StepResult.RUNNING,text(R.string.vm_checking_ports))
    update(session=s,busy=text(R.string.vm_checking_ots))
    val report=diagnostics.run(p.manifest.server)
    val networkOk=report.checks.filter{it.id in setOf("dns","api","web","cot")}.all{it.state==CheckState.READY}
    s=if(networkOk) engine.transition(s,DeploymentStage.OTS_CONNECTED,StepResult.WARNING,text(R.string.vm_server_reachable))
      else engine.transition(s,DeploymentStage.OTS_CONNECTED,StepResult.ACTION_REQUIRED,text(R.string.vm_server_attention))
    val readiness=applySessionReadiness(withServerReadiness(provisioning.computeReadiness(p,_state.value.trusted),report),s)
    if(networkOk && readiness.items.none{it.state==CheckState.PROBLEM} && provisioning.pendingPlugins(p.root).isEmpty()){
      s=engine.transition(s,DeploymentStage.FINAL_VERIFY,StepResult.READY,text(R.string.vm_local_checks_complete))
      s=engine.complete(s,text(R.string.vm_phone_prepared))
    }
    _state.value=_state.value.copy(session=s,report=report,readiness=applySessionReadiness(readiness,s),busyMessage=null,error=null,history=historySnapshot())
  }

  fun checkForUpdates(silent:Boolean=false)=viewModelScope.launch(Dispatchers.IO){
    runCatching { updates.check() }.onSuccess { info ->
      _state.value=_state.value.copy(appUpdate=info,updateMessage=if(info==null && !silent) text(R.string.no_update) else null)
    }.onFailure { t -> if(!silent) _state.value=_state.value.copy(updateMessage=text(R.string.update_check_failed,t.message ?: t::class.java.simpleName)) }
  }

  fun installAppUpdate()=viewModelScope.launch(Dispatchers.IO){
    val info=_state.value.appUpdate ?: return@launch
    runCatching {
      update(busy=text(R.string.vm_downloading_package),progress=0L to null)
      val apk=updates.download(info){cur,total->_state.value=_state.value.copy(downloadCurrent=cur,downloadTotal=total)}
      update(busy=null)
      if(!provisioning.canInstallPackages()){
        _state.value=_state.value.copy(updateMessage=text(R.string.update_install_permission)); withContext(Dispatchers.Main){provisioning.openUnknownSourcesSettings()}
      } else {
        _state.value=_state.value.copy(updateMessage=text(R.string.update_downloaded)); withContext(Dispatchers.Main){provisioning.installApk(apk)}
      }
    }.onFailure { t -> _state.value=_state.value.copy(busyMessage=null,updateMessage=text(R.string.update_download_failed,t.message ?: t::class.java.simpleName)) }
  }

  fun cleanupStorage(){
    storage.cleanup(store.retainedPaths())
    _state.value=_state.value.copy(storageBytes=storage.totalBytes(),updateMessage=text(R.string.cleanup_done))
  }

  fun retryCurrent(){ preparePhone() }

  fun openAtak(){ provisioning.openAtak() }

  fun resetPackage(){
    _state.value=_state.value.copy(pkg=null,trusted=false,readiness=null,session=null,report=null,error=null,busyMessage=null,downloadCurrent=0,downloadTotal=null,updateNotice=null,storageBytes=storage.totalBytes())
    store.clearActive()
  }

  fun clearError(){ _state.value=_state.value.copy(error=null,busyMessage=null) }

  fun clearHistory(){ store.clearHistory(); _state.value=_state.value.copy(history=historySnapshot()) }

  fun restoreHistory(id:String)=viewModelScope.launch(Dispatchers.IO){
    val old=historySnapshot().firstOrNull{it.id==id} ?: return@launch
    val path=old.localPackagePath ?: return@launch
    val f=File(path); if(!f.isFile){ _state.value=_state.value.copy(error=text(R.string.vm_history_missing)); return@launch }
    runCatching {
      val session=engine.start(old.sourceDescriptor,old.packageUrl)
      loadPackage(f,engine.transition(session,DeploymentStage.BUNDLE_DOWNLOADED,StepResult.READY,text(R.string.vm_history_reuse)))
    }.onFailure{fail(it)}
  }

  private fun historySnapshot():List<DeploymentSession> {
    val archived=store.history()
    val active=store.active()?.takeIf{it.stage!=DeploymentStage.COMPLETE}
    return listOfNotNull(active)+archived.filterNot{it.id==active?.id}
  }

  private fun resumeActive(){
    val s=store.active() ?: return
    val path=s.localPackagePath
    if(path.isNullOrBlank()){
      if(s.stage in setOf(DeploymentStage.DOWNLOADING,DeploymentStage.DESCRIPTOR_VERIFIED,DeploymentStage.QR_SCANNED) && !s.sourceDescriptor.isNullOrBlank()){
        fromDescriptor(s.sourceDescriptor); return
      }
      return
    }
    val f=File(path); if(!f.isFile) return
    viewModelScope.launch(Dispatchers.IO){ runCatching { loadPackage(f,s) }.onFailure { fail(it) } }
  }

  private fun fail(t:Throwable){
    val message=t.message ?: t::class.java.simpleName
    val s=_state.value.session?.let{engine.fail(it,message)}
    _state.value=_state.value.copy(error=message,busyMessage=null,session=s,history=historySnapshot())
  }

  private fun applySessionReadiness(base:PhoneReadiness,session:DeploymentSession?):PhoneReadiness {
    if(session==null) return base
    val missionDone=session.steps.any{it.stage==DeploymentStage.MAPS_READY && it.result in setOf(StepResult.READY,StepResult.WARNING)}
    val complete=session.stage==DeploymentStage.COMPLETE
    val items=base.items.map { item ->
      when {
        item.id=="mission" && missionDone -> item.copy(state=CheckState.READY,detail=text(R.string.vm_mission_done))
        item.id=="ots" && complete -> item.copy(state=CheckState.READY,detail=text(R.string.vm_ots_tests_done))
        else -> item
      }
    }
    val score=items.sumOf{when(it.state){CheckState.READY->100;CheckState.UNKNOWN->55;CheckState.ACTION_REQUIRED->25;CheckState.PROBLEM->0}}/maxOf(1,items.size)
    val ready=items.isNotEmpty() && items.all{it.state==CheckState.READY}
    return PhoneReadiness(score,items,ready)
  }

  private fun withServerReadiness(base:PhoneReadiness,report:ServiceReport?):PhoneReadiness {
    if(report==null) return base
    val net=report.checks.filter{it.id in setOf("dns","api","web","cot")}
    val state=when { net.any{it.state==CheckState.PROBLEM}->CheckState.ACTION_REQUIRED; net.isNotEmpty() && net.all{it.state==CheckState.READY}->CheckState.READY; else->CheckState.UNKNOWN }
    val items=base.items.filterNot{it.id in setOf("ots","atak_auth")}+listOf(
      ReadinessItem("ots","OpenTAK Server",state,if(state==CheckState.READY)text(R.string.vm_ots_reachable) else text(R.string.vm_check_diagnostics)),
      ReadinessItem("atak_auth",text(R.string.atak_connection),CheckState.UNKNOWN,text(R.string.vm_atak_auth_detail))
    )
    val score=items.sumOf{when(it.state){CheckState.READY->100;CheckState.UNKNOWN->55;CheckState.ACTION_REQUIRED->25;CheckState.PROBLEM->0}}/maxOf(1,items.size)
    return PhoneReadiness(score,items,base.ready && state==CheckState.READY && items.none{it.state==CheckState.UNKNOWN})
  }

  private fun update(session:DeploymentSession?=_state.value.session,readiness:PhoneReadiness?=_state.value.readiness,report:ServiceReport?=_state.value.report,busy:String?=_state.value.busyMessage,progress:Pair<Long,Long?>?=null){
    _state.value=_state.value.copy(session=session,readiness=readiness,report=report,busyMessage=busy,error=null,
      downloadCurrent=progress?.first ?: _state.value.downloadCurrent,downloadTotal=progress?.second ?: _state.value.downloadTotal,history=historySnapshot())
  }

  private fun normalize(v:String):String {
    val t=v.trim()
    return if(t.startsWith("fieldtak://provision")) {
      val uri=Uri.parse(t); uri.getQueryParameter("url")?.let{URLDecoder.decode(it,"UTF-8")} ?: error(text(R.string.vm_qr_missing_url))
    } else t
  }

  private fun stableKey(v:String):String = MessageDigest.getInstance("SHA-256").digest(v.toByteArray()).take(12).joinToString(""){"%02x".format(it)}
}
