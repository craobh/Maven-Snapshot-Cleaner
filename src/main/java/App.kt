import javafx.application.Application
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.stage.Stage
import java.io.File


fun main(args: Array<String>) {
    Application.launch(App::class.java, *args)
}

class App : Application() {

    private val config = Configuration()

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Hello World!"
        val btn = Button()
        btn.text = "Say 'Hello World'"

        val dryRunCheckbox = CheckBox("Dry Run")
        dryRunCheckbox.isSelected = config.dryRun
        dryRunCheckbox.selectedProperty().addListener({ _, _, newValue -> config.dryRun = newValue })

        val homeFolder = TextField()
        val homeFolderLabel = Label("m2 Folder")
        homeFolder.text = config.path

        btn.onAction = EventHandler<ActionEvent> { println("Hello World!") }

        val root = BorderPane()

        val topBox = HBox()
        topBox.children.addAll(btn, homeFolderLabel, homeFolder)
        topBox.children.addAll(dryRunCheckbox)

        val statusPane = HBox()
        val messages = FXCollections.observableArrayList<FileTarget>()
        val statusField = TableView<FileTarget>(messages)

        val pathColumn = TableColumn<FileTarget, String>("File Path")
        pathColumn.setCellValueFactory { target -> SimpleStringProperty(target.value.path) }
        pathColumn.prefWidthProperty().bind(statusField.widthProperty().multiply(0.8))

        val sizeColumn = TableColumn<FileTarget, Long>("Size (KB)")
        sizeColumn.setCellValueFactory { target -> SimpleLongProperty(target.value.size).asObject() }
        sizeColumn.prefWidthProperty().bind(statusField.widthProperty().multiply(0.1))

        val ageColumn = TableColumn<FileTarget, Int>("Age (days)")
        ageColumn.setCellValueFactory { target -> SimpleIntegerProperty(target.value.age).asObject() }
        sizeColumn.prefWidthProperty().bind(statusField.widthProperty().multiply(0.1))

        statusField.columns.addAll(pathColumn, sizeColumn, ageColumn)
        HBox.setHgrow(statusField, Priority.ALWAYS)
        statusPane.children.add(statusField)


        root.top = topBox
        root.bottom = statusPane

        primaryStage.scene = Scene(root, 1000.0, 250.0)
        primaryStage.show()

//        val messageQueue = LinkedBlockingQueue<String>()
//        Platform.runLater({
//            val mainDir = File(config.path)
//            val cleaner = main.java.Cleaner(config, messages)
//            cleaner.cleanMavenRepository(mainDir)
//            //main.java.MessageConsumer(messageQueue, statusField).start()
//        })

        val task = object : Task<Void>() {
            public override fun call(): Void? {
                val mainDir = File(config.path)
                val cleaner = Cleaner(config, messages)
                cleaner.cleanMavenRepository(mainDir)
                return null
            }
        }

        task.setOnFailed({ _ ->
            System.err.println("The task failed with the following exception:")
            task.exception.printStackTrace(System.err)
        })


        val thread = Thread(task)
        thread.isDaemon = true
        thread.start()
    }
}
