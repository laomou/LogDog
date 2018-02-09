package bean

class CmdContainer {
    var title = ""
    var cmd = ""

    override fun toString(): String {
        return title
    }

    fun detail(): String {
        return cmd
    }
}