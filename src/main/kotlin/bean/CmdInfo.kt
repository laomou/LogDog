package bean

class CmdInfo {
    var title = ""
    var cmd = ""

    override fun toString(): String {
        return title
    }

    fun cmd(): String {
        return cmd
    }
}