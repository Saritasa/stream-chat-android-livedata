package io.getstream.chat.android.livedata.usecase

import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.livedata.ChatDomainImpl
import io.getstream.chat.android.livedata.utils.Call2
import io.getstream.chat.android.livedata.utils.CallImpl2
import io.getstream.chat.android.livedata.utils.validateCid

interface EditMessage {
    /**
     * Edits the specified message. Local storage is updated immediately
     * The API request is retried according to the retry policy specified on the chatDomain
     * @param message the message to edit
     * @return A call object with Message as the return type
     * @see io.getstream.chat.android.livedata.utils.RetryPolicy
     */
    operator fun invoke(message: Message): Call2<Message>
}

class EditMessageImpl(var domainImpl: ChatDomainImpl) : EditMessage {
    override operator fun invoke(message: Message): Call2<Message> {
        var cid = message.cid
        if (cid.isEmpty()) {
            cid = message.channel.cid
        }
        validateCid(cid)

        val channelRepo = domainImpl.channel(cid)

        val runnable = suspend {
            channelRepo.editMessage(message)
        }
        return CallImpl2<Message>(
            runnable,
            channelRepo.scope
        )
    }
}
