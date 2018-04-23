import javafx.concurrent.Service
import javafx.concurrent.Task
import java.io.File
import java.io.FileFilter
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.concurrent.LinkedBlockingQueue

/**
 * Service which recursively traverses the filesystem and finds potential files to be deleted
 */
class FileDiscoveryService(val config: Configuration) : Service<Void>() {

    private lateinit var task: Task<Void>

    private val now = LocalDate.now()
    private val directoryFilter = DirectoryFilter()
    private val oldSnapshotFileFilter = OldSnapshotFileFilter()
    val fileList = LinkedBlockingQueue<FileTarget>()

    override fun createTask(): Task<Void> {
        task = object : Task<Void>() {
            public override fun call(): Void? {
                traverseFolder(File(config.path))
                return null
            }
        }
        return task
    }

    private fun traverseFolder(file: File) {
        if (task.isCancelled) {
            return
        }

        val lastModified = Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

        val ageInDays = Period.between(lastModified, now).days
        val directories = file.listFiles(directoryFilter)

        if (directories.isNotEmpty()) {
            directories.forEach {
                if (task.isCancelled) {
                    return
                }
                traverseFolder(it)
            }
        } else {
            if (file.canonicalPath.endsWith("-SNAPSHOT")) {
                val files = file.listFiles(oldSnapshotFileFilter)
                files.forEach {
                    if (task.isCancelled) {
                        return
                    }
                    val size = file.length() / 1024
                    fileList.add(FileTarget(it, size, ageInDays))
                    println("Adding ${it.name}")
                }
            }
        }
    }

    /**
     * FileFilter which only accepts directories
     */
    class DirectoryFilter : FileFilter {
        override fun accept(pathname: File): Boolean {
            return pathname.isDirectory
        }
    }

    /**
     * FileFilter which only accepts directories
     */
    class OldSnapshotFileFilter : FileFilter {
        override fun accept(file: File): Boolean {
            return !file.name.contains("-SNAPSHOT") &&
                    (file.name.endsWith(".jar") || file.name.endsWith(".jar.sha1") ||
                            file.name.endsWith(".pom") || file.name.endsWith(".pom.sha1"))
        }
    }
}