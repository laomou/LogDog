package utils

import bean.LogContainer
import java.util.regex.Pattern


class EventLogParser {
    val COLOR_GUIDE = 0x00000000
    val COLOR_DEBUG = 0x000000AA
    val COLOR_ERROR = 0x00FF0000
    val COLOR_FATAL = 0x00FF0000
    val COLOR_INFO = 0x00009A00
    val COLOR_WARN = 0x00FF9A00

    var COLOR_0 = 0x00000000
    var COLOR_1 = 0x00000000
    var COLOR_2 = 0x00000000
    var COLOR_3 = COLOR_ERROR
    var COLOR_4 = COLOR_WARN
    var COLOR_5 = 0x00000000
    var COLOR_6 = COLOR_INFO
    var COLOR_7 = COLOR_DEBUG
    var COLOR_8 = COLOR_ERROR

    fun parse(textLogLine: String): LogContainer {
        val log = LogContainer()
        if (!textLogLine.isEmpty()) {
            log.valid = true
            log.strMsg = textLogLine
            return processLogLine(log, textLogLine)
        } else {
            log.valid = false
        }
        return log
    }

    interface ColorFilter {
        fun filter(level: String)
    }

    private fun logLevel(regex: String, textLogLine: String, filter: ColorFilter): Boolean {
        val pt = Pattern.compile(regex)
        val match = pt.matcher(textLogLine)
        var matched = false
        while (match.find()) {
            filter.filter(match.group())
            matched = true
        }
        return matched
    }

    private fun processLogLine(log: LogContainer, textLogLine: String): LogContainer {
        val matched = arrayOf(logLevel("\\s[VDEWIF]\\s", textLogLine, object : ColorFilter {
            override fun filter(level: String) {
                when (level) {
                    " V " -> log.strColor = COLOR_GUIDE
                    " D " -> log.strColor = COLOR_DEBUG
                    " I " -> log.strColor = COLOR_INFO
                    " W " -> log.strColor = COLOR_WARN
                    " E " -> log.strColor = COLOR_ERROR
                    " F " -> log.strColor = COLOR_FATAL
                }
            }
        }), logLevel("\\s\\[[1-7]:\\s", textLogLine, object : ColorFilter {
            override fun filter(level: String) {
                when (level) {
                    " [1: " -> log.strColor = COLOR_1
                    " [2: " -> log.strColor = COLOR_2
                    " [3: " -> log.strColor = COLOR_3
                    " [4: " -> log.strColor = COLOR_4
                    " [5: " -> log.strColor = COLOR_5
                    " [6: " -> log.strColor = COLOR_6
                    " [7: " -> log.strColor = COLOR_7
                }
            }
        }))
        for (b in matched) {
            if (b) {
                break
            }
        }
        return log
    }
}