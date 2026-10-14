package com.example.zerotrace

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.zerotrace.data.model.FileOpType
import com.example.zerotrace.simulator.SandboxSimulator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SandboxSimulatorInstrumentedTest {

    private lateinit var simulator: SandboxSimulator
    private lateinit var sandboxDir: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        simulator = SandboxSimulator(context)
        sandboxDir = File(context.filesDir, "sandbox")
        runBlocking {
            simulator.clearSandbox()
        }
    }

    @Test
    fun testSandboxOperationsAreConfinedToPrivateSandboxDir() = runBlocking {
        val event = simulator.performSingleOperation(FileOpType.CREATE, "test_file.txt", false)

        val createdFile = File(sandboxDir, "test_file.txt")
        assertTrue("Created file must exist strictly inside private sandbox", createdFile.exists())
        assertTrue("File canonical path must start with sandbox canonical path", createdFile.canonicalPath.startsWith(sandboxDir.canonicalPath))
        assertEquals("test_file.txt", event.filePath)
    }

    @Test
    fun testClearSandboxDeletesAllSandboxContents() = runBlocking {
        simulator.performSingleOperation(FileOpType.CREATE, "file1.txt", false)
        simulator.performSingleOperation(FileOpType.CREATE, "file2.txt", false)

        val filesBefore = simulator.getSandboxFiles()
        assertTrue(filesBefore.size >= 2)

        simulator.clearSandbox()

        val filesAfter = simulator.getSandboxFiles()
        assertEquals(0, filesAfter.size)
    }
}
