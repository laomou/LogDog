package view

import bean.FilterContainer
import interfces.*
import model.FilterModel
import org.slf4j.LoggerFactory
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.EventListenerList


class FilterList : JList<FilterContainer>(), Observer<FilterContainer>, IView {
    private val logger = LoggerFactory.getLogger(FilterList::class.java)

    private val defaultMode = DefaultFilterListModel()
    private var eventlisteners = EventListenerList()

    init {
        model = defaultMode
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        cellRenderer = DefaultCellRenderer()
    }

    private val mouseClick = object : MouseAdapter() {
        override fun mouseClicked(p0: MouseEvent?) {
            val index = locationToIndex(p0?.point)
            val value = model.getElementAt(index)
            value.toggle()
            val rect = getCellBounds(index, index)
            repaint(rect)
            updateFilterData()
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
        logger.debug("addCustomActionListener " + l)
        eventlisteners.add(CustomActionListener::class.java, l)
    }

    fun removeCustomActionListener(l: CustomActionListener) {
        logger.debug("removeCustomActionListener " + l)
        eventlisteners.remove(CustomActionListener::class.java, l)
    }

    private fun updateFilterData() {
        val event = CustomEvent(this, "CMD_RUN_FILTER")
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
                foreground = p0.selectionForeground
            } else {
                background = p0.background
                foreground = p0.foreground
            }
            toolTipText = p1.detail()
            return this
        }
    }

    inner class DefaultFilterListModel : AbstractListModel<FilterContainer>() {
        private var arData = ArrayList<FilterContainer>()
        override fun getElementAt(p0: Int): FilterContainer {
            return arData[p0]
        }

        override fun getSize(): Int {
            return arData.size
        }

        fun setData(data: ArrayList<FilterContainer>) {
            arData.clear()
            arData.addAll(data)
            fireContentsChanged(this, 0, arData.size)
        }
    }
}