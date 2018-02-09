package model

import bean.LogContainer
import interfces.ObservableSubject
import interfces.Observer


class LogModel : ObservableSubject<LogContainer> {

    private val observers = ArrayList<Observer<LogContainer>>()
    private var datas = ArrayList<LogContainer>()


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

    fun getDatas(): ArrayList<LogContainer> {
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
        this.datas.addAll(data)
    }
}