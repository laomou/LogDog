package interfces

interface ObservableSubject<T> {
    fun registerObserver(o: Observer<T>)

    fun removeObserver(o: Observer<T>)

    fun notifyAllObservers()
}