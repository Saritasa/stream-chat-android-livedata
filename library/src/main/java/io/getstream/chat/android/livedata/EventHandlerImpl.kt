package io.getstream.chat.android.livedata

import io.getstream.chat.android.client.events.*
import io.getstream.chat.android.client.models.ChannelUserRead
import io.getstream.chat.android.livedata.entity.ChannelEntity
import io.getstream.chat.android.livedata.entity.MessageEntity
import io.getstream.chat.android.livedata.entity.UserEntity
import kotlinx.coroutines.*

class EventHandlerImpl(var repo: io.getstream.chat.android.livedata.ChatRepo, var runAsync: Boolean=true) {
    fun handleEvents(events: List<ChatEvent>) {
        if (runAsync) {
            GlobalScope.launch(Dispatchers.IO) {
                handleEventsInternal(events)
            }
        } else {
            runBlocking(Dispatchers.IO) { handleEventsInternal(events) }
        }
    }

    internal suspend fun handleEvent(event: ChatEvent) {
        handleEventsInternal(listOf(event))
    }

    internal suspend fun handleEventsInternal(events: List<ChatEvent>) {
        val users: MutableMap<String, UserEntity> = mutableMapOf()
        val channels: MutableMap<String, ChannelEntity> = mutableMapOf()

        val messages: MutableMap<String, MessageEntity> = mutableMapOf()
        var unreadChannels: Int? = null
        var totalUnreadCount: Int? = null
        var channelEvents: MutableMap<String, MutableList<ChatEvent>> = mutableMapOf()

        val channelsToFetch = mutableSetOf<String>()
        val messagesToFetch = mutableSetOf<String>()

        // step 1. see which data we need to retrieve from offline storage
        for (event in events) {
            when (event) {
                is MessageReadEvent, is MemberAddedEvent, is MemberRemovedEvent, is MemberUpdatedEvent, is ChannelUpdatedEvent, is ChannelHiddenEvent, is ChannelDeletedEvent -> {
                    // get the channel, update reads, write the channel
                    channelsToFetch.add(event.cid!!)
                }
                is ReactionNewEvent -> {
                    // get the message, update the reaction data, update the message
                    // note that we need to use event.reaction and not event.message
                    // event.message only has a subset of reactions
                    messagesToFetch.add(event.reaction!!.messageId)
                }
                is ReactionDeletedEvent -> {
                    // get the message, update the reaction data, update the message
                    messagesToFetch.add(event.reaction!!.messageId)
                }
            }
        }
        // actually fetch the data
        val channelMap = repo.repos.channels.select(channelsToFetch.toList()).associateBy{it.cid}
        val messageMap = repo.repos.messages.select(messagesToFetch.toList()).associateBy{it.id}


        // step 2. second pass through the events, make a list of what we need to update
        for (event in events) {
            // any event can have channel and unread count information
            event.unreadChannels?.let { unreadChannels=it }
            event.totalUnreadCount?.let { totalUnreadCount=it }

            event.user?.let {
                users[it.id] = UserEntity(it)
            }
            if (event.isChannelEvent()) {
                if (!channelEvents.containsKey(event.cid!!)) {
                    channelEvents[event.cid!!] = mutableListOf()
                }
                channelEvents[event.cid!!]!!.add(event)
            }

            when (event) {
                // keep the data in Room updated based on the various events..
                // TODO: all of these events should also update user information
                is NewMessageEvent, is MessageDeletedEvent, is MessageUpdatedEvent -> {
                    messages[event.message.id] = MessageEntity(event.message)
                    channels[event.message.channel.id] = ChannelEntity(event.message.channel)
                }
                is MessageReadEvent -> {
                    // get the channel, update reads, write the channel
                    val channel = channelMap[event.cid]
                    val read = ChannelUserRead()
                    read.user = event.user!!
                    read.lastRead = event.createdAt
                    channel?.let {
                        it.updateReads(read)
                        channels[it.cid] = it
                    }
                }
                is ReactionNewEvent -> {
                    // get the message, update the reaction data, update the message
                    // note that we need to use event.reaction and not event.message
                    // event.message only has a subset of reactions
                    val message = messageMap[event.reaction!!.messageId]
                    message?.let {
                        val userId = event.reaction!!.user!!.id
                        it.addReaction(event.reaction!!, repo.currentUser.id == userId)
                        messages[it.id] = it
                    }
                }
                is ReactionDeletedEvent -> {
                    // get the message, update the reaction data, update the message
                    val message = messageMap[event.reaction!!.messageId]
                    message?.let {
                        val userId = event.reaction!!.user!!.id
                        it.removeReaction(event.reaction!!, false)
                        it.reactionCounts = event.message.reactionCounts
                        messages[it.id] = it
                    }
                }
                is MemberAddedEvent, is MemberRemovedEvent, is MemberUpdatedEvent -> {
                    // get the channel, update members, write the channel
                    val channelEntity = channelMap.get(event.cid!!)
                    if (channelEntity != null) {
                        var member = event.member
                        val userId = event.member!!.user.id
                        if (event is MemberRemovedEvent) {
                            member = null
                        }
                        channelEntity.setMember(userId, member)
                        channels[channelEntity.cid] = channelEntity
                    }
                }
                is ChannelUpdatedEvent, is ChannelHiddenEvent, is ChannelDeletedEvent -> {
                    // get the channel, update members, write the channel
                    event.channel?.let {
                        channels[it.cid] = ChannelEntity(it)
                    }
                }
            }
            // actually insert the data
            repo.repos.users.insertUserEntities(users.values.toList())
            repo.repos.channels.insertChannelEntities(channels.values.toList())
            // we only cache messages for which we're receiving events
            repo.repos.messages.insertMessageEntities(messages.values.toList(), true)

            unreadChannels?.let { repo.setChannelUnreadCount(it) }
            totalUnreadCount?.let { repo.setTotalUnreadCount(it) }

            // step 3 - forward the events to the active queries and channels
            for ((cid, cEvents) in channelEvents) {
                if (repo.isActiveChannel(cid)) {
                    repo.channel(cid).handleEvents(cEvents)
                }
            }

            for (event in events) {

                // connection events are never send on the recovery endpoint, so handle them 1 by 1
                when (event) {
                    is DisconnectedEvent -> {
                        repo.postOffline()
                    }
                    is ConnectedEvent -> {
                        val recovered = repo.isInitialized()
                        repo.postOnline()
                        repo.postInitialized()
                        if (recovered) {
                            repo.connectionRecovered(true)
                        } else {
                            repo.connectionRecovered(false)
                        }
                    }
                }
            }
            // queryRepo mainly monitors for the notification added to channel event
            for (queryRepo in repo.getActiveQueries()) {
                queryRepo.handleEvents(events)
            }

        }

    }
}