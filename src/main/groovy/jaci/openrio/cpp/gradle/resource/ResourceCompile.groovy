package jaci.openrio.cpp.gradle.resource

import org.gradle.api.tasks.*;

class ResourceCompile extends SourceTask {
    @OutputFile
    File destinationFile

    @TaskAction
    void compile_partials() {
        destinationFile.createNewFile()

        def outs = new FileOutputStream(destinationFile)
        def total = 0
        def values = []
        getSource().each { partialFile ->
            def reader = new BufferedReader(new FileReader(partialFile))
            def count = reader.readLine() as Integer
            total += count
            (0..count-1).each { i ->
                def (line, file) = reader.readLine().split("@")
                def (filename, size) = line.split(";")
                values << [filename, size as Integer, file]
            }
            reader.close()
        }
        outs.write((total.toString() + ":").getBytes())
        def pos = 0
        values.each { entry ->
            def (filename, size, file) = entry
            outs.write((filename + ";" + size.toString() + "@" + pos.toString() + ";").getBytes())
            pos += size
        }

        def buf = new byte[4096]
        values.each { entry ->
            def ins = new FileInputStream(entry[2])
            def n = 0
            while (-1 != (n = ins.read(buf))) outs.write(buf, 0, n)
            ins.close()
        }
        outs.close()
    }
}