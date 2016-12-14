import org.gradle.nativeplatform.*
import org.gradle.nativeplatform.internal.*

// There may be properties added to this component at some point, but at the moment
// it serves as an alias for NativeExecutableSpec, as well as a way to automate
// generation of robotCommand
interface FRCUserProgram extends NativeExecutableSpec { }

class DefaultFRCUserProgram extends DefaultNativeExecutableSpec implements FRCUserProgram { }