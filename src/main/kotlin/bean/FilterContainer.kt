package bean

import java.util.*


class FilterContainer(uuid: String? = null) {
    var enabled = false
    var text = ""
    var color = "#000000"

    var uuid = uuid ?: UUID.randomUUID().toString()

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
        return text
    }

    fun toggle() {
        enabled = !enabled
    }

}