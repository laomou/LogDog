package utils

import bean.CmdContainer
import bean.FilterContainer

class LogToolConfig {
    var tool_path = ""
    var tool_cmd = ArrayList<CmdContainer>()
    var filter_rule = ArrayList<FilterContainer>()

    fun copy(config: LogToolConfig) {
        tool_cmd = config.tool_cmd
        tool_path = config.tool_path
        filter_rule = config.filter_rule
    }
}