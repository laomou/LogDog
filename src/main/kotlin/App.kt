import controller.MainController
import org.slf4j.LoggerFactory
import javax.swing.SwingUtilities

private val logger = LoggerFactory.getLogger("App")

fun main(args: Array<String>) {
    logger.debug("app start")
    val mainController = MainController()
    SwingUtilities.invokeLater({
        mainController.launcher()
    })
}