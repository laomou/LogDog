package controller

import bean.FilterInfo
import bean.LogInfo
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import event.CustomActionListener
import event.CustomEvent
import model.CmdModel
import model.DisplayLogModel
import model.FilterEditModel
import model.FilterMapModel
import org.slf4j.LoggerFactory
import utils.*
import utils.DefaultConfig.DEFAULT_HEIGHT
import utils.DefaultConfig.DEFAULT_LOG_PATH
import utils.DefaultConfig.DEFAULT_WIDTH
import utils.DefaultConfig.MIN_HEIGHT
import utils.DefaultConfig.MIN_WIDTH
import view.MainWindow
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JFrame
import javax.swing.JOptionPane
import kotlin.collections.ArrayList
import kotlin.system.exitProcess


class MainController {
    private val logger = LoggerFactory.getLogger(MainController::class.java)
    private var logConfig = LogDogConfig.instance()

    private val logParser = LogCatParser()

    private val filterLock = Object()
    private val fileLock = Object()

    @Volatile
    private var nChangedFilter = STATUS_READY

    private val displayLogMode = DisplayLogModel()
    private val filterModel = FilterMapModel()
    private val filterEditModel = FilterEditModel()
    private val cmdModel = CmdModel()

    private var strLogFileName = ""

    private var logCatProcess: Process? = null

    private var filterThread: Thread? = null
    private var logCatThread: Thread? = null
    private var fileReadThread: Thread? = null
    private var fileLoadThread: Thread? = null

    private var filterLoop = false
    private var fileReadLoop = false

    private var needMarkDataHide = false
    private var lastFilterTag = filterModel.getFilterTag()

    private var mainWindow = MainWindow(displayLogMode, filterModel, filterEditModel, cmdModel)

    companion object {
        private const val STATUS_CHANGE = 1
        private const val STATUS_PARSING = 2
        private const val STATUS_READY = 4
    }

