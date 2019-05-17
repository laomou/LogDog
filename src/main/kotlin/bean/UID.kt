package bean

object UID {
    private var uid = 10000

    fun setUID(id: String) {
        uid = id.toInt()
    }

    fun getNewUID(): String {
        return "${uid++}"
    }

    fun getUID(): String {
        return "$uid"
    }
}