package view

import interfces.CustomActionListener
import interfces.CustomEvent
import interfces.IView
import org.slf4j.LoggerFactory
import utils.ConstCmd
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.EventListenerList


class MainMenuBar : JMenuBar(), IView {
    private val logger = LoggerFactory.getLogger(MainMenuBar::class.java)
    private var eventListener = EventListenerList()
    private val fileMenu = JMenu("File")
    private val configMenu = JMenu("Tools")
    private val helpMenus = JMenu("Help")
    private val openItem = JMenuItem("Open Log File")
    private val exitItem = JMenuItem("Exit")
    private val adbItem = JMenuItem("Config Tool Path")
    private val aboutItem = JMenuItem("About")

    init {
        openItem.mnemonic = KeyEvent.VK_O
        openItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK)
        fileMenu.add(openItem)
        exitItem.mnemonic = KeyEvent.VK_X
        exitItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK)
        fileMenu.add(exitItem)
        add(fileMenu)
        configMenu.add(adbItem)
        add(configMenu)
        helpMenus.add(aboutItem)
        add(helpMenus)
    }

    private val actionListener = ActionListener {
        when {
            it.source == openItem -> {
                logger.debug("openItem->click")
                updateEvent(ConstCmd.CMD_OPEN_FILE)
            }
            it.source == adbItem -> {
                logger.debug("adbItem->click")
                updateEvent(ConstCmd.CMD_CONFIG_ADB)
            }
            it.source == exitItem -> {
                logger.debug("exitItem->click")
                updateEvent(ConstCmd.CMD_EXIT_LOGDOG)
            }
            it.source == aboutItem -> {
                logger.debug("aboutItem->click")
                updateEvent(ConstCmd.CMD_ABOUT_LOGDOG)
            }
        }
    }

    override fun registerListener() {
        logger.debug("registerListener")
        openItem.addActionListener(actionListener)
        exitItem.addActionListener(actionListener)
        adbItem.addActionListener(actionListener)
        aboutItem.addActionListener(actionListener)
    }

    override fun unregisterListener() {
        logger.debug("unregisterListener")
        openItem.removeActionListener(actionListener)
        exitItem.removeActionListener(actionListener)
        adbItem.removeActionListener(actionListener)
        aboutItem.removeActionListener(actionListener)
    }

    fun addCustomActionListener(l: CustomActionListener) {
        logger.debug("addCustomActionListener $l")
        eventListener.add(CustomActionListener::class.java, l)
    }

    fun removeCustomActionListener(l: CustomActionListener) {
        logger.debug("removeCustomActionListener $l")
        eventListener.remove(CustomActionListener::class.java, l)
    }

    private fun updateEvent(action: String) {
        val event = CustomEvent(this, action)
        for (listener in eventListener.getListeners(CustomActionListener::class.java)) {
            listener.actionPerformed(event)
        }
    }
}