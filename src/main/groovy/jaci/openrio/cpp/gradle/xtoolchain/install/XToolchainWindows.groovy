package jaci.openrio.cpp.gradle.xtoolchain.install

import jaci.openrio.cpp.gradle.xtoolchain.*

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
            outputs.files(XToolchain.download_file(project, "windows", filename))
            doLast {
                XToolchain.download_xtoolchain_file(project, "windows", filename)
            }
        }

        return project.tasks.create("install_frc_toolchain_winblows") {
            description = "Install the FRC Toolchain on a Windows System"
            dependsOn dltask
            def msifile = dltask.outputs.files[0]
            def targetdir = XToolchain.get_toolchain_extraction_dir("windows")
            outputs.files(targetdir)
            doLast {
                project.exec {
                    commandLine 'msiexec'
                    args '/a', msifile.absolutePath, '/qb', "TARGETDIR=${targetdir.absoluteFile}"
                }
            }
        }
    }

    @Override
    File get_toolchain_root() {
        return new File(XToolchain.get_toolchain_extraction_dir("windows"), "frc")
    }
}