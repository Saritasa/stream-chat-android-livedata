package io.getstream.chat.android.livedata.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.getstream.chat.android.client.api.models.QuerySort
import io.getstream.chat.android.client.utils.FilterObject
import java.util.*

@Entity(tableName = "stream_channel_query")
data class QueryChannelsEntity(var filter: FilterObject, val sort: QuerySort? = null) {
    @PrimaryKey
    var id: String

    init {
        // ugly hack to cleanup the filter object to prevent issues with filter object equality
        filter = FilterObject(filter.toMap())
        id = (Objects.hash(filter.toMap()) + Objects.hash(sort?.data)).toString()
    }

    var channelCIDs: SortedSet<String> = sortedSetOf()

    /** we track when the query was created and updated so we can clear out old results */
    var createdAt: Date? = null
    var updatedAt: Date? = null
}
