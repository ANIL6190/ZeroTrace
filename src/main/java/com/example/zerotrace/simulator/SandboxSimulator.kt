package com.example.zerotrace.simulator

import android.content.Context
import com.example.zerotrace.data.model.FileOpEvent
import com.example.zerotrace.data.model.FileOpType
import com.example.zerotrace.data.model.SandboxFileInfo
import com.example.zerotrace.data.model.WorkloadMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Random

class SandboxSimulator(private val context: Context) {

    private val sandboxDir: File = File(context.filesDir, "sandbox")

    private val _eventFlow = MutableSharedFlow<FileOpEvent>(replay = 200, extraBufferCapacity = 1000)
    val eventFlow: SharedFlow<FileOpEvent> = _eventFlow.asSharedFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _currentMode = MutableStateFlow(WorkloadMode.IDLE)
    val currentMode: StateFlow<WorkloadMode> = _currentMode.asStateFlow()

    private var simulationJob: Job? = null
    private val random = Random()

    companion object {
        val SUSPICIOUS_EXTENSIONS = setOf("locked", "crypto", "enc", "crypted", "ransom", "enc_sim", "locked_sim")
        val NORMAL_EXTENSIONS = listOf("txt", "json", "dat", "doc", "pdf", "csv")
    }

    init {
        initSandboxDirectory()
    }

    private fun initSandboxDirectory() {
        if (!sandboxDir.exists()) {
            sandboxDir.mkdirs()
        }
        // Strict boundary validation: ensure sandbox is strictly inside context.filesDir
        checkBoundary(sandboxDir)
    }

    private fun checkBoundary(file: File) {
        val canonicalSandbox = sandboxDir.canonicalPath
        val canonicalFile = file.canonicalPath
        if (!canonicalFile.startsWith(canonicalSandbox)) {
            throw SecurityException("Security violation: Target path '$canonicalFile' is outside private sandbox '$canonicalSandbox'")
        }
    }

    suspend fun clearSandbox(): Unit = withContext(Dispatchers.IO) {
        initSandboxDirectory()
        sandboxDir.listFiles()?.forEach { file ->
            checkBoundary(file)
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        }
    }

    suspend fun getSandboxFiles(): List<SandboxFileInfo> = withContext(Dispatchers.IO) {
        initSandboxDirectory()
        val files = sandboxDir.listFiles() ?: emptyArray()
        files.map { file ->
            val ext = file.extension.lowercase()
            SandboxFileInfo(
                fileName = file.name,
                relativePath = "sandbox/${file.name}",
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                isEncryptedExtension = SUSPICIOUS_EXTENSIONS.contains(ext)
            )
        }.sortedByDescending { it.lastModified }
    }

