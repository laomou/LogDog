package model

import bean.FilterColor
import bean.LogContainer
import interfces.ObservableSubject
import interfces.Observer


class DisplayLogModel : ObservableSubject<LogContainer> {
    private val observers = ArrayList<Observer<LogContainer>>()
    private var datas = ArrayList<LogContainer>()
    private var filterColors = ArrayList<FilterColor>()

    override fun registerObserver(o: Observer<LogContainer>) {
        observers.add(o)
    }

    override fun removeObserver(o: Observer<LogContainer>) {
        observers.remove(o)
    }

    override fun notifyAllObservers() {
        observers.forEach {
            it.update(this)
        }
    }

    fun getData(): List<LogContainer> {
        return datas
    }

    fun addLogInfo(logInfo: LogContainer) {
        datas.add(logInfo)
    }

    fun updateData() {
        notifyAllObservers()
    }

    fun cleanData() {
        datas.clear()
    }

    fun setData(data: List<LogContainer>) {
        datas.clear()
        datas.addAll(data)
    }

    fun getFilterColors(): List<FilterColor> {
        return filterColors
    }

    fun getDataSize(): Int {
        return datas.size
    }

    fun setFilterColors(colorList: List<FilterColor>) {
        filterColors.clear()
        filterColors.addAll(colorList)
    }
}