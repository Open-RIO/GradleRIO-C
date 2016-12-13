package jaci.openrio.cpp.gradle.wpi

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.model.*
import org.gradle.language.base.plugins.ComponentModelBasePlugin
import org.gradle.nativeplatform.*
import org.gradle.platform.base.*
import org.gradle.language.cpp.CppSourceSet
import org.gradle.language.cpp.tasks.CppCompile
import org.gradle.language.c.CSourceSet
import org.gradle.language.c.tasks.CCompile
import org.gradle.api.plugins.ExtensionContainer
import groovy.transform.TupleConstructor

import jaci.openrio.cpp.gradle.GradleRIO_C
import org.ajoberstar.grgit.Grgit

class ProjectWrapper {
    Project project
}

class WPIPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.getPluginManager().apply(ComponentModelBasePlugin.class)
        project.extensions.create('projectWrapper', ProjectWrapper)
        project.projectWrapper.project = project
    }

    static class Rules extends RuleSource {
        @Model("wpi")
        void createWpiModel(WpiSpec spec) { }

        @Defaults
        void defaultWpiModel(WpiSpec spec) {
            spec.setGit(false)
            spec.setGitVersion("master")
            spec.setGitRemote("https://github.com/wpilibsuite/allwpilib.git")
        }

        @Mutate
        void addPrebuiltLibraries(Repositories repos, @Path("wpi") WpiSpec wpiSpec) {
            PrebuiltLibraries libs = repos.maybeCreate("wpilib", PrebuiltLibraries.class)

            if (wpiSpec.getGit()) {
                // Use WPILib Github
                libs.create("wpilib") {
                    def basedir = new File(GradleRIO_C.getGlobalDirectory(), "wpi")
                    headers.srcDir "${basedir}/include"
                    binaries.withType(StaticLibraryBinary) {
                        staticLibraryFile = new File("${basedir}/lib/libwpi.so")
                    }
                }
            } else {
                // Use ~/wpilib/ (WPILib Eclipse Plugins) [default]
                libs.create("wpilib") {
                    def basedir = new File("${System.getProperty('user.home')}/wpilib/cpp/current")
                    headers.srcDir "${basedir}/include"
                    binaries.withType(StaticLibraryBinary) {
                        staticLibraryFile = new File("${basedir}/lib/libwpi.so")
                    }
                }
            }
        }

        // We can't add a Library Search Path in PrebuiltLibraries, so we have to do it as an additional step to 
        // tell the linker where WPILib's other libraries (HAL, NTCore etc) are
        @BinaryTasks
        void addWpiGitTasks(ModelMap<Task> tasks, final NativeBinarySpec binary, final ExtensionContainer extensions, @Path("wpi") WpiSpec wpiSpec) {
            def link_wpi = false
            binary.inputs.withType(CppSourceSet) { sourceSet ->
                sourceSet.libs.each { lib ->
                    if (lib instanceof LinkedHashMap && lib["library"] == "wpilib") {
                        link_wpi = true
                    }
                }
            }
            if (link_wpi) {
                final ProjectWrapper projectWrapper = extensions.getByType(ProjectWrapper)
                final Project project = projectWrapper.project
                def libSearchPath = "${System.getProperty('user.home')}/wpilib/cpp/current/lib"
                if (wpiSpec.getGit()) {
                    // This is mostly going to have to change as soon as 2017's full releases roll around due to changes in
                    // WPILib's build system
                    // This will likely be incubating for a week after kickoff so I can get everything together, as unfortunately
                    // OpenRIO does not have Beta Access (wink wink nudge nudge)
                    // TODO A way to update the repo
                    def clone_task = null, checkout_task = null
                    tasks.create(binary.tasks.taskName("clone_wpi_git"), DefaultTask) { task ->
                        clone_task = task
                        def outdir = new File(GradleRIO_C.getGlobalDirectory(), "wpilib")
                        task.doLast {
                            if (!outdir.exists()) {
                                println "Fetching WPILib from Github for the first time. This may take a while..."
                                Grgit.clone(dir: outdir, uri: wpiSpec.getGitRemote())
                            }
                        }
                    }
                    tasks.create(binary.tasks.taskName("checkout_wpi_git"), DefaultTask) { task ->
                        task.dependsOn clone_task
                        checkout_task = task
                        def gitdir = new File(GradleRIO_C.getGlobalDirectory(), "wpilib")
                        task.doLast {
                            def git = Grgit.open(dir: gitdir)
                            def git_target = wpiSpec.getGitVersion()

                            git.checkout(branch: git_target)
                        }
                    }
                    def builddir = new File(GradleRIO_C.getGlobalDirectory(), "wpi")
                    tasks.create(binary.tasks.taskName("build_wpi_git"), GradleBuild) { task ->
                        task.dependsOn checkout_task
                        def gitdir = new File(GradleRIO_C.getGlobalDirectory(), "wpilib")
                        task.buildFile = new File(gitdir, "build.gradle").absolutePath
                        task.tasks = [':wpilibc:build', 'wpilibcZip']

                        task.doLast {
                            def git = Grgit.open(dir: gitdir)
                            builddir.mkdirs()
                            project.delete builddir
                            project.copy {
                                from project.zipTree(new File(gitdir, "wpilibc/build/wpilibc.zip"))
                                into builddir
                            }
                        }
                    }
                    libSearchPath = new File(builddir, "lib").absolutePath
                }
                binary.tasks.withType(CppCompile) {
                    binary.linker.args << "-L${libSearchPath}".toString()
                }
                binary.tasks.withType(CCompile) {
                    binary.linker.args << "-L${libSearchPath}".toString()
                }
            }
        }
    }
}