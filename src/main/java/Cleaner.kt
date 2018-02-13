import javafx.application.Platform
import javafx.collections.ObservableList
import java.io.File
import java.io.FileFilter
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

class Cleaner constructor(val config: Configuration, val messageQueue: ObservableList<FileTarget>) {

    val now = LocalDate.now()
    val cleanedSize = 0
    val details = mutableListOf<String>()
    val directoryFilter = DirectoryFilter()
    val oldSnapshotFileFilter = OldSnapshotFileFilter()

    fun cleanMavenRepository(file: File) {
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
                    Platform.runLater({
                        // TODO batch up to prevent flooding with runnables
                        messageQueue.add(FileTarget(it.canonicalPath, size, ageInDays))
                    })
                    //messageQueue.add("About to remove directory $it.canonicalPath with total size $size and $ageInDays days old")
                    details.add("About to remove directory $it.canonicalPath with total size $size and $ageInDays days old")
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
            return !file.name.contains("-SNAPSHOT") && (file.name.endsWith(".jar") || file.name.endsWith(".jar.sha1") || file.name.endsWith(".pom") || file.name.endsWith(".pom.sha1"))
        }
    }
}