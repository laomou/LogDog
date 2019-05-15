import controller.MainController
import javax.swing.SwingUtilities

fun main(args: Array<String>) {
    val mainController = MainController()
    SwingUtilities.invokeLater {
        mainController.launcher()
    }
}