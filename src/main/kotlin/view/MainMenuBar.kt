package view

import interfces.CustomActionListener
import interfces.CustomEvent
import interfces.IView
import org.slf4j.LoggerFactory
import java.awt.event.ActionListener
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.event.EventListenerList


class MainMenuBar : JMenuBar(), IView {
    private val logger = LoggerFactory.getLogger(MainMenuBar::class.java)

    private var eventlisteners = EventListenerList()

    val fileMenu = JMenu("File")
    val helpMenus = JMenu("Help")
    val openItem = JMenuItem("Open")
    val adbItem = JMenuItem("Log Tool")

    init {
        fileMenu.add(openItem)
        fileMenu.add(adbItem)
        add(fileMenu)
        add(helpMenus)
    }

    private val actionListener = ActionListener {
        if (it.source == openItem) {
            logger.debug("openItem->click")
            val action = CustomEvent(this, "CMD_OPEN_FILE")
            for (listener in eventlisteners.getListeners(CustomActionListener::class.java)) {
                listener.actionPerformed(action)
            }
        } else if (it.source == adbItem) {
            logger.debug("settingsItem->click")
            val action = CustomEvent(this, "CMD_CONFIG_ADB")
            for (listener in eventlisteners.getListeners(CustomActionListener::class.java)) {
                listener.actionPerformed(action)
            }
        }
    }

    override fun initListener() {
        logger.debug("initListener")
        openItem.addActionListener(actionListener)
        adbItem.addActionListener(actionListener)
    }

    override fun deinitListenr() {
        logger.debug("deinitListenr")
        openItem.removeActionListener(actionListener)
        adbItem.removeActionListener(actionListener)
    }

    fun addCustomActionListener(l: CustomActionListener) {
        logger.debug("addCustomActionListener " + l)
        eventlisteners.add(CustomActionListener::class.java, l)
    }

    fun removeCustomActionListener(l: CustomActionListener) {
        logger.debug("removeCustomActionListener " + l)
        eventlisteners.remove(CustomActionListener::class.java, l)
    }
}