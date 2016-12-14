package jaci.openrio.cpp.gradle.frc

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.model.*
import org.gradle.nativeplatform.*
import org.gradle.platform.base.*
import org.gradle.language.base.plugins.ComponentModelBasePlugin
import org.gradle.api.plugins.ExtensionContainer
import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.*
import org.hidetake.groovy.ssh.connection.*

class ProjectWrapper {
    Project project
}

class DeployPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.getPluginManager().apply(ComponentModelBasePlugin.class)
        project.extensions.create('deploy_project_wrapper', ProjectWrapper)
        project.extensions.deploy_ssh = Ssh.newService()
        project.deploy_project_wrapper.project = project
        project.task('deploy') {
            group "GradleRIO"
            description "Deploy all Binaries to the RoboRIO"
        }
    }

    static class Rules extends RuleSource {
        @Model("frc")
        void createDeployModel(FRCSpec spec) {}

        @Defaults
        void defaultDeployModel(FRCSpec spec) {
            spec.setTeam("0000")
            spec.setRioIP("") // Nothing, defaults to 10.TE.AM.20
            spec.setRioHost("") // Nothing, defaults to roboRIO-TEAM-frc.local
            spec.setDeployDirectory("/home/lvuser")
            spec.setDeployTimeout(3)
            spec.setRobotCommand("") // Nothing, try to figure it out based on FRCUserProgram spec. Setting this to null bypasses generation

            spec.setActiveRioAddress(new TargetRIOAddress())
        }

        @ComponentType
        void registerFRCComponent(TypeBuilder<FRCUserProgram> builder) {
            builder.defaultImplementation(DefaultFRCUserProgram.class)
        }

        // Called after buildscript evaluation
        @Finalize
        void finalizeDeployModel(FRCSpec spec, final ExtensionContainer extensions) {
            final ProjectWrapper projectWrapper = extensions.getByType(ProjectWrapper)
            if (spec.getRioHost() == "") { // Empty String, default to roboRIO-TEAM-frc.local
                spec.setRioHost("roboRIO-${spec.getTeam()}-frc.local")
            }
            if (spec.getRioIP() == "") { // Empty String, default 10.TE.AM.20
                def team = spec.getTeam()
                def teamlen = team.length()
                if (teamlen < 4)
                    for (int i = 0; i < 4-teamlen; i++)
                        team = "0" + team
                spec.setRioIP("10.${team.substring(0,2)}.${team.substring(2,4)}.20")
            }
            
            def runSshTest = { addr ->
                try {
                    def result = projectWrapper.project.deploy_ssh.run {
                        session(host: addr, user: 'lvuser', timeoutSec: spec.getDeployTimeout(), knownHosts: AllowAnyHosts.instance) {
                            println "--> SUCCESS"
                        }
                    }
                    return true
                } catch (all) {
                    return false
                }
            }

            projectWrapper.project.task("determine_rio_address") {
                group "GradleRIO"
                description "Determine the active address for the RoboRIO"
                doLast {
                    println "============ FINDING ROBORIO ============"
                    def address = [
                        "mDNS" : spec.getRioHost(),
                        "USB" : "172.22.11.2",
                        "Static IP" : spec.getRioIP() 
                    ]
                    address.any { name, addr ->
                        println "-> ${name} (${addr})..."
                        if (runSshTest(addr)) {
                            spec.getActiveRioAddress().address = addr
                            println "============ ROBORIO FOUND ============"
                            return true
                        }
                    }
                }
            }

            projectWrapper.project.task("restart_rio_code") {
                group "GradleRIO"
                description "Restart User Code running on the RoboRIO"
                projectWrapper.project.tasks.getByName("deploy").finalizedBy(it)
                dependsOn(projectWrapper.project.tasks.getByName("determine_rio_address"))
                doLast {
                    projectWrapper.project.deploy_ssh.run {
                        session(host: spec.getActiveRioAddress().address, user: 'lvuser', timeoutSec: spec.getDeployTimeout(), knownHosts: AllowAnyHosts.instance) {
                            execute ". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r"
                        }
                    }
                }
            }

            if (spec.getRobotCommand() != "" && spec.getRobotCommand() != null) {
                createRobotCommandTask(spec.getRobotCommand(), projectWrapper, spec)
            }
        }

        @Mutate
        void addRobotCommandTask(ComponentSpecContainer container, final ExtensionContainer extensions, @Path("frc") FRCSpec spec) {
            if (spec.getRobotCommand() == "") {
                def done = false
                container.withType(FRCUserProgram) {
                    if (!done) createRobotCommandTask("bash -c 'cd ${spec.getDeployDirectory()} && ./${it.name}'", extensions.getByType(ProjectWrapper), spec)
                    done = true
                }
            }
        }

        static void createRobotCommandTask(String command, ProjectWrapper projectWrapper, FRCSpec spec) {
            projectWrapper.project.task("deploy_robotcommand") {
                group "GradleRIO"
                description "Set the RobotCommand on the RoboRIO to launch the User Program"
                projectWrapper.project.tasks.getByName("deploy").dependsOn(it)
                dependsOn(projectWrapper.project.tasks.getByName("determine_rio_address"))
                doLast {
                    def rc_local = new File(projectWrapper.project.buildDir, "robotCommand")
                    rc_local.write("${command}\n")
                     projectWrapper.project.deploy_ssh.run {
                        session(host: spec.getActiveRioAddress().address, user: 'lvuser', timeoutSec: spec.getDeployTimeout(), knownHosts: AllowAnyHosts.instance) {
                            put from: rc_local, into: "/home/lvuser"
                            execute "chmod +x /home/lvuser/robotCommand"
                        }
                     }
                }
            }
        }

        @Mutate
        void addDeployTask(BinaryContainer binaries, final ExtensionContainer extensions, @Path("frc") FRCSpec spec) {
            final ProjectWrapper projectWrapper = extensions.getByType(ProjectWrapper)
            binaries.all { bin ->
                if (bin in NativeExecutableBinarySpec || bin in SharedLibraryBinarySpec || bin in ToastResourceBinary) {
                    if (bin in ToastResourceBinary || bin.targetPlatform.name == "roborio-arm") {
                        def file = ""
                        if (bin in NativeExecutableBinarySpec) file = bin.executable.file
                        else if (bin in SharedLibraryBinarySpec) file = bin.sharedLibraryFile
                        else if (bin in ToastResourceBinary) file = new File(bin.outputDir, bin.filename + ".trx")

                        def taskName = tasks.taskName("deploy")
                        projectWrapper.project.task(taskName) {
                            description "Deploy Binary ${file.name} to the RoboRIO"
                            projectWrapper.project.tasks.getByName("deploy").dependsOn(it)
                            dependsOn(projectWrapper.project.tasks.getByName("determine_rio_address"))
                            doLast {
                                if (file.exists())
                                    projectWrapper.project.deploy_ssh.run {
                                        session(host: spec.getActiveRioAddress().address, user: 'lvuser', timeoutSec: spec.getDeployTimeout(), knownHosts: AllowAnyHosts.instance) {
                                            execute ". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t" // Just in case
                                            execute "mkdir -p ${spec.getDeployDirectory()}"
                                            put from: file, into: spec.getDeployDirectory()
                                            execute "chmod +x ${spec.getDeployDirectory()}/${file.name}"
                                            execute "sync"
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

}