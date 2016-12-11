package jaci.openrio.cpp.gradle

import org.gradle.api.*;
import groovy.util.*;

import org.gradle.internal.service.ServiceRegistry;
import org.gradle.language.base.internal.registry.LanguageTransformContainer;
import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.platform.base.*;
import org.gradle.model.*;

import org.gradle.api.internal.file.FileResolver;
import org.gradle.process.internal.ExecActionFactory;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.nativeplatform.toolchain.*;
import org.gradle.nativeplatform.platform.NativePlatform;
import org.gradle.nativeplatform.plugins.NativeComponentPlugin;
import org.gradle.internal.operations.BuildOperationProcessor;
import org.gradle.nativeplatform.toolchain.internal.gcc.version.CompilerMetaDataProviderFactory;

import jaci.openrio.cpp.gradle.xtoolchain.*;
import org.gradle.internal.os.OperatingSystem;

import jaci.openrio.cpp.gradle.resource.*

class GradleRIO_C implements Plugin<Project> {

    static def xtoolchains = new ArrayList<XToolchainBase>()

    void apply(Project project) {
        project.getPluginManager().apply(NativeComponentPlugin.class)
        project.getPluginManager().apply(ComponentModelBasePlugin.class)
        project.with {
            extensions.create("gradlerio_c", GradleRIOCExtensions)

            xtoolchains += [new XToolchainWindows(), new XToolchainMac(), new XToolchainLinux()]

            def xtoolchain_install_task = tasks.create("install_frc_toolchain") {
                description = "Install the FRC RoboRIO arm-frc-linux-gnueabi Toolchain"
            }

            xtoolchain_install_task.dependsOn getActiveToolchain().apply(it)
        }
    }

    // ~/.gradle
    static File getGlobalDirectory() {
        return new File("${System.getProperty('user.home')}/.gradle", "gradlerioc")
    }

    static XToolchainBase getActiveToolchain() {
        xtoolchains.find {
            it.canApply(OperatingSystem.current())
        }
    }

    static class ToastRules extends RuleSource {
        @Mutate
        void addPlatform(PlatformContainer platforms) {
            NativePlatform platform = platforms.maybeCreate("roborio-arm", NativePlatform.class)
            platform.architecture("arm")
            platform.operatingSystem("linux")
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

        @ComponentType
        void registerComponent(TypeBuilder<ToastResourceSpec> builder) { }

        @ComponentType
        void registerBinary(TypeBuilder<ToastResourceBinary> builder) { }

        @ComponentType
        void registerLanguage(TypeBuilder<ToastResources> builder) {}

        @ComponentBinaries
        void generateResourceBinaries(ModelMap<ToastResourceBinary> binaries, VariantComponentSpec component, @Path("buildDir") File buildDir) {
            binaries.create("trx") { binary ->
                binary.outputDir = new File(buildDir, "trx/${component.name}")
                binary.filename = component.name.toLowerCase()
            }
        }

        @BinaryTasks
        void generateResourceTasks(ModelMap<Task> tasks, final ToastResourceBinary binary) {
            def _tasks = []
            binary.inputs.withType(ToastResources) { sourceSet ->
                def taskName = binary.tasks.taskName("analyse", sourceSet.name)
                def outdir = new File(binary.outputDir, "partial")
                tasks.create(taskName, ResourceAnalyse) { compile_task ->
                    compile_task.source = sourceSet.source
                    compile_task.destinationFile = new File(outdir, sourceSet.name + ".trxp")
                    compile_task.baseDir = sourceSet.baseDir == null ? sourceSet.source.getSrcDirs()[0] : sourceSet.baseDir
                    _tasks << compile_task
                }
            }
            def taskName = binary.tasks.taskName("compile")
            tasks.create(taskName, ResourceCompile) { t ->
                _tasks.each { t2 -> t.dependsOn t2 }
                t.destinationFile = new File(binary.outputDir, binary.filename + ".trx")
                t.source = _tasks.collect { t2 -> t2.outputs.files[0] }
            }
        }
    }
}