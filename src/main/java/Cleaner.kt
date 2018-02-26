import java.io.File
import java.io.FileFilter
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.concurrent.LinkedBlockingQueue

class Cleaner(val config: Configuration) {

    val now = LocalDate.now()
    val cleanedSize = 0
    val details = mutableListOf<String>()
    val directoryFilter = DirectoryFilter()
    val oldSnapshotFileFilter = OldSnapshotFileFilter()
    val fileList = LinkedBlockingQueue<FileTarget>()

    fun start() {
        cleanMavenRepository(File(config.path))
    }

    private fun cleanMavenRepository(file: File) {
        val lastModified = Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

        val ageInDays = Period.between(lastModified, now).days
        val directories = file.listFiles(directoryFilter)

        if (directories.isNotEmpty()) {
            directories.forEach {
                cleanMavenRepository(it)
            }
        } else {
            if (file.canonicalPath.endsWith("-SNAPSHOT")) {
                val files = file.listFiles(oldSnapshotFileFilter)
                files.forEach {
                    val size = removeFileAndReturnFreedKBytes(it)
                    fileList.add(FileTarget(it, size, ageInDays))
                    println("Adding ${it.name}")
                    details.add("About to remove directory ${it.canonicalPath} with total size $size and $ageInDays days old")
                }
            }
        }
    }

    private fun removeFileAndReturnFreedKBytes(file: File): Long {
        val size = file.length() / 1024

        if (!config.dryRun) {
            //file.delete()
        }
        return size
    }

    class DirectoryFilter : FileFilter {
        override fun accept(pathname: File): Boolean {
            return pathname.isDirectory
        }
    }

    class OldSnapshotFileFilter : FileFilter {
        override fun accept(file: File): Boolean {
            return !file.name.contains("-SNAPSHOT") &&
                    (file.name.endsWith(".jar") ||
                            file.name.endsWith(".jar.sha1") ||
                            file.name.endsWith(".pom") ||
                            file.name.endsWith(".pom.sha1")
                            )
        }
    }
}