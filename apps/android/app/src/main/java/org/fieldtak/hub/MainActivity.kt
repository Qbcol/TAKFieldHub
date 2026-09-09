package org.fieldtak.hub

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.fieldtak.hub.deployment.DeploymentStage
import org.fieldtak.hub.deployment.StepResult
import org.fieldtak.hub.i18n.LanguageManager
import org.fieldtak.hub.model.CheckState
import org.fieldtak.hub.model.PluginInstallState
import org.fieldtak.hub.ui.QrScanner

class MainActivity:AppCompatActivity(){
  private val vm:MainViewModel by viewModels()
  private val packageReceiver=object:BroadcastReceiver(){ override fun onReceive(context:Context?,intent:Intent?){ vm.onPackageChanged() } }

  override fun onCreate(savedInstanceState:Bundle?){
    super.onCreate(savedInstanceState)
    val filter=IntentFilter().apply { addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REPLACED); addAction(Intent.ACTION_PACKAGE_REMOVED); addDataScheme("package") }
    ContextCompat.registerReceiver(this,packageReceiver,filter,ContextCompat.RECEIVER_EXPORTED)
    setContent{MaterialTheme{FieldTakApp(vm,this)}}
    handleIntent(intent)
  }
  override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent)}
  override fun onResume(){super.onResume();vm.onExternalReturn()}
  override fun onDestroy(){runCatching{unregisterReceiver(packageReceiver)};super.onDestroy()}
  private fun handleIntent(intent:Intent?){intent?.data?.let{if(it.scheme=="fieldtak") vm.fromDescriptor(it.toString())}}
}

enum class AppScreen { HOME, SCAN, DETAILS, SERVICE, HISTORY, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldTakApp(vm:MainViewModel,activity:MainActivity){
  val state by vm.state.collectAsStateWithLifecycle()
  val stack=remember{mutableStateListOf(AppScreen.HOME)}
  val screen=stack.last()
  fun push(s:AppScreen){if(stack.lastOrNull()!=s) stack.add(s)}
  fun back(){if(stack.size>1) stack.removeAt(stack.lastIndex)}
  BackHandler(enabled=stack.size>1){back()}
  val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){u:Uri?->u?.let{vm.fromFile(it);while(stack.size>1)stack.removeAt(stack.lastIndex)}}
  var url by remember{mutableStateOf("")}

  Scaffold(topBar={TopAppBar(
    title={Text(stringResource(R.string.app_title))},
    navigationIcon={if(stack.size>1) IconButton(onClick={back()}){Text("‹",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.semantics{contentDescription=activity.getString(R.string.back)})}},
    actions={IconButton(onClick={push(AppScreen.SETTINGS)}){Text("⚙",modifier=Modifier.semantics{contentDescription=activity.getString(R.string.settings)})}}
  )}){pad->
    Box(Modifier.padding(pad).fillMaxSize()){
      when(screen){
        AppScreen.HOME->HomeScreen(state,vm,onScan={push(AppScreen.SCAN)},onImport={picker.launch(arrayOf("application/zip","application/octet-stream","application/vnd.fieldtak.package"))},url=url,onUrl={url=it},onOpenUrl={vm.fromDescriptor(url)},onDetails={push(AppScreen.DETAILS)},onService={push(AppScreen.SERVICE)},onHistory={push(AppScreen.HISTORY)})
        AppScreen.SCAN->Box(Modifier.fillMaxSize()){QrScanner{v->back();vm.fromDescriptor(v)};FilledTonalButton(onClick={back()},modifier=Modifier.align(Alignment.BottomCenter).padding(24.dp).heightIn(min=48.dp)){Text(stringResource(R.string.cancel))}}
        AppScreen.DETAILS->DetailsScreen(state,vm)
        AppScreen.SERVICE->ServiceScreen(state,vm,activity)
        AppScreen.HISTORY->HistoryScreen(state,vm)
        AppScreen.SETTINGS->SettingsScreen(state,vm)
      }
      state.busyMessage?.let{BusyOverlay(it,state.downloadCurrent,state.downloadTotal)}
      state.error?.let{ErrorDialog(it,onDismiss=vm::clearError)}
    }
  }
}

