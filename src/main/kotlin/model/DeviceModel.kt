package model

import bean.DeviceInfo
import event.ObservableSubject
import event.Observer

class DeviceModel : ObservableSubject<DeviceInfo> {
    private val observers = arrayListOf<Observer<DeviceInfo>>()
    private val data = arrayListOf<DeviceInfo>()
    private var selectedIndex = 0

    override fun registerObserver(o: Observer<DeviceInfo>) {
        observers.add(o)
    }

    override fun removeObserver(o: Observer<DeviceInfo>) {
        observers.remove(o)
    }

    override fun notifyAllObservers() {
        observers.forEach {
            it.update(this)
        }
    }

    fun getData(): List<DeviceInfo> {
        return data
    }

    fun addDeviceInfo(filterInfo: DeviceInfo) {
        data.add(filterInfo)
    }

    fun updateData() {
        notifyAllObservers()
    }

    @Synchronized
    fun cleanData() {
        data.clear()
    }

    fun setSelectedDevice(index: Int) {
        selectedIndex = index
    }

    fun getSelectedDevice(): String {
        return data[selectedIndex].device
    }
}