package io.getstream.chat.android.livedata

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.getstream.chat.android.client.errors.ChatNetworkError
import io.getstream.chat.android.client.models.Message
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatErrorTest : BaseConnectedIntegrationTest() {
    @Test
    fun invalidMessageInput() {
        val message = Message(text = "hi", id = "thesame")
        client.sendMessage("messaging", data.channel1.id, message).execute()
        // this will always fail since the id is the same
        val result2 = client.sendMessage("messaging", data.channel1.id, message).execute()
        val error = result2.error()
        Truth.assertThat(error.isPermanent()).isTrue()
    }

    @Test
    fun rateLimit() {
        val error = ChatNetworkError.create(1, "", 429, null)
        Truth.assertThat(error.isPermanent()).isFalse()
    }

    @Test
    fun brokenAPI() {
        val error = ChatNetworkError.create(0, "", 500, null)
        Truth.assertThat(error.isPermanent()).isFalse()
    }
}
