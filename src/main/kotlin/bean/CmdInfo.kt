package bean

class CmdInfo {
    var title = ""
    var cmd = ""

    override fun toString(): String {
        return title
    }

    fun detail(): String {
        return cmd
    }
}