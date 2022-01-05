package data.domain

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val userName: String,
    val followersCount: Int,
    val tweetCount: Int
)

@Serializable
data class Tweet(
    val user: User,
    val id: String,
    val text: String,
    val likeCount: Int,
    val replyCount: Int,
    val quoteCount: Int,
    val retweetCount: Int,
    val hashTags: List<String>?,
    val mediaType: TweetMediaType,
    val threadLength: Int,
    val createdAt: String,
    val timeAndDay: TimeAndDay
)

@Serializable
data class TimeAndDay(val dayOfWeek: Int, val hourOfDay: Int)

enum class TweetMediaType {
    Video {
        override val displayName: String = "וידאו"
    },
    Gif {
        override val displayName: String = "גיף"
    },
    Image {
        override val displayName: String = "תמונה"
    },
    None {
        override val displayName: String = "ללא מדיה"
    };

    abstract val displayName: String

    companion object {
        fun fromServerName(name: String): TweetMediaType {
            return when (name) {
                "video" -> Video
                "animated_gif" -> Gif
                "photo" -> Image
                else -> None
            }
        }
    }
}

val Tweet.totalRetweets: Int
    get() = quoteCount + retweetCount

val Tweet.engagementRate: Double
    get() = (100.0 * likeCount + 500 * replyCount + 1000.0 * totalRetweets) / (user.followersCount)
