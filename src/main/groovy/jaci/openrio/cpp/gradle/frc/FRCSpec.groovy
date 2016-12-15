package jaci.openrio.cpp.gradle.frc

import org.gradle.model.Managed
import org.gradle.model.Unmanaged

class TargetRIOAddress {
    String address
}

@Managed
public interface FRCSpec {
    String getTeam()
    void setTeam(String team)

    String getRioIP()
    void setRioIP(String rioIP)

    String getRioHost()
    void setRioHost(String rioHost)

    String getDeployDirectory()
    void setDeployDirectory(String directory)
    
    int getDeployTimeout()
    void setDeployTimeout(int seconds)

    String getRobotCommand()
    void setRobotCommand(String cmd)

    String getRunArguments()
    void setRunArguments(String args)

    @Unmanaged
    TargetRIOAddress getActiveRioAddress()
    void setActiveRioAddress(TargetRIOAddress address)
}