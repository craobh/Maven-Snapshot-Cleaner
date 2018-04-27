import javafx.concurrent.Task
import java.nio.file.Files
import java.nio.file.Paths

class DeleteTask(private val filesToDelete: List<FileTarget>, private val deleteMetadata: Boolean) : Task<Long>() {

    override fun call(): Long {
        var cleanedSize = 0L
        filesToDelete.forEach {
            Files.delete(Paths.get(it.file.toURI()))
            if (deleteMetadata) {
                it.metadata.forEach {
                    Files.delete(Paths.get(it.toURI()))
                }
            }
            cleanedSize += it.size.value
        }
        return cleanedSize
    }
}