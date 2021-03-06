package io.getstream.chat.android.livedata.controller

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.utils.FilterObject
import io.getstream.chat.android.livedata.BaseConnectedIntegrationTest
import io.getstream.chat.android.livedata.entity.QueryChannelsEntity
import io.getstream.chat.android.livedata.request.QueryChannelsPaginationRequest
import io.getstream.chat.android.livedata.utils.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QueryChannelsControllerTest : BaseConnectedIntegrationTest() {

    @Test
    fun newChannelAdded() = runBlocking(Dispatchers.IO) {
        val request = QueryChannelsPaginationRequest()
        queryControllerImpl.runQuery(request)
        var channels = queryControllerImpl.channels.getOrAwaitValue()
        val oldSize = channels.size
        // verify that a new channel is added to the list
        val addedEvent = data.notificationAddedToChannel3Event
        queryControllerImpl.handleEvent(addedEvent)
        channels = queryControllerImpl.channels.getOrAwaitValue()
        val newSize = channels.size
        Truth.assertThat(newSize - oldSize).isEqualTo(1)
        val channelController = chatDomainImpl.channel(addedEvent.channel!!)
        val channel = channelController.toChannel()
    }

    @Test
    fun newChannelFiltered() = runBlocking(Dispatchers.IO) {
        val request = QueryChannelsPaginationRequest()
        val queryChannelsController = chatDomainImpl.queryChannels(data.filter2)
        queryChannelsController.newChannelEventFilter = { channel: Channel, filterObject: FilterObject ->
            // ignore everything
            false
        }
        queryChannelsController.runQuery(request)
        var channels = queryChannelsController.channels.getOrAwaitValue()
        val oldSize = channels.size
        // verify that a new channel is NOT added to the list
        val addedEvent = data.notificationAddedToChannel3Event
        queryChannelsController.handleEvent(addedEvent)
        channels = queryChannelsController.channels.getOrAwaitValue()
        val newSize = channels.size
        Truth.assertThat(newSize - oldSize).isEqualTo(0)
    }

    @Test
    fun testChannelIdPagination() {
        val list = sortedSetOf("a", "b", "c")

        var sub = queryControllerImpl.paginateChannelIds(list, QueryChannelsPaginationRequest(0, 5))
        Truth.assertThat(sub).isEqualTo(listOf("a", "b", "c"))

        sub = queryControllerImpl.paginateChannelIds(list, QueryChannelsPaginationRequest(1, 2))
        Truth.assertThat(sub).isEqualTo(listOf("b", "c"))

        sub = queryControllerImpl.paginateChannelIds(list, QueryChannelsPaginationRequest(3, 2))
        Truth.assertThat(sub).isEqualTo(listOf<String>())

        sub = queryControllerImpl.paginateChannelIds(list, QueryChannelsPaginationRequest(4, 2))
        Truth.assertThat(sub).isEqualTo(listOf<String>())
    }

    @Test
    fun testLoadMore() = runBlocking(Dispatchers.IO) {
        val paginate = QueryChannelsPaginationRequest(0, 2)
        val result = queryControllerImpl.runQuery(paginate)
        assertSuccess(result)
        var channels = queryControllerImpl.channels.getOrAwaitValue()
        Truth.assertThat(channels.size).isEqualTo(2)
        val request = queryControllerImpl.loadMoreRequest(1)
        Truth.assertThat(request.channelOffset).isEqualTo(2)
        val result2 = queryControllerImpl.runQuery(request)
        assertSuccess(result2)
        channels = queryControllerImpl.channels.getOrAwaitValue()
        Truth.assertThat(channels.size).isEqualTo(3)
    }

    @Test
    fun offlineRunQuery() = runBlocking(Dispatchers.IO) {
        // insert the query result into offline storage
        val query = QueryChannelsEntity(query.filter, query.sort)
        query.channelCIDs = sortedSetOf(data.channel1.cid)
        chatDomainImpl.repos.queryChannels.insert(query)
        chatDomainImpl.repos.messages.insertMessage(data.message1)
        chatDomainImpl.storeStateForChannel(data.channel1)
        chatDomainImpl.setOffline()
        val channels = queryControllerImpl.runQueryOffline(QueryChannelsPaginationRequest(0, 2))
        // should return 1 since only 1 is stored in offline storage
        Truth.assertThat(channels?.size).isEqualTo(1)
        // verify we load messages correctly
        Truth.assertThat(channels!!.first().messages.size).isEqualTo(1)
    }

    @Test
    fun onlineRunQuery() = runBlocking(Dispatchers.IO) {
        // insert the query result into offline storage
        val query = QueryChannelsEntity(query.filter, query.sort)
        query.channelCIDs = sortedSetOf(data.channel1.cid)
        chatDomainImpl.repos.queryChannels.insert(query)
        chatDomainImpl.storeStateForChannel(data.channel1)
        chatDomainImpl.setOffline()
        queryControllerImpl.runQuery(QueryChannelsPaginationRequest(0, 2))
        val channels = queryControllerImpl.channels.getOrAwaitValue()
        // should return 1 since only 1 is stored in offline storage
        Truth.assertThat(channels.size).isEqualTo(1)
    }
}
