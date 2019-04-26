package com.emberjs.configuration

import com.emberjs.cli.EmberCli
import com.emberjs.utils.emberRoot
import com.emberjs.utils.parentModule
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.LangDataKeys

open class EmberCommandLineState(environment: ExecutionEnvironment) : CommandLineState(environment) {
    override fun startProcess(): ProcessHandler {
        val configuration = (environment.runProfile as EmberConfiguration)
        val argList = configuration.options.toCommandLineOptions()

        val workingDirectory =
        // if module configured, use that as workDirectory
                configuration.module?.moduleFile?.parentModule?.path
                        ?: environment.dataContext?.getData(LangDataKeys.MODULE)?.emberRoot?.path
                        ?: environment.project.basePath

        val cmd = EmberCli(environment.project, configuration.command, *argList)
                .apply {
                    workDirectory = workingDirectory
                    nodeInterpreter = configuration.nodeInterpreter
                    env = configuration.env
                }
                .commandLine()
                .apply {
                    // taken from intellij-rust
                    // @see https://github.com/intellij-rust/intellij-rust/blob/3fe2e01828e4bfce0617a301467da5f61cc33202/src/main/kotlin/org/rust/cargo/toolchain/Cargo.kt#L110
                    withCharset(Charsets.UTF_8)
                    withEnvironment("TERM", "ansi")
                    withRedirectErrorStream(true)
                }

        val handler = KillableColoredProcessHandler(cmd)

        ProcessTerminatedListener.attach(handler)

        return handler
    }
}