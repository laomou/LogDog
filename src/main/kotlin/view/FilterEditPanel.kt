package view

import bean.ColorInfo
import bean.FilterInfo
import interfces.*
import model.FilterEditModel
import org.slf4j.LoggerFactory
import utils.ConstCmd
import utils.DefaultConfig
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.GridLayout
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.EventListenerList

class FilterEditPanel : JPanel(), Observer<ColorInfo>, IView {
    private val logger = LoggerFactory.getLogger(FilterEditPanel::class.java)
    private var eventListener = EventListenerList()

    private val rbContains = JRadioButton("Contains")
    private val rbRegex = JRadioButton("Match")
    private val bgType = ButtonGroup()
    private val tfText = JTextField()
    private val cbColor = JComboBox<ColorInfo>()

    private val btnClean = JButton()
    private val btnOk = JButton()

    private val defaultMode = DefaultColorModel()

    private var iFilterType = 1
    private var strColor = DefaultConfig.DEFAULT_BG_COLOR
    private var strText = ""
    private var strUuid = ""
    private var bEnable = true
    private var newFilterInfo = true

    init {
        layout = BorderLayout()

        val jpEditPane = JPanel(GridLayout(3, 1))
        jpEditPane.border = BorderFactory.createTitledBorder("Filter Edit")

        bgType.add(rbContains)
        bgType.add(rbRegex)
        rbContains.isSelected = true

        val jpColor = JPanel(BorderLayout())
        val jlColor = JLabel()
        jlColor.text = "Color :"
        jpColor.add(jlColor, BorderLayout.WEST)
        cbColor.model = defaultMode
        cbColor.renderer = DefaultCellRenderer()
        jpColor.add(cbColor, BorderLayout.CENTER)

        val jpText = JPanel(BorderLayout())
        val jlText = JLabel()
        jlText.text = " Text :"
        jpText.add(jlText, BorderLayout.WEST)
        jpText.add(tfText, BorderLayout.CENTER)

        btnOk.text = " OK "
        btnOk.addActionListener {
            if (strText.isEmpty()) {
                return@addActionListener
            }
            if (newFilterInfo) {
                updateFilterData(formatNewFilterData(), ConstCmd.CMD_ADD_FILTER)
            } else {
                updateFilterData(formatFilterData(), ConstCmd.CMD_EDIT_FILTER_END)
            }
            tfText.text = ""
            newFilterInfo = true
        }

        btnClean.text = "Clean"
        btnClean.addActionListener {
            tfText.text = ""
            bEnable = true
            newFilterInfo = true
        }

        val jpBtn = JPanel(GridLayout(1, 4))
        jpBtn.add(rbContains)
        jpBtn.add(rbRegex)
        jpBtn.add(btnClean)
        jpBtn.add(btnOk)

        jpEditPane.add(jpColor)
        jpEditPane.add(jpText)
        jpEditPane.add(jpBtn)

        add(jpEditPane, BorderLayout.CENTER)
    }

    override fun update(s: ObservableSubject<ColorInfo>) {
        logger.debug("update")
        if (s is FilterEditModel) {
            defaultMode.setData(s.getData())
        }
        cbColor.selectedIndex = 0
    }

    override fun registerListener() {
        logger.debug("registerListener")
        tfText.document.addDocumentListener(dlListener)
        rbContains.addActionListener {
            iFilterType = 1
        }
        rbRegex.addActionListener {
            iFilterType = 2
        }
        cbColor.addItemListener(itemListener)
    }

    override fun unregisterListener() {
        logger.debug("unregisterListener")
        tfText.document.removeDocumentListener(dlListener)

        cbColor.removeItemListener(itemListener)
    }

    fun addCustomActionListener(l: CustomActionListener) {
        logger.debug("addCustomActionListener $l")
        eventListener.add(CustomActionListener::class.java, l)
    }

    fun removeCustomActionListener(l: CustomActionListener) {
        logger.debug("removeCustomActionListener $l")
        eventListener.remove(CustomActionListener::class.java, l)
    }

    private fun formatNewFilterData(): FilterInfo {
        val data = FilterInfo()
        data.text = strText
        data.enabled = bEnable
        data.color = strColor
        data.type = iFilterType
        return data
    }

    private fun formatFilterData(): FilterInfo {
        val data = FilterInfo(strUuid)
        data.text = strText
        data.enabled = bEnable
        data.color = strColor
        data.type = iFilterType
        return data
    }

    private fun updateFilterData(data: FilterInfo?, str: String) {
        val event = CustomEvent(this, str, data)
        for (listener in eventListener.getListeners(CustomActionListener::class.java)) {
            listener.actionPerformed(event)
        }
    }

    private var itemListener = ItemListener {
        if (it.stateChange != ItemEvent.SELECTED) return@ItemListener
        val colorItem = it.item
        if (colorItem is ColorInfo) {
            strColor = colorItem.color
        }
    }

    private var dlListener = object : DocumentListener {
        override fun changedUpdate(p0: DocumentEvent) {
            val text = p0.document.getText(0, p0.document.length)
            when (p0.document) {
                tfText.document -> {
                    strText = text
                }
            }
        }

        override fun insertUpdate(p0: DocumentEvent) {
            val text = p0.document.getText(0, p0.document.length)
            when (p0.document) {
                tfText.document -> {
                    strText = text
                }
            }
        }

        override fun removeUpdate(p0: DocumentEvent) {
            val text = p0.document.getText(0, p0.document.length)
            when (p0.document) {
                tfText.document -> {
                    strText = text
                }
            }
        }
    }

    fun editFilterInfo(filterInfo: FilterInfo) {
        tfText.text = filterInfo.text
        strUuid = filterInfo.uuid
        bEnable = filterInfo.enabled
        strColor = filterInfo.color
        cbColor.selectedIndex = defaultMode.indexOf(strColor)
        iFilterType = filterInfo.type
        if (iFilterType == 2) {
            rbRegex.isSelected = true
        } else {
            rbContains.isSelected = true
        }
        newFilterInfo = false
    }

    fun cleanFilterInfo() {
        tfText.text = ""
        strColor = DefaultConfig.DEFAULT_BG_COLOR
        bEnable = true
        newFilterInfo = true
    }

    inner class DefaultCellRenderer: JLabel(), ListCellRenderer<ColorInfo> {
        override fun getListCellRendererComponent(p0: JList<out ColorInfo>, p1: ColorInfo, p2: Int, p3: Boolean, p4: Boolean): Component {
            text = p1.toString()
            isOpaque = true
            if (p3) {
                foreground = Color.RED
                background = Color.decode(p1.color())
            } else {
                foreground = Color.BLACK
                background = Color.decode(p1.color())
            }
            return this
        }
    }

    inner class DefaultColorModel : DefaultComboBoxModel<ColorInfo>() {
        private var arData = ArrayList<ColorInfo>()

        @Synchronized
        override fun getElementAt(p0: Int): ColorInfo {
            return arData[p0]
        }

        @Synchronized
        override fun getSize(): Int {
            return arData.size
        }

        @Synchronized
        fun setData(data: List<ColorInfo>) {
            arData.clear()
            arData.addAll(data)
        }

        @Synchronized
        fun indexOf(value: String): Int {
            return arData.indexOfFirst { it.color == value }
        }
    }
}