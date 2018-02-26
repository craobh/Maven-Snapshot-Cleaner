import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import java.io.File

/**
 * Represents a target file to be deleted
 */
class FileTarget(filePath: File, size: Long, age: Int, delete: Boolean = true) {
    val filePath = SimpleStringProperty(filePath.absolutePath)
    val size = SimpleLongProperty(size)
    val age = SimpleIntegerProperty(age)
    var delete = SimpleBooleanProperty(delete)

    fun filePathProperty(): SimpleStringProperty = filePath
    fun sizeProperty(): SimpleLongProperty = size
    fun ageProperty(): SimpleIntegerProperty = age
    fun deleteProperty(): SimpleBooleanProperty = delete
}