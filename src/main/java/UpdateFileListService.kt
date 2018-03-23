import javafx.concurrent.ScheduledService
import javafx.concurrent.Task
import java.util.concurrent.LinkedBlockingQueue

class UpdateFileListService(val cleaner: Cleaner) : ScheduledService<LinkedBlockingQueue<FileTarget>>() {

    override fun createTask(): Task<LinkedBlockingQueue<FileTarget>> {
        return object : Task<LinkedBlockingQueue<FileTarget>>() {
            public override fun call(): LinkedBlockingQueue<FileTarget> {
                return cleaner.fileList
            }
        }
    }
}