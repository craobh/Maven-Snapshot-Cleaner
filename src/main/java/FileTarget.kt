import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import java.io.File

/**
 * Represents a target file to be deleted
 */
class FileTarget(val file: File, size: Long, age: Int, delete: Boolean = true, val metadata: List<File> = listOf()) {
    val filePath = SimpleStringProperty(file.absolutePath)
    val size = SimpleLongProperty(size)
    val age = SimpleIntegerProperty(age)
    var delete = SimpleBooleanProperty(delete)

    fun filePathProperty(): SimpleStringProperty = filePath
    fun sizeProperty(): SimpleLongProperty = size
    fun ageProperty(): SimpleIntegerProperty = age
    fun deleteProperty(): SimpleBooleanProperty = delete
}