package view

import bean.ConstCmd
import utils.DefaultConfig
import bean.FilterContainer
import interfces.CustomActionListener
import interfces.CustomEvent
import interfces.IView
import org.slf4j.LoggerFactory
import utils.LogDogConfig
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

class FilterEditPanel : JPanel(), IView {
    private val logger = LoggerFactory.getLogger(FilterEditPanel::class.java)
    private var eventListener = EventListenerList()

    private val rbContains = JRadioButton("Contains")
    private val rbRegex = JRadioButton("Match")
    private val bgType= ButtonGroup()
    private val tfText = JTextField()
    private val cbColor = JComboBox<String>()

    private val btnClean = JButton()
    private val btnOk = JButton()

    private var iFilterType = 1
    private var strColor = DefaultConfig.DEFAULT_BG_COLOR
    private var strText = ""
    private var strUuid = ""
    private var bEnable = true
    private var newFilterInfo = true

    init {
        layout = BorderLayout()

        val jpEditPane = JPanel(GridLayout(4, 1))
        jpEditPane.border = BorderFactory.createTitledBorder("Filter Edit")

        bgType.add(rbContains)
        bgType.add(rbRegex)
        rbContains.isSelected = true

        val jpColor = JPanel(BorderLayout())
        val jlColor = JLabel()
        jlColor.text = "Color  :"
        jpColor.add(jlColor, BorderLayout.WEST)
        cbColor.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(p0: JList<*>?, p1: Any?, p2: Int, p3: Boolean, p4: Boolean): Component {
                val c = super.getListCellRendererComponent(p0, p1, p2, p3, p4)
                if (p3) {
                    c.foreground = Color.RED
                } else {
                    c.foreground = Color.BLACK
                }
                p1?.run {
                    c.background = Color.decode(this.toString())
                }
                return c
            }
        }
        jpColor.add(cbColor, BorderLayout.CENTER)

        val jpText = JPanel(BorderLayout())
        val jlText = JLabel()
        jlText.text = "Text   :"
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

    fun loadItemData() {
        LogDogConfig.instance().custom_color.forEach {
            cbColor.addItem(it)
        }
    }

    override fun initListener() {
        logger.debug("initListener")
        tfText.document.addDocumentListener(dlListener)
        rbContains.addActionListener {
            iFilterType = 1
        }
        rbRegex.addActionListener {
            iFilterType = 2
        }
        cbColor.addItemListener(itemListener)
    }

    override fun deinitListenr() {
        logger.debug("deinitListenr")
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

    private fun formatNewFilterData(): FilterContainer {
        val data = FilterContainer()
        data.text = strText
        data.enabled = bEnable
        data.color = strColor
        data.type = iFilterType
        return data
    }

    private fun formatFilterData(): FilterContainer {
        val data = FilterContainer(strUuid)
        data.text = strText
        data.enabled = bEnable
        data.color = strColor
        data.type = iFilterType
        return data
    }

    private fun updateFilterData(data: FilterContainer?, str: String) {
        val event = CustomEvent(this, str, data)
        for (listener in eventListener.getListeners(CustomActionListener::class.java)) {
            listener.actionPerformed(event)
        }
    }

    private var itemListener = ItemListener {
        if (it.stateChange != ItemEvent.SELECTED) return@ItemListener
        strColor = it.item.toString()
        cbColor.foreground = Color.decode(strColor)
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

    fun editFilterInfo(filterInfo: FilterContainer) {
        tfText.text = filterInfo.text
        strUuid = filterInfo.uuid
        bEnable = filterInfo.enabled
        strColor = filterInfo.color
        iFilterType = filterInfo.type
        newFilterInfo = false
    }

    fun cleanFilterInfo() {
        tfText.text = ""
        strColor = DefaultConfig.DEFAULT_BG_COLOR
        bEnable = true
        newFilterInfo = true
    }
}