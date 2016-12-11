package jaci.openrio.cpp.gradle.xtoolchain

import jaci.openrio.cpp.gradle.GradleRIO_C
import org.gradle.internal.os.OperatingSystem
import org.gradle.api.*

interface XToolchainBase {
    boolean canApply(OperatingSystem os)
    Task apply(Project project)
    File get_toolchain_root(Project project)
}

class XToolchain {
    static def url_base = "http://first.wpi.edu/FRC/roborio/toolchains/"

    static File download_file(Project project, String platform, String filename) {
        def dest = new File(GradleRIO_C.getGlobalDirectory(project), "cache/${platform}")
        dest.mkdirs()
        return new File(dest, filename)
    }

    static File get_toolchain_extraction_dir(Project project, String platform) {
        if (project.gradlerio_c.xtoolchain_extraction_dir != null) {
            return new File(project.gradlerio_c.xtoolchain_extraction_dir, "toolchain/${platform}").absoluteFile
        }
        return new File(GradleRIO_C.getGlobalDirectory(project), "toolchain/${platform}").absoluteFile
    }

    static void download_xtoolchain_file(Project project, String platform, String filename) {
        def dlfile = download_file(project, platform, filename)
        if (!dlfile.exists()) {
            new URL(url_base + filename).withInputStream{ i -> dlfile.withOutputStream{ it << i }}
        }
    }
}