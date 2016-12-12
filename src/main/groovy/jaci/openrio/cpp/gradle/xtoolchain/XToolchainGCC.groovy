package jaci.openrio.cpp.gradle.xtoolchain

import org.gradle.api.*
import org.gradle.api.internal.file.FileResolver
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.internal.NativePlatformInternal 
import org.gradle.nativeplatform.platform.internal.ArchitectureInternal
import org.gradle.nativeplatform.toolchain.internal.gcc.*
import org.gradle.nativeplatform.toolchain.internal.*
import org.gradle.process.internal.ExecActionFactory
import org.gradle.nativeplatform.toolchain.internal.gcc.version.CompilerMetaDataProviderFactory
import org.gradle.internal.operations.BuildOperationProcessor
import org.gradle.internal.reflect.Instantiator
import org.gradle.nativeplatform.toolchain.GccPlatformToolChain

public class XToolchainGCC extends GccToolChain {
    public XToolchainGCC(Instantiator instantiator, String name, BuildOperationProcessor buildOperationProcessor, OperatingSystem operatingSystem, FileResolver fileResolver, ExecActionFactory execActionFactory, CompilerMetaDataProviderFactory metaDataProviderFactory) {
        super(instantiator, name, buildOperationProcessor, operatingSystem, fileResolver, execActionFactory, metaDataProviderFactory)

        target("roborio-arm", new Action<GccPlatformToolChain>() {
            @Override
            public void execute(GccPlatformToolChain target) {
                String gccPrefix = "arm-frc-linux-gnueabi-";
                String gccSuffix = OperatingSystem.current().isWindows() ? ".exe" : ""
                target.cCompiler.executable =           gccPrefix + "gcc" + gccSuffix;
                target.cppCompiler.executable =         gccPrefix + "g++" + gccSuffix;
                target.linker.executable =              gccPrefix + "g++" + gccSuffix;
                target.assembler.executable =           gccPrefix + "as"  + gccSuffix;
                target.staticLibArchiver.executable =   gccPrefix + "ar"  + gccSuffix;
            }
        })

        def bindir = new File(XToolchainPlugin.getActiveToolchain().get_toolchain_root(), "bin").absolutePath
        path(bindir)
    }

    @Override
    protected String getTypeName() {
        return "RoboRioArmGcc";
    }
}