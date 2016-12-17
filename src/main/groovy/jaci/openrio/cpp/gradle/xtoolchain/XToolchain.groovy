package jaci.openrio.cpp.gradle.xtoolchain

import jaci.openrio.cpp.gradle.GradleRIO_C
import jaci.openrio.cpp.gradle.xtoolchain.install.*
import org.gradle.internal.os.OperatingSystem
import org.gradle.api.*
import org.gradle.model.*
import org.gradle.platform.base.*
import org.gradle.language.base.plugins.ComponentModelBasePlugin

import org.gradle.api.internal.file.FileResolver
import org.gradle.process.internal.ExecActionFactory
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.nativeplatform.*
import org.gradle.nativeplatform.toolchain.*
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.nativeplatform.plugins.NativeComponentPlugin
import org.gradle.internal.operations.BuildOperationProcessor
import org.gradle.nativeplatform.toolchain.internal.gcc.version.CompilerMetaDataProviderFactory

interface XToolchainBase {
    boolean canApply(OperatingSystem os)
    Task apply(Project project)
    // WARNING!!!!
    // Do not trust that GradleRIO owns the file given by get_toolchain_root.
    // On Windows and Mac systems, it is installed under ~/.gradle/gradlerioc/toolchain
    // by default, HOWEVER, on Linux systems, it is installed under /usr as packages
    // are installed by apt. DO NOT remove files from this directory.
    // For this reason, no 'clean_frc_toolchain' task is provided.
    File get_toolchain_root()
}

class XToolchainPlugin implements Plugin<Project> {
    static def xtoolchains = new ArrayList<XToolchainBase>()

    void apply(Project project) {
        project.getPluginManager().apply(NativeComponentPlugin.class)
        project.getPluginManager().apply(ComponentModelBasePlugin.class)
        project.with {
            xtoolchains += [new XToolchainWindows(), new XToolchainMac(), new XToolchainLinux()]

            def xtoolchain_install_task = tasks.create("install_frc_toolchain") {
                group = "GradleRIO"
                description = "Install the FRC RoboRIO arm-frc-linux-gnueabi Toolchain"
            }

            xtoolchain_install_task.dependsOn getActiveToolchain().apply(it)
        }
    }

    static XToolchainBase getActiveToolchain() {
        xtoolchains.find {
            it.canApply(OperatingSystem.current())
        }
    }

    static class Rules extends RuleSource {
        @Model("cpp")
        void createCppModel(CppSpec spec) {}

        @Defaults 
        void defaultCppModel(CppSpec spec) {
            spec.setCppVersion("c++1y") // C++1Y = (roughly) C++14. The RoboRIO supports C++1Y (not C++14 ISO standard)
            spec.setDebugInfo(true) // This adds -g (gcc) and /Zi,/FS,/DEBUG (msvc). This is used for debugging and symbol info in a crash
        }

        // TODO Platform Compilers for ARM (Rasp Pi, Pine64)?
        @Mutate
        void addPlatform(PlatformContainer platforms) {
            // RoboRIO ARM Cross Compilation (XToolchain GCC)
            NativePlatform arm = platforms.maybeCreate("roborio-arm", NativePlatform.class)
            arm.architecture("arm")
            arm.operatingSystem("linux")

            // Cross-Platform 32(x86) and 64(x86_64) targets
            NativePlatform x86 = platforms.maybeCreate("any-32", NativePlatform.class)
            x86.architecture("x86")

            NativePlatform x64 = platforms.maybeCreate("any-64", NativePlatform.class)
            x64.architecture("x86_64")
        }

