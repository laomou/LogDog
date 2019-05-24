package view

import bean.FilterInfo
import interfces.CustomActionListener
import interfces.CustomEvent
import interfces.IView
import model.CmdModel
import model.DisplayLogModel
import model.FilterEditModel
import model.FilterMapModel
import org.slf4j.LoggerFactory
import utils.ConstCmd
import java.awt.*
import javax.swing.*
import javax.swing.event.EventListenerList


class MainWindow(lModel: DisplayLogModel, fModel: FilterMapModel, fcModel: FilterEditModel, cModel: CmdModel) : JFrame(), IView {
    private val logger = LoggerFactory.getLogger(MainWindow::class.java)

    private val eventListener = EventListenerList()

    private val logTable = LogTable()
    private val filterList = FilterList()
    private val filterEdit = FilterEditPanel()
    private val menuBar = MainMenuBar()
    private val cmdComboBox = CmdComboBox()

    private val tfStatus = JTextField()

    private var btnRun: JButton? = null
    private var btnClean: JButton? = null
    private var btnStop: JButton? = null

    private var btnFilterType: JButton? = null

    private val logModel = lModel
    private val filterModel = fModel
    private val filterColorModel = fcModel
    private val cmdModel = cModel

    init {
        jMenuBar = menuBar
        contentPane.layout = BorderLayout()
        contentPane.add(getStatusPanel(), BorderLayout.SOUTH)
        val splitPanel = JSplitPane()
        splitPanel.leftComponent = getLeftPanel()
        splitPanel.rightComponent = getMainTabPanel()
        contentPane.add(splitPanel, BorderLayout.CENTER)
    }

    private fun getMainTabPanel(): Component {
        val rootPane = JPanel(BorderLayout())
        val scrollVBar = JScrollPane(logTable)
        rootPane.add(scrollVBar, BorderLayout.CENTER)
        return rootPane
    }

    private fun getLeftPanel(): Component {
        val rootPane = JPanel(BorderLayout())

        val jpDevicePanel = JPanel(BorderLayout())
        jpDevicePanel.border = BorderFactory.createTitledBorder("Device")

        val jpCmd = JPanel(BorderLayout())
        val jlCmd = JLabel()
        jlCmd.text = "Command :"
        jpCmd.add(jlCmd, BorderLayout.WEST)
        jpCmd.add(cmdComboBox)
        jpDevicePanel.add(jpCmd, BorderLayout.CENTER)

        val jpLog = JPanel(GridLayout(0, 5))
        jpLog.add(JLabel(""))
        jpLog.add(JLabel(""))

        btnClean = JButton("Clean")
        btnClean?.margin = Insets(0, 0, 0, 0)
        btnClean?.isEnabled = false
        btnClean?.addActionListener {
            updateButton(ConstCmd.CMD_RUN_CLEAN)
        }
        jpLog.add(btnClean)
        btnRun = JButton("Run")
        btnRun?.margin = Insets(0, 0, 0, 0)
        btnRun?.addActionListener {
            updateButton(ConstCmd.CMD_RUN_LOGCAT)
        }
        jpLog.add(btnRun)
        btnStop = JButton("Stop")
        btnStop?.margin = Insets(0, 0, 0, 0)
        btnStop?.isEnabled = false
        btnStop?.addActionListener {
            updateButton(ConstCmd.CMD_STOP_LOGCAT)
        }
        jpLog.add(btnStop)
        jpDevicePanel.add(jpLog, BorderLayout.SOUTH)

        //add jpDevicePanel
        rootPane.add(jpDevicePanel, BorderLayout.NORTH)

        val jpFilterPanel = JPanel(BorderLayout())
        jpFilterPanel.border = BorderFactory.createTitledBorder("Filter Bookmark")

        val jpFilterType = JPanel()
        btnFilterType = JButton("Filter:  TAG1")
        btnFilterType?.addActionListener {
            filterModel.toggleFilterTag()
            btnFilterType?.text = when (filterModel.getFilterTag()) {
                FilterMapModel.TYPE_FILTER_TAG1 -> "Filter: TAG1"
                FilterMapModel.TYPE_FILTER_TAG2 -> "Filter: TAG2"
                FilterMapModel.TYPE_FILTER_TAG3 -> "Filter: TAG3"
                else -> "Filter type: None"
            }
            updateFilterAndTable()
        }
        jpFilterType.add(btnFilterType)
        jpFilterPanel.add(jpFilterType, BorderLayout.NORTH)

        val scrollPane = JScrollPane(filterList)
        scrollPane.preferredSize = Dimension(125, 0)
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        jpFilterPanel.add(scrollPane, BorderLayout.CENTER)

        // add jpFilterPanel
        rootPane.add(jpFilterPanel, BorderLayout.CENTER)

        //add jpEditPane
        rootPane.add(filterEdit, BorderLayout.SOUTH)

        return rootPane
    }

