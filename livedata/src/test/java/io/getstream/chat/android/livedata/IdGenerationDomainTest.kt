package io.getstream.chat.android.livedata

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.getstream.chat.android.client.api.models.QuerySort
import io.getstream.chat.android.client.utils.FilterObject
import io.getstream.chat.android.livedata.entity.QueryChannelsEntity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IdGenerationDomainTest : BaseDomainTest() {

    @Test
    fun messageIdGeneration() {
        val messageId = chatDomainImpl.generateMessageId()

        Truth.assertThat(messageId).isNotNull()
        Truth.assertThat(messageId).isNotEmpty()
    }

    @Test
    fun messageIdGenerationIsUnique() {
        val idMap = sortedSetOf<String>()
        for (x in 0..100) {
            val messageId = chatDomainImpl.generateMessageId()
            Truth.assertThat(idMap).doesNotContain(messageId)
            idMap.add(messageId)
        }
    }

    @Test
    fun queryId() {
        val query = QueryChannelsEntity(
            FilterObject(
                "type",
                "messaging"
            ), QuerySort()
        )
        val query2 = QueryChannelsEntity(
            FilterObject(
                "type",
                "messaging"
            ), QuerySort()
        )
        val query3 = QueryChannelsEntity(
            FilterObject(
                "type",
                "commerce"
            ), QuerySort()
        )
        val query4 = QueryChannelsEntity(
            FilterObject(
                "type",
                "messaging"
            ), QuerySort().asc("name")
        )
        // verify that 1 and 2 are equal
        Truth.assertThat(query2.id).isEqualTo(query.id)
        // verify that 3 and 4 are not equal to 2
        Truth.assertThat(query2.id).isNotEqualTo(query3.id)
        Truth.assertThat(query2.id).isNotEqualTo(query4.id)
    }
}
