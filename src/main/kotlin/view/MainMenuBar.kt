package view

import bean.ConstCmd
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
    private val fileMenu = JMenu("File")
    private val helpMenus = JMenu("Help")
    private val openItem = JMenuItem("Open...")
    private val adbItem = JMenuItem("Log Tool Path")
    private val aboutItem = JMenuItem("About")

    init {
        fileMenu.add(openItem)
        fileMenu.add(adbItem)
        add(fileMenu)
        helpMenus.add(aboutItem)
        add(helpMenus)
    }

    private val actionListener = ActionListener {
        when {
            it.source == openItem -> {
                logger.debug("openItem->click")
                val action = CustomEvent(this, ConstCmd.CMD_OPEN_FILE)
                for (listener in eventlisteners.getListeners(CustomActionListener::class.java)) {
                    listener.actionPerformed(action)
                }
            }
            it.source == adbItem -> {
                logger.debug("adbItem->click")
                val action = CustomEvent(this, ConstCmd.CMD_CONFIG_ADB)
                for (listener in eventlisteners.getListeners(CustomActionListener::class.java)) {
                    listener.actionPerformed(action)
                }
            }
            it.source == aboutItem -> {
                logger.debug("aboutItem->click")
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
        logger.debug("addCustomActionListener $l")
        eventlisteners.add(CustomActionListener::class.java, l)
    }

    fun removeCustomActionListener(l: CustomActionListener) {
        logger.debug("removeCustomActionListener $l")
        eventlisteners.remove(CustomActionListener::class.java, l)
    }
}