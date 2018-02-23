package bean

import java.util.*


class FilterContainer(uuid: String? = null) {
    var title: String = ""
    var enabled = false
    var regex = -1
    var text = ""

    var uuid = uuid ?: UUID.randomUUID().toString()

    override fun toString(): String {
        return nick()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other === this) {
            return true
        }
        if (other !is FilterContainer) {
            return false
        }
        if (other.uuid == this.uuid) {
            return true
        }
        return false
    }

    private fun nick(): String {
        return title
    }

    fun detail(): String {
        var str = ""
        if (regex == 0) {
            str += "Remove "
        } else if (regex == 1) {
            str += "Contains "
        } else if (regex == 2) {
            str += "Matches "
        }
        str += "\"$text\""
        return str
    }

    fun toggle() {
        enabled = !enabled
    }
}