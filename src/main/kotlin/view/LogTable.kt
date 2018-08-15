package view

import bean.LogContainer
import interfces.IView
import interfces.ObservableSubject
import interfces.Observer
import model.LogModel
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.ActionListener
import java.util.*
import javax.swing.*
import javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
import javax.swing.event.ListSelectionListener
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer


class LogTable : JTable(), Observer<LogContainer>, IView {
    private val logger = LoggerFactory.getLogger(LogTable::class.java)
    private val colWidth = intArrayOf(20, 600)
    private var defaultModel = LogTableViewModel()

    private var realLineNumber = 1
    private var recordLineNumber = 1

    private var gotoItem: JMenuItem
    private var recLineItem: JMenuItem

    init {
        model = defaultModel
        isOpaque = false
        autoscrolls = false
        intercellSpacing = Dimension(0, 0)
        setShowGrid(false)
        setSelectionMode(MULTIPLE_INTERVAL_SELECTION)
        for (iIndex in 0 until columnCount) {
            getColumnModel().getColumn(iIndex).let {
                it.cellRenderer = LogCellRenderer()
                it.resizable = true
                it.maxWidth = colWidth[iIndex] * 1000
                it.minWidth = 0
                it.width = colWidth[iIndex]
                it.preferredWidth = colWidth[iIndex]
            }
        }
        val popup = JPopupMenu()
        gotoItem = JMenuItem("Go To")
        recLineItem = JMenuItem("Record Line")
        popup.add(gotoItem)
        popup.add(recLineItem)
        componentPopupMenu = popup
    }

    private val gotoActionListener = ActionListener {
        val inputContent = JOptionPane.showInputDialog(this, "Go To", recordLineNumber)
        if (inputContent != null) {
            val line = defaultModel.findRearLineByIndex(inputContent)
            showRow(line, true)
        }
    }

    private val recLineActionListener = ActionListener {
        recordLineNumber = realLineNumber
        logger.debug("record line number: $recordLineNumber")
    }

    private val listSelectionListener = ListSelectionListener {
        val lsm = it.source as ListSelectionModel
        if (!lsm.isSelectionEmpty) {
            val index = lsm.minSelectionIndex
            realLineNumber = defaultModel.findIndexByRearLine(index)
            logger.debug("select line number: $realLineNumber")
        }
    }

    override fun initListener() {
        logger.debug("initListener")
        gotoItem.addActionListener(gotoActionListener)
        recLineItem.addActionListener(recLineActionListener)
        selectionModel.addListSelectionListener(listSelectionListener)
    }

    override fun deinitListenr() {
        logger.debug("deinitListenr")
        gotoItem.removeActionListener(gotoActionListener)
        recLineItem.removeActionListener(recLineActionListener)
        selectionModel.removeListSelectionListener(listSelectionListener)
    }

    override fun update(s: ObservableSubject<LogContainer>) {
        logger.debug("update")
        if (s is LogModel) {
            defaultModel.setData(s.getData())
            defaultModel.setHighLightStr(s.getHighLightStr())
            defaultModel.fireTableDataChanged()
        }
    }

    private fun showRow(row: Int, bCenter: Boolean) {
        val nLastSelectedIndex = selectedRow
        changeSelection(row, 0, false, false)
        var nVisible: Int
        nVisible = if (nLastSelectedIndex <= row || nLastSelectedIndex == -1) {
            row + getVisibleRowCount() / 2
        } else {
            row - getVisibleRowCount() / 2
        }
        if (nVisible < 0) {
            nVisible = 0
        } else if (nVisible > rowCount - 1) {
            nVisible = rowCount - 1
        }
        showRow(nVisible)
    }

    private fun showRow(row: Int) {
        var row1 = row
        if (row1 < 0) row1 = 0
        if (row1 > rowCount - 1) row1 = rowCount - 1

        val rList = visibleRect
        val rCell = getCellRect(row1, 0, true)
        if (rList != null && rCell != null) {
            val scrollToRect = Rectangle(rList.getX().toInt(), rCell.getY().toInt(), rList.getWidth().toInt(), rCell.getHeight().toInt())
            scrollRectToVisible(scrollToRect)
        }
    }

    private fun getVisibleRowCount(): Int {
        return visibleRect.height / getRowHeight()
    }

    inner class LogTableViewModel : AbstractTableModel() {
        private var arData = ArrayList<LogContainer>()
        private val colName = arrayOf("Line", "Message")
        private var highLight = ""

        @Synchronized
        fun setData(data: ArrayList<LogContainer>) {
            arData.clear()
            arData.addAll(data)
        }

        @Synchronized
        override fun getRowCount(): Int {
            return arData.size
        }

        override fun getColumnCount(): Int {
            return colName.size
        }

        @Synchronized
        fun getLogColor(p0: Int): Int {
            return arData[p0].strColor
        }

        override fun getColumnName(col: Int): String {
            return colName[col]
        }

        @Synchronized
        override fun getValueAt(p0: Int, p1: Int): Any {
            return when (p1) {
                0 -> "${arData[p0].strLine}"
                1 -> arData[p0].strMsg
                else -> ""
            }
        }

        fun setHighLightStr(text: String) {
            highLight = text
        }

        fun getHighLightStr(): String {
            return highLight
        }

        fun findRearLineByIndex(row: String): Int {
            for ((index, log) in arData.withIndex()) {
                if ("${log.strLine}" == row) {
                    return index
                }
            }
            return 1
        }

        fun findIndexByRearLine(index: Int): Int {
            if (index >= 0 && index < arData.size) {
                return arData[index].strLine
            }
            return 1
        }
    }

    inner class LogCellRenderer : DefaultTableCellRenderer() {
        private var bChanged = false
        override fun getTableCellRendererComponent(p0: JTable?, p1: Any?, p2: Boolean, p3: Boolean, p4: Int, p5: Int): Component {
            val data = remakeData(p5, p1 as String)
            val component = super.getTableCellRendererComponent(p0, data, p2, p3, p4, p5)
            val strColor = (model as LogTableViewModel).getLogColor(p4)
            component.foreground = Color(strColor)
            return component
        }

        private fun remakeData(index: Int, text: String): String {
            if (index != 1) {
                return text
            }

            val high = (model as LogTableViewModel).getHighLightStr()
            bChanged = false
            val arColor = arrayOf("#00FF00", "#EEEE00", "#EE9A49", "#8A2BE2", "#EE1289")
            var strRet = remakeFind(text, high, arColor, true)
            if (bChanged) {
                strRet = "<html><nobr>$strRet</nobr></html>"
            }

            return strRet
        }

        private fun remakeFind(strText: String, strFind: String, arColor: Array<String>, bUseSpan: Boolean): String {
            if (strFind.isEmpty()) return strText

            var strText1 = strText
            val stk = StringTokenizer(strFind, "|")
            var newText: String
            var strToken: String
            var nIndex = 0

            while (stk.hasMoreElements()) {
                if (nIndex >= arColor.size)
                    nIndex = 0
                strToken = stk.nextToken()

                if (strText1.contains(strToken, true)) {
                    newText = if (bUseSpan)
                        "<span style=\"background-color:${arColor[nIndex]}\"><b>"
                    else
                        "<font color=${arColor[nIndex]}><b>"
                    newText += strToken
                    newText += if (bUseSpan)
                        "</b></span>"
                    else
                        "</b></font>"
                    strText1 = strText1.replace(strToken, newText)
                    bChanged = true
                    nIndex++
                }
            }
            return strText1
        }
    }
}
