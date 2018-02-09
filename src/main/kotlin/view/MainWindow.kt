package view

import bean.FilterContainer
import interfces.CustomActionListener
import interfces.CustomEvent
import interfces.IView
import model.CmdModel
import model.FilterModel
import model.LogModel
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Insets
import javax.swing.*
import javax.swing.event.EventListenerList


class MainWindow(lModel: LogModel, fModel: FilterModel, cModel: CmdModel) : JFrame(), IView {
    private val logger = LoggerFactory.getLogger(MainWindow::class.java)

    private var eventlisteners = EventListenerList()

    private var logTable = LogTable()
    private var filterList = FilterList()
    private var menuBar = MainMenuBar()
    private var cmdComboBox = CmdComboBox()

    private var tfStatus = JTextField()

    private var btnRun: JButton? = null
    private var btnClean: JButton? = null
    private var btnStop: JButton? = null

    private val logModel = lModel
    private val filterModel = fModel
    private val cmdModel = cModel

    init {
        jMenuBar = menuBar
        contentPane.layout = BorderLayout()
        contentPane.add(getStatusPanel(), BorderLayout.SOUTH)
        contentPane.add(getLeftPanel(), BorderLayout.WEST)
        contentPane.add(getMainTabPanel(), BorderLayout.CENTER)
    }

    private fun getMainTabPanel(): Component {
        val scrollVBar = JScrollPane(logTable)
        return scrollVBar
    }

    private fun getLeftPanel(): Component {
        val rootPane = JPanel(BorderLayout())

        val jpLogPanel = JPanel(BorderLayout())
        jpLogPanel.border = BorderFactory.createTitledBorder("Device")

        val jpCmd = JPanel()
        //val btnDevice = JButton("OK")
        //btnDevice.margin = Insets(0, 0, 0, 0)
        jpCmd.add(cmdComboBox)
        //jpCmd.add(btnDevice)
        jpLogPanel.add(jpCmd, BorderLayout.NORTH)

        val jpLog = JPanel()
        btnClean = JButton("Clean")
        btnClean?.margin = Insets(0, 0, 0, 0)
        btnClean?.isEnabled = false
        btnClean?.addActionListener {
            updateButton("CMD_RUN_CLEAN")
        }
        jpLog.add(btnClean)
        btnRun = JButton("Run")
        btnRun?.margin = Insets(0, 0, 0, 0)
        btnRun?.addActionListener {
            updateButton("CMD_RUN_LOGCAT")
        }
        jpLog.add(btnRun)
        btnStop = JButton("Stop")
        btnStop?.margin = Insets(0, 0, 0, 0)
        btnStop?.isEnabled = false
        btnStop?.addActionListener {
            updateButton("CMD_STOP_LOGCAT")
        }
        jpLog.add(btnStop)
        jpLogPanel.add(jpLog, BorderLayout.CENTER)

        rootPane.add(jpLogPanel, BorderLayout.NORTH)

        val jpFilterRoot = JPanel(BorderLayout())
        jpFilterRoot.border = BorderFactory.createTitledBorder("Filter")

        filterList = FilterList()
        val popup = JPopupMenu()
        val addItem = JMenuItem("Add")
        addItem.addActionListener {
            val str = JOptionPane.showInputDialog(null, "Add", FilterContainer.FMTSTR)
            val data = FilterContainer.formatBean(str)
            if (data != null) {
                filterModel.addFilterInfo(data)
                filterModel.updateData()
            }
        }
        popup.add(addItem)
        val editItem = JMenuItem("Edit")
        editItem.addActionListener {
            val value = filterList.selectedValue
            if (value != null) {
                val str = JOptionPane.showInputDialog(null, "Edit", FilterContainer.formatString(value))
                val data = FilterContainer.formatBean(str, value.uuid)
                if (data != null) {
                    filterModel.editFilterInfo(data)
                    filterModel.updateData()
                }
            }
        }
        popup.add(editItem)
        val removeItem = JMenuItem("Remove")
        removeItem.addActionListener {
            val value = filterList.selectedValue
            if (value != null) {
                filterModel.removeFilterInfo(value)
            }
            filterModel.updateData()
        }
        popup.add(removeItem)
        filterList.componentPopupMenu = popup
        val scrollPane = JScrollPane(filterList)
        scrollPane.preferredSize = Dimension(100, 0)
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        jpFilterRoot.add(scrollPane, BorderLayout.CENTER)

        rootPane.add(jpFilterRoot, BorderLayout.CENTER)

        return rootPane
    }

    private fun getStatusPanel(): Component {
        val rootPane = JPanel(BorderLayout())
        tfStatus = JTextField("ready")
        tfStatus.isEditable = false
        rootPane.add(tfStatus)
        return rootPane
    }

    override fun initListener() {
        logger.debug("initListener")
        logModel.registerObserver(logTable)
        logTable.initListener()

        filterModel.registerObserver(filterList)
        filterList.addCustomActionListener(customListener)
        filterList.initListener()

        cmdModel.registerObserver(cmdComboBox)
        cmdComboBox.addCustomActionListener(customListener)
        cmdComboBox.initListener()

        menuBar.addCustomActionListener(customListener)
        menuBar.initListener()
    }

    override fun deinitListenr() {
        logger.debug("deinitListenr")
        logModel.removeObserver(logTable)
        logTable.deinitListenr()

        filterModel.removeObserver(filterList)
        filterList.removeCustomActionListener(customListener)
        filterList.deinitListenr()

        cmdModel.removeObserver(cmdComboBox)
        cmdComboBox.removeCustomActionListener(customListener)
        cmdComboBox.deinitListenr()

        menuBar.removeCustomActionListener(customListener)
        menuBar.deinitListenr()
    }

    fun setStatus(text: String) {
        tfStatus.text = text
    }

    fun setWindowTitle(text: String) {
        val file = "$title File($text)"
        title = file
    }

    private var customListener = object : CustomActionListener {

        override fun actionPerformed(event: CustomEvent) {
            logger.debug("actionPerformed " + event.actionCommand)
            when (event.actionCommand) {
                "CMD_SELECT_RUN" -> {
                    val index = cmdComboBox.selectedIndex
                    cmdModel.selectedIndex = index
                }
                else -> {
                    updateButton(event.actionCommand)
                }
            }
        }
    }

    fun addCustomActionListener(l: CustomActionListener) {
        eventlisteners.add(CustomActionListener::class.java, l)
    }

    fun removeCustomActionListener(l: CustomActionListener) {
        eventlisteners.remove(CustomActionListener::class.java, l)
    }

    private fun updateButton(cmd: String) {
        val action = CustomEvent(this, cmd)
        for (listener in eventlisteners.getListeners(CustomActionListener::class.java)) {
            listener.actionPerformed(action)
        }
    }

    fun setProcessBtn(b: Boolean) {
        if (b) {
            btnRun?.isEnabled = false
            btnStop?.isEnabled = true
            btnClean?.isEnabled = true
        } else {
            btnRun?.isEnabled = true
            btnStop?.isEnabled = false
            btnClean?.isEnabled = false
        }
    }
}