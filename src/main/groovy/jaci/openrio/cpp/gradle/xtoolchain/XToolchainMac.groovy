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
            outputs.files(XToolchain.download_file(project, "osx", filename))
            doLast {
                XToolchain.download_xtoolchain_file(project, "osx", filename)
            }
        }

        return project.tasks.create("install_frc_toolchain_osx") {
            description = "Install the FRC Toolchain on a Mac OS X System"
            dependsOn dltask
            def targzfile = dltask.outputs.files[0]
            def targetdir = XToolchain.get_toolchain_extraction_dir(project, "osx")
            outputs.files(targetdir)
            // Do tar / gzip | pax here
            doLast {
                project.exec {
                    commandLine 'tar'
                    workingDir targetdir
                    args '-xzf', targzfile.absolutePath
                }
                project.exec {
                    commandLine 'gzip'
                    workingDir targetdir
                    args '-cd', "FRC ARM Toolchain.pkg/Contents/Archive.pax.gz", "|", "pax", "-r"
                }
            }
        }
    }

    @Override
    File get_toolchain_root(Project project) {
        return new File(XToolchain.get_toolchain_extraction_dir(project, "osx"), "frc")
    }
}