    fun startSimulation(mode: WorkloadMode, scope: CoroutineScope) {
        if (mode == WorkloadMode.IDLE) {
            stopSimulation()
            return
        }

        stopSimulation()
        _currentMode.value = mode
        _isSimulating.value = true

        simulationJob = scope.launch(Dispatchers.IO) {
            initSandboxDirectory()
            // Populate initial seed benign files if sandbox is empty
            ensureSeedFiles()

            while (isActive && _isSimulating.value) {
                when (_currentMode.value) {
                    WorkloadMode.NORMAL_USER_WORKLOAD -> {
                        executeNormalWorkloadStep()
                        delay(1200L + random.nextInt(800)) // 1.2s - 2.0s pacing
                    }
                    WorkloadMode.RANSOMWARE_SIMULATION_WORKLOAD -> {
                        executeRansomwareWorkloadStep()
                        delay(150L + random.nextInt(100)) // 150ms - 250ms rapid pacing
                    }
                    WorkloadMode.BURST_STORM_WORKLOAD -> {
                        executeRansomwareWorkloadStep()
                        delay(50L + random.nextInt(50)) // 50ms - 100ms intense burst
                    }
                    WorkloadMode.IDLE -> break
                }
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        _isSimulating.value = false
        _currentMode.value = WorkloadMode.IDLE
    }

    suspend fun performSingleOperation(
        opType: FileOpType,
        customName: String? = null,
        isSuspicious: Boolean = false
    ): FileOpEvent = withContext(Dispatchers.IO) {
        initSandboxDirectory()
        val event = when (opType) {
            FileOpType.CREATE -> createRandomFile(customName, isSuspicious)
            FileOpType.WRITE -> writeRandomFile(customName)
            FileOpType.RENAME -> renameRandomFile(isSuspicious)
            FileOpType.DELETE -> deleteRandomFile()
        }
        _eventFlow.emit(event)
        event
    }

    private suspend fun ensureSeedFiles() {
        val files = sandboxDir.listFiles()
        if (files == null || files.size < 5) {
            for (i in 1..8) {
                val ext = NORMAL_EXTENSIONS[i % NORMAL_EXTENSIONS.size]
                val file = File(sandboxDir, "user_doc_$i.$ext")
                checkBoundary(file)
                if (!file.exists()) {
                    file.writeText("Synthetic benign user content for document #$i\nDate: ${System.currentTimeMillis()}")
                    _eventFlow.emit(
                        FileOpEvent(
                            filePath = file.name,
                            operationType = FileOpType.CREATE,
                            bytesAffected = file.length(),
                            extensionAfter = ext,
                            isSuspiciousExtension = false
                        )
                    )
                }
            }
        }
    }

    private suspend fun executeNormalWorkloadStep() {
        val opChoice = random.nextInt(10)
        val event = when {
            opChoice < 4 -> writeRandomFile(null)
            opChoice < 8 -> createRandomFile(null, false)
            else -> renameRandomFile(false)
        }
        _eventFlow.emit(event)
    }

    private suspend fun executeRansomwareWorkloadStep() {
        // Ransomware pattern:
        // 1. Target existing benign file or create new
        // 2. Overwrite with "encrypted" payload (WRITE)
        // 3. Rename with suspicious extension (.locked / .crypto)
        val opChoice = random.nextInt(10)
        val event = when {
            opChoice < 3 -> createRandomFile(null, false)
            opChoice < 6 -> writeRandomFile(null)
            else -> renameRandomFile(true)
        }
        _eventFlow.emit(event)
    }

    private fun createRandomFile(customName: String?, isSuspicious: Boolean): FileOpEvent {
        val ext = if (isSuspicious) SUSPICIOUS_EXTENSIONS.random() else NORMAL_EXTENSIONS.random()
        val name = customName ?: "file_${System.currentTimeMillis()}_${random.nextInt(1000)}.$ext"
        val targetFile = File(sandboxDir, name)
        checkBoundary(targetFile)

        val content = "Synthetic data payload: ${"X".repeat(random.nextInt(512) + 128)}"
        targetFile.writeText(content)

        val extensionAfter = targetFile.extension.lowercase()
        return FileOpEvent(
            filePath = targetFile.name,
            operationType = FileOpType.CREATE,
            bytesAffected = targetFile.length(),
            extensionAfter = extensionAfter,
            isSuspiciousExtension = SUSPICIOUS_EXTENSIONS.contains(extensionAfter)
        )
    }

    private fun writeRandomFile(customName: String?): FileOpEvent {
        val targetFile = if (customName != null) {
            File(sandboxDir, customName)
        } else {
            val existing = sandboxDir.listFiles()?.filter { it.isFile }
            if (!existing.isNullOrEmpty()) {
                existing.random()
            } else {
                File(sandboxDir, "normal_file_${System.currentTimeMillis()}.txt")
            }
        }
        checkBoundary(targetFile)

        val appendText = "\n[Modified at ${System.currentTimeMillis()}] Data buffer: ${"A".repeat(random.nextInt(256) + 64)}"
        targetFile.appendText(appendText)

        val extensionAfter = targetFile.extension.lowercase()
        return FileOpEvent(
            filePath = targetFile.name,
            operationType = FileOpType.WRITE,
            bytesAffected = appendText.toByteArray().size.toLong(),
            extensionAfter = extensionAfter,
            isSuspiciousExtension = SUSPICIOUS_EXTENSIONS.contains(extensionAfter)
        )
    }

    private fun renameRandomFile(isSuspiciousExt: Boolean): FileOpEvent {
        val existingFiles = sandboxDir.listFiles()?.filter { it.isFile } ?: emptyList()
        val sourceFile = if (existingFiles.isNotEmpty()) {
            existingFiles.random()
        } else {
            val f = File(sandboxDir, "temp_rename_${System.currentTimeMillis()}.txt")
            checkBoundary(f)
            f.writeText("rename target")
            f
        }
        checkBoundary(sourceFile)

        val targetExt = if (isSuspiciousExt) {
            SUSPICIOUS_EXTENSIONS.random()
        } else {
            NORMAL_EXTENSIONS.random()
        }

        val baseName = sourceFile.nameWithoutExtension
        val newName = "$baseName.$targetExt"
        val destFile = File(sandboxDir, newName)
        checkBoundary(destFile)

        sourceFile.renameTo(destFile)

        val extensionAfter = destFile.extension.lowercase()
        return FileOpEvent(
            filePath = "${sourceFile.name} -> ${destFile.name}",
            operationType = FileOpType.RENAME,
            bytesAffected = destFile.length(),
            extensionAfter = extensionAfter,
            isSuspiciousExtension = SUSPICIOUS_EXTENSIONS.contains(extensionAfter)
        )
    }

    private fun deleteRandomFile(): FileOpEvent {
        val existingFiles = sandboxDir.listFiles()?.filter { it.isFile } ?: emptyList()
        if (existingFiles.isNotEmpty()) {
            val target = existingFiles.random()
            checkBoundary(target)
            val name = target.name
            val size = target.length()
            val ext = target.extension.lowercase()
            target.delete()
            return FileOpEvent(
                filePath = name,
                operationType = FileOpType.DELETE,
                bytesAffected = size,
                extensionAfter = ext,
                isSuspiciousExtension = SUSPICIOUS_EXTENSIONS.contains(ext)
            )
        } else {
            return FileOpEvent(
                filePath = "none.txt",
                operationType = FileOpType.DELETE,
                bytesAffected = 0L,
                extensionAfter = "txt",
                isSuspiciousExtension = false
            )
        }
    }
}
