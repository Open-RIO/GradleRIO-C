package jaci.openrio.cpp.gradle.xtoolchain

import org.gradle.internal.os.OperatingSystem
import org.gradle.api.*

class XToolchainLinux implements XToolchainBase {
    @Override
    boolean canApply(OperatingSystem os) { return os.isLinux() }

    @Override
    Task apply(Project project) {
        // We'll need to do some tricky stuff here to specify the install path

        return project.tasks.create("install_frc_toolchain_linux") {
            description = "Install the FRC Toolchain on a Linux System"
        }
    }

    @Override
    File get_toolchain_root(Project project) {
        return new File(XToolchain.get_toolchain_extraction_dir(project, "linux"), "frc")
    }
}