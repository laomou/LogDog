package model

import bean.ColorInfo
import event.ObservableSubject
import event.Observer

class FilterEditModel : ObservableSubject<ColorInfo> {
    private val observers = arrayListOf<Observer<ColorInfo>>()
    private val data = arrayListOf<ColorInfo>()

    override fun registerObserver(o: Observer<ColorInfo>) {
        observers.add(o)
    }

    override fun removeObserver(o: Observer<ColorInfo>) {
        observers.remove(o)
    }

    override fun notifyAllObservers() {
        observers.forEach {
            it.update(this)
        }
    }

    fun getData(): List<ColorInfo> {
        return data
    }

    fun addColorInfo(color: ColorInfo) {
        data.add(color)
    }

    fun updateData() {
        notifyAllObservers()
    }
}
