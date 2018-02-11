package bean

class LogContainer {
    var valid = false

    var strLine = ""
    var strDate = ""
    var strTime = ""
    var strLogLV = ""
    var strPid = ""
    var strThread = ""
    var strTag = ""
    var strMsg = ""
    var strColor = 0x00000000

    override fun toString(): String {
        return strMsg
    }
}