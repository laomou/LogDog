package view

import bean.ConstCmd
import bean.FilterContainer
import interfces.*
import interfces.Observer
import model.FilterModel
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.event.EventListenerList


class FilterList : JList<FilterContainer>(), Observer<FilterContainer>, IView {
    private val logger = LoggerFactory.getLogger(FilterList::class.java)

    private val defaultMode = DefaultFilterListModel()
    private var eventlisteners = EventListenerList()

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
            if (p0.button == MouseEvent.BUTTON1) {
                if (selectedValue != null) {
                    selectedValue.toggle()
                    val index = locationToIndex(p0.point)
                    val rect = getCellBounds(index, index)
                    repaint(rect)
                    updateTableData()
                }
            } else if (p0.button == MouseEvent.BUTTON3) {
                if (selectedValue != null) {
                    popupEditMenu.show(p0.component, p0.x, p0.y)
                }
            }
        }
    }

    override fun initListener() {
        logger.debug("initListener")
        addMouseListener(mouseClick)
    }

    override fun deinitListenr() {
        logger.debug("deinitListenr")
        removeMouseListener(mouseClick)
    }

    fun addCustomActionListener(l: CustomActionListener) {
        logger.debug("addCustomActionListener $l")
        eventlisteners.add(CustomActionListener::class.java, l)
    }

    fun removeCustomActionListener(l: CustomActionListener) {
        logger.debug("removeCustomActionListener $l")
        eventlisteners.remove(CustomActionListener::class.java, l)
    }

    private fun updateTableData() {
        val event = CustomEvent(this, ConstCmd.CMD_RUN_FILTER)
        for (listener in eventlisteners.getListeners(CustomActionListener::class.java)) {
            listener.actionPerformed(event)
        }
    }

    private fun updateFilterData(data: FilterContainer?, str: String) {
        val event = CustomEvent(this, str)
        event.objectValue = data
        for (listener in eventlisteners.getListeners(CustomActionListener::class.java)) {
            listener.actionPerformed(event)
        }
    }

    override fun update(s: ObservableSubject<FilterContainer>) {
        logger.debug("update")
        if (s is FilterModel) {
            defaultMode.setData(s.getData())
        }
    }

    inner class DefaultCellRenderer : JCheckBox(), ListCellRenderer<FilterContainer> {
        override fun getListCellRendererComponent(p0: JList<out FilterContainer>, p1: FilterContainer, p2: Int, p3: Boolean, p4: Boolean): Component {
            text = p1.toString()
            isSelected = p1.enabled
            if (p3) {
                background = p0.selectionBackground
                foreground = Color.decode(p1.color)
            } else {
                background = p0.background
                foreground = Color.decode(p1.color)
            }
            toolTipText = p1.detail()
            return this
        }
    }

    inner class DefaultFilterListModel : AbstractListModel<FilterContainer>() {
        private var arData = ArrayList<FilterContainer>()

        @Synchronized
        override fun getElementAt(p0: Int): FilterContainer {
            return arData[p0]
        }

        @Synchronized
        override fun getSize(): Int {
            return arData.size
        }

        @Synchronized
        fun setData(data: ArrayList<FilterContainer>) {
            arData.clear()
            arData.addAll(data)
            fireContentsChanged(this, 0, arData.size)
        }
    }
}