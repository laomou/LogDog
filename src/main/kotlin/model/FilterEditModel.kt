package model

import interfces.ObservableSubject
import interfces.Observer

class FilterEditModel : ObservableSubject<String> {
    private val observers = arrayListOf<Observer<String>>()
    private val data = arrayListOf<String>()

    override fun registerObserver(o: Observer<String>) {
        observers.add(o)
    }

    override fun removeObserver(o: Observer<String>) {
        observers.remove(o)
    }

    override fun notifyAllObservers() {
        observers.forEach {
            it.update(this)
        }
    }

    fun getData(): List<String> {
        return data
    }

    fun addColorInfo(color: String) {
        data.add(color)
    }

    fun updateData() {
        notifyAllObservers()
    }
}
