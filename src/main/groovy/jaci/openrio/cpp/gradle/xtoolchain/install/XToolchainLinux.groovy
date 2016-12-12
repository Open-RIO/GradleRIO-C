package jaci.openrio.cpp.gradle.xtoolchain.install

import jaci.openrio.cpp.gradle.xtoolchain.*

import org.gradle.internal.os.OperatingSystem
import org.gradle.api.*

class XToolchainLinux implements XToolchainBase {
    @Override
    boolean canApply(OperatingSystem os) { return os.isLinux() }

    @Override
    Task apply(Project project) {
        // Since we're running in apt-get, we can't specify the install location
        // This does mean that you have to have access to install packages on the System
        // (most commonly, sudo access)

        return project.tasks.create("install_frc_toolchain_linux") {
            description = "Install the FRC Toolchain on a Linux System"
            doLast {
                project.exec {
                    commandLine 'apt-add-repository'
                    args 'ppa:wpilib/toolchain'
                }
                project.exec {
                    commandLine 'apt'
                    args 'update'
                }
                project.exec {
                    commandLine 'apt'
                    args 'install', 'frc-toolchain'
                }
            }
        }
    }

    @Override
    File get_toolchain_root() {
        return new File("/usr").absoluteFile
    }
}