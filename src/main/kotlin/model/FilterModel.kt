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
        datas.filter { it.enabled }.forEach {
            if (!str.isEmpty()) {
                str.append(",")
            }
            str.append(it.detail())
        }
        return str.toString()
    }

    fun getHighlight(): String {
        val str = StringBuilder()
        datas.filter { it.enabled }.forEach {
            when (it.regex) {
                1 -> {
                    str.append(it.text + "|")
                }
            }
        }
        return str.toString()
    }

    fun checkFilter(logInfo: LogContainer): Boolean {
        for (data in datas) {
            if (data.enabled) {
                when (data.regex) {
                    0 -> {
                        val stk = StringTokenizer(data.text, "|", false)
                        while (stk.hasMoreElements()) {
                            val token = stk.nextToken()
                            if (token.toLowerCase() in logInfo.strMsg.toLowerCase()) {
                                return false
                            }
                        }
                    }
                    1 -> {
                        val stk = StringTokenizer(data.text, "|", false)
                        while (stk.hasMoreElements()) {
                            val token = stk.nextToken()
                            if (token.toLowerCase() !in logInfo.strMsg.toLowerCase()) {
                                return false
                            }
                        }
                    }
                    2 -> {
                        val pattern = Pattern.compile(data.text)
                        val matcher = pattern.matcher(logInfo.strMsg)
                        if (!matcher.find()) {
                            return false
                        }
                    }
                }
            }
        }
        return true
    }

    fun hasFilter(): Boolean {
        return !datas.none { it.enabled }
    }
}