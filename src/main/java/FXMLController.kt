import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.cell.CheckBoxTableCell


class FXMLController {

    @FXML
    lateinit var startButton: Button

    @FXML
    lateinit var progressBar: ProgressBar

    @FXML
    lateinit var m2PathField: TextField

    @FXML
    lateinit var dryRunCheckbox: CheckBox

    @FXML
    lateinit var fileTable: TableView<FileTarget>

    @FXML
    lateinit var pathColumn: TableColumn<FileTarget, String>

    @FXML
    lateinit var sizeColumn: TableColumn<FileTarget, Long>

    @FXML
    lateinit var ageColumn: TableColumn<FileTarget, Int>

    @FXML
    lateinit var deleteColumn: TableColumn<FileTarget, Boolean>

    var messages: ObservableList<FileTarget> = FXCollections.observableArrayList<FileTarget>()

    private val cleanerTask = object : Task<Void>() {
        public override fun call(): Void? {
            cleaner.start()
            return null
        }
    }

    private val updateFileListTask = object : Task<Void>() {
        public override fun call(): Void? {

            while (!isCancelled) {
                println("Draining ${cleaner.fileList.size} messages")
                Platform.runLater {
                    cleaner.fileList.drainTo(messages)
                }
                Thread.sleep(100)
            }
            // Drain any pending messages discovered while sleeping
            Platform.runLater {
                cleaner.fileList.drainTo(messages)
            }
            return null
        }
    }

    private lateinit var cleaner: Cleaner

    private val cleanerThread = Thread(cleanerTask)
    private val updateFileListThread = Thread(updateFileListTask)

    fun initialize() {
        val config = Configuration()
        cleaner = Cleaner(config)

        progressBar.visibleProperty().bind(cleanerTask.runningProperty())

        dryRunCheckbox.isSelected = config.dryRun
        dryRunCheckbox.selectedProperty().addListener { _, _, newValue -> config.dryRun = newValue }

        m2PathField.text = config.path

        cleanerThread.isDaemon = true
        updateFileListThread.isDaemon = true

        deleteColumn.cellFactory = CheckBoxTableCell.forTableColumn(deleteColumn)

        // Handle select all rows
        val selectAllCheckBox = CheckBox()
        deleteColumn.graphic = selectAllCheckBox
        selectAllCheckBox.onAction = EventHandler<ActionEvent> {
            if (selectAllCheckBox.isSelected) {
                messages.map { row -> row.delete.set(true) }
            } else {
                messages.map { row -> row.delete.set(false) }
            }
        }

        startButton.onAction = EventHandler<ActionEvent> {
            println("Start clicked!")
            if (!cleanerThread.isAlive) {
                println("Starting!")
                startButton.text = "Stop"
                cleanerThread.start()
                updateFileListThread.start()
            } else {
                println("Stopping!")
                // TODO kill thread some other way than Thread.stop()
                startButton.text = "Start"
                cleanerThread.stop()
                updateFileListTask.cancel()
            }
        }
    }
}