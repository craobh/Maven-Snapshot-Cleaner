import javafx.concurrent.Service
import javafx.concurrent.Task

class CleanerService(val cleaner: Cleaner) : Service<Void>() {

    override fun createTask(): Task<Void> {
        return object : Task<Void>() {
            public override fun call(): Void? {
                cleaner.cancelled = false
                cleaner.start()
                return null
            }
        }
    }

    override fun cancel(): Boolean {
        cleaner.cancelled = true
        return super.cancel()
    }
}
