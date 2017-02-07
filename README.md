# GradleRIO-C
Managing FRC C++ Projects, the Gradle way. Build, Develop and Deploy your FRC Code on any system, without the need for eclipse or administrative priviledges.

Unlike the standard FRC Development Tools, GradleRIO-C can be run on any system, even if they're locked down (like school PCs). This includes installing
the C++ toolchain (with the exception of debian linux, which requires sudo and/or `apt` access). The only requirement is the Java Virtual Machine to run Gradle itself, a Command Line, and an Internet Connection.

## Commands
_Replace `gradlew` with `./gradlew` if you're on Mac or Linux_  
`gradlew install_frc_toolchain` will install the FRC C++ Toolchain, allowing you to build for the RoboRIO (note: while this is available on linux, it is best to install it yourself with the instructions [here](http://first.wpi.edu/FRC/roborio/toolchains/FRCLinuxToolchain2016.txt) as it requires sudo access)  
`gradlew build` will build your FRC Code  
`gradlew build deploy` will build and deploy your FRC Code. Can also be run without build as `gradlew deploy`  
`gradlew restart_rio_code` will restart user code running on the RoboRIO
  
`gradlew clion` will generate a CMakeLists.txt file for the Clion IDE. When running, make sure you're using the `<project>-build` or `<project>-debug` configuration, not `fake-<project>`.  
`gradlew cleanClion` will delete all Clion associated files (e.g. CMakeLists.txt, .idea and cmake-build-debug)  

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

    // This block is entirely optional. If not specified, it will use the WPILib from the WPILib Maven Repository and the default options
    // NOTE: If you plan to use WPI from a Git repo, the FRC Toolchain Bin directory must be on your System PATH. If you installed
    // on linux (or without GradleRIO), this is already done. If you installed on Windows or Mac with gradlew install_frc_toolchain,
    // you must set your path before building, as WPILib's Git Repo does not look in ~/.gradle/gradlerioc/toolchain/<platform>/bin
    // for the cross-compiling toolchain
    wpi {
        git = true                          // Set to true to use Git instead of the WPI Maven (github/wpilibsuite/allwpilib)
        gitVersion = "3784b66"              // Commit, Branch or Tag to checkout before building

        eclipsePlugins = true               // Set to true to use Local WPILib instead of the WPI Maven

        local = true                        // Set to true if you want to use the WPILib libraries from a local path on your filesystem
        localDirectory = file('libs/wpi')   // Set this to where you have extracted the WPILibC Zip file (subdirs: lib, include). Must be set if local is true

        // Maven is the default provider for the WPILib library
        mavenBranch = "development"         // Set which branch of the WPILib Maven to use. By default, this is 'release'
        mavenWpilib = "2017.1.1-beta-3"     // Set which version of WPILib to use from the Maven. By default, this is '+' (latest release)
        mavenHal    = "2017.1.1-beta-3"     // Set which version of the HAL to use from the Maven. By default, this is the same as mavenWpilib
        mavenWpiutil = "1.0.2"              // Set which version of WPIUtil to use from the Maven. By default, this is '+' (latest release)
        mavenNtcore = "3.1.2"               // Set which version of NTCore to use from the Maven. By default, this is '+' (latest release)
        mavenCscore = "0.9.1-beta-3"        // Set which version of CSCore to use from the Maven. By default, this is '+' (latest release)
    }

    // Use this block to add libraries that have been prebuilt (i.e. device libraries like NavX, or other code)
    // If you're building a library from source, it is better to add that as a dependency or as another component.
    // If the library uses GradleRIO, use the multi_project example. If it doesn't, you can add the sources as shown
    // in the custom_library example. 
    // This block is optional.
    libraries {
        navx(LibraryPrebuilt) {
            headers "navx/include"
            staticFile "navx/navx.a"
            sharedFile "navx/navx.so"
        }
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
                lib library: "navx", linkage: "static"      // From our libraries {} block
            }
        }
    }
}
```

## The Windows Subsystem For Linux
GradleRIO-C supports the Windows Subsystem for Linux, a Beta feature that can be run with Windows 10. For this to work, however, you need the following requirements:
- Windows Build 14986+ (Windows Creator Update, Available now with the Windows Insider Program)
- 4GB+ Physical Memory, 4GB - 8GB Windows Pagefile

The following packages are required to be installed on WSL assuming a clean image:
- `sudo apt-get install git build-essential`
- `sudo apt-get install gcc-multilib g++-multilib`
- Java 8 update 6+ (you can get the instructions [here, just ignore the gradle part if you want to use the gradle wrapper provided](https://github.com/Microsoft/BashOnWindows/issues/196#issuecomment-225305971))

Please note that, as of current, the Windows Subsystem for Linux does **NOT** support the FRC Toolchain. This may change for the 2017 season, however using the 2016 branch is not possible. Sorry.
