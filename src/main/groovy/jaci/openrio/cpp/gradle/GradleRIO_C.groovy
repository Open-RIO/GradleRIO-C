package jaci.openrio.cpp.gradle

import org.gradle.api.*;
import groovy.util.*;

import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.model.*;
import org.gradle.platform.base.*;

import jaci.openrio.cpp.gradle.xtoolchain.XToolchainPlugin;
import jaci.openrio.cpp.gradle.wpi.WPIPlugin;
import org.gradle.internal.os.OperatingSystem;

import jaci.openrio.cpp.gradle.resource.*

class GradleRIO_C implements Plugin<Project> {

    void apply(Project project) {
        project.getPluginManager().apply(ComponentModelBasePlugin.class)
        project.getPluginManager().apply(XToolchainPlugin.class)
        project.getPluginManager().apply(WPIPlugin.class)
        project.with {
            extensions.create("gradlerio_c", GradleRIOCExtensions)
        }
    }

    // ~/.gradle
    static File getGlobalDirectory() {
        return new File("${System.getProperty('user.home')}/.gradle", "gradlerioc")
    }

    static class ToastRules extends RuleSource {
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