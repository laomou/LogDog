package interfces

interface Observer<T> {
    fun update(s: ObservableSubject<T>)
}