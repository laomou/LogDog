package model

import bean.CmdContainer
import interfces.ObservableSubject
import interfces.Observer

class CmdModel : ObservableSubject<CmdContainer> {
    private val observers = arrayListOf<Observer<CmdContainer>>()
    private val datas = arrayListOf<CmdContainer>()
    private var selectedIndex = 0

    override fun registerObserver(o: Observer<CmdContainer>) {
        observers.add(o)
    }

    override fun removeObserver(o: Observer<CmdContainer>) {
        observers.remove(o)
    }

    override fun notifyAllObservers() {
        observers.forEach {
            it.update(this)
        }
    }

    fun getData(): List<CmdContainer> {
        return datas
    }

    fun addCmdInfo(filterInfo: CmdContainer) {
        datas.add(filterInfo)
    }

    fun updateData() {
        notifyAllObservers()
    }

    fun setSelectedCmd(index: Int) {
        selectedIndex = index
    }

    fun getSelectedCmd(): String {
        return datas[selectedIndex].cmd
    }
}