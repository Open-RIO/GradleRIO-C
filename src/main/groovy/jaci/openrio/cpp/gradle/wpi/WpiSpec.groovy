package jaci.openrio.cpp.gradle.wpi

import org.gradle.model.Managed

@Managed
public interface WpiSpec {
    // Maven

    String getMavenBranch()
    void setMavenBranch(String branch)

    String getWpilibVersion()
    void setWpilibVersion(String version)

    String getHalVersion()
    void setHalVersion(String version)

    String getWpiutilVersion()
    void setWpiutilVersion(String version)

    String getNtcoreVersion()
    void setNtcoreVersion(String version)

    String getCscoreVersion()
    void setCscoreVersion(String version)

    String getTalonSrxVersion()
    void setTalonSrxVersion(String version)
}