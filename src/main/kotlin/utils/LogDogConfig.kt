package utils

import bean.CmdInfo
import bean.ColorInfo
import bean.FilterInfo
import model.FilterMapModel

class LogDogConfig private constructor() {
    var toolPath = ""
    var toolCmd = arrayListOf<CmdInfo>()
    var filterRule = hashMapOf<Int, ArrayList<FilterInfo>>(
            Pair(FilterMapModel.TYPE_FILTER_NONE, ArrayList()),
            Pair(FilterMapModel.TYPE_FILTER_TAG1, ArrayList()),
            Pair(FilterMapModel.TYPE_FILTER_TAG2, ArrayList()),
            Pair(FilterMapModel.TYPE_FILTER_TAG3, ArrayList()))
    var customColor = arrayListOf<ColorInfo>()
    var uuid = UID.getUID()

    fun loadFromGson(config: LogDogConfig) {
        toolCmd.addAll(config.toolCmd)
        toolPath = config.toolPath
        config.filterRule.forEach { (i, arrayList) ->
            filterRule[i] = arrayList
        }
        customColor.addAll(config.customColor)
        uuid = config.uuid
    }

    fun preSave(filterMap: Map<Int, ArrayList<FilterInfo>>) {
        filterMap.forEach { (i, arrayList) ->
            arrayList.forEach { it.lines.clear() }
            filterRule[i] = arrayList
        }
    }

    companion object {
        private val instance = LogDogConfig()

        fun instance(): LogDogConfig {
            return instance
        }
    }
}