        @Defaults
        void addToolchain(NativeToolChainRegistry toolChainRegistry, ServiceRegistry serviceRegistry) {
            def fileResolver = serviceRegistry.get(FileResolver.class);
            def execActionFactory = serviceRegistry.get(ExecActionFactory.class);
            def instantiator = serviceRegistry.get(Instantiator.class);
            def buildOperationProcessor = serviceRegistry.get(BuildOperationProcessor.class);
            def metaDataProviderFactory = serviceRegistry.get(CompilerMetaDataProviderFactory.class);
            toolChainRegistry.registerFactory(XToolchainGCC.class, { String name ->
                return instantiator.newInstance(XToolchainGCC.class, instantiator, name, buildOperationProcessor, OperatingSystem.LINUX, fileResolver, execActionFactory, metaDataProviderFactory)
            })
            toolChainRegistry.registerDefaultToolChain("roborioGcc", XToolchainGCC.class)
        }

        @Mutate
        void configureToolchains(NativeToolChainRegistry toolChains, @Path("cpp") CppSpec cppSpec) {
            toolChains.all { toolchain ->
                if (toolchain in VisualCpp) {
                    if (OperatingSystem.current().isWindows()) {
                        // Taken from nt-core, fixes VS2015 compilation issues 
                        // Workaround for VS2015 adapted from https://github.com/couchbase/couchbase-lite-java-native/issues/23
                        def VS_2015_INCLUDE_DIR = "C:/Program Files (x86)/Windows Kits/10/Include/10.0.10240.0/ucrt"
                        def VS_2015_LIB_DIR = "C:/Program Files (x86)/Windows Kits/10/Lib/10.0.10240.0/ucrt"
                        def VS_2015_INSTALL_DIR = 'C:/Program Files (x86)/Microsoft Visual Studio 14.0'
                        def vsInstallDir = new File(VS_2015_INSTALL_DIR)

                        eachPlatform {
                            cppCompiler.withArguments { args ->
                                if (cppSpec.getDebugInfo()) {
                                    args << "/Zi"
                                    args << "/FS"
                                }
                                args << "/EHsc"
                                if (new File(VS_2015_INCLUDE_DIR).exists()) {
                                    args << "/I$VS_2015_INCLUDE_DIR"
                                }
                            }
                            linker.withArguments { args ->
                                if (cppSpec.getDebugInfo()) args << "/DEBUG"
                                if (new File(VS_2015_LIB_DIR).exists()) {
                                    if (platform.architecture.name == 'x86') {
                                        args << "/LIBPATH:$VS_2015_LIB_DIR/x86"
                                    } else {
                                        args << "/LIBPATH:$VS_2015_LIB_DIR/x64"
                                    }
                                }
                            }
                        }
                    }
                } else {
                    eachPlatform {
                        cppCompiler.withArguments { args ->
                            args << "-std=${cppSpec.getCppVersion()}"
                            if (cppSpec.getDebugInfo()) args << "-g"
                        }
                        if (toolchain in Gcc) {
                            linker.withArguments { args ->
                                args << "-ldl"
                                args << "-lrt"
                                args << "-lm"
                                // For a Toast project we would need -rdynamic in here
                            }
                        }
                    }
                }
            }
        }

        @Mutate
        void configureRoborioBuildable(BinaryContainer binaries) {
            binaries.withType(NativeBinarySpec) {
                if (it.toolChain in XToolchainGCC && !it.toolChain.isCrossCompilerPresent()) {
                    it.buildable = false
                }
            }
        }
    }
}

class XToolchain {
    static def url_base = "http://first.wpi.edu/FRC/roborio/toolchains/"

    static File download_file(Project project, String platform, String filename) {
        def dest = new File(GradleRIO_C.getGlobalDirectory(), "cache/${platform}")
        dest.mkdirs()
        return new File(dest, filename)
    }

    static File get_toolchain_extraction_dir(String platform) {
        return new File(GradleRIO_C.getGlobalDirectory(), "toolchain/${platform}").absoluteFile
    }

    static void download_xtoolchain_file(Project project, String platform, String filename) {
        def dlfile = download_file(project, platform, filename)
        if (!dlfile.exists()) {
            new URL(url_base + filename).withInputStream{ i -> dlfile.withOutputStream{ it << i }}
        }
    }
}