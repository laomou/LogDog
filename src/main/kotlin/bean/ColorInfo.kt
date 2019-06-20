package bean

class ColorInfo {
    var title = ""
    var color = ""

    override fun toString(): String {
        return title
    }

    fun color(): String {
        return color
    }
}