    init {
        logger.debug("init")
        mainWindow.title = DefaultConfig.TITLE
        mainWindow.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        mainWindow.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        mainWindow.extendedState = JFrame.MAXIMIZED_BOTH
        mainWindow.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT)
        logger.debug("init window done")
        filterLoop = true
        logFilterParseThread()
        logger.debug("init done")
    }

    private fun registerListener() {
        mainWindow.registerListener()
        mainWindow.addCustomActionListener(customListener)
        mainWindow.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(p0: WindowEvent?) {
                logger.debug("windowClosing")
                exit()
            }
        })
        DropTarget(mainWindow, DnDConstants.ACTION_COPY_OR_MOVE, object : DropTargetListener {
            override fun dropActionChanged(event: DropTargetDragEvent?) {
            }

            override fun drop(event: DropTargetDropEvent?) {
                logger.debug("drop")
                event?.acceptDrop(DnDConstants.ACTION_COPY)
                cleanData()
                val t = event?.transferable
                val list = t?.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                val iterator = list.iterator()
                val files = ArrayList<File>()
                while (iterator.hasNext()) {
                    val file = iterator.next() as File
                    files.add(file)
                }
                parseFiles(files.toTypedArray())
            }

            override fun dragOver(event: DropTargetDragEvent?) {
            }

            override fun dragExit(event: DropTargetEvent?) {
            }

            override fun dragEnter(event: DropTargetDragEvent?) {
            }

        })
    }

    private fun unregisterListener() {
        mainWindow.unregisterListener()
        mainWindow.removeCustomActionListener(customListener)
    }

    fun launcher() {
        mainWindow.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        mainWindow.isVisible = true
        registerListener()
        loadConfigData()
        filterModel.updateData()
        filterEditModel.updateData()
        cmdModel.updateData()
    }

    fun exit() {
        filterLoop = false
        logCatProcess?.destroy()
        filterThread?.interrupt()
        logCatThread?.interrupt()
        fileReadLoop = false
        fileReadThread?.interrupt()
        fileLoadThread?.interrupt()

        unregisterListener()
        saveConfigData()
        exitProcess(0)
    }

    private val customListener = object : CustomActionListener {
        override fun actionPerformed(event: CustomEvent) {
            when {
                event.action == ConstCmd.CMD_OPEN_FILE -> startOpenFile()
                event.action == ConstCmd.CMD_RUN_LOGCAT -> startProcess()
                event.action == ConstCmd.CMD_STOP_LOGCAT -> stopProcess()
                event.action == ConstCmd.CMD_RUN_FILTER -> runFilter()
                event.action == ConstCmd.CMD_RUN_CLEAN -> cleanData()
                event.action == ConstCmd.CMD_CONFIG_ADB -> configAdbFile()
                event.action == ConstCmd.CMD_ABOUT_LOGDOG -> about()
                event.action == ConstCmd.CMD_EXIT_LOGDOG -> {
                    mainWindow.dispose()
                    exit()
                }
            }
        }
    }

    private fun logFilterParseThread() {
        filterThread = Thread(Runnable {
            logger.debug("in")
            try {
                while (filterLoop) {
                    synchronized(filterLock) {
                        nChangedFilter = STATUS_READY

                        logger.debug("filterLock->wait")
                        mainWindow.setStatus("Idle")
                        filterLock.wait()

                        val filter = filterModel.getEnableFilterString()
                        logger.debug("parsing $filter")
                        mainWindow.setStatus("Parsing Filter: $filter")
                        nChangedFilter = STATUS_PARSING

                        if (!filterModel.hasFilter()) {
                            logger.debug("updateData(no filter)")
                            displayLogMode.markDataShow()
                            needMarkDataHide = true
                            logger.debug("none filter tag need mark data show")
                            updateTableData()
                            nChangedFilter = STATUS_READY
                        } else {
                            when {
                                filterModel.hasNewFilter() -> {
                                    val newFilters = filterModel.getNewFilters()
                                    logger.debug("new filter changeFilter->size: ${newFilters.size}")

                                    newFilters.forEach {
                                        logger.debug("new filter id: ${it.uuid} enable: ${it.enabled} state ${it.state} lines: ${it.lines.size}")
                                        it.lines.clear()
                                        val newData = displayLogMode.getData()
                                        newData.forEach { it1 ->
                                            filterModel.updateLineInfo(it, it1)
                                        }
                                    }
                                }
                                filterModel.hasEditedFilter() -> {
                                    val newFilters = filterModel.getEditedFilters()
                                    logger.debug("edit filter changeFilter->size: ${newFilters.size}")

                                    newFilters.forEach {
                                        logger.debug("edit filter id: ${it.uuid} enable: ${it.enabled} state ${it.state} lines: ${it.lines.size}")
                                        it.lines.forEach { line ->
                                            val logInfo = displayLogMode.getItemData(line - 1)
                                            logInfo?.let { nLogInfo ->
                                                nLogInfo.show = false
                                                nLogInfo.filters.remove(it.uuid)
                                            }
                                        }
                                        it.lines.clear()
                                        val newData = displayLogMode.getData()
                                        newData.forEach { it1 ->
                                            filterModel.updateLineInfo(it, it1)
                                        }
                                    }

                                    needMarkDataHide = true
                                }
                                filterModel.hasDelFilter() -> {
                                    val newFilters = filterModel.getDelFilters()
                                    logger.debug("del filter changeFilter->size: ${newFilters.size}")

                                    newFilters.forEach {
                                        logger.debug("del filter id: ${it.uuid} enable: ${it.enabled} state ${it.state} lines: ${it.lines.size}")
                                        it.lines.forEach { line ->
                                            val logInfo = displayLogMode.getItemData(line - 1)
                                            logInfo?.let { nLogInfo ->
                                                nLogInfo.show = false
                                                nLogInfo.filters.remove(it.uuid)
                                            }
                                        }
                                    }
                                }
                            }

                            if (filterModel.getFilterTag() != lastFilterTag || needMarkDataHide) {
                                logger.debug("new filter tag need mark data hide")
                                displayLogMode.markDataHide()
                            }

                            val newFilters = if (filterModel.getFilterTag() != lastFilterTag || needMarkDataHide) {
                                filterModel.getEnableFilters()
                            } else {
                                filterModel.getChangesFilters()
                            }
                            logger.debug("changes filter changeFilter->size: ${newFilters.size}")

                            if (needMarkDataHide) {
                                needMarkDataHide = false
                            }

                            newFilters.forEach { nFilter ->
                                logger.debug("changed filter id: ${nFilter.uuid} enable: ${nFilter.enabled} state: ${nFilter.state} lines: ${nFilter.lines.size}")
                                nFilter.lines.forEach { line ->
                                    val logInfo = displayLogMode.getItemData(line - 1)
                                    logInfo?.let { nLogInfo ->
                                        if (nFilter.enabled) {
                                            nLogInfo.show = true
                                            nLogInfo.filterColor = nFilter.color
                                        } else {
                                            nLogInfo.show = false
                                        }
                                        nLogInfo.filters.forEach { iFilter ->
                                            val filterInfo = filterModel.findItemDataByUUID(iFilter)
                                            filterInfo?.let { lFilter ->
                                                markShowInfoByFilter(lFilter, logInfo)
                                            }
                                        }
                                    }
                                }

                                if (nFilter.state != 3) {
                                    nFilter.state = 0
                                }
                            }
                        }

                        if (filterModel.hasDelFilter()) {
                            filterModel.doDelFilter()
                            filterModel.updateData()
                        }

                        if (nChangedFilter == STATUS_PARSING) {
                            nChangedFilter = STATUS_READY
                            logger.debug("filter done updateData")
                            updateTableData()
                        }

                        lastFilterTag = filterModel.getFilterTag()
                    }
                }
            } catch (e: Exception) {
                //e.printStackTrace()
                logger.warn(e.toString())
            }
            logger.debug("exit")
        })
        filterThread?.name = "filterThread"
        filterThread?.start()
    }

    private fun logFileReadThread() {
        fileReadThread = Thread(Runnable {
            logger.debug("in")
            try {
                val fis = FileInputStream(DEFAULT_LOG_PATH + File.separatorChar + strLogFileName)
                val dis = DataInputStream(fis)
                val br = BufferedReader(InputStreamReader(dis, "UTF-8"))

                mainWindow.setStatus("Parsing File: $strLogFileName")

                while (filterLoop) {
                    Thread.sleep(500)

                    if (nChangedFilter == STATUS_CHANGE || nChangedFilter == STATUS_PARSING) {
                        continue
                    }

                    synchronized(fileLock) {
                        var strLine: String? = ""
                        var nLine = displayLogMode.getDataSize() + 1
                        while ({ strLine = br.readLine(); strLine }() != null) {
                            val logInfo = logParser.parse(strLine!!)
                            logInfo.strLine = nLine++
                            addFilterLogInfo(logInfo)
                        }
                    }

                    synchronized(filterLock) {
                        logger.debug("updateData(read file)")
                        updateTableData()
                    }
                }

                br.close()
                dis.close()
                fis.close()
            } catch (e: Exception) {
                //e.printStackTrace()
                logger.warn(e.toString())
            }
            mainWindow.setStatus("Parse File Complete")
            logger.debug("exit")
        })
        fileReadThread?.name = "fileReadThread"
        fileReadThread?.start()
    }

    private fun logCatThread() {
        logCatThread = Thread(Runnable {
            logger.debug("in")
            try {
                val cmd = "${logConfig.tool_path} ${cmdModel.getSelectedCmd()}"
                logger.debug("cmd run: $cmd")
                logCatProcess = Runtime.getRuntime().exec(cmd)

                strLogFileName = makeFilename()

                val dir = File(DEFAULT_LOG_PATH)
                if (!dir.exists()) {
                    dir.mkdirs()
                }

                mainWindow.setWindowTitle("LogDog File($strLogFileName)")

                val br = BufferedReader(InputStreamReader(logCatProcess!!.inputStream, "UTF-8"))
                val bw = BufferedWriter(OutputStreamWriter(FileOutputStream(DEFAULT_LOG_PATH + File.separatorChar + strLogFileName), "UTF-8"))

                logFileReadThread()

                var strLine: String? = ""
                while ({ strLine = br.readLine(); strLine }() != null) {
                    synchronized(fileLock) {
                        bw.write(strLine!!)
                        bw.write("\r\n")
                        bw.flush()
                    }
                }

                br.close()
                bw.close()
            } catch (e: Exception) {
                //e.printStackTrace()
                logger.warn(e.toString())
            }
            logger.debug("exit")
            stopProcess()
        })
        logCatThread?.name = "logCatThread"
        logCatThread?.start()
    }

    private fun parseFiles(files: Array<File>) {
        var titles = ""
        var nLine = 1
        fileLoadThread = Thread(Runnable {
            files.forEach { file ->
                logger.debug("parseFile ${file.path} start")
                mainWindow.setStatus("Parse File: ${file.path}")
                file.forEachLine { line ->
                    val logInfo = logParser.parse(line)
                    logInfo.strLine = nLine++
                    if (logInfo.valid) {
                        addFilterLogInfo(logInfo)
                    }
                }
                if (titles.isNotEmpty()) {
                    titles += "|"
                }
                titles += file.name
                logger.debug("parseFile ${file.path} end")
            }
            mainWindow.setWindowTitle("LogDog File($titles)")
            runFilter()
            mainWindow.setStatus("Parse File Complete")
        })
        fileLoadThread?.name = "fileLoadThread"
        fileLoadThread?.start()
    }

    private fun cleanData() {
        filterModel.cleanLines()
        filterModel.updateData()
        displayLogMode.cleanData()
        displayLogMode.updateData()
    }

    private fun configAdbFile() {
        val fd = FileDialog(mainWindow, "Config Tool Path", FileDialog.LOAD)
        fd.isMultipleMode = false
        fd.isVisible = true
        if (fd.file != null) {
            val tool = "${fd.directory}${fd.file}"
            logConfig.tool_path = tool
        }
    }

    private fun startOpenFile() {
        val fd = FileDialog(mainWindow, "Open Log File", FileDialog.LOAD)
        fd.isMultipleMode = true
        fd.isVisible = true
        if (fd.file != null) {
            logger.debug("file " + fd.files)
            cleanData()
            parseFiles(fd.files)
        }
    }

    private fun about() {
        val message = StringBuffer()
        message.append(DefaultConfig.TITLE + "\n")
        message.append("LogDog is the gui tool for analysis and filter android log" + "\n")
        message.append("<html><a href=\"${DefaultConfig.URL}\">${DefaultConfig.URL}</a></html>")
        JOptionPane.showMessageDialog(mainWindow, message.toString(), "About", JOptionPane.PLAIN_MESSAGE)
    }

    private fun startProcess() {
        logger.debug("startProcess")
        cleanData()
        fileReadLoop = true
        logCatThread()
        mainWindow.setProcessBtn(true)
    }

    private fun stopProcess() {
        logger.debug("stopProcess")
        mainWindow.setProcessBtn(false)
        logCatProcess?.destroy()
        logCatThread?.interrupt()
        fileReadLoop = false
        fileReadThread?.interrupt()
        fileLoadThread?.interrupt()
    }

    private fun runFilter() {
        nChangedFilter = STATUS_CHANGE
        checkFilter()
    }

    private fun checkFilter() {
        logger.debug("checkFilter")
        while (nChangedFilter == STATUS_PARSING) {
            try {
                logger.debug("sleep 100")
                Thread.sleep(100)
            } catch (e: Exception) {
                //e.printStackTrace()
            }
        }

        synchronized(filterLock) {
            logger.debug("filterLock->notify")
            filterLock.notify()
        }
    }

    private fun updateTableData() {
        filterModel.updateData()
        displayLogMode.updateData()
    }

    private fun addFilterLogInfo(logInfo: LogInfo) {
        synchronized(filterLock) {
            displayLogMode.addLogInfo(logInfo)
            filterModel.updateLineInfo(logInfo)
            logInfo.show = false
            if (filterModel.getFilterTag() in FilterMapModel.TYPE_FILTER_TAG1..FilterMapModel.TYPE_FILTER_TAG3) {
                if (filterModel.hasFilter()) {
                    logInfo.show = filterModel.checkEnableOrFilter(logInfo)
                    logInfo.filterColor = filterModel.findFilersColor(logInfo.strMsg)
                } else {
                    logInfo.show = true
                }
            } else {
                logInfo.show = true
            }
        }
    }

    private fun markShowInfoByFilter(filterInfo: FilterInfo, logInfo: LogInfo) {
        synchronized(filterLock) {
            //logger.debug("Item => id: ${filterInfo.uuid} enable: ${filterInfo.enabled} state: ${filterInfo.state} + line: ${logInfo.strLine}")
            if (filterModel.getFilterTag() in FilterMapModel.TYPE_FILTER_TAG1..FilterMapModel.TYPE_FILTER_TAG3) {
                filterModel.updateShowInfo(filterInfo, logInfo)
            } else {
                logInfo.show = true
            }
        }
    }

    private fun makeFilename(): String {
        val now = Date()
        val format = SimpleDateFormat("yyyyMMdd_HHmmss")
        return "LogDog_" + format.format(now) + ".txt"
    }

    private fun loadConfigData() {
        try {
            val file = File("config.json")
            val contents = file.readText()
            val gson = Gson()
            val type = object : TypeToken<LogDogConfig>() {}.type
            val config: LogDogConfig = gson.fromJson(contents, type)
            logConfig.loadFromGson(config)
            logConfig.filter_rule.forEach {
                filterModel.loadFilterInfo(it.key, it.value)
            }
            logConfig.tool_cmd.forEach {
                cmdModel.addCmdInfo(it)
            }
            logConfig.custom_color.forEach {
                filterEditModel.addColorInfo(it)
            }
            UID.setUID(logConfig.uuid)
        } catch (e: FileNotFoundException) {
            //e.printStackTrace()
            logger.error("File(config.json) not found")
        }
    }

    private fun saveConfigData() {
        try {
            val file = File("config.json")
            val gson = GsonBuilder().setPrettyPrinting().create()
            logConfig.preSave(filterModel.getMapData())
            logConfig.uuid = UID.getUID()
            val contents = gson.toJson(logConfig)
            file.writeText(contents)
        } catch (e: FileNotFoundException) {
            //e.printStackTrace()
            logger.error("File(config.json) not found")
        }
    }

}
