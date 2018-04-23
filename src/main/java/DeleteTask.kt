import javafx.concurrent.Task
import java.nio.file.Files
import java.nio.file.Paths

class DeleteTask(private val filesToDelete: List<FileTarget>) : Task<Long>() {

    override fun call(): Long {
        var cleanedSize = 0L
        filesToDelete.forEach {
            //Files.delete(Paths.get(it.filePath.value))
            cleanedSize += it.size.value
        }
        return cleanedSize
    }
}