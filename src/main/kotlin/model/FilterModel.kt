package model

import bean.Default
import bean.FilterContainer
import bean.LogContainer
import interfces.ObservableSubject
import interfces.Observer
import java.util.*

class FilterModel : ObservableSubject<FilterContainer> {
    private val observers = ArrayList<Observer<FilterContainer>>()
    private val datas = LinkedList<FilterContainer>()
    private var filterType = TYPE_FILTER_OR

    companion object {
        const val TYPE_FILTER_HIGHLIGHT = 0
        const val TYPE_FILTER_OR = 1
    }

    override fun registerObserver(o: Observer<FilterContainer>) {
        observers.add(o)
    }

    override fun removeObserver(o: Observer<FilterContainer>) {
        observers.remove(o)
    }

    override fun notifyAllObservers() {
        observers.forEach {
            it.update(this)
        }
    }

    @Synchronized
    fun loadFilterInfo(filterInfo: FilterContainer) {
        datas.add(filterInfo)
    }

    @Synchronized
    fun addFilterInfo(filterInfo: FilterContainer) {
        filterInfo.state = 1
        datas.add(filterInfo)
    }

    @Synchronized
    fun removeFilterInfo(data: FilterContainer) {
        val index = datas.indexOf(data)
        if (index != -1) {
            datas.removeAt(index)
        }
    }

    @Synchronized
    fun editFilterInfo(data: FilterContainer) {
        val index = datas.indexOf(data)
        if (index != -1) {
            data.state = 2
            datas[index] = data
        }
    }

    @Synchronized
    fun enableFilterInfo(data: FilterContainer) {
        val index = datas.indexOf(data)
        if (index != -1) {
            data.state = 3
            datas[index] = data
        }
    }

    @Synchronized
    fun getData(): List<FilterContainer> {
        return datas
    }

    @Synchronized
    fun findItemDataByUUID(uuid: String): FilterContainer? {
        return datas.find { it.uuid == uuid }
    }

    fun updateData() {
        notifyAllObservers()
    }

    @Synchronized
    fun getEnableFilterString(): String {
        val str = StringBuilder()
        when (filterType) {
            TYPE_FILTER_OR -> str.append("filterType: Or")
            else -> str.append("filterType: HL")
        }
        datas.filter { it.enabled }.forEach {
            if (!str.isEmpty()) {
                str.append(",")
            }
            str.append(it.detail())
        }
        return str.toString()
    }

    @Synchronized
    fun getEnableFilters(): List<FilterContainer> {
        return datas.filter { it.enabled }
    }

    @Synchronized
    fun getChangesFilters(): List<FilterContainer> {
        return datas.filter { it.state >= 1 }
    }

    @Synchronized
    fun getEnableNewFilters(): List<FilterContainer> {
        return datas.filter { it.enabled && it.state == 1 }
    }

    @Synchronized
    fun findFilersColor(line: String): String {
        datas.filter { it.enabled }.forEach { it1 ->
            val stk = StringTokenizer(it1.text, "|", false)
            while (stk.hasMoreElements()) {
                val token = stk.nextToken()
                if (line.contains(token, true)) {
                    return it1.color
                }
            }
        }
        return Default.DEFAULT_BG_COLOR
    }

    fun getFilterType(): Int {
        return filterType
    }

    fun toggleFilterType() {
        filterType = if (filterType == TYPE_FILTER_HIGHLIGHT) {
            TYPE_FILTER_OR
        } else {
            TYPE_FILTER_HIGHLIGHT
        }
    }

    @Synchronized
    fun checkEnableOrFilter(logInfo: LogContainer): Boolean {
        datas.filter { it.enabled }.forEach { it1 ->
            val stk = StringTokenizer(it1.text, "|", false)
            while (stk.hasMoreElements()) {
                val token = stk.nextToken()
                if (logInfo.strMsg.contains(token, true)) {
                    return true
                }
            }
        }
        return false
    }

    @Synchronized
    fun updateLineInfo(logInfo: LogContainer) {
        datas.forEach {
            val stk = StringTokenizer(it.text, "|", false)
            while (stk.hasMoreElements()) {
                val token = stk.nextToken()
                if (logInfo.strMsg.contains(token, true)) {
                    it.lines.add(logInfo.strLine)
                    logInfo.filters.add(it.uuid)
                }
            }
        }
    }

    @Synchronized
    fun updateLineInfo(filterInfo: FilterContainer, logInfo: LogContainer) {
        if (filterInfo.enabled) {
            val stk = StringTokenizer(filterInfo.text, "|", false)
            while (stk.hasMoreElements()) {
                val token = stk.nextToken()
                if (logInfo.strMsg.contains(token, true)) {
                    filterInfo.lines.add(logInfo.strLine)
                    logInfo.filters.add(filterInfo.uuid)
                }
            }
        }
    }

    fun updateShowInfo(filterInfo: FilterContainer, logInfo: LogContainer) {
        if (filterInfo.enabled) {
            val stk = StringTokenizer(filterInfo.text, "|", false)
            while (stk.hasMoreElements()) {
                val token = stk.nextToken()
                if (logInfo.strMsg.contains(token, true)) {
                    logInfo.filterColor = filterInfo.color
                    logInfo.show = true
                    break
                }
            }
        }
    }

    @Synchronized
    fun cleanLines() {
        datas.forEach { it.lines.clear() }
    }

    @Synchronized
    fun hasFilter(): Boolean {
        return !datas.none { it.enabled }
    }


    @Synchronized
    fun hasNewFilter(): Boolean {
        return !datas.none { it.enabled && it.state in 1..2 }
    }
}
