import javafx.concurrent.Service
import javafx.concurrent.Task
import java.io.File
import java.io.FileFilter
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.concurrent.LinkedBlockingQueue

/**
 * Service which recursively traverses the filesystem and finds potential files to be deleted
 */
class FileDiscoveryService(private val config: Configuration) : Service<Void>() {

    private lateinit var task: Task<Void>

    private val now = LocalDate.now()
    private val deleteCandidateFileFilter = DeleteCandidateFileFilter()
    val fileList = LinkedBlockingQueue<FileTarget>()

    override fun createTask(): Task<Void> {
        task = object : Task<Void>() {
            public override fun call(): Void? {

                val visitor = object: SimpleFileVisitor<Path>() {

                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?): FileVisitResult {
                        println("Scanning: $dir")
                        if (isCancelled) {
                            return FileVisitResult.TERMINATE
                        }

                        updateMessage(dir.toFile().canonicalPath)

                        return if (dir.toFile().canonicalPath.endsWith("-SNAPSHOT")) {
                            val files = dir.toFile().listFiles(deleteCandidateFileFilter)
                            val jars = files.filter { it.name.endsWith(".jar") }
                            jars.forEach { jar ->
                                val size = jar.length() / 1024
                                val fileName = jar.name.substringBefore(".jar")
                                val metadata = files.filter { !it.name.endsWith(".jar") && it.name.startsWith("$fileName.") }
                                val fileAge = getFileAge(jar)
                                val fileTarget = FileTarget(jar, size, fileAge, metadata = metadata)
                                fileList.add(fileTarget)
                            }
                            FileVisitResult.SKIP_SUBTREE
                        } else {
                            FileVisitResult.CONTINUE
                        }
                    }
                }

                Files.walkFileTree(Paths.get(config.path), visitor)
                return null
            }
        }
        return task
    }

    private fun getFileAge(file: File): Int {
        val lastModified = Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

        return Period.between(lastModified, now).days
    }

    /**
     * FileFilter which finds all but the latest snapshot jars (and the metadata files for each of them)
     */
    private class DeleteCandidateFileFilter : FileFilter {
        override fun accept(file: File): Boolean {
            return !file.name.contains("-SNAPSHOT") &&
                    (file.name.endsWith(".jar") || file.name.endsWith(".jar.sha1") ||
                            file.name.endsWith(".pom") || file.name.endsWith(".pom.sha1"))
        }
    }
}