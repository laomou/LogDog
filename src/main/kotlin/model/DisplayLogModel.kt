package model

import bean.Default
import bean.LogContainer
import interfces.ObservableSubject
import interfces.Observer


class DisplayLogModel : ObservableSubject<LogContainer> {
    private val observers = arrayListOf<Observer<LogContainer>>()
    private var datas = arrayListOf<LogContainer>()
    private var reset = false

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

    @Synchronized
    fun tryShowData() {
        if (reset) {
            datas.forEach {
                it.show = false
            }
            reset = false
        }
    }

    @Synchronized
    fun showData() {
        datas.forEach {
            it.show = true
            it.filterColor = Default.DEFAULT_BG_COLOR
        }
        reset = true
    }

    @Synchronized
    fun getData(): List<LogContainer> {
        return datas
    }

    @Synchronized
    fun getDisplayData(): List<LogContainer> {
        return datas.filter { it.show }
    }

    @Synchronized
    fun addLogInfo(logInfo: LogContainer) {
        datas.add(logInfo)
    }

    fun updateData() {
        notifyAllObservers()
    }

    @Synchronized
    fun cleanData() {
        datas.forEach {
            it.filters.clear()
        }
        datas.clear()
    }

    @Synchronized
    fun getItemData(index: Int): LogContainer {
        return datas[index]
    }

    @Synchronized
    fun getDataSize(): Int {
        return datas.size
    }
}