package view

import bean.DeviceInfo
import event.*
import event.Observer
import model.DeviceModel
import org.slf4j.LoggerFactory
import utils.ConstCmd
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.event.EventListenerList
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

class DeviceList : JList<DeviceInfo>(), Observer<DeviceInfo>, IView {
    private val logger = LoggerFactory.getLogger(DeviceList::class.java)
    private val defaultMode = DefaultDeviceListModel()
    private var eventListener = EventListenerList()

    private val popupEditMenu = JPopupMenu()

    init {
        model = defaultMode
        selectionMode = ListSelectionModel.SINGLE_SELECTION

        val refreshItem = JMenuItem("Refresh")
        refreshItem.addActionListener {
            refreshDeviceList()
        }

        popupEditMenu.add(refreshItem)
    }

    private val mouseClick = object : MouseAdapter() {
        override fun mouseClicked(p0: MouseEvent) {
           if (p0.button == MouseEvent.BUTTON3) {
               popupEditMenu.show(p0.component, p0.x, p0.y)
           }
        }
    }

    private val listSelectionListener = object : ListSelectionListener {
        override fun valueChanged(e: ListSelectionEvent) {
            val event = CustomEvent(this, ConstCmd.CMD_SELECT_DEVICE)
            for (listener in eventListener.getListeners(CustomActionListener::class.java)) {
                listener.actionPerformed(event)
            }
        }
    }

    override fun registerListener() {
        logger.debug("registerListener")
        addMouseListener(mouseClick)
        addListSelectionListener(listSelectionListener)
    }

    override fun unregisterListener() {
        logger.debug("unregisterListener")
        removeMouseListener(mouseClick)
        removeListSelectionListener(listSelectionListener)
    }

    fun addCustomActionListener(l: CustomActionListener) {
        logger.debug("addCustomActionListener {}", l)
        eventListener.add(CustomActionListener::class.java, l)
    }

    fun removeCustomActionListener(l: CustomActionListener) {
        logger.debug("removeCustomActionListener {}", l)
        eventListener.remove(CustomActionListener::class.java, l)
    }

    private fun refreshDeviceList() {
        val event = CustomEvent(this, ConstCmd.CMD_REFRESH_DEVICES, null)
        for (listener in eventListener.getListeners(CustomActionListener::class.java)) {
            listener.actionPerformed(event)
        }
    }

    override fun update(s: ObservableSubject<DeviceInfo>) {
        logger.debug("update")
        if (s is DeviceModel) {
            defaultMode.setData(s.getData())
        }
    }

    inner class DefaultDeviceListModel : AbstractListModel<DeviceInfo>() {
        private var arData = LinkedList<DeviceInfo>()

        @Synchronized
        override fun getElementAt(p0: Int): DeviceInfo {
            return arData[p0]
        }

        @Synchronized
        override fun getSize(): Int {
            return arData.size
        }

        @Synchronized
        fun setData(data: List<DeviceInfo>) {
            arData.clear()
            arData.addAll(data)
            fireContentsChanged(this, 0, arData.size)
        }
    }
}