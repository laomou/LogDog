package controller

import bean.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import interfces.CustomActionListener
import interfces.CustomEvent
import model.CmdModel
import model.DisplayLogModel
import model.FilterModel
import org.slf4j.LoggerFactory
import utils.LogCatParser
import utils.LogToolConfig
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
import kotlin.collections.ArrayList


class MainController {
    private val logger = LoggerFactory.getLogger(MainController::class.java)

    private val LOG_PATH = "log"

    private var logConfig = LogToolConfig.instance()

    private val logParser = LogCatParser()

    private val filterLock = java.lang.Object()
    private val fileLock = java.lang.Object()

    private val STATUS_CHANGE = 1
    private val STATUS_PARSING = 2
    private val STATUS_READY = 4
    @Volatile
    private var nChangedFilter = STATUS_READY

    private val displayLogMode = DisplayLogModel()
    private val filterModel = FilterModel()
    private val cmdModel = CmdModel()

    private var strLogFileName = ""

    private var logCatProcess: Process? = null

    private var filterThread: Thread? = null
    private var logCatThread: Thread? = null
    private var fileReadThread: Thread? = null
    private var fileLoadThread: Thread? = null

    private var filterLoop = false
    private var fileReadLoop = false

    private val DEFAULT_WIDTH = 1200
    private val DEFAULT_HEIGHT = 720
    private val MIN_WIDTH = 1100
    private val MIN_HEIGHT = 500


    private var mainWindow = MainWindow(displayLogMode, filterModel, cmdModel)


