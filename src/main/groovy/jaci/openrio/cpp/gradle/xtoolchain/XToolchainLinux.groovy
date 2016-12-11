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
}