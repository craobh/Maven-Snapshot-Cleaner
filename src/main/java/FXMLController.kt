import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.layout.AnchorPane
import javafx.stage.DirectoryChooser
import javafx.util.Duration
import java.io.PrintWriter
import java.io.StringWriter


class FXMLController {

    @FXML
    lateinit var anchor: AnchorPane

    @FXML
    lateinit var startButton: Button

    @FXML
    lateinit var deleteButton: Button

    @FXML
    lateinit var progressBar: ProgressBar

    @FXML
    lateinit var m2PathField: TextField

    @FXML
    lateinit var dryRunCheckbox: CheckBox

    @FXML
    lateinit var filterJarsCheckbox: CheckBox

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

    private var messages: ObservableList<FileTarget> = FXCollections.observableArrayList<FileTarget>()
    var filteredMessages = FilteredList(messages, { true })

    fun initialize() {
        val config = Configuration()

        pathColumn.prefWidthProperty().bind(fileTable.widthProperty().subtract(deleteColumn.width).multiply(0.6))
        sizeColumn.prefWidthProperty().bind(fileTable.widthProperty().subtract(deleteColumn.width).multiply(0.2))
        ageColumn.prefWidthProperty().bind(fileTable.widthProperty().subtract(deleteColumn.width).multiply(0.2))

        deleteButton.isDisable = true

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

        filterJarsCheckbox.selectedProperty().addListener { _, _, selected ->
            run {
                filteredMessages.setPredicate { p ->
                    if (selected) {
                        p.filePath.value.endsWith(".jar")
                    } else {
                        true
                    }
                }
            }
        }

        m2PathField.text = config.path
        m2PathField.setOnMouseClicked {
            val directoryChooser = DirectoryChooser()
            directoryChooser.title = "Select .m2 folder"
            val selectedFile = directoryChooser.showDialog(anchor.scene.window)?.absolutePath ?: config.path
            m2PathField.text = selectedFile
            config.path = selectedFile
        }

        deleteColumn.cellFactory = CheckBoxTableCell.forTableColumn(deleteColumn)

        // Handle select all rows
        val selectAllCheckBox = CheckBox()
        selectAllCheckBox.isSelected = true
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
                deleteButton.isDisable = true
                messages.clear()
                fileDiscoveryService.restart()
                updateFileListService.restart()
            } else {
                println("Stopping!")
                startButton.text = "Start"
                if (!messages.none { it.delete.value }) {
                    deleteButton.isDisable = false
                }
                fileDiscoveryService.cancel()
                updateFileListService.cancel()
            }
        }

        fileDiscoveryService.setOnSucceeded {
            startButton.text = "Start"
            if (!messages.none { it.delete.value }) {
                deleteButton.isDisable = false
            }
        }

        deleteButton.onAction = EventHandler<ActionEvent> {
            println("Delete clicked")
            val confirmationAlert = Alert(Alert.AlertType.CONFIRMATION)
            confirmationAlert.title = "Confirm Delete"
            confirmationAlert.headerText = "Space to be cleaned: ${calculateCleanSize()}KiB"
            confirmationAlert.contentText = "Are you sure you want to delete the selected files? This operation cannot be undone."

            val confirmationResult = confirmationAlert.showAndWait()
            if (confirmationResult.get() == ButtonType.OK) {
                val deleteTask = DeleteTask(messages.filter { it.delete.value })

                deleteTask.setOnSucceeded {
                    val alert = Alert(Alert.AlertType.INFORMATION)
                    alert.title = "Clean complete"
                    alert.headerText = null
                    alert.contentText = "Cleaned " + deleteTask.get() + "KB"
                    alert.show()
                    messages.clear()
                }

                deleteTask.setOnFailed {
                    val exception = deleteTask.exception
                    val alert = Alert(Alert.AlertType.ERROR)
                    alert.title = "Error"
                    alert.headerText = "An error occurred during cleaning.\nMessage: " + exception.message
                    alert.contentText = "Stacktrace:"

                    val stringWriter = StringWriter()
                    val printWriter = PrintWriter(stringWriter)
                    exception.printStackTrace(printWriter)
                    val exceptionText = stringWriter.toString()

                    val textArea = TextArea(exceptionText)
                    textArea.isEditable = false
                    textArea.isWrapText = true
                    textArea.maxWidth = Double.MAX_VALUE
                    textArea.maxHeight = Double.MAX_VALUE

                    alert.dialogPane.expandableContent = textArea
                    alert.showAndWait()
                }

                deleteTask.run()
            }
        }
    }

    private fun calculateCleanSize(): String {
        return messages.filter { it.delete.value }
                .map { it.size.intValue() }
                .sum()
                .toString()
    }
}