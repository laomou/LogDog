package controller

import bean.ConstCmd
import bean.LogContainer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import interfces.CustomActionListener
import interfces.CustomEvent
import model.CmdModel
import model.FilterModel
import model.LogModel
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

    private var logConfig = LogToolConfig()

    private val logParser = LogCatParser()

    private val filterLock = java.lang.Object()
    private val fileLock = java.lang.Object()

    private val STATUS_CHANGE = 1
    private val STATUS_PARSING = 2
    private val STATUS_READY = 4
    @Volatile
    private var nChangedFilter = STATUS_READY

    private var arLogList = ArrayList<LogContainer>()


    private val logModel = LogModel()
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


    private var mainWindow = MainWindow(logModel, filterModel, cmdModel)


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
                saveFilterData()
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
        loadFilterData()
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
            logger.debug("logFilterParseThread -int-")
            try {
                while (filterLoop) {
                    synchronized(filterLock) {
                        nChangedFilter = STATUS_READY

                        logger.debug("filterLock->wait")
                        mainWindow.setStatus("Idle")
                        filterLock.wait()

                        val filter = filterModel.getEnableFilter()
                        logger.debug("parsing $filter")
                        mainWindow.setStatus("Parsing $filter")
                        nChangedFilter = STATUS_PARSING

                        logModel.cleanFilterData()

                        if (!filterModel.hasFilter()) {
                            logModel.setData(arLogList)
                            logger.debug("updateData(no filter)")
                            updateTableData()
                            nChangedFilter = STATUS_READY
                        }

                        val nRowCount = arLogList.size
                        logger.debug("arFullLogList->size: " + nRowCount)

                        for (nIndex in 0 until nRowCount) {
                            if (nIndex % 10000 == 0) {
                                Thread.sleep(1)
                            }

                            if (nChangedFilter == STATUS_CHANGE) {
                                break
                            }

                            val logInfo = arLogList[nIndex]

                            addFilterLogInfo(logInfo)
                        }

                        logger.debug("arFilterLogList->size: " + logModel.getDataSize())

                        if (nChangedFilter == STATUS_PARSING) {
                            nChangedFilter = STATUS_READY
                            logger.debug("updateData")
                            updateTableData()
                        }
                    }
                }
            } catch (e: Exception) {
                //e.printStackTrace()
                logger.warn(e.toString())
            }
            logger.debug("logFilterParseThread -out-")
        })
        filterThread?.name = "filterThread"
        filterThread?.start()
    }

    private fun logFileReadThread() {
        fileReadThread = Thread(Runnable {
            logger.debug("logFileReadThread -int-")
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
                        var nLine = arLogList.size + 1
                        while (let { strLine = br.readLine(); strLine != null }) {
                            val logInfo = logParser.parse(strLine!!)
                            logInfo.strLine = "" + nLine++
                            addLogInfo(logInfo)
                            addFilterLogInfo(logInfo)
                        }
                    }

                    synchronized(filterLock) {
                        if (!filterModel.hasFilter()) {
                            logModel.setData(arLogList)
                        }
                        logger.debug("updateData")
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
            logger.debug("logFileReadThread -out-")
        })
        fileReadThread?.name = "fileReadThread"
        fileReadThread?.start()
    }

    private fun logCatThread() {
        logCatThread = Thread(Runnable {
            logger.debug("logCatThread -in-")
            try {
                val cmd = "${logConfig.tool_path} ${cmdModel.getSelectedCmd()}"
                logger.debug("logCatThread run-> $cmd")
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
                while (let { strLine = br.readLine(); strLine != null }) {
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
            logger.debug("logCatThread -out-")
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
        logger.debug("parseFile: " + fileName)
        fileLoadThread = Thread(Runnable {
            val file = File(fileName)
            var nIndex = 1
            logger.debug("parseFile start")
            mainWindow.setStatus("Parsing")
            file.forEachLine {
                val logInfo = logParser.parse(it)
                logInfo.strLine = "" + nIndex++
                if (logInfo.valid) {
                    addLogInfo(logInfo)
                }
            }
            logger.debug("parseFile->size: " + arLogList.size)
            runFilter()
            mainWindow.setStatus("Parse complete")
            logger.debug("parseFile end")
        })
        fileLoadThread?.name = "fileLoadThread"
        fileLoadThread?.start()
    }

    private fun cleanData() {
        arLogList.clear()
        logModel.cleanFilterData()
        logModel.updateData()
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
        logModel.highLight = filterModel.getHighlight()
        logModel.updateData()
    }

    private fun addLogInfo(loginfo: LogContainer) {
        synchronized(filterLock) {
            arLogList.add(loginfo)
        }
    }

    private fun addFilterLogInfo(loginfo: LogContainer) {
        synchronized(filterLock) {
            if (filterModel.checkFilter(loginfo)) {
                logModel.addLogInfo(loginfo)
            }
        }
    }

    private fun makeFilename(): String {
        val now = Date()
        val format = SimpleDateFormat("yyyyMMdd_HHmmss")
        return "LogDog_" + format.format(now) + ".txt"
    }

    private fun loadFilterData() {
        try {
            val file = File("config.json")
            val contents = file.readText()
            val gson = Gson()
            val type = object : TypeToken<LogToolConfig>() {}.type
            val config: LogToolConfig = gson.fromJson(contents, type)
            logConfig.copy(config)
            logConfig.filter_rule.forEach {
                filterModel.addFilterInfo(it)
            }
            logConfig.tool_cmd.forEach {
                cmdModel.addCmdInfo(it)
            }
        } catch (e : FileNotFoundException) {
            //e.printStackTrace()
            logger.error("File(config.json) not found")
        }

    }

    private fun saveFilterData() {
        try {
            val file = File("config.json")
            val gson = Gson()
            logConfig.filter_rule = filterModel.getData()
            val contents = gson.toJson(logConfig)
            file.writeText(contents)
        } catch (e : FileNotFoundException) {
            //e.printStackTrace()
            logger.error("File(config.json) not found")
        }
    }

}