    private fun getStatusPanel(): Component {
        val rootPane = JPanel(BorderLayout())
        tfStatus.text = "ready"
        tfStatus.isEditable = false
        rootPane.add(tfStatus)
        return rootPane
    }

    override fun registerListener() {
        logger.debug("registerListener")

        logModel.registerObserver(logTable)
        logTable.registerListener()

        filterModel.registerObserver(filterList)
        filterList.addCustomActionListener(customListener)
        filterList.registerListener()

        filterColorModel.registerObserver(filterEdit)
        filterEdit.addCustomActionListener(customListener)
        filterEdit.registerListener()

        cmdModel.registerObserver(cmdComboBox)
        cmdComboBox.addCustomActionListener(customListener)
        cmdComboBox.registerListener()

        menuBar.addCustomActionListener(customListener)
        menuBar.registerListener()
    }

    override fun unregisterListener() {
        logger.debug("unregisterListener")

        logModel.removeObserver(logTable)
        logTable.unregisterListener()

        filterModel.removeObserver(filterList)
        filterList.removeCustomActionListener(customListener)
        filterList.unregisterListener()

        filterColorModel.removeObserver(filterEdit)
        filterEdit.removeCustomActionListener(customListener)
        filterEdit.unregisterListener()

        cmdModel.removeObserver(cmdComboBox)
        cmdComboBox.removeCustomActionListener(customListener)
        cmdComboBox.unregisterListener()

        menuBar.removeCustomActionListener(customListener)
        menuBar.unregisterListener()
    }

    fun setStatus(text: String) {
        tfStatus.text = text
    }

    fun setWindowTitle(text: String) {
        title = text
    }

    private var customListener = object : CustomActionListener {

        override fun actionPerformed(event: CustomEvent) {
            logger.debug("actionPerformed " + event.action)
            when (event.action) {
                ConstCmd.CMD_SELECT_RUN -> {
                    val index = cmdComboBox.selectedIndex
                    cmdModel.setSelectedCmd(index)
                }
                ConstCmd.CMD_ADD_FILTER -> {
                    filterModel.addFilterInfo(event.obj as FilterInfo)
                    updateFilterAndTable()
                }
                ConstCmd.CMD_DEL_FILTER -> {
                    filterEdit.cleanFilterInfo()
                    filterModel.removeFilterInfo(event.obj as FilterInfo)
                    updateFilterAndTable()
                }
                ConstCmd.CMD_EDIT_FILTER_END -> {
                    filterModel.editFilterInfo(event.obj as FilterInfo)
                    updateFilterAndTable()
                }
                ConstCmd.CMD_EDIT_FILTER_START -> {
                    filterEdit.editFilterInfo(event.obj as FilterInfo)
                }
                ConstCmd.CMD_ENABLE_FILTER -> {
                    filterModel.enableFilterInfo(event.obj as FilterInfo)
                    updateFilterAndTable()
                }
                else -> {
                    updateEvent(event)
                }
            }
        }
    }

    fun addCustomActionListener(l: CustomActionListener) {
        eventListener.add(CustomActionListener::class.java, l)
    }

    fun removeCustomActionListener(l: CustomActionListener) {
        eventListener.remove(CustomActionListener::class.java, l)
    }

    private fun updateButton(action: String) {
        val event = CustomEvent(this, action)
        for (listener in eventListener.getListeners(CustomActionListener::class.java)) {
            listener.actionPerformed(event)
        }
    }

    private fun updateEvent(event: CustomEvent) {
        for (listener in eventListener.getListeners(CustomActionListener::class.java)) {
            listener.actionPerformed(event)
        }
    }

    fun setProcessBtn(b: Boolean) {
        logger.debug("setProcessBtn $b")
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

    private fun updateFilterAndTable() {
        filterModel.updateData()
        updateButton(ConstCmd.CMD_RUN_FILTER)
    }
}