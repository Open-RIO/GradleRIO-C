package jaci.openrio.cpp.gradle.xtoolchain

import org.gradle.model.Managed

@Managed
public interface CppSpec {
    String getCppVersion()
    void setCppVersion(String cppVersion)

    boolean getDebugInfo()
    void setDebugInfo(boolean debug)
}