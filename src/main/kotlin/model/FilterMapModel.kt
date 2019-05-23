package model

import bean.FilterInfo
import bean.LogInfo
import interfces.ObservableSubject
import interfces.Observer
import utils.DefaultConfig
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class FilterMapModel : ObservableSubject<FilterInfo> {
    private val observers = arrayListOf<Observer<FilterInfo>>()
    private val mapData = hashMapOf<Int, ArrayList<FilterInfo>>(
            Pair(TYPE_FILTER_NONE, ArrayList()),
            Pair(TYPE_FILTER_TAG1, ArrayList()),
            Pair(TYPE_FILTER_TAG2, ArrayList()),
            Pair(TYPE_FILTER_TAG3, ArrayList()))
    private var filterTag = TYPE_FILTER_TAG1

    companion object {
        const val TYPE_FILTER_NONE = 0
        const val TYPE_FILTER_TAG1 = 1
        const val TYPE_FILTER_TAG2 = 2
        const val TYPE_FILTER_TAG3 = 3
    }

    override fun registerObserver(o: Observer<FilterInfo>) {
        observers.add(o)
    }

    override fun removeObserver(o: Observer<FilterInfo>) {
        observers.remove(o)
    }

    override fun notifyAllObservers() {
        observers.forEach {
            it.update(this)
        }
    }

    @Synchronized
    private fun data(): ArrayList<FilterInfo> = mapData.getValue(filterTag)

    @Synchronized
    private fun data(filterTag: Int): ArrayList<FilterInfo> = mapData.getValue(filterTag)

    @Synchronized
    fun loadFilterInfo(type: Int, arrayList: ArrayList<FilterInfo>) {
        mapData[type] = arrayList
    }

    @Synchronized
    fun addFilterInfo(filterInfo: FilterInfo) {
        filterInfo.state = 1
        data().add(filterInfo)
    }

    @Synchronized
    fun editFilterInfo(filterInfo: FilterInfo) {
        val index = data().indexOf(filterInfo)
        if (index != -1) {
            data()[index].state = 2
            data()[index].color = filterInfo.color
            data()[index].text = filterInfo.text
            data()[index].type = filterInfo.type
            data()[index].lines.clear()
        }
    }

    @Synchronized
    fun removeFilterInfo(filterInfo: FilterInfo) {
        val index = data().indexOf(filterInfo)
        if (index != -1) {
            if (!data()[index].enabled || data()[index].lines.isEmpty()) {
                data().removeAt(index)
            } else {
                data()[index].state = 3
            }

        }
    }

    @Synchronized
    fun enableFilterInfo(filterInfo: FilterInfo) {
        val index = data().indexOf(filterInfo)
        if (index != -1) {
            data()[index].state = 4
            data()[index].enabled = filterInfo.enabled
        }
    }

    @Synchronized
    fun getData(): List<FilterInfo> {
        return data()
    }

    @Synchronized
    fun getMapData(): Map<Int, ArrayList<FilterInfo>> {
        return mapData
    }

    @Synchronized
    fun findItemDataByUUID(uuid: String): FilterInfo? {
        return data().find { it.uuid == uuid }
    }

    fun updateData() {
        notifyAllObservers()
    }

    @Synchronized
    fun getEnableFilterString(): String {
        val str = StringBuilder()
        when (filterTag) {
            TYPE_FILTER_TAG1,
            TYPE_FILTER_TAG2,
            TYPE_FILTER_TAG3 -> {
                str.append("Filter: Or (")
                data().filter { it.enabled }.forEach {
                    str.append(it.detail())
                    if (str.isNotEmpty()) {
                        str.append(",")
                    }
                }
                str.append(")")
            }
            else -> {
                str.append("Filter: None")
            }
        }
        return str.toString()
    }

    @Synchronized
    fun getEnableFilters(): List<FilterInfo> {
        return data().filter { it.enabled }
    }

    @Synchronized
    fun getChangesFilters(): List<FilterInfo> {
        return data().filter { it.state >= 1 }
    }

    @Synchronized
    fun getNewOrEditedFilters(): List<FilterInfo> {
        return data().filter { it.state in 1..3 }
    }

    @Synchronized
    fun findFilersColor(line: String): String {
        data().filter { it.enabled }.forEach { it1 ->
            when (it1.type) {
                1 -> {
                    val stk = StringTokenizer(it1.text, "|", false)
                    while (stk.hasMoreElements()) {
                        val token = stk.nextToken()
                        if (line.contains(token, true)) {
                            return it1.color
                        }
                    }
                }
                2 -> {
                    if (Pattern.matches(it1.text, line)) {
                        return it1.color
                    }
                }
                else -> {
                    return DefaultConfig.DEFAULT_BG_COLOR
                }
            }
        }
        return DefaultConfig.DEFAULT_BG_COLOR
    }

    fun getFilterTag(): Int {
        return filterTag
    }

    fun toggleFilterTag() {
        filterTag = if (filterTag >= TYPE_FILTER_TAG3) {
            TYPE_FILTER_TAG1
        } else {
            ++filterTag
        }
    }

    @Synchronized
    fun checkEnableOrFilter(logInfo: LogInfo): Boolean {
        data().filter { it.enabled }.forEach { it1 ->
            when (it1.type) {
                1 -> {
                    val stk = StringTokenizer(it1.text, "|", false)
                    while (stk.hasMoreElements()) {
                        val token = stk.nextToken()
                        if (logInfo.strMsg.contains(token, true)) {
                            return true
                        }
                    }
                }
                2 -> {
                    if (Pattern.matches(it1.text, logInfo.strMsg)) {
                        return true
                    }
                }
                else -> {
                    return false
                }
            }
        }
        return false
    }

    @Synchronized
    fun updateLineInfo(logInfo: LogInfo) {
        mapData.values.forEach {
            it.forEach { it1 ->
                when (it1.type) {
                1 -> {
                    val stk = StringTokenizer(it1.text, "|", false)
                    while (stk.hasMoreElements()) {
                        val token = stk.nextToken()
                        if (logInfo.strMsg.contains(token, true)) {
                            it1.lines.add(logInfo.strLine)
                            logInfo.filters.add(it1.uuid)
                        }
                    }
                }
                2 -> {
                    if (Pattern.matches(it1.text, logInfo.strMsg)) {
                        it1.lines.add(logInfo.strLine)
                        logInfo.filters.add(it1.uuid)
                    }
                }
            } }
        }
    }

    @Synchronized
    fun updateLineInfo(filterInfo: FilterInfo, logInfo: LogInfo) {
        if (filterInfo.enabled) {
            when (filterInfo.type) {
                1 -> {
                    val stk = StringTokenizer(filterInfo.text, "|", false)
                    while (stk.hasMoreElements()) {
                        val token = stk.nextToken()
                        if (logInfo.strMsg.contains(token, true)) {
                            filterInfo.lines.add(logInfo.strLine)
                            logInfo.filters.add(filterInfo.uuid)
                        }
                    }
                }
                2 -> {
                    if (Pattern.matches(filterInfo.text, logInfo.strMsg)) {
                        filterInfo.lines.add(logInfo.strLine)
                        logInfo.filters.add(filterInfo.uuid)
                    }
                }
            }
        }
    }

    @Synchronized
    fun updateShowInfo(filterInfo: FilterInfo, logInfo: LogInfo) {
        if (filterInfo.enabled) {
            when (filterInfo.type) {
                1 -> {
                    val stk = StringTokenizer(filterInfo.text, "|", false)
                    while (stk.hasMoreElements()) {
                        val token = stk.nextToken()
                        if (logInfo.strMsg.contains(token, true)) {
                            logInfo.filterColor = filterInfo.color
                            logInfo.show = true
                        }
                    }
                }
                2 -> {
                    if (Pattern.matches(filterInfo.text, logInfo.strMsg)) {
                        logInfo.filterColor = filterInfo.color
                        logInfo.show = true
                    }
                }
            }
        }
        if (filterInfo.state == 3) {
            logInfo.filterColor = DefaultConfig.DEFAULT_BG_COLOR
            logInfo.show = false
        }
    }

    @Synchronized
    fun cleanLines() {
        data().forEach { it.lines.clear() }
    }

    @Synchronized
    fun hasFilter(): Boolean {
        return !data().none { it.enabled }
    }

    @Synchronized
    fun hasNewOrEditedFilter(): Boolean {
        return !data().none { it.state in 1..3 }
    }

    @Synchronized
    fun hasDelFilter(): Boolean {
        return !data().none { it.state == 3 }
    }

    @Synchronized
    fun doDelFilter() {
        data().removeIf { it.state == 3 }
    }
}
