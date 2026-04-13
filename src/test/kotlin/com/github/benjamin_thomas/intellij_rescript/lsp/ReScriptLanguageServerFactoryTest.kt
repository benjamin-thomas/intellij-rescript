package com.github.benjamin_thomas.intellij_rescript.lsp

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReScriptLanguageServerFactoryTest {
    @Test
    fun `it resolves a valid configured path`() {
        val configuredDir = Files.createTempDirectory("rescript-configured")
        val configuredServer = configuredDir.resolve("rescript-language-server").apply {
            writeText("#!/bin/sh\n")
            toFile().setExecutable(true)
        }

        val serverPath = executablePath(configuredServer.toString())

        assertEquals(configuredServer.toAbsolutePath().toString(), serverPath)
    }

    @Test
    fun `it returns null when configured path is blank`() {
        val serverPath = executablePath("   ")

        assertNull(serverPath)
        assertNull(normalizedPath("   "))
    }

    @Test
    fun `it returns null when configured path is invalid`() {
        val serverPath = executablePath("/missing/rescript-language-server")

        assertNull(serverPath)
    }

    @Test
    fun `it auto detects a language server from PATH`() {
        val pathDir = Files.createTempDirectory("rescript-path")
        val pathServer = pathDir.resolve("rescript-language-server").apply {
            writeText("#!/bin/sh\n")
            toFile().setExecutable(true)
        }

        val detectedPath = autoDetectLanguageServerPath(
            pathEnv = pathDir.toString(),
            extraSearchDirs = emptySequence(),
        )

        assertEquals(pathServer.toAbsolutePath().toString(), detectedPath)
    }

    @Test
    fun `it auto detects deterministically from extra search dirs`() {
        val firstDir = Files.createTempDirectory("rescript-first")
        val secondDir = Files.createTempDirectory("rescript-second")
        val firstServer = firstDir.resolve("rescript-language-server").apply {
            writeText("#!/bin/sh\n")
            toFile().setExecutable(true)
        }
        secondDir.resolve("rescript-language-server").apply {
            writeText("#!/bin/sh\n")
            toFile().setExecutable(true)
        }

        val detectedPath = autoDetectLanguageServerPath(
            pathEnv = "",
            extraSearchDirs = sequenceOf(firstDir, secondDir),
        )

        assertEquals(firstServer.toAbsolutePath().toString(), detectedPath)
    }

    @Test
    fun `it auto detects node from PATH`() {
        val pathDir = Files.createTempDirectory("node-path")
        val nodeBin = pathDir.resolve("node").apply {
            writeText("#!/bin/sh\n")
            toFile().setExecutable(true)
        }

        val detectedPath = autoDetectNodePath(
            pathEnv = pathDir.toString(),
            extraSearchDirs = emptySequence(),
        )

        assertEquals(nodeBin.toAbsolutePath().toString(), detectedPath)
    }

    @Test
    fun `it auto detects node deterministically from extra search dirs`() {
        val firstDir = Files.createTempDirectory("node-first")
        val secondDir = Files.createTempDirectory("node-second")
        val firstNode = firstDir.resolve("node").apply {
            writeText("#!/bin/sh\n")
            toFile().setExecutable(true)
        }
        secondDir.resolve("node").apply {
            writeText("#!/bin/sh\n")
            toFile().setExecutable(true)
        }

        val detectedPath = autoDetectNodePath(
            pathEnv = "",
            extraSearchDirs = sequenceOf(firstDir, secondDir),
        )

        assertEquals(firstNode.toAbsolutePath().toString(), detectedPath)
    }

    @Test
    fun `it seeds launch property only when the current path is blank`() {
        val seededPath = seededLaunchPath(
            currentPath = "   ",
            launchProperty = "/tmp/rescript-language-server",
        )
        val preservedPath = seededLaunchPath(
            currentPath = "/existing/rescript-language-server",
            launchProperty = "/tmp/rescript-language-server",
        )

        assertEquals("/tmp/rescript-language-server", seededPath)
        assertEquals("/existing/rescript-language-server", preservedPath)
    }

    @Test
    fun `it returns an executable path only for executables`() {
        val server = Files.createTempFile("rescript", "").apply {
            writeText("#!/bin/sh\n")
            toFile().setExecutable(true)
        }
        val nonExecutable = Files.createTempFile("rescript", "")

        assertEquals(server.toAbsolutePath().toString(), executablePath(server.toString()))
        assertNull(executablePath(nonExecutable.toString()))
    }
}
