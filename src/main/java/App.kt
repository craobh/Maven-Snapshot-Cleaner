import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Screen
import javafx.stage.Stage


fun main(args: Array<String>) {
    Application.launch(App::class.java, *args)
}

class App : Application() {

    override fun start(stage: Stage) {

        val root = FXMLLoader.load<Parent>(javaClass.getResource("main.fxml"))
        val visualBounds = Screen.getPrimary().visualBounds

        stage.title = "Maven Cleaner"
        stage.scene = Scene(root)

        stage.show()
    }
}
