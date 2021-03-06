package io.getstream.chat.android.livedata.usecase

import io.getstream.chat.android.client.events.ChatEvent
import io.getstream.chat.android.livedata.ChatDomainImpl
import io.getstream.chat.android.livedata.utils.Call2
import io.getstream.chat.android.livedata.utils.CallImpl2
import io.getstream.chat.android.livedata.utils.validateCid

interface ReplayEventsForActiveChannels {
    /**
     * Adds the specified channel to the active channels
     * Replays events for all active channels
     * This ensures that your local storage is up to date with the server
     *
     * @param cid: the full channel id IE messaging:123
     *
     * @return A call object with List<ChatEvent> as the return type
     */
    operator fun invoke(cid: String): Call2<List<ChatEvent>>
}

class ReplayEventsForActiveChannelsImpl(var domainImpl: ChatDomainImpl) : ReplayEventsForActiveChannels {
    override operator fun invoke(cid: String): Call2<List<ChatEvent>> {
        validateCid(cid)
        var runnable = suspend {
            domainImpl.replayEventsForActiveChannels(cid)
        }
        return CallImpl2<List<ChatEvent>>(
            runnable,
            domainImpl.scope
        )
    }
}
