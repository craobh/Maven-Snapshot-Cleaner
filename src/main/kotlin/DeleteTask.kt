import javafx.concurrent.Task
import java.nio.file.Files
import java.nio.file.Paths

class DeleteTask(private val filesToDelete: List<FileTarget>, private val deleteMetadata: Boolean) : Task<Long>() {

    override fun call(): Long {
        var cleanedSize = 0L
        filesToDelete.forEach { file ->
            Files.delete(Paths.get(file.file.toURI()))
            if (deleteMetadata) {
                file.metadata.forEach {
                    Files.delete(Paths.get(it.toURI()))
                }
            }
            cleanedSize += file.size.value
        }
        return cleanedSize
    }
}