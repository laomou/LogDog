package event

class CustomEvent(val source: Any, val action: String, var obj: Any? = null)