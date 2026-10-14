package com.example.zerotrace.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zerotrace.analyzer.TwoStageBehaviorAnalyzer
import com.example.zerotrace.data.model.AlertHistoryEntry
import com.example.zerotrace.data.model.AlertLevel
import com.example.zerotrace.data.model.BaselineDeltas
import com.example.zerotrace.data.model.FileOpEvent
import com.example.zerotrace.data.model.FileOpType
import com.example.zerotrace.data.model.SandboxFileInfo
import com.example.zerotrace.data.model.TwoStageAnalysisResult
import com.example.zerotrace.data.model.WorkloadMode
import com.example.zerotrace.simulator.SandboxSimulator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SentinelPulseUiState(
    val workloadMode: WorkloadMode = WorkloadMode.IDLE,
    val isSimulating: Boolean = false,
    val analysisResult: TwoStageAnalysisResult = TwoStageAnalysisResult(),
    val baselineDeltas: BaselineDeltas = BaselineDeltas(),
    val recentEvents: List<FileOpEvent> = emptyList(),
    val sandboxFiles: List<SandboxFileInfo> = emptyList(),
    val totalEventsCount: Int = 0,
    val statusMessage: String = "Engine Ready",
    val selectedTab: Int = 0, // 0: Overview & Metrics, 1: Activity Timeline, 2: Risk & Explainability, 3: Sandbox Files
    val showWarningDialog: Boolean = false,
    val alertHistory: List<AlertHistoryEntry> = emptyList(),
    val backupMessage: String? = null,
    val isProcessIsolated: Boolean = false
)

class SentinelPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val simulator = SandboxSimulator(application)
    private val analyzer = TwoStageBehaviorAnalyzer()

    private val _uiState = MutableStateFlow(SentinelPulseUiState())
    val uiState: StateFlow<SentinelPulseUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private val eventHistoryBuffer = mutableListOf<FileOpEvent>()
    private val alertHistoryBuffer = mutableListOf<AlertHistoryEntry>()
    private var lastRecordedAlertLevel: AlertLevel = AlertLevel.NORMAL
    private var lastRecordedBurstCount: Int = 0

    init {
        // Collect simulator events
        viewModelScope.launch {
            simulator.eventFlow.collect { event ->
                handleNewEvent(event)
            }
        }

        // Collect simulator state
        viewModelScope.launch {
            simulator.isSimulating.collect { isSimulating ->
                _uiState.update { it.copy(isSimulating = isSimulating) }
            }
        }

        viewModelScope.launch {
            simulator.currentMode.collect { mode ->
                _uiState.update { it.copy(workloadMode = mode) }
            }
        }

        // Initial files refresh
        refreshSandboxFiles()
        // Start periodic sliding window evaluation ticker
        startTicker()
    }

    private fun handleNewEvent(event: FileOpEvent) {
        val newAnalysis = analyzer.addEvent(event)
        val deltas = BaselineDeltas.calculate(newAnalysis.metrics)
        eventHistoryBuffer.add(0, event) // newest first

        // Keep last 100 events in memory buffer
        if (eventHistoryBuffer.size > 100) {
            eventHistoryBuffer.removeAt(eventHistoryBuffer.lastIndex)
        }

        checkAndRecordAlertHistory(newAnalysis)

        _uiState.update { current ->
            current.copy(
                analysisResult = newAnalysis,
                baselineDeltas = deltas,
                recentEvents = eventHistoryBuffer.toList(),
                totalEventsCount = current.totalEventsCount + 1,
                statusMessage = "Event processed: ${event.operationType} on ${event.filePath}"
            )
        }

        refreshSandboxFiles()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(500L) // Refresh window metrics every 500ms
                val currentAnalysis = analyzer.analyze()
                val deltas = BaselineDeltas.calculate(currentAnalysis.metrics)
                checkAndRecordAlertHistory(currentAnalysis)

                _uiState.update { current ->
                    current.copy(
                        analysisResult = currentAnalysis,
                        baselineDeltas = deltas,
                        alertHistory = alertHistoryBuffer.toList()
                    )
                }
            }
        }
    }

    private fun checkAndRecordAlertHistory(analysis: TwoStageAnalysisResult) {
        val currentLevel = analysis.alertLevel
        val burstCount = analysis.consecutiveBurstCount

        if (currentLevel != AlertLevel.NORMAL && (currentLevel != lastRecordedAlertLevel || burstCount != lastRecordedBurstCount)) {
            val summary = when (currentLevel) {
                AlertLevel.HIGH_RISK_WARNING -> "STAGE 2 persistence / threat pattern verified (Bursts: $burstCount)"
                AlertLevel.ELEVATED_ACTIVITY -> "STAGE 1 burst anomaly detected (Rate: %.2f ops/sec)".format(analysis.metrics.writeRate)
                else -> "Normal activity resumed"
            }

            val entry = AlertHistoryEntry(
                alertLevel = currentLevel,
                consecutiveBursts = burstCount,
                summary = summary,
                writeRate = analysis.metrics.writeRate,
                suspiciousExtCount = analysis.metrics.suspiciousRenamesCount
            )

            alertHistoryBuffer.add(0, entry) // newest first
            if (alertHistoryBuffer.size > 50) {
                alertHistoryBuffer.removeAt(alertHistoryBuffer.lastIndex)
            }

            lastRecordedAlertLevel = currentLevel
            lastRecordedBurstCount = burstCount
        } else if (currentLevel == AlertLevel.NORMAL) {
            lastRecordedAlertLevel = AlertLevel.NORMAL
            lastRecordedBurstCount = 0
        }
    }

    fun startSimulation(mode: WorkloadMode) {
        if (mode == WorkloadMode.IDLE) {
            stopSimulation()
            return
        }
        _uiState.update {
            it.copy(
                isProcessIsolated = false,
                statusMessage = "Starting simulation mode: $mode"
            )
        }
        simulator.startSimulation(mode, viewModelScope)
    }

    fun stopSimulation() {
        simulator.stopSimulation()
        _uiState.update {
            it.copy(
                workloadMode = WorkloadMode.IDLE,
                isSimulating = false,
                statusMessage = "Simulation stopped."
            )
        }
    }

    fun clearSandbox() {
        viewModelScope.launch {
            stopSimulation()
            simulator.clearSandbox()
            analyzer.reset()
            eventHistoryBuffer.clear()
            alertHistoryBuffer.clear()
            lastRecordedAlertLevel = AlertLevel.NORMAL
            lastRecordedBurstCount = 0
            val cleanAnalysis = analyzer.analyze()
            val deltas = BaselineDeltas.calculate(cleanAnalysis.metrics)
            _uiState.update {
                it.copy(
                    analysisResult = cleanAnalysis,
                    baselineDeltas = deltas,
                    recentEvents = emptyList(),
                    alertHistory = emptyList(),
                    totalEventsCount = 0,
                    isProcessIsolated = false,
                    backupMessage = null,
                    statusMessage = "Sandbox cleared and analyzer reset."
                )
            }
            refreshSandboxFiles()
        }
    }

    fun resetAnalyzer() {
        analyzer.reset()
        alertHistoryBuffer.clear()
        lastRecordedAlertLevel = AlertLevel.NORMAL
        lastRecordedBurstCount = 0
        val cleanAnalysis = analyzer.analyze()
        val deltas = BaselineDeltas.calculate(cleanAnalysis.metrics)
        _uiState.update {
            it.copy(
                analysisResult = cleanAnalysis,
                baselineDeltas = deltas,
                alertHistory = emptyList(),
                statusMessage = "Analyzer reset."
            )
        }
    }

    fun performSingleOperation(opType: FileOpType, customName: String? = null, isSuspicious: Boolean = false) {
        viewModelScope.launch {
            simulator.performSingleOperation(opType, customName, isSuspicious)
        }
    }

    fun refreshSandboxFiles() {
        viewModelScope.launch {
            val files = simulator.getSandboxFiles()
            _uiState.update { it.copy(sandboxFiles = files) }
        }
    }

    // Navigation and Modal control
    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun showWarningDialog() {
        _uiState.update { it.copy(showWarningDialog = true) }
    }

    fun dismissWarningDialog() {
        _uiState.update { it.copy(showWarningDialog = false) }
    }

    // Recommended Safe Responses
    fun isolateProcess() {
        stopSimulation()
        _uiState.update {
            it.copy(
                isProcessIsolated = true,
                showWarningDialog = false,
                statusMessage = "SAFE RESPONSE EXECUTED: Process Isolated. All suspicious workload threads halted immediately."
            )
        }
    }

    fun backupState() {
        val filesCount = _uiState.value.sandboxFiles.size
        val timestamp = System.currentTimeMillis()
        val msg = "Snapshot created at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(timestamp)} — $filesCount sandbox files safely preserved."
        _uiState.update {
            it.copy(
                backupMessage = msg,
                statusMessage = "SAFE RESPONSE EXECUTED: $msg"
            )
        }
    }

    fun inspectSandbox() {
        _uiState.update {
            it.copy(
                selectedTab = 3, // Sandbox tab
                showWarningDialog = false,
                statusMessage = "Navigated to Sandbox Inspection view."
            )
        }
    }

    fun unIsolateProcess() {
        _uiState.update {
            it.copy(
                isProcessIsolated = false,
                statusMessage = "Process isolation cleared."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        simulator.stopSimulation()
    }
}
