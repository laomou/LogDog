package bean

import utils.DefaultConfig
import java.util.*

class LogContainer {
    var valid = false
    var strLine = 0
    var strMsg = ""
    var strColor = "#000000"
    var filterColor = DefaultConfig.DEFAULT_BG_COLOR
    var show = false
    var filters = LinkedList<String>()

    override fun toString(): String {
        return strMsg
    }
}