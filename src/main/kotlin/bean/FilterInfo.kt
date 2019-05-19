package bean

import utils.DefaultConfig
import utils.UID
import java.util.*

class FilterInfo(uuid: String? = null) {
    var enabled = false
    // 1 new 2 edit 3 delete 4 enable
    var state = -1
    // 1 contains 2 regex
    var type = -1
    var text = ""
    var color = DefaultConfig.DEFAULT_BG_COLOR
    var uuid = uuid ?: UID.getNewUID()
    var lines = LinkedList<Int>()

    override fun toString(): String {
        return detail()
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + enabled.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other === this) {
            return true
        }
        if (other !is FilterInfo) {
            return false
        }
        if (other.uuid == this.uuid) {
            return true
        }
        return false
    }

    fun detail(): String {
        return when (type) {
            1 -> {
                "C $text (${lines.size})"
            }
            2 -> {
                "M $text (${lines.size})"
            }
            else -> {
                "$text (${lines.size})"
            }
        }
    }

    fun toggle() {
        enabled = !enabled
    }

}