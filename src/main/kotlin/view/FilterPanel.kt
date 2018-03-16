package view

import interfces.CustomActionListener
import interfces.IView
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.ItemListener
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.EventListenerList

class FilterPanel : JPanel(), IView {
    private val logger = LoggerFactory.getLogger(FilterPanel::class.java)
    private var eventlisteners = EventListenerList()

    val tfShowTag = JTextField()
    val tfRemoveTag = JTextField()

    val chkEnableShowTag = JCheckBox()
    val chkEnableRemoveTag = JCheckBox()

    init {
        layout = BorderLayout()

        val jpTagFilter = JPanel(GridLayout(2, 1))
        jpTagFilter.border = BorderFactory.createTitledBorder("Filter")

        val jpShow = JPanel(BorderLayout())
        val show = JLabel()
        show.text = " Show   : "
        jpShow.add(show, BorderLayout.WEST)
        jpShow.add(tfShowTag, BorderLayout.CENTER)
        jpShow.add(chkEnableShowTag, BorderLayout.EAST)

        val jpRemoveTag = JPanel(BorderLayout())
        val removeTag = JLabel()
        removeTag.text = " Remove : "
        jpRemoveTag.add(removeTag, BorderLayout.WEST)
        jpRemoveTag.add(tfRemoveTag, BorderLayout.CENTER)
        jpRemoveTag.add(chkEnableRemoveTag, BorderLayout.EAST)

        jpTagFilter.add(jpShow)
        jpTagFilter.add(jpRemoveTag)

        add(jpTagFilter, BorderLayout.CENTER)
    }

    override fun initListener() {
        logger.debug("initListener")
        tfShowTag.document.addDocumentListener(dlListener)
        tfRemoveTag.document.addDocumentListener(dlListener)

        chkEnableShowTag.addItemListener(itemListener)
        chkEnableRemoveTag.addItemListener(itemListener)
    }

    override fun deinitListenr() {
        logger.debug("deinitListenr")
        tfShowTag.document.removeDocumentListener(dlListener)
        tfRemoveTag.document.removeDocumentListener(dlListener)

        chkEnableShowTag.removeItemListener(itemListener)
        chkEnableRemoveTag.removeItemListener(itemListener)
    }

    fun addCustomActionListener(l: CustomActionListener) {
        logger.debug("addCustomActionListener $l")
        eventlisteners.add(CustomActionListener::class.java, l)
    }

    fun removeCustomActionListener(l: CustomActionListener) {
        logger.debug("removeCustomActionListener $l")
        eventlisteners.remove(CustomActionListener::class.java, l)
    }

    private var itemListener = ItemListener {

    }

    private var dlListener = object : DocumentListener {
        override fun changedUpdate(p0: DocumentEvent?) {
        }

        override fun insertUpdate(p0: DocumentEvent?) {
        }

        override fun removeUpdate(p0: DocumentEvent?) {
        }
    }
}