package bean

import utils.DefaultConfig
import java.util.*

class FilterContainer(uuid: String? = null) {
    var enabled = false
    var state = -1
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
        if (other !is FilterContainer) {
            return false
        }
        if (other.uuid == this.uuid) {
            return true
        }
        return false
    }

    fun detail(): String {
        return "$text (${lines.size})"
    }

    fun toggle() {
        enabled = !enabled
    }

}