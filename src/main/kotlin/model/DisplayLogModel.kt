package model

import bean.LogInfo
import event.ObservableSubject
import event.Observer
import utils.DefaultConfig


class DisplayLogModel : ObservableSubject<LogInfo> {
    private val observers = arrayListOf<Observer<LogInfo>>()
    private var data = arrayListOf<LogInfo>()

    override fun registerObserver(o: Observer<LogInfo>) {
        observers.add(o)
    }

    override fun removeObserver(o: Observer<LogInfo>) {
        observers.remove(o)
    }

    override fun notifyAllObservers() {
        observers.forEach {
            it.update(this)
        }
    }

    @Synchronized
    fun markDataHide() {
        data.forEach {
            it.show = false
        }
    }

    @Synchronized
    fun markDataShow() {
        data.forEach {
            it.show = true
            it.filterColor = DefaultConfig.DEFAULT_BG_COLOR
        }
    }

    @Synchronized
    fun getData(): List<LogInfo> {
        return data
    }

    @Synchronized
    fun getDisplayData(): List<LogInfo> {
        return data.filter { it.show }
    }

    @Synchronized
    fun addLogInfo(logInfo: LogInfo) {
        data.add(logInfo)
    }

    fun updateData() {
        notifyAllObservers()
    }

    @Synchronized
    fun cleanData() {
        data.forEach {
            it.filters.clear()
        }
        data.clear()
    }

    @Synchronized
    fun getItemData(index: Int): LogInfo? {
        if (index >= data.size) {
            return null
        }
        return data[index]
    }

    @Synchronized
    fun getDataSize(): Int {
        return data.size
    }
}