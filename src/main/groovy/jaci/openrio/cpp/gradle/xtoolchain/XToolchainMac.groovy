package jaci.openrio.cpp.gradle.xtoolchain

import org.gradle.internal.os.OperatingSystem
import org.gradle.api.*

class XToolchainMac implements XToolchainBase {
    def filename = "FRC-2016%20Mac%20OS%20X%20Toolchain%204.9.3.pkg.tar.gz"

    @Override
    boolean canApply(OperatingSystem os) { return os.isMacOsX() }

    @Override
    Task apply(Project project) {
        def dltask = project.tasks.create("download_frc_toolchain_osx") {
            description = "Download the FRC Toolchain"
            outputs.files(XToolchainDownloader.download_file(project, "osx", filename))
            doLast {
                XToolchainDownloader.download_xtoolchain_file(project, "osx", filename)
            }
        }

        return project.tasks.create("install_frc_toolchain_osx") {
            description = "Install the FRC Toolchain on a Mac OS X System"
            dependsOn dltask
            def targzfile = dltask.outputs.files[0]
            // Do tar / gzip | pax here
        }
    }
}