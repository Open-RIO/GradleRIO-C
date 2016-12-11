package jaci.openrio.cpp.gradle.xtoolchain

import org.gradle.internal.os.OperatingSystem
import org.gradle.api.*

class XToolchainWindows implements XToolchainBase {
    def filename = "FRC-2016%20Windows%20Toolchain%204.9.3.msi"

    @Override
    boolean canApply(OperatingSystem os) { return os.isWindows() }

    @Override
    Task apply(Project project) {
        def dltask = project.tasks.create("download_frc_toolchain_winblows") {
            description = "Download the FRC Toolchain"
            outputs.files(XToolchainDownloader.download_file(project, "windows", filename))
            doLast {
                XToolchainDownloader.download_xtoolchain_file(project, "windows", filename)
            }
        }

        return project.tasks.create("install_frc_toolchain_winblows") {
            description = "Install the FRC Toolchain on a Windows System"
            dependsOn dltask
            def msifile = dltask.outputs.files[0]
            // Do msiexec here
        }
    }
}