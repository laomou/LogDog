package utils

import bean.CmdInfo
import bean.FilterInfo
import model.FilterMapModel

class LogDogConfig private constructor() {
    var tool_path = ""
    var tool_cmd = arrayListOf<CmdInfo>()
    var filter_rule = hashMapOf<Int, ArrayList<FilterInfo>>(
            Pair(FilterMapModel.TYPE_FILTER_TAG1, ArrayList()),
            Pair(FilterMapModel.TYPE_FILTER_TAG2, ArrayList()),
            Pair(FilterMapModel.TYPE_FILTER_TAG3, ArrayList()))
    var custom_color = arrayListOf<String>()
    var uuid = UID.getUID()

    fun loadFromGson(config: LogDogConfig) {
        tool_cmd.addAll(config.tool_cmd)
        tool_path = config.tool_path
        config.filter_rule.forEach { i, arrayList ->
            filter_rule[i] = arrayList
        }
        custom_color.addAll(config.custom_color)
        uuid = config.uuid
    }

    fun preSave(filterMap: Map<Int, ArrayList<FilterInfo>>) {
        filterMap.forEach { i, arrayList ->
            filter_rule[i] = arrayList
        }
    }

    companion object {
        private val instance = LogDogConfig()

        fun instance(): LogDogConfig {
            return instance
        }
    }
}