@Composable
private fun HomeScreen(state:AppUiState,vm:MainViewModel,onScan:()->Unit,onImport:()->Unit,url:String,onUrl:(String)->Unit,onOpenUrl:()->Unit,onDetails:()->Unit,onService:()->Unit,onHistory:()->Unit){
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
    val pkg=state.pkg
    if(pkg==null){
      Text(stringResource(R.string.prepare_title),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
      Text(stringResource(R.string.prepare_subtitle))
      Button(onClick=onScan,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text(stringResource(R.string.scan_qr))}
      OutlinedButton(onClick=onImport,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.import_ftak))}
      HorizontalDivider()
      OutlinedTextField(value=url,onValueChange=onUrl,label={Text(stringResource(R.string.descriptor_url))},modifier=Modifier.fillMaxWidth(),singleLine=true)
      FilledTonalButton(onClick=onOpenUrl,enabled=url.isNotBlank(),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.open_link))}
      if(state.history.isNotEmpty()) OutlinedButton(onClick=onHistory,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.history_count,state.history.size))}
      state.appUpdate?.let{UpdateCard(it.version,vm::installAppUpdate)}
      return@Column
    }

    val r=state.readiness
    Text(pkg.manifest.packageInfo.name.ifBlank{"Field TAK Package"},style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
    Text(stringResource(R.string.package_line,pkg.manifest.packageInfo.version,pkg.manifest.packageInfo.publisher))
    state.updateNotice?.let{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(stringResource(R.string.update),fontWeight=FontWeight.Bold);Text(it)}}}
    state.appUpdate?.let{UpdateCard(it.version,vm::installAppUpdate)}
    ReadinessGauge(r?.percent ?: 0)
    val headline=when {
      state.session?.stage==DeploymentStage.COMPLETE && r?.ready==true -> stringResource(R.string.phone_ready)
      state.session?.stage==DeploymentStage.COMPLETE -> stringResource(R.string.configuration_complete_check_atak)
      else -> stringResource(R.string.phone_status,r?.percent ?: 0)
    }
    Text(headline,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
    r?.items?.take(6)?.forEach{ReadinessRow(it.label,it.state,it.detail)}

    if(!state.trusted) FilledTonalButton(onClick=vm::trustPublisher,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text(stringResource(R.string.trust_publisher))}
    Button(onClick=vm::preparePhone,modifier=Modifier.fillMaxWidth().heightIn(min=60.dp)){Text(if(state.session?.stage==DeploymentStage.COMPLETE)stringResource(R.string.check_again) else stringResource(R.string.finish_configuration))}
    OutlinedButton(onClick=onDetails,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.details_checklist))}
    OutlinedButton(onClick=onScan,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.scan_new_qr))}
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()){
      FilledTonalButton(onClick=onService,modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text(stringResource(R.string.diagnostics))}
      FilledTonalButton(onClick=onHistory,modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text(stringResource(R.string.history))}
    }
    TextButton(onClick=vm::resetPackage,modifier=Modifier.align(Alignment.CenterHorizontally).heightIn(min=48.dp)){Text(stringResource(R.string.close_current_package))}
  }
}

@Composable
private fun UpdateCard(version:String,onInstall:()->Unit){
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text(stringResource(R.string.update),fontWeight=FontWeight.Bold)
    Text(stringResource(R.string.update_available,version))
    Button(onClick=onInstall,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.download_update))}
  }}
}

