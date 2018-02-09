package bean

class LogContainer {
    var valid = false

    var strMsg = ""
    var strColor = 0x00000000

    fun getData(): String {
        return strMsg
    }

    override fun toString(): String {
        return strMsg
    }

    fun getColor(): Int {
        return strColor
    }
}