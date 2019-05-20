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
            Pair(TYPE_FILTER_TAG1, ArrayList()),
            Pair(TYPE_FILTER_TAG2, ArrayList()),
            Pair(TYPE_FILTER_TAG3, ArrayList()))
    private var filterType = TYPE_FILTER_TAG1

    companion object {
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
    private fun data(): ArrayList<FilterInfo> = mapData.getValue(filterType)

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
            filterInfo.state = 2
            data()[index] = filterInfo
        }
    }

    @Synchronized
    fun removeFilterInfo(filterInfo: FilterInfo, remove: Boolean = false) {
        val index = data().indexOf(filterInfo)
        if (index != -1) {
            if (remove) {
                data().removeAt(index)
            } else {
                filterInfo.state = 3
                data()[index] = filterInfo
            }
        }
    }

    @Synchronized
    fun enableFilterInfo(filterInfo: FilterInfo) {
        val index = data().indexOf(filterInfo)
        if (index != -1) {
            filterInfo.state = 4
            data()[index] = filterInfo
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
        when (filterType) {
            TYPE_FILTER_TAG1,
            TYPE_FILTER_TAG2,
            TYPE_FILTER_TAG3 -> {
                str.append("filterType: Or")
                data().filter { it.enabled }.forEach {
                    if (str.isNotEmpty()) {
                        str.append(",")
                    }
                    str.append(it.detail())
                }
            }
            else -> {
                str.append("filterType: None")
            }
        }
        return str.toString()
    }

    @Synchronized
    fun getChangesFilters(): List<FilterInfo> {
        return data().filter { it.state >= 1 }
    }

    @Synchronized
    fun getEnableNewFilters(): List<FilterInfo> {
        return data().filter { it.enabled && it.state == 1 }
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
                    val pattern = Pattern.compile(it1.text)
                    val matcher = pattern.matcher(line)
                    if (matcher.matches()) {
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

    fun getFilterType(): Int {
        return filterType
    }

    fun toggleFilterType() {
        filterType = if (filterType >= TYPE_FILTER_TAG3) {
            TYPE_FILTER_TAG1
        } else {
            ++filterType
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
                    val pattern = Pattern.compile(it1.text)
                    val matcher = pattern.matcher(logInfo.strMsg)
                    if (matcher.matches()) {
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
        data().forEach {
            when (it.type) {
                1 -> {
                    val stk = StringTokenizer(it.text, "|", false)
                    while (stk.hasMoreElements()) {
                        val token = stk.nextToken()
                        if (logInfo.strMsg.contains(token, true)) {
                            it.lines.add(logInfo.strLine)
                            logInfo.filters.add(it.uuid)
                        }
                    }
                }
                2 -> {
                    val pattern = Pattern.compile(it.text)
                    val matcher = pattern.matcher(logInfo.strMsg)
                    if (matcher.matches()) {
                        it.lines.add(logInfo.strLine)
                        logInfo.filters.add(it.uuid)
                    }
                }
            }
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
                    val pattern = Pattern.compile(filterInfo.text)
                    val matcher = pattern.matcher(logInfo.strMsg)
                    if (matcher.matches()) {
                        filterInfo.lines.add(logInfo.strLine)
                        logInfo.filters.add(filterInfo.uuid)
                    }
                }
            }
        }
    }

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
                    val pattern = Pattern.compile(filterInfo.text)
                    val matcher = pattern.matcher(logInfo.strMsg)
                    if (matcher.matches()) {
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
    fun hasNewFilter(): Boolean {
        return !data().none { it.enabled && it.state in 1..3 }
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
