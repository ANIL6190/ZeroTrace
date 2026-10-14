package com.example.zerotrace.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zerotrace.data.model.AlertHistoryEntry
import com.example.zerotrace.data.model.AlertLevel
import com.example.zerotrace.data.model.BaselineDeltas
import com.example.zerotrace.data.model.FileOpEvent
import com.example.zerotrace.data.model.FileOpType
import com.example.zerotrace.data.model.SandboxFileInfo
import com.example.zerotrace.data.model.SlidingWindowMetrics
import com.example.zerotrace.data.model.TwoStageAnalysisResult
import com.example.zerotrace.data.model.WorkloadMode
import com.example.zerotrace.ui.theme.ZeroTraceTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentinelPulseScreen(
    viewModel: SentinelPulseViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SentinelPulseContent(
        uiState = uiState,
        onSelectTab = { viewModel.selectTab(it) },
        onStartSimulation = { viewModel.startSimulation(it) },
        onStopSimulation = { viewModel.stopSimulation() },
        onClearSandbox = { viewModel.clearSandbox() },
        onResetAnalyzer = { viewModel.resetAnalyzer() },
        onManualOp = { type, isSuspicious ->
            viewModel.performSingleOperation(type, isSuspicious = isSuspicious)
        },
        onShowWarningDialog = { viewModel.showWarningDialog() },
        onDismissWarningDialog = { viewModel.dismissWarningDialog() },
        onIsolateProcess = { viewModel.isolateProcess() },
        onBackupState = { viewModel.backupState() },
        onInspectSandbox = { viewModel.inspectSandbox() },
        onUnIsolateProcess = { viewModel.unIsolateProcess() },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentinelPulseContent(
    uiState: SentinelPulseUiState,
    onSelectTab: (Int) -> Unit,
    onStartSimulation: (WorkloadMode) -> Unit,
    onStopSimulation: () -> Unit,
    onClearSandbox: () -> Unit,
    onResetAnalyzer: () -> Unit,
    onManualOp: (FileOpType, Boolean) -> Unit,
    onShowWarningDialog: () -> Unit,
    onDismissWarningDialog: () -> Unit,
    onIsolateProcess: () -> Unit,
    onBackupState: () -> Unit,
    onInspectSandbox: () -> Unit,
    onUnIsolateProcess: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("SentinelPulse", fontWeight = FontWeight.Bold, fontSize = 20.sp)

                        if (uiState.isProcessIsolated) {
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Block,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("ISOLATED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (uiState.analysisResult.alertLevel != AlertLevel.NORMAL) {
                        IconButton(onClick = onShowWarningDialog) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "View Warning",
                                tint = if (uiState.analysisResult.alertLevel == AlertLevel.HIGH_RISK_WARNING)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Status Alert Banner
            AlertBanner(
                alertLevel = uiState.analysisResult.alertLevel,
                isSimulating = uiState.isSimulating,
                isIsolated = uiState.isProcessIsolated,
                mode = uiState.workloadMode,
                consecutiveBursts = uiState.analysisResult.consecutiveBurstCount,
                onShowWarningDialog = onShowWarningDialog
            )

            // Status message bar if any
            if (uiState.statusMessage.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.statusMessage,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Primary Tabs
            PrimaryTabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { onSelectTab(0) },
                    text = { Text("Dashboard", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    text = { Text("Timeline", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { onSelectTab(2) },
                    text = { Text("Risk & History", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == 3,
                    onClick = { onSelectTab(3) },
                    text = { Text("Sandbox", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Main Tab Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.selectedTab) {
                    0 -> DashboardTab(
                        uiState = uiState,
                        onStartSimulation = onStartSimulation,
                        onStopSimulation = onStopSimulation,
                        onClearSandbox = onClearSandbox,
                        onResetAnalyzer = onResetAnalyzer,
                        onManualOp = onManualOp,
                        onShowWarningDialog = onShowWarningDialog,
                        onUnIsolateProcess = onUnIsolateProcess
                    )
                    1 -> TimelineTab(
                        events = uiState.recentEvents,
                        totalEventsCount = uiState.totalEventsCount
                    )
                    2 -> RiskAndHistoryTab(
                        analysisResult = uiState.analysisResult,
                        baselineDeltas = uiState.baselineDeltas,
                        alertHistory = uiState.alertHistory,
                        onShowWarningDialog = onShowWarningDialog
                    )
                    3 -> SandboxTab(
                        sandboxFiles = uiState.sandboxFiles,
                        backupMessage = uiState.backupMessage,
                        onClearSandbox = onClearSandbox,
                        onBackupState = onBackupState,
                        onManualOp = onManualOp
                    )
                }
            }
        }

        // Explainable Warning Modal / Dialog
        if (uiState.showWarningDialog) {
            ExplainableWarningDialog(
                analysisResult = uiState.analysisResult,
                baselineDeltas = uiState.baselineDeltas,
                isProcessIsolated = uiState.isProcessIsolated,
                onDismiss = onDismissWarningDialog,
                onIsolateProcess = onIsolateProcess,
                onBackupState = onBackupState,
                onInspectSandbox = onInspectSandbox
            )
        }
    }
}

// -----------------------------------------------------------------------------
// ALERT BANNER
// -----------------------------------------------------------------------------

@Composable
fun AlertBanner(
    alertLevel: AlertLevel,
    isSimulating: Boolean,
    isIsolated: Boolean,
    mode: WorkloadMode,
    consecutiveBursts: Int,
    onShowWarningDialog: () -> Unit
) {
    val (bgColor, contentColor, statusText, subText, icon) = when {
        isIsolated -> Tuple5(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "PROCESS ISOLATED — Threats Halted",
            "Workload process has been isolated by administrator safe response.",
            Icons.Default.Block
        )
        alertLevel == AlertLevel.NORMAL -> Tuple5(
            Color(0xFF1B382B),
            Color(0xFF81E8AF),
            "NORMAL BASELINE — Systems Safe",
            if (isSimulating) "Workload: $mode | Normal velocity monitored" else "Sandbox Idle | Private Sandbox Boundary Protected",
            Icons.Default.Shield
        )
        alertLevel == AlertLevel.ELEVATED_ACTIVITY -> Tuple5(
            Color(0xFF3E3113),
            Color(0xFFFFD562),
            "STAGE 1 BURST ANOMALY DETECTED",
            "High operation velocity / rename burst. Monitoring consecutive windows ($consecutiveBursts/2).",
            Icons.Default.Warning
        )
        else -> Tuple5(
            Color(0xFF451316),
            Color(0xFFFFB3AC),
            "STAGE 2 HIGH RISK PERSISTENCE WARNING!",
            "Ransomware behavioral pattern verified across persistent time windows.",
            Icons.Default.BugReport
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    color = contentColor.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
            }

            if (alertLevel != AlertLevel.NORMAL) {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onShowWarningDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = contentColor,
                        contentColor = bgColor
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Explain", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

// -----------------------------------------------------------------------------
// TAB 0: DASHBOARD & METRICS
// -----------------------------------------------------------------------------

@Composable
fun DashboardTab(
    uiState: SentinelPulseUiState,
    onStartSimulation: (WorkloadMode) -> Unit,
    onStopSimulation: () -> Unit,
    onClearSandbox: () -> Unit,
    onResetAnalyzer: () -> Unit,
    onManualOp: (FileOpType, Boolean) -> Unit,
    onShowWarningDialog: () -> Unit,
    onUnIsolateProcess: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Simulator Control Panel
        item {
            SimulatorControlCard(
                isSimulating = uiState.isSimulating,
                isIsolated = uiState.isProcessIsolated,
                currentMode = uiState.workloadMode,
                onStartSimulation = onStartSimulation,
                onStopSimulation = onStopSimulation,
                onClearSandbox = onClearSandbox,
                onResetAnalyzer = onResetAnalyzer,
                onManualOp = onManualOp,
                onUnIsolateProcess = onUnIsolateProcess
            )
        }

        // 2. Visual Metric Indicators & Baseline Deltas
        item {
            MetricsAndDeltasCard(
                metrics = uiState.analysisResult.metrics,
                deltas = uiState.baselineDeltas,
                stage1Triggered = uiState.analysisResult.stage1BurstTriggered,
                stage2Triggered = uiState.analysisResult.stage2PersistenceTriggered,
                consecutiveBursts = uiState.analysisResult.consecutiveBurstCount
            )
        }

        // 3. Explainable Reasons Summary Card
        item {
            ExplainableReasonsCard(
                reasons = uiState.analysisResult.explainableReasons,
                alertLevel = uiState.analysisResult.alertLevel,
                onShowWarningDialog = onShowWarningDialog
            )
        }

        // 4. Quick Event Snapshot
        item {
            QuickEventsCard(
                events = uiState.recentEvents.take(5),
                totalEventsCount = uiState.totalEventsCount
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// -----------------------------------------------------------------------------
// CONTROL PANEL CARD
// -----------------------------------------------------------------------------

@Composable
fun SimulatorControlCard(
    isSimulating: Boolean,
    isIsolated: Boolean,
    currentMode: WorkloadMode,
    onStartSimulation: (WorkloadMode) -> Unit,
    onStopSimulation: () -> Unit,
    onClearSandbox: () -> Unit,
    onResetAnalyzer: () -> Unit,
    onManualOp: (FileOpType, Boolean) -> Unit,
    onUnIsolateProcess: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Simulator Control Panel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isIsolated) {
                    TextButton(onClick = onUnIsolateProcess) {
                        Text("Clear Isolation", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Workload Trigger Buttons
            Text(
                text = "Initiate Workload Simulation:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onStartSimulation(WorkloadMode.NORMAL_USER_WORKLOAD) },
                    enabled = !isIsolated && (!isSimulating || currentMode != WorkloadMode.NORMAL_USER_WORKLOAD),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Normal", fontSize = 11.sp)
                }

                Button(
                    onClick = { onStartSimulation(WorkloadMode.RANSOMWARE_SIMULATION_WORKLOAD) },
                    enabled = !isIsolated && (!isSimulating || currentMode != WorkloadMode.RANSOMWARE_SIMULATION_WORKLOAD),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ransomware", fontSize = 11.sp)
                }

                Button(
                    onClick = { onStartSimulation(WorkloadMode.BURST_STORM_WORKLOAD) },
                    enabled = !isIsolated && (!isSimulating || currentMode != WorkloadMode.BURST_STORM_WORKLOAD),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text("Burst Storm", fontSize = 11.sp)
                }
            }

            // Engine Management Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSimulating) {
                    Button(
                        onClick = onStopSimulation,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = onResetAnalyzer,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset Engine", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onClearSandbox,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear Sandbox", fontSize = 11.sp)
                }
            }

            // Manual Single Operation Triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onManualOp(FileOpType.WRITE, false) },
                    enabled = !isIsolated,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+1 Write", fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = { onManualOp(FileOpType.RENAME, false) },
                    enabled = !isIsolated,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+1 Rename", fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = { onManualOp(FileOpType.RENAME, true) },
                    enabled = !isIsolated,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+1 .locked", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// METRICS & BASELINE DELTAS CARD
// -----------------------------------------------------------------------------

@Composable
fun MetricsAndDeltasCard(
    metrics: SlidingWindowMetrics,
    deltas: BaselineDeltas,
    stage1Triggered: Boolean,
    stage2Triggered: Boolean,
    consecutiveBursts: Int
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Feature Evidence & Baseline Deltas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${metrics.windowDurationMs / 1000}s Window",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Stage Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StageIndicatorChip(
                    title = "STAGE 1: Burst Anomaly",
                    isTriggered = stage1Triggered,
                    statusText = if (stage1Triggered) "BURST DETECTED" else "Normal Velocity",
                    modifier = Modifier.weight(1f)
                )
                StageIndicatorChip(
                    title = "STAGE 2: Persistence",
                    isTriggered = stage2Triggered,
                    statusText = if (stage2Triggered) "THREAT VERIFIED" else "Streak: $consecutiveBursts/2",
                    modifier = Modifier.weight(1f)
                )
            }

            // Metric Gauges (Progress Bars vs Baseline)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Write Rate Gauge
                MetricGaugeRow(
                    label = "Write Velocity",
                    currentValue = "%.2f ops/sec".format(metrics.writeRate),
                    baselineText = "Baseline: 1.60 ops/sec",
                    progress = (metrics.writeRate / 5.0).toFloat().coerceIn(0f, 1f),
                    isExceeded = deltas.isWriteRateExceeded,
                    deltaText = if (deltas.isWriteRateExceeded) "+%.2f ops/s".format(deltas.writeRateDelta) else "Normal"
                )

                // Renames Gauge
                MetricGaugeRow(
                    label = "Window Renames",
                    currentValue = "${metrics.fileRenames}",
                    baselineText = "Baseline Threshold: 3",
                    progress = (metrics.fileRenames / 6.0f).coerceIn(0f, 1f),
                    isExceeded = deltas.isRenamesExceeded,
                    deltaText = if (deltas.isRenamesExceeded) "+${deltas.renamesDelta}" else "Normal"
                )

                // Unique Target Files Gauge
                MetricGaugeRow(
                    label = "Unique Files Modified",
                    currentValue = "${metrics.uniqueFiles}",
                    baselineText = "Threshold: 5 files",
                    progress = (metrics.uniqueFiles / 10.0f).coerceIn(0f, 1f),
                    isExceeded = deltas.isUniqueFilesExceeded,
                    deltaText = if (deltas.isUniqueFilesExceeded) "+${deltas.uniqueFilesDelta}" else "Normal"
                )

                // Suspicious Extensions Indicator
                MetricGaugeRow(
                    label = "Suspicious Extensions",
                    currentValue = "${metrics.suspiciousRenamesCount}",
                    baselineText = "Baseline: 0 (.locked/.crypto)",
                    progress = (metrics.suspiciousRenamesCount / 4.0f).coerceIn(0f, 1f),
                    isExceeded = deltas.isSuspiciousExtExceeded,
                    deltaText = if (deltas.isSuspiciousExtExceeded) "+${deltas.suspiciousExtDelta} ENCRYPTED" else "0"
                )
            }

            // Summary Stats Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBadge(label = "Total Ops", value = "${metrics.totalOperations}")
                StatBadge(label = "Creates", value = "${metrics.fileCreates}")
                StatBadge(label = "Writes", value = "${metrics.fileWrites}")
                StatBadge(label = "Deletes", value = "${metrics.fileDeletes}")
                StatBadge(label = "Bytes", value = "${metrics.totalBytesWritten} B")
            }
        }
    }
}

@Composable
fun MetricGaugeRow(
    label: String,
    currentValue: String,
    baselineText: String,
    progress: Float,
    isExceeded: Boolean,
    deltaText: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentValue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = if (isExceeded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = deltaText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Text(text = baselineText, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun StageIndicatorChip(
    title: String,
    isTriggered: Boolean,
    statusText: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isTriggered) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isTriggered) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(text = statusText, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// -----------------------------------------------------------------------------
// EXPLAINABLE REASONS CARD
// -----------------------------------------------------------------------------

@Composable
fun ExplainableReasonsCard(
    reasons: List<String>,
    alertLevel: AlertLevel,
    onShowWarningDialog: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Explainable Analyzer Output",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (alertLevel != AlertLevel.NORMAL) {
                    TextButton(onClick = onShowWarningDialog) {
                        Text("Details", fontSize = 11.sp)
                    }
                }
            }

            if (reasons.isEmpty()) {
                Text("No reasoning available", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            } else {
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = reason,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// QUICK EVENTS SNAPSHOT CARD
// -----------------------------------------------------------------------------

@Composable
fun QuickEventsCard(events: List<FileOpEvent>, totalEventsCount: Int) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Operations Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Total: $totalEventsCount", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }

            if (events.isEmpty()) {
                Text("No operations recorded.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            } else {
                events.forEach { event ->
                    EventCompactRow(event = event)
                }
            }
        }
    }
}

@Composable
fun EventCompactRow(event: FileOpEvent) {
    val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(event.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            OpBadge(type = event.operationType)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = event.filePath,
                    fontSize = 11.sp,
                    fontWeight = if (event.isSuspiciousExtension) FontWeight.Bold else FontWeight.Normal,
                    color = if (event.isSuspiciousExtension) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$timeStr | ${event.bytesAffected} bytes",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (event.isSuspiciousExtension) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = ".${event.extensionAfter}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun OpBadge(type: FileOpType) {
    val (color, label) = when (type) {
        FileOpType.CREATE -> Color(0xFF2E7D32) to "CREATE"
        FileOpType.WRITE -> Color(0xFF1565C0) to "WRITE"
        FileOpType.RENAME -> Color(0xFF6A1B9A) to "RENAME"
        FileOpType.DELETE -> Color(0xFFC62828) to "DELETE"
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// -----------------------------------------------------------------------------
// TAB 1: REAL-TIME ACTIVITY TIMELINE
// -----------------------------------------------------------------------------

@Composable
fun TimelineTab(
    events: List<FileOpEvent>,
    totalEventsCount: Int
) {
    var selectedFilter by remember { mutableStateOf<FileOpType?>(null) }
    var showOnlySuspicious by remember { mutableStateOf(false) }

    val filteredEvents = remember(events, selectedFilter, showOnlySuspicious) {
        events.filter { event ->
            (selectedFilter == null || event.operationType == selectedFilter) &&
                (!showOnlySuspicious || event.isSuspiciousExtension)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Real-Time Activity Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Total Events: $totalEventsCount", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null && !showOnlySuspicious,
                onClick = {
                    selectedFilter = null
                    showOnlySuspicious = false
                },
                label = { Text("ALL", fontSize = 10.sp) }
            )
            FilterChip(
                selected = selectedFilter == FileOpType.WRITE,
                onClick = { selectedFilter = if (selectedFilter == FileOpType.WRITE) null else FileOpType.WRITE },
                label = { Text("WRITE", fontSize = 10.sp) }
            )
            FilterChip(
                selected = selectedFilter == FileOpType.RENAME,
                onClick = { selectedFilter = if (selectedFilter == FileOpType.RENAME) null else FileOpType.RENAME },
                label = { Text("RENAME", fontSize = 10.sp) }
            )
            FilterChip(
                selected = showOnlySuspicious,
                onClick = { showOnlySuspicious = !showOnlySuspicious },
                label = { Text("SUSPICIOUS", fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
            )
        }

        if (filteredEvents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching file events recorded.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredEvents, key = { it.id }) { event ->
                    TimelineItemCard(event = event)
                }
            }
        }
    }
}

@Composable
fun TimelineItemCard(event: FileOpEvent) {
    val formattedTime = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(event.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isSuspiciousExtension)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (event.isSuspiciousExtension) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                OpBadge(type = event.operationType)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = event.filePath,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (event.isSuspiciousExtension) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bytes Affected: ${event.bytesAffected} B | Extension: .${event.extensionAfter}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (event.isSuspiciousExtension) {
                        Text(
                            text = "ENCRYPTED ANOMALY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 2: RISK ANALYTICS & ALERT HISTORY
// -----------------------------------------------------------------------------

@Composable
fun RiskAndHistoryTab(
    analysisResult: TwoStageAnalysisResult,
    baselineDeltas: BaselineDeltas,
    alertHistory: List<AlertHistoryEntry>,
    onShowWarningDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Two-Stage Threat Model & Alert History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Current Threat Status Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Risk Evaluation",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (analysisResult.alertLevel != AlertLevel.NORMAL) {
                            Button(
                                onClick = onShowWarningDialog,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Explain Risk", fontSize = 11.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ThreatStageCard(
                            stageName = "Stage 1: Velocity Burst",
                            isTriggered = analysisResult.stage1BurstTriggered,
                            description = "Triggers when write rate >= 1.6 ops/sec or renames >= 3 or suspicious ext detected.",
                            modifier = Modifier.weight(1f)
                        )

                        ThreatStageCard(
                            stageName = "Stage 2: Persistence",
                            isTriggered = analysisResult.stage2PersistenceTriggered,
                            description = "Triggers when Stage 1 persists for 2 consecutive windows or combines with .locked extensions.",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Alert History Progression
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Consecutive Alert History Log",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("${alertHistory.size} recorded", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    if (alertHistory.isEmpty()) {
                        Text(
                            text = "No elevated or high-risk alert triggers recorded in history.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        alertHistory.forEach { entry ->
                            AlertHistoryItemRow(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreatStageCard(
    stageName: String,
    isTriggered: Boolean,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isTriggered) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isTriggered) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isTriggered) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stageName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTriggered) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 10.sp,
                color = if (isTriggered) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun AlertHistoryItemRow(entry: AlertHistoryEntry) {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.summary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.alertLevel == AlertLevel.HIGH_RISK_WARNING)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.tertiary
                )
            }
            Text(
                text = "$timeStr | Rate: %.2f ops/sec | Encrypted Files: %d".format(entry.writeRate, entry.suspiciousExtCount),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 3: SANDBOX FILE INSPECTOR
// -----------------------------------------------------------------------------

@Composable
fun SandboxTab(
    sandboxFiles: List<SandboxFileInfo>,
    backupMessage: String?,
    onClearSandbox: () -> Unit,
    onBackupState: () -> Unit,
    onManualOp: (FileOpType, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sandbox File Inspector",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("context.filesDir/sandbox", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
        }

        // Backup Snapshot Status Banner
        if (backupMessage != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = backupMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Action Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBackupState,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Backup Snapshot", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onClearSandbox,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear Sandbox", fontSize = 11.sp)
                }
            }
        }

        if (sandboxFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sandbox directory is currently empty.", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            items(sandboxFiles) { file ->
                SandboxFileCard(file = file)
            }
        }
    }
}

@Composable
fun SandboxFileCard(file: SandboxFileInfo) {
    val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (file.isEncryptedExtension)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (file.isEncryptedExtension) Icons.Default.Lock else Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (file.isEncryptedExtension) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = file.fileName,
                        fontSize = 12.sp,
                        fontWeight = if (file.isEncryptedExtension) FontWeight.Bold else FontWeight.Normal,
                        color = if (file.isEncryptedExtension) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Path: ${file.relativePath} | Modified: $dateStr",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Text(
                text = "${file.sizeBytes} B",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -----------------------------------------------------------------------------
// EXPLAINABLE WARNING DIALOG (MODAL)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplainableWarningDialog(
    analysisResult: TwoStageAnalysisResult,
    baselineDeltas: BaselineDeltas,
    isProcessIsolated: Boolean,
    onDismiss: () -> Unit,
    onIsolateProcess: () -> Unit,
    onBackupState: () -> Unit,
    onInspectSandbox: () -> Unit
) {
    val isStage2 = analysisResult.stage2PersistenceTriggered || analysisResult.alertLevel == AlertLevel.HIGH_RISK_WARNING
    val titleText = if (isStage2) "STAGE 2 HIGH RISK WARNING: Persistent Ransomware Threat Verified" else "STAGE 1 ELEVATED ACTIVITY: Burst Anomaly Detected"
    val headerColor = if (isStage2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isStage2) Icons.Default.BugReport else Icons.Default.Warning,
                    contentDescription = null,
                    tint = headerColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isStage2) "STAGE 2 PERSISTENCE WARNING" else "STAGE 1 BURST ANOMALY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = headerColor
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = titleText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Behavioral Evidence List
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Specific Behavioral Evidence:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        analysisResult.explainableReasons.forEach { reason ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", fontWeight = FontWeight.Bold, color = headerColor)
                                Text(
                                    text = reason,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Baseline Deltas Summary
                Text(
                    text = "Feature Evidence vs Normal Baseline:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DeltaItem("Write Velocity", "%.2f ops/s".format(analysisResult.metrics.writeRate), baselineDeltas.isWriteRateExceeded)
                    DeltaItem("Renames", "${analysisResult.metrics.fileRenames}", baselineDeltas.isRenamesExceeded)
                    DeltaItem("Unique Files", "${analysisResult.metrics.uniqueFiles}", baselineDeltas.isUniqueFilesExceeded)
                    DeltaItem("Enc Exts", "${analysisResult.metrics.suspiciousRenamesCount}", baselineDeltas.isSuspiciousExtExceeded)
                }

                Spacer(Modifier.height(4.dp))

                // Recommended Safe Responses Header
                Text(
                    text = "Recommended Safe Responses:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Action 1: Isolate Process
                    Button(
                        onClick = onIsolateProcess,
                        enabled = !isProcessIsolated,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isProcessIsolated) "Process Already Isolated" else "Isolate Process (Halt Workload)", fontSize = 12.sp)
                    }

                    // Action 2: Backup State Snapshot
                    Button(
                        onClick = onBackupState,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Backup State Snapshot", fontSize = 12.sp)
                    }

                    // Action 3: Inspect Sandbox
                    OutlinedButton(
                        onClick = onInspectSandbox,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Inspect Sandbox Files", fontSize = 12.sp)
                    }

                    // Dismiss
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Dismiss Modal", fontSize = 12.sp)
                    }
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun DeltaItem(label: String, value: String, isExceeded: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

// -----------------------------------------------------------------------------
// PREVIEWS
// -----------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun SentinelPulseScreenDashboardPreview() {
    ZeroTraceTheme {
        SentinelPulseContent(
            uiState = SentinelPulseUiState(
                workloadMode = WorkloadMode.RANSOMWARE_SIMULATION_WORKLOAD,
                isSimulating = true,
                analysisResult = TwoStageAnalysisResult(
                    stage1BurstTriggered = true,
                    stage2PersistenceTriggered = true,
                    alertLevel = AlertLevel.HIGH_RISK_WARNING,
                    explainableReasons = listOf(
                        "Stage 1 High Velocity Burst: Operation rate of 4.20 ops/sec (21 operations in 5.0s window).",
                        "Stage 2 Persistence Verified: High-activity burst persisted for 2 consecutive time windows.",
                        "Stage 2 High Severity Threat Pattern: Operation burst combined with 3 suspicious extension renames (.locked)."
                    ),
                    metrics = SlidingWindowMetrics(
                        totalOperations = 21,
                        fileCreates = 5,
                        fileWrites = 10,
                        fileRenames = 6,
                        uniqueFiles = 8,
                        writeRate = 4.2,
                        suspiciousRenamesCount = 3
                    ),
                    consecutiveBurstCount = 2
                ),
                baselineDeltas = BaselineDeltas(
                    writeRateDelta = 2.6,
                    renamesDelta = 3,
                    uniqueFilesDelta = 3,
                    suspiciousExtDelta = 3,
                    isWriteRateExceeded = true,
                    isRenamesExceeded = true,
                    isUniqueFilesExceeded = true,
                    isSuspiciousExtExceeded = true
                ),
                totalEventsCount = 45,
                sandboxFiles = listOf(
                    SandboxFileInfo("doc_1.txt.locked", "sandbox/doc_1.txt.locked", 1024, System.currentTimeMillis(), true),
                    SandboxFileInfo("photo_2.jpg.crypto", "sandbox/photo_2.jpg.crypto", 2048, System.currentTimeMillis(), true)
                )
            ),
            onSelectTab = {},
            onStartSimulation = {},
            onStopSimulation = {},
            onClearSandbox = {},
            onResetAnalyzer = {},
            onManualOp = { _, _ -> },
            onShowWarningDialog = {},
            onDismissWarningDialog = {},
            onIsolateProcess = {},
            onBackupState = {},
            onInspectSandbox = {},
            onUnIsolateProcess = {}
        )
    }
}
