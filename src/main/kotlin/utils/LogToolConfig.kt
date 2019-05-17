package utils

import bean.CmdContainer
import bean.FilterContainer
import bean.UID

class LogToolConfig private constructor() {
    var tool_path = ""
    var tool_cmd = arrayListOf<CmdContainer>()
    var filter_rule = arrayListOf<FilterContainer>()
    var custom_color = arrayListOf<String>()
    var uuid = UID.getUID()

    fun load(config: LogToolConfig) {
        tool_cmd.addAll(config.tool_cmd)
        tool_path = config.tool_path
        filter_rule.addAll(config.filter_rule)
        custom_color.addAll(config.custom_color)
        uuid = config.uuid
    }

    companion object {
        private val instance = LogToolConfig()

        fun instance(): LogToolConfig {
            return instance
        }
    }
}