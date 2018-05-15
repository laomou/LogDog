package model

import bean.LogContainer
import interfces.ObservableSubject
import interfces.Observer


class LogModel : ObservableSubject<LogContainer> {
    private val observers = ArrayList<Observer<LogContainer>>()
    private var datas = ArrayList<LogContainer>()
    private var highLight = ""

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

    fun getData(): ArrayList<LogContainer> {
        return datas
    }

    fun addLogInfo(loginfo: LogContainer) {
        datas.add(loginfo)
    }

    fun updateData() {
        notifyAllObservers()
    }

    fun cleanFilterData() {
        datas.clear()
    }

    fun setData(data: ArrayList<LogContainer>) {
        datas.clear()
        datas.addAll(data)
    }

    fun getDataSize(): Int {
        return datas.size
    }

    fun setHighLightStr(text: String) {
        highLight = text
    }

    fun getHighLightStr(): String {
        return highLight
    }
}