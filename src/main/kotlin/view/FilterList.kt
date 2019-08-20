package view

import bean.FilterInfo
import event.*
import event.Observer
import model.FilterMapModel
import org.slf4j.LoggerFactory
import utils.ConstCmd
import java.awt.Color
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.event.EventListenerList


class FilterList : JList<FilterInfo>(), Observer<FilterInfo>, IView {
    private val logger = LoggerFactory.getLogger(FilterList::class.java)

    private val defaultMode = DefaultFilterListModel()
    private var eventListener = EventListenerList()

    private val popupEditMenu = JPopupMenu()

    init {
        model = defaultMode
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = DefaultCellRenderer()

        val editItem = JMenuItem("Edit")
        editItem.addActionListener {
            if (selectedValue != null) {
                updateFilterData(selectedValue, ConstCmd.CMD_EDIT_FILTER_START)
            }
        }

        val removeItem = JMenuItem("Remove")
        removeItem.addActionListener {
            if (selectedValue != null) {
                updateFilterData(selectedValue, ConstCmd.CMD_DEL_FILTER)
                clearSelection()
            }
        }

        popupEditMenu.add(editItem)
        popupEditMenu.add(removeItem)
    }

    private val mouseClick = object : MouseAdapter() {
        override fun mouseClicked(p0: MouseEvent) {
            val index = locationToIndex(p0.point)
            val downMask = MouseEvent.BUTTON3_DOWN_MASK
            if (p0.modifiersEx and downMask != downMask) {
                selectedIndex = index
            }
            if (p0.button == MouseEvent.BUTTON1) {
                if (selectedValue != null) {
                    selectedValue.toggle()
                }
                val rect = getCellBounds(index, index)
                repaint(rect)
                updateFilterData(selectedValue, ConstCmd.CMD_ENABLE_FILTER)
            } else if (p0.button == MouseEvent.BUTTON3) {
                if (selectedValue != null) {
                    popupEditMenu.show(p0.component, p0.x, p0.y)
                }
            }
        }
    }

    override fun registerListener() {
        logger.debug("registerListener")
        addMouseListener(mouseClick)
    }

    override fun unregisterListener() {
        logger.debug("unregisterListener")
        removeMouseListener(mouseClick)
    }

    fun addCustomActionListener(l: CustomActionListener) {
        logger.debug("addCustomActionListener $l")
        eventListener.add(CustomActionListener::class.java, l)
    }

    fun removeCustomActionListener(l: CustomActionListener) {
        logger.debug("removeCustomActionListener $l")
        eventListener.remove(CustomActionListener::class.java, l)
    }

    private fun updateFilterData(data: FilterInfo?, str: String) {
        val event = CustomEvent(this, str, data)
        for (listener in eventListener.getListeners(CustomActionListener::class.java)) {
            listener.actionPerformed(event)
        }
    }

    override fun update(s: ObservableSubject<FilterInfo>) {
        logger.debug("update")
        if (s is FilterMapModel) {
            defaultMode.setData(s.getData())
        }
    }

    inner class DefaultCellRenderer : JCheckBox(), ListCellRenderer<FilterInfo> {
        override fun getListCellRendererComponent(p0: JList<out FilterInfo>, p1: FilterInfo, p2: Int, p3: Boolean, p4: Boolean): Component {
            text = p1.toString()
            isSelected = p1.enabled
            if (p3) {
                background = Color.BLUE
                foreground = Color.decode(p1.color)
            } else {
                background = Color.WHITE
                foreground = Color.decode(p1.color)
            }
            toolTipText = p1.detail()
            return this
        }
    }

    inner class DefaultFilterListModel : AbstractListModel<FilterInfo>() {
        private var arData = LinkedList<FilterInfo>()

        @Synchronized
        override fun getElementAt(p0: Int): FilterInfo {
            return arData[p0]
        }

        @Synchronized
        override fun getSize(): Int {
            return arData.size
        }

        @Synchronized
        fun setData(data: List<FilterInfo>) {
            arData.clear()
            arData.addAll(data)
            fireContentsChanged(this, 0, arData.size)
        }
    }
}