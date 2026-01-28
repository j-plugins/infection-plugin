package com.github.xepozz.infection.tests.metainfo

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.execution.testframework.sm.runner.SMTestProxy

/**
 * Format: JSON {"key1":"value1","key2":"value2"}
 */
object TestProxyMetainfo {
    const val KEY_MUTATION_ID = "mutationId"

    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, String>>() {}.type

    fun getAttribute(proxy: SMTestProxy, key: String): String? {
        return getAttributes(proxy)[key]
    }

    fun getAttributes(proxy: SMTestProxy): Map<String, String> {
        val metainfo = proxy.metainfo ?: return emptyMap()
        return deserialize(metainfo)
    }

    private fun deserialize(metainfo: String): Map<String, String> {
        if (metainfo.isBlank()) return emptyMap()
        return try {
            gson.fromJson(metainfo, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
