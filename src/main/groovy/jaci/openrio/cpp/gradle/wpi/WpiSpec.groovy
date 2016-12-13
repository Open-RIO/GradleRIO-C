package jaci.openrio.cpp.gradle.wpi

import org.gradle.model.Managed

@Managed
public interface WpiSpec {
    boolean getGit()
    void setGit(boolean useGit)

    String getGitVersion()
    void setGitVersion(String version)

    String getGitRemote()
    void setGitRemote(String remote)
}