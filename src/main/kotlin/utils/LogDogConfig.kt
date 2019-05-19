package utils

import bean.CmdInfo
import bean.FilterInfo

class LogDogConfig private constructor() {
    var tool_path = ""
    var tool_cmd = arrayListOf<CmdInfo>()
    var filter_rule = arrayListOf<FilterInfo>()
    var custom_color = arrayListOf<String>()
    var uuid = UID.getUID()

    fun load(config: LogDogConfig) {
        tool_cmd.addAll(config.tool_cmd)
        tool_path = config.tool_path
        filter_rule.addAll(config.filter_rule)
        custom_color.addAll(config.custom_color)
        uuid = config.uuid
    }

    fun preSave(filter: List<FilterInfo>) {
        filter.forEach { it.lines.clear() }
        filter_rule.clear()
        filter_rule.addAll(filter)
    }

    companion object {
        private val instance = LogDogConfig()

        fun instance(): LogDogConfig {
            return instance
        }
    }
}