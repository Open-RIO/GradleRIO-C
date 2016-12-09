import org.gradle.language.nativeplatform.NativeResourceSet;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.base.internal.AbstractLanguageSourceSet;
import org.gradle.platform.base.BinarySpec;
import org.gradle.platform.base.GeneralComponentSpec;
import org.gradle.model.Managed;

@Managed
interface ToastResourceSpec extends GeneralComponentSpec { }

@Managed
interface ToastResourceBinary extends BinarySpec { 
    File getOutputDir()
    void setOutputDir(File outputDir)

    String getFilename()
    void setFilename(String filename)
}

@Managed
interface ToastResources extends LanguageSourceSet {
    File getBaseDir()
    void setBaseDir(File baseDir)
}