@Composable
private fun DetailsScreen(state:AppUiState,vm:MainViewModel){
  val pkg=state.pkg
  if(pkg==null){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(stringResource(R.string.no_active_package))};return}
  val plugins=remember(pkg.root,state.session?.updatedUtc){vm.provisioning.pluginStatuses(pkg.root)}
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Text(stringResource(R.string.deployment_checklist),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
    state.readiness?.items?.forEach{ReadinessRow(it.label,it.state,it.detail)}
    HorizontalDivider()
    Text(stringResource(R.string.atak),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
    val atak=vm.provisioning.detectAtak(pkg.manifest.target); Text("${statusMark(atak.compatibility)} ATAK ${atak.versionName ?: stringResource(R.string.missing)} — ${atak.compatibilityMessage}")
    Text(stringResource(R.string.plugins),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
    if(plugins.isEmpty()) Text(stringResource(R.string.no_plugins))
    plugins.forEach{p->
      val label=when(p.state){PluginInstallState.CURRENT->stringResource(R.string.state_ready);PluginInstallState.UPDATE_REQUIRED->stringResource(R.string.state_update);PluginInstallState.NOT_INSTALLED->stringResource(R.string.state_install);PluginInstallState.UNKNOWN->stringResource(R.string.state_unknown)}
      Text("• ${p.label}: ${p.installedVersion ?: "—"} → ${p.sourceVersion ?: "?"} [$label]")
    }
    HorizontalDivider()
    Text(stringResource(R.string.state_machine),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
    state.session?.steps?.sortedBy{it.updatedUtc}?.forEach{st->Text("${stepMark(st.result)} ${st.stage.name} — ${st.detail}",style=MaterialTheme.typography.bodySmall)}
    Button(onClick=vm::preparePhone,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.repair_continue))}
    OutlinedButton(onClick=vm::preflight,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.refresh_preflight))}
  }
}

@Composable
private fun ServiceScreen(state:AppUiState,vm:MainViewModel,activity:MainActivity){
  val report=state.report
  val context=LocalContext.current
  val saveReport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->
    if(uri!=null && report!=null) runCatching{context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(report.asText())}}
  }
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Text(stringResource(R.string.service_mode),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
    Text(stringResource(R.string.service_explanation))
    Button(onClick=vm::runDiagnostics,enabled=state.pkg!=null,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.run_diagnostics))}
    report?.checks?.forEach{ReadinessRow(it.label,it.state,it.detail+(if(it.technical.isNotBlank())"\n${it.technical}" else ""))}
    if(report!=null){
      OutlinedButton(onClick={ val cb=context.getSystemService(ClipboardManager::class.java); cb.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.report_title),report.asText())) },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.copy_report))}
      OutlinedButton(onClick={saveReport.launch("fieldtak-report-${System.currentTimeMillis()}.txt")},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.save_report))}
      TextButton(onClick={vm.provisioning.shareText(activity,context.getString(R.string.report_title),report.asText())},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.share_report)) }
    }
    OutlinedButton(onClick=vm::openAtak,enabled=state.pkg!=null,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.open_atak))}
  }
}

@Composable
private fun HistoryScreen(state:AppUiState,vm:MainViewModel){
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Text(stringResource(R.string.recent_deployments),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
    if(state.history.isEmpty()) Text(stringResource(R.string.history_empty))
    state.history.forEach{s->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(s.packageName.ifBlank{stringResource(R.string.unknown_package)},fontWeight=FontWeight.Bold);Text("${s.packageVersion} • ${s.stage.name}");Text(s.completedUtc ?: s.updatedUtc,style=MaterialTheme.typography.bodySmall);TextButton(onClick={vm.restoreHistory(s.id)},modifier=Modifier.heightIn(min=48.dp)){Text(stringResource(R.string.open_redeploy))}}}}
    if(state.history.isNotEmpty()) TextButton(onClick=vm::clearHistory,modifier=Modifier.align(Alignment.End).heightIn(min=48.dp)){Text(stringResource(R.string.clear_history))}
  }
}

