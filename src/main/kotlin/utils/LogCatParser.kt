package utils

import bean.LogContainer
import java.util.regex.Pattern


class LogCatParser {
    val COLOR_GUIDE = 0x00000000
    val COLOR_DEBUG = 0x000000AA
    val COLOR_ERROR = 0x00FF0000
    val COLOR_FATAL = 0x00FF0000
    val COLOR_INFO = 0x00009A00
    val COLOR_WARN = 0x00FF9A00

    val COLOR_0 = 0x00000000
    val COLOR_1 = 0x00000000
    val COLOR_2 = 0x00000000
    val COLOR_3 = COLOR_ERROR
    val COLOR_4 = COLOR_WARN
    val COLOR_5 = 0x00000000
    val COLOR_6 = COLOR_INFO
    val COLOR_7 = COLOR_DEBUG
    val COLOR_8 = COLOR_ERROR

    val TOKEN_KERNEL = "<>[]"
    val TOKEN_SPACE = " "
    val TOKEN_SLASH = "/"
    val TOKEN = "/()"
    val TOKEN_PID = "/() "
    val TOKEN_MESSAGE = "'"

    fun parse(textLogLine: String): LogContainer {
        val log = LogContainer()
        if (!textLogLine.isEmpty()) {
            log.valid = true
            log.strMsg = textLogLine
            return processLogLevel(log, textLogLine)
        }
        return log
    }

    interface LogFilter {
        fun colorFilter(level: String)
    }

    private fun logLevel(regex: String, textLogLine: String, filter: LogFilter) {
        val pt = Pattern.compile(regex)
        val match = pt.matcher(textLogLine)
        while (match.find()) {
            filter.colorFilter(match.group())
        }
    }

    private fun processLogLevel(logInfo: LogContainer, strText: String): LogContainer {
        try {
            logLevel("\\s[VDIWEF]\\s", strText, object : LogFilter {

                override fun colorFilter(level: String) {
                    when (level) {
                        " V " -> logInfo.strColor = COLOR_GUIDE
                        " D " -> logInfo.strColor = COLOR_DEBUG
                        " I " -> logInfo.strColor = COLOR_INFO
                        " W " -> logInfo.strColor = COLOR_WARN
                        " E " -> logInfo.strColor = COLOR_ERROR
                        " F " -> logInfo.strColor = COLOR_FATAL
                    }
                    throw Exception("Done")
                }
            })

            logLevel("\\s[VDIWEF]/", strText, object : LogFilter {
                override fun colorFilter(level: String) {
                    when (level) {
                        " V/" -> logInfo.strColor = COLOR_GUIDE
                        " D/" -> logInfo.strColor = COLOR_DEBUG
                        " I/" -> logInfo.strColor = COLOR_INFO
                        " W/" -> logInfo.strColor = COLOR_WARN
                        " E/" -> logInfo.strColor = COLOR_ERROR
                        " F/" -> logInfo.strColor = COLOR_FATAL
                    }
                    throw Exception("Done")
                }
            })
        } catch (e: Exception) {
        }

        return logInfo
    }
}