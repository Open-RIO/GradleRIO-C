# GradleRIO-C
Managing FRC C++ Projects, the Gradle way. Build, Develop and Deploy your FRC Code on any system, without the need for eclipse or administrative priviledges.

Unlike the standard FRC Development Tools, GradleRIO-C can be run on any system, even if they're locked down (like school PCs). This includes installing
the C++ toolchain (with the exception of debian linux, which requires sudo and/or `apt` access). The only requirement is the Java Virtual Machine to run Gradle itself, a Command Line, and an Internet Connection.

## Commands
_Replace `gradlew` with `./gradlew` if you're on Mac or Linux_  
`gradlew install_frc_toolchain` will install the FRC C++ Toolchain, allowing you to build for the RoboRIO  
`gradlew build` will build your FRC Code  
`gradlew build deploy` will build and deploy your FRC Code. Can also be run without build as `gradlew deploy`  
`gradlew restart_rio_code` will restart user code runningo n the RoboRIO

Run `gradlew tasks --all` for more information.


GradleRIO will deploy to the RoboRIO through the following preference list:  
1: mDNS (`roborio-TEAM-frc.local`)  
2: USB (`172.22.11.2`)  
3: Static IP (`10.TE.AM.20`)   
The addresses for mDNS and Static IP can be changed in the buildscript.

## Quick Start
Download the [Quick Start Sample Project](Quickstart.zip) and edit it to your own accord.
You can get examples [here](examples/). Feel free to download them into your own project.

## Custom Implementation
Put the following at the top of your `build.gradle` file
```
plugins {
    // Replace latest-version with the latest version available.
    id "jaci.openrio.cpp.gradle.GradleRIO-C" version "latest-version"
}

apply plugin: "cpp"
```
You can copy-paste this [install instruction from here](https://plugins.gradle.org/plugin/jaci.openrio.cpp.gradle.GradleRIO-C)

You can now setup your project just like any other Gradle C++ project.  
You can get examples [here](examples/). Feel free to download them into your own project.

Example (barebones) `build.gradle` below.

```gradle
plugins {
    id "jaci.openrio.cpp.gradle.GradleRIO-C" version "latest-version"
}
apply plugin: "cpp"

model {
    frc {
        team = "5333"
    }
    components {
        my_program(FRCUserProgram) {                        // This is your program, my_program
            targetPlatform "roborio-arm"                    // Build on the RoboRIO
            sources.cpp {
                source.srcDirs "src"                        // Where your .cpp files are stored
                exportedHeaders.srcDirs "include"           // Where your .h / .hpp files are stored
                lib library: "wpilib", linkage: "static"    // Compile with WPILib
            }
        }
    }
}
```

Full GradleRIO-C Spec
```gradle
plugins {
    id "jaci.openrio.cpp.gradle.GradleRIO-C" version "latest-version"
}

apply plugin: "cpp"
// You can use Visual Studio for development and Gradle for building if you so desire.
// my_programVisualStudio to generate the Visual Studio 2010+ solution file
apply plugin: "visual-studio"

model {
    frc {
        team = "5333"
        // The below options are optional. Values are the defaults
        deployTimeout = 3                   // Timeout (in seconds) when trying to find the RoboRIO on the network
        deployDirectory = "/home/lvuser"    // Directory to deploy to on the RoboRIO
        rioIP = "10.53.33.20"               // IP Address of the RoboRIO. This is automatically calculated from team number
        rioHost = "roborio-5333-frc.local"  // Hostname of the RoboRIO. This is automatically calculated from team number
        robotCommand = "./my_program"       // Script to run when booting the RoboRIO. This is automatically calculated based on the components below
        runArguments = ""                   // Arguments to add to robotCommand. No effect if robotCommand is manually set
    }

    // This block is entirely optional. If not specified, it will use the WPILib included with the WPILib Eclipse Plugins.
    wpi {
        git = true                          // Set to true to use Git instead of the local WPILib (github/wpilibsuite/allwpilib)
        gitVersion = "3784b66"              // Commit, Branch or Tag to checkout before building
    }

    components {
        // This is where you define your actual C++ binary
        my_program(FRCUserProgram) {
            targetPlatform "roborio-arm"        // Build using the RoboRIO Toolchain
            sources.cpp {
                source {
                    srcDirs "src"               // Where your .cpp files are stored
                }
                exportedHeaders {
                    srcDirs "include"           // Where your .h/.hpp files are stored
                }
                lib library: "wpilib", linkage: "static"    // Compile with WPILib
            }
        }
    }
}
```