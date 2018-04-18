import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
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
import javafx.util.Duration


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

    fun initialize() {
        val config = Configuration()

        val fileDiscoveryService = FileDiscoveryService(config)
        fileDiscoveryService.setOnCancelled {
            fileDiscoveryService.reset()
        }

        val updateFileListService = UpdateFileListService(fileDiscoveryService)
        updateFileListService.period = Duration.millis(500.0)
        updateFileListService.delay = Duration.millis(500.0)
        updateFileListService.setOnSucceeded { _ ->
            Platform.runLater {
                updateFileListService.lastValue.drainTo(messages)
            }
        }
        updateFileListService.setOnCancelled {
            updateFileListService.reset()
        }

        progressBar.visibleProperty().bind(fileDiscoveryService.runningProperty())

        dryRunCheckbox.isSelected = config.dryRun
        dryRunCheckbox.selectedProperty().addListener { _, _, newValue -> config.dryRun = newValue }

        m2PathField.text = config.path

        deleteColumn.cellFactory = CheckBoxTableCell.forTableColumn(deleteColumn)

        // Handle select all rows
        val selectAllCheckBox = CheckBox()
        deleteColumn.graphic = selectAllCheckBox
        selectAllCheckBox.onAction = EventHandler<ActionEvent> {
            if (selectAllCheckBox.isSelected) {
                messages.forEach { row -> row.delete.set(true) }
            } else {
                messages.forEach { row -> row.delete.set(false) }
            }
        }

        startButton.onAction = EventHandler<ActionEvent> {
            println("Start clicked!")
            if (startButton.text == "Start") {
                startButton.text = "Stop"
                messages.clear()
                fileDiscoveryService.start()
                updateFileListService.start()
            } else {
                println("Stopping!")
                startButton.text = "Start"
                fileDiscoveryService.cancel()
                updateFileListService.cancel()
            }
        }
    }
}