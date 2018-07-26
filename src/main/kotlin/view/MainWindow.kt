package view

import bean.ConstCmd
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

    private val eventlisteners = EventListenerList()

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
    private val cmdModel = cModel

    init {
        jMenuBar = menuBar
        contentPane.layout = BorderLayout()
        contentPane.add(getStatusPanel(), BorderLayout.SOUTH)
        contentPane.add(getLeftPanel(), BorderLayout.WEST)
        contentPane.add(getMainTabPanel(), BorderLayout.CENTER)
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

        val jpCmd = JPanel()
        jpCmd.add(cmdComboBox)
        jpDevicePanel.add(jpCmd, BorderLayout.NORTH)

        val jpLog = JPanel()
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
        jpDevicePanel.add(jpLog, BorderLayout.CENTER)

        //add jpDevicePanel
        rootPane.add(jpDevicePanel, BorderLayout.NORTH)

        val jpFilterPanel = JPanel(BorderLayout())
        jpFilterPanel.border = BorderFactory.createTitledBorder("Filter Bookmark")

        val jpFilterType = JPanel()
        btnFilterType = JButton("Filter type: HighLight")
        btnFilterType?.addActionListener {
            filterModel.toggleFilterType()
            btnFilterType?.text = when (filterModel.getFilterType()) {
                FilterModel.TYPE_FILTER_OR -> "Filter type: Or"
                FilterModel.TYPE_FILTER_AND -> "Filter type: And"
                else -> "Filter type: HighLight"
            }
            updateFilterAndTable()
        }
        jpFilterType.add(btnFilterType)
        jpFilterPanel.add(jpFilterType, BorderLayout.NORTH)

        val scrollPane = JScrollPane(filterList)
        scrollPane.preferredSize = Dimension(200, 0)
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

    override fun initListener() {
        logger.debug("initListener")

        logModel.registerObserver(logTable)
        logTable.initListener()

        filterModel.registerObserver(filterList)
        filterList.addCustomActionListener(customListener)
        filterList.initListener()

        filterEdit.addCustomActionListener(customListener)
        filterEdit.initListener()

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

        filterEdit.removeCustomActionListener(customListener)
        filterEdit.deinitListenr()

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
        title = text
    }

    private var customListener = object : CustomActionListener {

        override fun actionPerformed(event: CustomEvent) {
            logger.debug("actionPerformed " + event.actionCommand)
            when (event.actionCommand) {
                ConstCmd.CMD_SELECT_RUN -> {
                    val index = cmdComboBox.selectedIndex
                    cmdModel.selectedIndex = index
                }
                ConstCmd.CMD_ADD_FILTER -> {
                    filterModel.addFilterInfo(event.objectValue as FilterContainer)
                    updateFilterAndTable()
                }
                ConstCmd.CMD_DEL_FILTER -> {
                    filterEdit.cleanFilterInfo()
                    filterModel.removeFilterInfo(event.objectValue as FilterContainer)
                    updateFilterAndTable()
                }
                ConstCmd.CMD_EDIT_FILTER_END -> {
                    filterModel.editFilterInfo(event.objectValue as FilterContainer)
                    updateFilterAndTable()
                }
                ConstCmd.CMD_EDIT_FILTER_START -> {
                    filterEdit.editFilterInfo(event.objectValue as FilterContainer)
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

    private fun updateFilterAndTable() {
        filterModel.updateData()
        updateButton(ConstCmd.CMD_RUN_FILTER)
    }
}