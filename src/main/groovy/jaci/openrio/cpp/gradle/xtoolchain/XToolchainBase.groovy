package jaci.openrio.cpp.gradle.xtoolchain

import org.gradle.internal.os.OperatingSystem
import org.gradle.api.*

interface XToolchainBase {
    boolean canApply(OperatingSystem os)
    Task apply(Project project)
}

class XToolchainDownloader {
    static def url_base = "http://first.wpi.edu/FRC/roborio/toolchains/"

    static File download_file(Project project, String platform, String filename) {
        def dest = new File(project.getGradle().getGradleUserHomeDir(), "caches/gradlerioc/${platform}")
        dest.mkdirs()
        return new File(dest, filename)
    }

    static void download_xtoolchain_file(Project project, String platform, String filename) {
        def dlfile = download_file(project, platform, filename)
        if (!dlfile.exists()) {
            new URL(url_base + filename).withInputStream{ i -> dlfile.withOutputStream{ it << i }}
        }
    }
}