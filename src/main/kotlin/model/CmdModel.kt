package model

import bean.CmdInfo
import event.ObservableSubject
import event.Observer

class CmdModel : ObservableSubject<CmdInfo> {
    private val observers = arrayListOf<Observer<CmdInfo>>()
    private val data = arrayListOf<CmdInfo>()
    private var selectedIndex = 0

    override fun registerObserver(o: Observer<CmdInfo>) {
        observers.add(o)
    }

    override fun removeObserver(o: Observer<CmdInfo>) {
        observers.remove(o)
    }

    override fun notifyAllObservers() {
        observers.forEach {
            it.update(this)
        }
    }

    fun getData(): List<CmdInfo> {
        return data
    }

    fun addCmdInfo(filterInfo: CmdInfo) {
        data.add(filterInfo)
    }

    fun updateData() {
        notifyAllObservers()
    }

    fun setSelectedCmd(index: Int) {
        selectedIndex = index
    }

    fun getSelectedCmd(): String {
        return data[selectedIndex].cmd
    }
}