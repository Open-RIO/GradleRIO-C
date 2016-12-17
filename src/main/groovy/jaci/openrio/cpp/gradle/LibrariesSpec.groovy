import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.file.SourceDirectorySet
import org.gradle.model.Managed
import org.gradle.model.ModelMap
import org.gradle.language.cpp.CppSourceSet

@Managed
interface LibraryBase { }

@Managed
interface LibraryPrebuilt extends LibraryBase {
    File getHeaders()
    void setHeaders(File file)

    File getStaticFile()
    void setStaticFile(File file)

    File getSharedFile()
    void setSharedFile(File file)
}

@Managed
interface LibrariesSpec extends ModelMap<LibraryBase> {
}