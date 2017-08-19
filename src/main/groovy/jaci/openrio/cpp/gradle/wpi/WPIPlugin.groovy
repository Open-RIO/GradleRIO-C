package jaci.openrio.cpp.gradle.wpi

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.artifacts.*
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

class ProjectWrapper {
    Project project
}

class WPIPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.getPluginManager().apply(ComponentModelBasePlugin.class)
        project.extensions.create('wpi_project_wrapper', ProjectWrapper)
        project.wpi_project_wrapper.project = project

        project.tasks.create("download_wpi", DefaultTask) {
            group "GradleRIO"
            description "Download and Unzip WPILib if WPILib is fetched from Maven"
        }
    }

    static class Rules extends RuleSource {
        @Model("wpi")
        void createWpiModel(WpiSpec spec) { }

        @Defaults
        void defaultWpiModel(WpiSpec spec) {
            spec.setMavenBranch("release")

            spec.setWpilibVersion("+")
            spec.setHalVersion("<WPILIB>")
            spec.setWpiutilVersion("+")
            spec.setNtcoreVersion("+")
            spec.setCscoreVersion("+")
            spec.setTalonSrxVersion("+")
            spec.setNavxVersion("+")
        }

        @Mutate
        void addPrebuiltLibraries(Repositories repos, @Path("wpi") WpiSpec wpiSpec, final ExtensionContainer extensions) {
            PrebuiltLibraries libs = repos.maybeCreate("wpilib", PrebuiltLibraries.class)
            def project = extensions.getByType(ProjectWrapper).project

            // Use http://first.wpi.edu/FRC/roborio/maven/
            def url = "http://first.wpi.edu/FRC/roborio/maven/${wpiSpec.getMavenBranch()}"
            project.getConfigurations().maybeCreate("wpi_maven")
            project.getConfigurations().maybeCreate("third_party")
            
            project.repositories.maven {
                it.name = "WPI"
                it.url = url
            }
            project.repositories.maven {
                it.name = "Jaci"
                it.url = "http://dev.imjac.in/maven"
            }

            project.dependencies.add("wpi_maven", "edu.wpi.first.wpilibc:athena:${wpiSpec.getWpilibVersion()}")
            project.dependencies.add("wpi_maven", "edu.wpi.first.wpilib:hal:${wpiSpec.getHalVersion() == "<WPILIB>" ? wpiSpec.getWpilibVersion() : wpiSpec.getHalVersion()}")
            project.dependencies.add("wpi_maven", "edu.wpi.first.wpilib:wpiutil:${wpiSpec.getWpiutilVersion()}:arm@zip")
            project.dependencies.add("wpi_maven", "edu.wpi.first.wpilib.networktables.cpp:NetworkTables:${wpiSpec.getNtcoreVersion()}:arm@zip")
            project.dependencies.add("wpi_maven", "edu.wpi.cscore.cpp:cscore:${wpiSpec.getCscoreVersion()}:athena-uberzip@zip")
            
            project.dependencies.add("third_party", "thirdparty.frc.ctre:Toolsuite-Zip:${wpiSpec.getTalonSrxVersion()}@zip")
            project.dependencies.add("third_party", "thirdparty.frc.kauai:Navx-Zip:${wpiSpec.getNavxVersion()}@zip")

            def dltask = []
            project.getConfigurations().wpi_maven.files.each { file ->
                def dname = file.name.substring(0, file.name.indexOf('-'))
                dltask << project.tasks.create("download_wpi_${dname}", Copy) {
                    description = "Downloads and Unzips $dname from Maven"
                    group = "GradleRIO"
                    from project.zipTree(file)
                    into "${project.buildDir}/dependencies/wpi"
                }
            }
            project.getConfigurations().third_party.files.each { file ->
                def dname = file.name.substring(0, file.name.indexOf("-"))
                dltask << project.tasks.create("download_lib_${dname}", Copy) {
                    description = "Downloads and Unzips the $dname Third Party Library from Maven"
                    group = "GradleRIO"
                    from project.zipTree(file)
                    into "${project.buildDir}/dependencies/third/${dname}"
                }
            }
            dltask.each { t -> project.download_wpi.dependsOn(t) }
            libs.create("wpilib") {
                headers.srcDir "${project.buildDir}/dependencies/wpi/include"
                binaries.withType(StaticLibraryBinary) {
                    staticLibraryFile = new File("${project.buildDir}/dependencies/wpi/lib/libwpi.so")
                }
            }

            libs.create("talonSrx") {
                headers.srcDir "${project.buildDir}/dependencies/third/Toolsuite/cpp/include"
                binaries.withType(StaticLibraryBinary) {
                    staticLibraryFile = new File("${project.buildDir}/dependencies/third/Toolsuite/cpp/lib/libCTRLib.a")
                }
            }

            libs.create("navx") {
                headers.srcDir "${project.buildDir}/dependencies/third/Navx/roborio/cpp/include"
                binaries.withType(StaticLibraryBinary) {
                    staticLibraryFile = new File("${project.buildDir}/dependencies/third/Navx/roborio/cpp/lib/libnavx_frc_cpp.a")
                }
            }
        }

        // We can't add a Library Search Path in PrebuiltLibraries, so we have to do it as an additional step to 
        // tell the linker where WPILib's other libraries (HAL, NTCore etc) are
        @BinaryTasks
        void addWpiTasks(ModelMap<Task> tasks, final NativeBinarySpec binary, final ExtensionContainer extensions, @Path("wpi") WpiSpec wpiSpec) {
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
                
                def libSearchPath = "${project.buildDir}/dependencies/wpi/lib"
                tasks.withType(CCompile) {
                    dependsOn project.download_wpi
                    binary.linker.args << "-L${project.buildDir}/dependencies/wpi/Linux/arm".toString()
                    binary.linker.args << "-L${libSearchPath}".toString()
                    binary.linker.args << "-lntcore"
                    binary.linker.args << "-lFRC_NetworkCommunication"
                }
                tasks.withType(CppCompile) {
                    dependsOn project.download_wpi
                    binary.linker.args << "-L${project.buildDir}/dependencies/wpi/Linux/arm".toString()
                    binary.linker.args << "-L${libSearchPath}".toString()
                    binary.linker.args << "-lntcore"
                    binary.linker.args << "-lFRC_NetworkCommunication"
                }
            }
        }
    }
}