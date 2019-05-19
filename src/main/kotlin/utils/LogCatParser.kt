package utils

import bean.LogInfo
import java.util.regex.Pattern


class LogCatParser {

    fun parse(textLogLine: String): LogInfo {
        val log = LogInfo()
        if (textLogLine.isNotEmpty()) {
            log.valid = true
            log.strMsg = textLogLine
            return parseAndroidLogLevel(log, textLogLine)
        }
        return log
    }

    interface LogLevelFilter {
        fun colorFilter(level: String)
    }

    private fun logLevel(regex: String, textLogLine: String, filter: LogLevelFilter) {
        val pt = Pattern.compile(regex)
        val match = pt.matcher(textLogLine)
        while (match.find()) {
            filter.colorFilter(match.group())
        }
    }

    private fun parseAndroidLogLevel(logInfo: LogInfo, strText: String): LogInfo {
        try {
            logLevel("\\s[VDIWEF]\\s", strText, object : LogLevelFilter {
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

            logLevel("\\s[VDIWEF]/", strText, object : LogLevelFilter {
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

    companion object {
        const val COLOR_GUIDE = "#000000"
        const val COLOR_DEBUG = "#0000AA"
        const val COLOR_ERROR = "#FF0000"
        const val COLOR_FATAL = "#FF0000"
        const val COLOR_INFO = "#009A00"
        const val COLOR_WARN = "#FF9A00"
    }
}