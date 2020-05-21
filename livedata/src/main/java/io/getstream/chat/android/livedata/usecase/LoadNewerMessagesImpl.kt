package io.getstream.chat.android.livedata.usecase

import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.livedata.ChatDomainImpl
import io.getstream.chat.android.livedata.utils.Call2
import io.getstream.chat.android.livedata.utils.CallImpl2
import io.getstream.chat.android.livedata.utils.validateCid

interface LoadNewerMessages {
    /**
     * Loads newer messages for the channel
     *
     * @param cid: the full channel id IE messaging:123
     * @param messageLimit: how many new messages to load
     *
     * @return A call object with Channel as the return type
     */
    operator fun invoke(cid: String, messageLimit: Int, startMessageId: String? = null): Call2<Channel>
}

class LoadNewerMessagesImpl(var domainImpl: ChatDomainImpl) : LoadNewerMessages {
    override operator fun invoke(cid: String, messageLimit: Int, startMessageId: String?): Call2<Channel> {
        validateCid(cid)
        val channelRepo = domainImpl.channel(cid)
        val runnable = suspend {
            channelRepo.loadNewerMessages(messageLimit, startMessageId)
        }
        return CallImpl2<Channel>(
            runnable,
            channelRepo.scope
        )
    }
}
