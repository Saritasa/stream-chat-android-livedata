package io.getstream.chat.android.livedata.converter

import com.google.common.truth.Truth
import io.getstream.chat.android.livedata.BaseTest
import org.junit.Test

class SetConverterTest : BaseTest() {
    @Test
    fun testNullEncoding() {
        val converter = SetConverter()
        val output = converter.sortedSetToString(null)
        val converted = converter.stringToSortedSet(output)
        Truth.assertThat(converted).isEqualTo(sortedSetOf<String>())
    }

    @Test
    fun testSortEncoding() {
        val converter = SetConverter()
        val colors = sortedSetOf("green", "blue")
        val output = converter.sortedSetToString(colors)
        val converted = converter.stringToSortedSet(output)
        Truth.assertThat(converted).isEqualTo(colors)
    }
}
