package jaci.openrio.cpp.gradle.resource

import org.gradle.api.tasks.*;

class ResourceAnalyse extends SourceTask {
    @OutputFile
    File destinationFile

    @Input
    File baseDir

    @TaskAction
    void analyse_resources() {
        def arr = []
        getSource().each { sourceFile ->
            def size = sourceFile.length()
            def relpath = baseDir.absoluteFile.toURI().relativize(sourceFile.absoluteFile.toURI()).getPath()
            arr << [relpath, size, sourceFile.absolutePath]
        }

        destinationFile.createNewFile()
        def writer = new FileWriter(destinationFile)
        writer.write("${arr.size}\n")
        arr.each { a ->
            writer.write("${a[0]};${a[1]}@${a[2]}\n")
        }
        writer.close()
    }
}