@Composable
private fun SettingsScreen(state:AppUiState,vm:MainViewModel){
  var langMenu by remember{mutableStateOf(false)}
  val current=LanguageManager.currentTag()
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
    Text(stringResource(R.string.settings_title),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
    Text(stringResource(R.string.version_label,BuildConfig.VERSION_NAME))
    Text(stringResource(R.string.update_channel,BuildConfig.RELEASE_CHANNEL))
    Text(stringResource(R.string.accessibility_note))
    Text(stringResource(R.string.language),fontWeight=FontWeight.SemiBold)
    Box{
      OutlinedButton(onClick={langMenu=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(when(current){"pl"->stringResource(R.string.language_polish);"en"->stringResource(R.string.language_english);else->stringResource(R.string.language_system)})}
      DropdownMenu(expanded=langMenu,onDismissRequest={langMenu=false}){
        DropdownMenuItem(text={Text(stringResource(R.string.language_system))},onClick={langMenu=false;LanguageManager.apply("system")})
        DropdownMenuItem(text={Text(stringResource(R.string.language_polish))},onClick={langMenu=false;LanguageManager.apply("pl")})
        DropdownMenuItem(text={Text(stringResource(R.string.language_english))},onClick={langMenu=false;LanguageManager.apply("en")})
      }
    }
    HorizontalDivider()
    Text(stringResource(R.string.storage),fontWeight=FontWeight.SemiBold)
    Text(stringResource(R.string.storage_usage,humanBytes(state.storageBytes)))
    OutlinedButton(onClick=vm::cleanupStorage,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.cleanup_storage))}
    HorizontalDivider()
    Button(onClick={vm.checkForUpdates(false)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.check_updates))}
    state.appUpdate?.let{UpdateCard(it.version,vm::installAppUpdate)}
    state.updateMessage?.let{Text(it)}
    Text(stringResource(R.string.local_http_warning),style=MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun ReadinessGauge(percent:Int){
  val desc=stringResource(R.string.progress_description,percent.coerceIn(0,100))
  LinearProgressIndicator(progress={percent.coerceIn(0,100)/100f},modifier=Modifier.fillMaxWidth().height(10.dp).semantics { contentDescription=desc; progressBarRangeInfo=ProgressBarRangeInfo(percent.coerceIn(0,100)/100f,0f..1f) })
}

@Composable
private fun ReadinessRow(label:String,state:CheckState,detail:String){
  val stateLabel=when(state){CheckState.READY->stringResource(R.string.status_ready);CheckState.ACTION_REQUIRED->stringResource(R.string.status_action_required);CheckState.PROBLEM->stringResource(R.string.status_problem);CheckState.UNKNOWN->stringResource(R.string.status_unknown)}
  val description=stringResource(R.string.accessibility_readiness_row,label,stateLabel,detail)
  Row(Modifier.fillMaxWidth().semantics(mergeDescendants=true){contentDescription=description},horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.Top){Text("${statusMark(state)} $stateLabel",fontWeight=FontWeight.Bold);Column(Modifier.weight(1f)){Text(label,fontWeight=FontWeight.SemiBold);if(detail.isNotBlank())Text(detail,style=MaterialTheme.typography.bodySmall)}}
}

private fun statusMark(s:CheckState)=when(s){CheckState.READY->"✓";CheckState.ACTION_REQUIRED->"!";CheckState.PROBLEM->"✕";CheckState.UNKNOWN->"?"}
private fun stepMark(s:StepResult)=when(s){StepResult.READY->"✓";StepResult.ACTION_REQUIRED->"!";StepResult.WARNING->"~";StepResult.FAILED->"✕";StepResult.RUNNING->"…";StepResult.PENDING->"○";StepResult.SKIPPED->"-"}

@Composable
private fun BusyOverlay(message:String,current:Long,total:Long?){
  Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.scrim.copy(alpha=.55f)){
    Box(contentAlignment=Alignment.Center){Card{Column(Modifier.padding(24.dp).widthIn(min=260.dp,max=360.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){CircularProgressIndicator();Text(message);if(current>0){val mb=current/1024.0/1024.0;Text(if(total!=null)"%.1f / %.1f MB".format(mb,total/1024.0/1024.0) else "%.1f MB".format(mb),style=MaterialTheme.typography.bodySmall)}}}}
  }
}

@Composable
private fun ErrorDialog(message:String,onDismiss:()->Unit){AlertDialog(onDismissRequest=onDismiss,confirmButton={TextButton(onClick=onDismiss){Text("OK")}},title={Text(stringResource(R.string.problem))},text={Text(message)})}

private fun humanBytes(bytes:Long):String=when{bytes>=1024L*1024*1024->"%.1f GB".format(bytes/1024.0/1024.0/1024.0);bytes>=1024L*1024->"%.1f MB".format(bytes/1024.0/1024.0);else->"$bytes B"}
