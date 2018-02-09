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
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPopupMenu
import javax.swing.JTable
import javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer


class LogTable : JTable(), Observer<LogContainer>, IView {
    private val logger = LoggerFactory.getLogger(LogTable::class.java)
    private val colWidth = intArrayOf(30, 600)
    private var defaultModel = LogTableViewModel()

    private var gotoItem: JMenuItem? = null

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
        popup.add(gotoItem)
        componentPopupMenu = popup
    }

    private val gotoActionListener = ActionListener {
        val inputContent = JOptionPane.showInputDialog(null, "Go To", "1")
        if (inputContent != null) {
            val line = Integer.valueOf(inputContent)
            showRow(line - 1, true)
        }
    }

    override fun initListener() {
        logger.debug("initListener")

        gotoItem?.addActionListener(gotoActionListener)
    }

    override fun deinitListenr() {
        logger.debug("deinitListenr")
        gotoItem?.removeActionListener(gotoActionListener)
    }

    override fun update(s: ObservableSubject<LogContainer>) {
        logger.debug("update")
        if (s is LogModel) {
            defaultModel.setData(s.getDatas())
            defaultModel.setHighLight(s.highLight)
            defaultModel.fireTableDataChanged()
        }
    }

    private fun showRow(row: Int, bCenter: Boolean) {
        val nLastSelectedIndex = selectedRow

        changeSelection(row, 0, false, false)
        var nVisible: Int
        if (nLastSelectedIndex <= row || nLastSelectedIndex == -1) {
            nVisible = row + getVisibleRowCount() / 2
        } else {
            nVisible = row - getVisibleRowCount() / 2
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

        fun setData(data: ArrayList<LogContainer>) {
            arData.clear()
            arData.addAll(data)
        }

        override fun getRowCount(): Int {
            return arData.size
        }

        override fun getColumnCount(): Int {
            return colName.size
        }

        fun getLogColor(p0: Int): Int {
            return arData[p0].getColor()
        }

        override fun getColumnName(col: Int): String {
            return colName[col]
        }

        override fun getValueAt(p0: Int, p1: Int): Any {
            if (p1 == 0) {
                return "" + (p0 + 1)
            } else if (p1 == 1) {
                return arData[p0].getData()
            }
            return ""
        }

        fun setHighLight(text: String) {
            highLight = text
        }

        fun getHighLight(): String {
            return highLight
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
            val high = (model as LogTableViewModel).getHighLight()
            bChanged = false
            var strRet = remakeFind(text, high, "#00FF00", true)
            if (bChanged) {
                strRet = "<html><nobr>$strRet</nobr></html>"
            }
            return strRet
        }

        private fun remakeFind(strText: String, strFind: String?, strColor: String, bUseSpan: Boolean): String {
            if (strFind == null || strFind.isEmpty()) return strText

            var strText1 = strText
            val stk = StringTokenizer(strFind, ",")
            var newText: String
            var strToken: String

            while (stk.hasMoreElements()) {
                strToken = stk.nextToken()

                if (strText1.toLowerCase().contains(strToken.toLowerCase())) {
                    newText = if (bUseSpan)
                        "<span style=\"background-color:$strColor\"><b>"
                    else
                        "<font color=$strColor><b>"
                    newText += strToken
                    newText += if (bUseSpan)
                        "</b></span>"
                    else
                        "</b></font>"
                    strText1 = strText.replace(strToken, newText)
                    bChanged = true
                }
            }
            return strText1
        }
    }
}
