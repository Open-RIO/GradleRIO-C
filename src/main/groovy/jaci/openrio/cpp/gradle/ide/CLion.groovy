package jaci.openrio.cpp.gradle.ide

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.model.*
import org.gradle.nativeplatform.*
import org.gradle.platform.base.*
import org.gradle.internal.os.OperatingSystem
import org.gradle.language.base.plugins.ComponentModelBasePlugin
import org.gradle.language.cpp.CppSourceSet
import org.gradle.language.c.CSourceSet
import org.gradle.api.plugins.ExtensionContainer

class CLionExtension {
    ArrayList<ArrayList<Object>> _binaries;
}

class CLionPlugin implements Plugin<Project> {
    String relative(File base, File file) {
        return java.nio.file.Paths.get(base.path).relativize(java.nio.file.Paths.get(file.path)).toString().replaceAll("\\\\", "/")
    }

    void apply(Project project) {
        project.getPluginManager().apply(ComponentModelBasePlugin.class)
        project.extensions.create('clion', CLionExtension)
        project.with {
            clion._binaries = []
            model {
                binaries {
                    withType(NativeExecutableBinarySpec) {
                        clion._binaries << [it, it.executable.file]
                    }
                    withType(SharedLibraryBinarySpec) {
                        clion._binaries << [it, it.sharedLibraryFile]
                    }
                }
            }
        }

        project.task('cleanClion', type: Delete) {
            group "GradleRIO"
            description "Clean Clion Files"
            def files = ['.idea', 'cmake-build-debug', 'CMakeLists.txt']
            files.each {
                def f = new File(it)
                if (f.exists()) delete f
            }
        }

        project.task('clion') {
            group "GradleRIO"
            description "Generate CLion CMakeLists.txt files."
            doLast {
                def subdirs_write = false
                def file = new FileWriter(project.file("CMakeLists.txt"))
                file.write("cmake_minimum_required(VERSION 3.3)\n")
                file.write("project(${project.name})\nset(CMAKE_CXX_STANDARD 14)\n\n")
                project.clion._binaries.each { bin, binfile ->
                    def sources_dirs = []
                    def includes_dirs = []
                    def dep_includes_dirs = []
                    bin.inputs.withType(CppSourceSet) { sources ->
                        sources_dirs += sources.source.srcDirs
                        includes_dirs += sources.exportedHeaders.srcDirs
                    }
                    bin.inputs.withType(CSourceSet) { sources ->
                        sources_dirs += sources.source.srcDirs
                        includes_dirs += sources.exportedHeaders.srcDirs
                    }
                    bin.libs.each {
                        dep_includes_dirs += it.includeRoots
                    }
                    if (sources_dirs.size > 0) {
                        def files_str = sources_dirs.collect { '"' + relative(project.projectDir, it) + '/*.*"' }.join(" ")
                        file.write("file(GLOB_RECURSE SOURCES ${files_str})\n")
                    }
                    if (includes_dirs.size > 0) {
                        def rel_paths = includes_dirs.collect { relative(project.projectDir, it) }
                        def files_str = rel_paths.collect { '"' + it + '/*.*"' }.join(" ")
                        file.write("file(GLOB_RECURSE INCLUDES ${files_str})\n")
                        rel_paths.each {
                            file.write("include_directories(${it})\n")
                        }
                    }
                    file.write("\n")
                    if (dep_includes_dirs.size > 0) {
                        def rel_paths = dep_includes_dirs.collect { relative(project.projectDir, it) }
                        def files_str = rel_paths.collect { '"' + it + '/*.*"' }.join(" ")
                        file.write("file(GLOB_RECURSE DEP_INCLUDES ${files_str})\n")
                        rel_paths.each {
                            file.write("include_directories(${it})\n")
                        }
                        file.write("set(ALL_INCLUDES \${INCLUDES} \${DEP_INCLUDES})\n")
                    } else {
                        file.write("set(ALL_INCLUDES \${INCLUDES})\n")
                    }
                    file.write("\n")
                    file.write("add_executable(fake_${binfile.name} \${SOURCES} \${ALL_INCLUDES})\n")
                    
                    if (!subdirs_write) {
                        subdirs_write = true
                        for (proj in project.subprojects) {
                            file.write("add_subdirectory(${relative(project.projectDir, proj.projectDir)})\n")
                        }
                    }

                    file.write("\n")
                    def gradle_path = relative(project.projectDir, project.rootProject.projectDir)
                    def work_dir = "WORKING_DIRECTORY ../${gradle_path} "
                    def proj_path = project == project.rootProject ? "" : "${project.path}:"
                    def gradle_exe = OperatingSystem.current().isWindows() ? "gradlew.bat" : "./gradlew"
                    file.write("add_custom_target(${binfile.name}_build ${gradle_exe} ${proj_path}build ${work_dir}SOURCES \${SOURCES} \${ALL_INCLUDES})\n")
                    file.write("add_custom_target(${binfile.name}_deploy ${gradle_exe} ${proj_path}deploy ${work_dir}SOURCES \${SOURCES} \${ALL_INCLUDES})\n")
                }
                file.close()
            }
        }
    }
}