package view

import bean.CmdInfo
import interfces.*
import model.CmdModel
import org.slf4j.LoggerFactory
import utils.ConstCmd
import java.awt.event.ActionListener
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.event.EventListenerList

class CmdComboBox : JComboBox<String>(), Observer<CmdInfo>, IView {
    private val logger = LoggerFactory.getLogger(FilterList::class.java)
    private val defaultMode = DefaultCmdModel()
    private var eventListener = EventListenerList()

    init {
        model = defaultMode
    }

    private var actionListener = ActionListener {
        val event = CustomEvent(this, ConstCmd.CMD_SELECT_RUN)
        for (listener in eventListener.getListeners(CustomActionListener::class.java)) {
            listener.actionPerformed(event)
        }
    }

    fun addCustomActionListener(l: CustomActionListener) {
        logger.debug("addCustomActionListener $l")
        eventListener.add(CustomActionListener::class.java, l)
    }

    fun removeCustomActionListener(l: CustomActionListener) {
        logger.debug("removeCustomActionListener $l")
        eventListener.remove(CustomActionListener::class.java, l)
    }

    override fun update(s: ObservableSubject<CmdInfo>) {
        if (s is CmdModel) {
            defaultMode.setData(s.getData())
        }
        selectedIndex = 0
    }

    override fun registerListener() {
        addActionListener(actionListener)
    }

    override fun unregisterListener() {
        removeActionListener(actionListener)
    }

    inner class DefaultCmdModel : DefaultComboBoxModel<String>() {
        private var arData = ArrayList<CmdInfo>()

        @Synchronized
        override fun getElementAt(p0: Int): String {
            return arData[p0].title
        }

        @Synchronized
        override fun getSize(): Int {
            return arData.size
        }

        @Synchronized
        fun setData(data: List<CmdInfo>) {
            arData.clear()
            arData.addAll(data)
        }
    }
}