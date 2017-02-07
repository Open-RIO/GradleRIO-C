package jaci.openrio.cpp.gradle

import org.gradle.api.*;
import groovy.util.*;

import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.model.*;
import org.gradle.platform.base.*;
import org.gradle.nativeplatform.*;
import org.gradle.language.nativeplatform.HeaderExportingSourceSet;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Delete;

import jaci.openrio.cpp.gradle.xtoolchain.XToolchainPlugin;
import jaci.openrio.cpp.gradle.wpi.WPIPlugin;
import jaci.openrio.cpp.gradle.frc.DeployPlugin;
import jaci.openrio.cpp.gradle.ide.CLionPlugin;
import org.gradle.internal.os.OperatingSystem;

import jaci.openrio.cpp.gradle.resource.*

class GradleRIO_C implements Plugin<Project> {

    void apply(Project project) {
        project.getPluginManager().apply(ComponentModelBasePlugin.class)
        project.getPluginManager().apply(XToolchainPlugin.class)
        project.getPluginManager().apply(WPIPlugin.class)
        project.getPluginManager().apply(DeployPlugin.class)
        project.getPluginManager().apply(CLionPlugin.class)
    }

    // ~/.gradle
    static File getGlobalDirectory() {
        return new File("${System.getProperty('user.home')}/.gradle", "gradlerioc")
    }

    static class ToastRules extends RuleSource {
        @Model("libraries")
        void createLibrariesModel(LibrariesSpec spec) { }

        @Mutate
        void addPrebuiltLibraries(Repositories repos, @Path("libraries") LibrariesSpec spec) {
            PrebuiltLibraries libs = repos.maybeCreate("wpilib", PrebuiltLibraries.class)
            spec.each { lib ->
                def libname = lib.backingNode.path.name
                if (lib in LibraryPrebuilt) {
                    libs.create(libname) { plib ->
                        plib.headers.srcDir lib.getHeaders()
                        if (lib.getStaticFile() != null) {
                            binaries.withType(StaticLibraryBinary) {
                                staticLibraryFile = lib.getStaticFile()
                            }
                        }
                        if (lib.getSharedFile() != null) {
                            binaries.withType(SharedLibraryBinary) {
                                sharedLibraryFile = lib.getSharedFile()
                            }
                        }
                    }
                }
            }
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
        void addHeaderExportTask(ModelMap<Task> tasks, final NativeLibraryBinarySpec binary, @Path("buildDir") File buildDir) {
            binary.inputs.withType(HeaderExportingSourceSet) { sourceSet ->
                def deleteTaskName = binary.tasks.taskName("cleanHeaders", sourceSet.name)
                def exportTaskName = binary.tasks.taskName("exportHeaders", sourceSet.name)
                def outdir = new File(buildDir, "headers/${binary.getNamingScheme().getBaseName()}")

                def _task = null
                tasks.create(deleteTaskName, Delete) {
                    _task = it
                    delete outdir
                }
                tasks.create(exportTaskName, Copy) {
                    dependsOn _task
                    from sourceSet.exportedHeaders
                    destinationDir = outdir
                }
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