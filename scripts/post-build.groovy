#!/usr/bin/env groovyclient

import java.nio.file.Files
import java.nio.file.Paths
import static java.nio.file.attribute.PosixFilePermission.*

JAR = Files.readAllBytes(Paths.get("${project.build.directory}/${project.artifactId}.jar"))

def generate_executable(inName, outName) {
    def init = Files.readAllBytes(Paths.get("${project.basedir}/scripts/${inName}"))
    def outPath = Paths.get("${project.build.directory}/${outName}")
    try (def os = Files.newOutputStream(outPath)) {
        os.write(init)
        os.write(JAR)
    }
    if (outPath.fileSystem.supportedFileAttributeViews().contains("posix")) {
        def perm = Files.getPosixFilePermissions(outPath)
        perm.addAll(OWNER_EXECUTE, GROUP_EXECUTE, OTHERS_EXECUTE)
        Files.setPosixFilePermissions(outPath, perm)
    }
    println "Generated: ${outName}"
}

generate_executable("jd-cli.sh", "jd-cli")
generate_executable("jd-cli-cygwin.sh", "jd-cli-cygwin")
generate_executable("jd-cli.cmd", "jd-cli.cmd")