    init {
        logger.debug("init")
        mainWindow.title = "LogDog"
        mainWindow.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        mainWindow.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        mainWindow.extendedState = JFrame.MAXIMIZED_BOTH
        mainWindow.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT)
        logger.debug("init done")
        filterLoop = true
        logFilterParseThread()
    }

    private fun iniListener() {
        mainWindow.initListener()
        mainWindow.addCustomActionListener(customListener)
        mainWindow.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(p0: WindowEvent?) {
                logger.debug("windowClosing")
                deinitListenr()
                saveConfigData()
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
                parseFiles(files)
            }

            override fun dragOver(event: DropTargetDragEvent?) {
            }

            override fun dragExit(event: DropTargetEvent?) {
            }

            override fun dragEnter(event: DropTargetDragEvent?) {
            }

        })
    }

    fun deinitListenr() {
        mainWindow.deinitListenr()
        mainWindow.removeCustomActionListener(customListener)

        filterLoop = false
        filterThread?.interrupt()
        logCatThread?.interrupt()
        fileReadLoop = false
        fileReadThread?.interrupt()
        fileLoadThread?.interrupt()
    }

    fun launcher() {
        mainWindow.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        mainWindow.isVisible = true
        iniListener()
        loadConfigData()
        mainWindow.loadConfigData()
        filterModel.updateData()
        cmdModel.updateData()
    }

    private val customListener = object : CustomActionListener {
        override fun actionPerformed(event: CustomEvent) {
            when {
                event.actionCommand == ConstCmd.CMD_OPEN_FILE -> startOpenFile()
                event.actionCommand == ConstCmd.CMD_RUN_LOGCAT -> startProcess()
                event.actionCommand == ConstCmd.CMD_STOP_LOGCAT -> stopProcess()
                event.actionCommand == ConstCmd.CMD_RUN_FILTER -> runFilter()
                event.actionCommand == ConstCmd.CMD_RUN_CLEAN -> cleanData()
                event.actionCommand == ConstCmd.CMD_CONFIG_ADB -> configAdbFile()
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
                        mainWindow.setStatus("Parsing $filter")
                        nChangedFilter = STATUS_PARSING

                        if (!filterModel.hasFilter()) {
                            logger.debug("updateData(no filter)")
                            displayLogMode.showData()
                            updateTableData()
                            nChangedFilter = STATUS_READY
                        } else {
                            if (filterModel.hasNewFilter()) {
                                val newFilters = filterModel.getEnableNewFilters()
                                logger.debug("new filter changeFilter->size: ${newFilters.size}")

                                newFilters.forEach {
                                    logger.debug("id: ${it.uuid} enable: ${it.enabled} state ${it.state} lines: ${it.lines.size}")
                                    val newData = displayLogMode.getData()
                                    newData.forEach { it1 ->
                                        filterModel.updateLineInfo(it, it1)
                                    }
                                }
                            }

                            displayLogMode.tryShowData()

                            val newFilters = filterModel.getChangesFilters()
                            logger.debug("changes filter changeFilter->size: ${newFilters.size}")

                            newFilters.forEach {
                                logger.debug("id: ${it.uuid} enable: ${it.enabled} state ${it.state} lines: ${it.lines.size}")
                                val lines = it.lines
                                lines.forEach { it1 ->
                                    val logInfo = displayLogMode.getItemData(it1 - 1)
                                    logInfo.filters.forEach { it2 ->
                                        val f = filterModel.findItemDataByUUID(it2)
                                        f?.run {
                                            reMarkByFilter(this, logInfo)
                                        }
                                    }
                                }
                                it.state = -1
                            }
                        }

                        if (nChangedFilter == STATUS_PARSING) {
                            nChangedFilter = STATUS_READY
                            logger.debug("filter done updateData")
                            updateTableData()
                        }
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
                val fis = FileInputStream(LOG_PATH + File.separatorChar + strLogFileName)
                val dis = DataInputStream(fis)
                val br = BufferedReader(InputStreamReader(dis, "UTF-8"))

                mainWindow.setStatus("Parsing $strLogFileName")

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
            mainWindow.setStatus("Complete $strLogFileName")
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

                val dir = File(LOG_PATH)
                if (!dir.exists()) {
                    dir.mkdirs()
                }

                mainWindow.setWindowTitle("LogDog File($strLogFileName)")

                val br = BufferedReader(InputStreamReader(logCatProcess?.inputStream, "UTF-8"))
                val bw = BufferedWriter(OutputStreamWriter(FileOutputStream(LOG_PATH + File.separatorChar + strLogFileName), "UTF-8"))

                logFileReadThread()

                var strLine: String? = ""
                while ({ strLine = br.readLine(); strLine }() != null) {
                    synchronized(fileLock) {
                        bw.write(strLine)
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
        files.forEach {
            parseFile(it.path)
            if (!titles.isEmpty()) {
                titles += ","
            }
            titles += it.name
        }
        mainWindow.setStatus("Parse complete")
        mainWindow.setWindowTitle("LogDog File($titles)")
    }

    private fun parseFiles(files: ArrayList<File>) {
        var titles = ""
        files.forEach {
            parseFile(it.path)
            if (!titles.isEmpty()) {
                titles += ","
            }
            titles += it.name
        }
        mainWindow.setStatus("Parse complete")
        mainWindow.setWindowTitle("LogDog File($titles)")
    }

    private fun parseFile(fileName: String) {
        logger.debug("parseFile: $fileName")
        fileLoadThread = Thread(Runnable {
            val file = File(fileName)
            var nIndex = 1
            logger.debug("parseFile start")
            mainWindow.setStatus("Parsing")
            file.forEachLine {
                val logInfo = logParser.parse(it)
                logInfo.strLine = nIndex++
                if (logInfo.valid) {
                    addFilterLogInfo(logInfo)
                }
            }
            logger.debug("parseFile->size: " + displayLogMode.getDataSize())
            runFilter()
            mainWindow.setStatus("Parse complete")
            logger.debug("parseFile end")
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
        val fd = FileDialog(mainWindow, "Config Log Tool", FileDialog.LOAD)
        fd.isMultipleMode = false
        fd.isVisible = true
        if (fd.file != null) {
            val tool = "${fd.directory}${fd.file}"
            logConfig.tool_path = tool
        }
    }

    private fun startOpenFile() {
        val fd = FileDialog(mainWindow, "File open", FileDialog.LOAD)
        fd.isMultipleMode = true
        fd.isVisible = true
        if (fd.file != null) {
            logger.debug("file " + fd.files)
            cleanData()
            parseFiles(fd.files)
        }
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

    private fun addFilterLogInfo(logInfo: LogContainer) {
        synchronized(filterLock) {
            displayLogMode.addLogInfo(logInfo)
            filterModel.updateLineInfo(logInfo)
            logInfo.show = false
            if (filterModel.getFilterType() == FilterModel.TYPE_FILTER_OR) {
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

    private fun reMarkByFilter(filterInfo: FilterContainer, logInfo: LogContainer) {
        synchronized(filterLock) {
            if (filterModel.getFilterType() == FilterModel.TYPE_FILTER_OR) {
                logInfo.show = filterInfo.enabled
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
            val type = object : TypeToken<LogToolConfig>() {}.type
            val config: LogToolConfig = gson.fromJson(contents, type)
            logConfig.load(config)
            logConfig.filter_rule.forEach {
                filterModel.loadFilterInfo(it)
            }
            logConfig.tool_cmd.forEach {
                cmdModel.addCmdInfo(it)
            }
            if (logConfig.custom_color.isEmpty()) {
                logConfig.custom_color.add(Default.DEFAULT_BG_COLOR)
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
            filterModel.cleanLines()
            logConfig.filter_rule.clear()
            logConfig.filter_rule.addAll(filterModel.getData())
            logConfig.uuid = UID.getUID()
            val contents = gson.toJson(logConfig)
            file.writeText(contents)
        } catch (e: FileNotFoundException) {
            //e.printStackTrace()
            logger.error("File(config.json) not found")
        }
    }

}
