package bean

import java.util.*


class FilterContainer {
    var title: String = ""
    var enabled = false
    var condition = -1
    var msg = ""

    var uuid = UUID.randomUUID().toString()

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
        if (condition == 1) {
            str += "Contains "
        } else if (condition == 2) {
            str += "Matches "
        }
        str += "\"$msg\""
        return str
    }

    fun toggle() {
        enabled = !enabled
    }

    companion object {
        const val FMTSTR = "Name|2|\\s[V|D|I|W|E|F]\\s"

        fun formatString(data: FilterContainer): String {
            return String.format("%s|%s|%s", data.title, data.condition, data.msg)
        }

        fun formatBean(data: String, uuid: String? = null): FilterContainer? {
            val list = data.split("|")
            if (list.size == 3) {
                val bean = FilterContainer()
                if (uuid != null) {
                    bean.uuid = uuid
                }
                bean.enabled = false
                bean.title = list[0]
                bean.condition = Integer.valueOf(list[1])
                bean.msg = list[2]
                return bean
            }
            return null
        }

    }
}