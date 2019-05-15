package model

import bean.FilterColor
import bean.FilterContainer
import bean.LogContainer
import interfces.ObservableSubject
import interfces.Observer
import java.util.*

class FilterModel : ObservableSubject<FilterContainer> {
    private val observers = ArrayList<Observer<FilterContainer>>()
    private val datas = ArrayList<FilterContainer>()
    private var filterType = TYPE_FILTER_HIGHLIGHT

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

    fun getFilerColors(): List<FilterColor> {
        val list = arrayListOf<FilterColor>()
        datas.filter { it.enabled }.forEach {
            val filterColor = FilterColor()
            filterColor.hightLight = it.text + "|"
            filterColor.color = it.color
            list.add(filterColor)
        }
        return list
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

    fun checkOrFilter(logInfo: LogContainer): Boolean {
        datas.filter { it.enabled }.forEach {
            val stk = StringTokenizer(it.text, "|", false)
            while (stk.hasMoreElements()) {
                val token = stk.nextToken()
                if (logInfo.strMsg.contains(token, true)) {
                    return true
                }
            }
        }
        return false
    }

    fun hasFilter(): Boolean {
        return !datas.none { it.enabled }
    }
}
