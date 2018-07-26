package model

import bean.FilterContainer
import bean.LogContainer
import interfces.ObservableSubject
import interfces.Observer
import java.util.*
import java.util.regex.Pattern

class FilterModel : ObservableSubject<FilterContainer> {
    private val observers = ArrayList<Observer<FilterContainer>>()
    private val datas = ArrayList<FilterContainer>()
    private var filterType = TYPE_FILTER_HIGHLIGHT

    companion object {
        val TYPE_FILTER_HIGHLIGHT = 0
        val TYPE_FILTER_OR = 1
        val TYPE_FILTER_AND = 2
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

    fun addFilterInfo(filterInfo: FilterContainer) {
        datas.add(filterInfo)
    }

    fun removeFilterInfo(data: FilterContainer) {
        val index = datas.indexOf(data)
        if (index != -1) {
            datas.removeAt(index)
        }
    }

    fun editFilterInfo(data: FilterContainer) {
        val index = datas.indexOf(data)
        if (index != -1) {
            datas[index] = data
        }
    }

    fun getData(): ArrayList<FilterContainer> {
        return datas
    }

    fun updateData() {
        notifyAllObservers()
    }

    fun getEnableFilter(): String {
        val str = StringBuilder()
        when (filterType) {
            TYPE_FILTER_OR -> str.append("filterType: Or")
            TYPE_FILTER_AND -> str.append("filterType: And")
            else -> str.append("filterType: HighLight")
        }
        datas.filter { it.enabled }.forEach {
            if (!str.isEmpty()) {
                str.append(",")
            }
            str.append(it.detail())
        }
        return str.toString()
    }

    fun getHighlightStr(): String {
        val str = StringBuilder()
        datas.filter { it.enabled }.forEach {
            when (it.regex) {
                0 -> {
                    str.append(it.text + "|")
                }
            }
        }
        return str.toString()
    }

    fun setFilterType(type: Int) {
        filterType = type
    }

    fun getFilterType() : Int {
        return filterType
    }

    fun toggleFilterType() {
        if (filterType > TYPE_FILTER_AND) {
            filterType = TYPE_FILTER_HIGHLIGHT
        }
        filterType++
    }

    fun checkAndFilter(logInfo: LogContainer): Boolean {
        datas.filter { it.enabled }.forEach {
            when (it.regex) {
                0 -> {
                    val stk = StringTokenizer(it.text, "|", false)
                    while (stk.hasMoreElements()) {
                        val token = stk.nextToken()
                        if (!logInfo.strMsg.contains(token, true)) {
                            return false
                        }
                    }
                }
                1 -> {
                    val pattern = Pattern.compile(it.text)
                    val matcher = pattern.matcher(logInfo.strMsg)
                    if (!matcher.find()) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun checkOrFilter(logInfo: LogContainer): Boolean {
        datas.filter { it.enabled }.forEach {
            when (it.regex) {
                0 -> {
                    val stk = StringTokenizer(it.text, "|", false)
                    while (stk.hasMoreElements()) {
                        val token = stk.nextToken()
                        if (logInfo.strMsg.contains(token, true)) {
                            return true
                        }
                    }
                }
                1 -> {
                    val pattern = Pattern.compile(it.text)
                    val matcher = pattern.matcher(logInfo.strMsg)
                    if (matcher.find()) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun hasFilter(): Boolean {
        return !datas.none { it.enabled }
    }
}
