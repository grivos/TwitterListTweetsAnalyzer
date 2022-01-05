package data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListMembersResponse(val data: List<ListMemberData>, val meta: ListMembersMeta)

@Serializable
data class ListMemberData(val id: String)

@Serializable
data class ListMembersMeta(@SerialName("next_token") val nextToken: String? = null)

@Serializable
data class UsersResponse(val data: List<UserResponse>)

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    @SerialName("username") val userName: String,
    @SerialName("public_metrics") val publicMetrics: UserPublicMetrics
)

@Serializable
data class UserPublicMetrics(
    @SerialName("followers_count") val followersCount: Int,
    @SerialName("tweet_count") val tweetCount: Int
)

@Serializable
data class TweetsResponse(val data: List<TweetData>? = null, val meta: TweetsMeta, val includes: TweetIncludes? = null)

@Serializable
data class TweetIncludes(val media: List<TweetMediaInfo>)

@Serializable
data class TweetMediaInfo(@SerialName("media_key") val mediaKey: String, val type: String)

@Serializable
data class TweetData(
    val id: String,
    val text: String,
    @SerialName("in_reply_to_user_id") val inReplyToUserId: String? = null,
    @SerialName("public_metrics") val publicMetrics: TweetPublicMetrics,
    @SerialName("created_at") val createdAt: String,
    val lang: String,
    @SerialName("conversation_id") val conversationId: String,
    val entities: TweetsEntities? = null,
    val attachments: TweetAttachments? = null
)

@Serializable
data class TweetAttachments(@SerialName("media_keys") val medialKeys: List<String>? = null)

@Serializable
data class TweetsEntities(val hashtags: List<Hashtag>? = null)

@Serializable
data class Hashtag(val tag: String)

@Serializable
data class TweetsMeta(@SerialName("next_token") val nextToken: String? = null)

@Serializable
data class TweetPublicMetrics(
    @SerialName("like_count") val likeCount: Int,
    @SerialName("reply_count") val replyCount: Int,
    @SerialName("retweet_count") val retweetCount: Int,
    @SerialName("quote_count") val quoteCount: Int
)