package event

interface Observer<T> {
    fun update(s: ObservableSubject<T>)
}