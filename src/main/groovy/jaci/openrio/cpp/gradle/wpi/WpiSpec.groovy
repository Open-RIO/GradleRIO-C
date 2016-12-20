package jaci.openrio.cpp.gradle.wpi

import org.gradle.model.Managed

@Managed
public interface WpiSpec {
    // Git

    boolean getGit()
    void setGit(boolean useGit)

    String getGitVersion()
    void setGitVersion(String version)

    String getGitRemote()
    void setGitRemote(String remote)

    // Eclipse Plugins

    boolean getEclipsePlugins()
    void setEclipsePlugins(boolean useEclipse)

    // Maven

    String getMavenBranch()
    void setMavenBranch(String branch)

    String getMavenWpilib()
    void setMavenWpilib(String version)

    String getMavenHal()
    void setMavenHal(String version)

    String getMavenWpiutil()
    void setMavenWpiutil(String version)

    String getMavenNtcore()
    void setMavenNtcore(String version)

    String getMavenCscore()
    void setMavenCscore(String version)

    // Local

    boolean getLocal()
    void setLocal(boolean useLocalFile)

    File getLocalDirectory()
    void setLocalDirectory(File localDir)
}