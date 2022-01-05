package service

import data.domain.TimeAndDay
import data.domain.Tweet
import data.domain.TweetMediaType
import data.domain.User
import data.remote.ListMembersResponse
import data.remote.TweetData
import data.remote.TweetsResponse
import data.remote.UsersResponse
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

class TwitterService(private val bearerToken: String) {

    private val client = HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(
                        bearerToken,
                        ""
                    )
                }
            }
        }
    }

    suspend fun getListMembers(listId: String): List<User> {
        val responses = mutableListOf<ListMembersResponse>()
        var nextToken: String? = null
        do {
            val response = getListMemberPage(listId, nextToken)
            responses.add(response)
            nextToken = response.meta.nextToken
        } while (nextToken != null)
        val chunkedListMembers = responses.flatMap { it.data }.chunked(100)
        val usersDto = chunkedListMembers.map { chunk -> getUsers(chunk.map { it.id }) }.flatMap { it.data }
        return usersDto.map { dto ->
            User(
                dto.id,
                dto.name,
                dto.userName,
                dto.publicMetrics.followersCount,
                dto.publicMetrics.tweetCount
            )
        }
    }

    private suspend fun getUsers(ids: List<String>): UsersResponse {
        return client.get("https://api.twitter.com/2/users") {
            parameter("ids", ids.joinToString(","))
            parameter("user.fields", "public_metrics")
        }
    }

    private suspend fun getListMemberPage(listId: String, nextToken: String? = null): ListMembersResponse {
        return client.get("https://api.twitter.com/2/lists/$listId/members") {
            if (nextToken != null) {
                parameter("pagination_token", nextToken)
            }
        }
    }

    suspend fun getTweets(user: User, month: Int, year: Int): List<Tweet> {
        val responses = mutableListOf<TweetsResponse>()
        var nextToken: String? = null
        do {
            val startDate = DateTime(dateTimeZone).withDayOfMonth(1).withMonthOfYear(month).withYear(year).withTimeAtStartOfDay().toDateTime(DateTimeZone.UTC)
            val endDate = DateTime(dateTimeZone).withDayOfMonth(1).withMonthOfYear(month).withYear(year).withTimeAtStartOfDay().plusMonths(1).toDateTime(DateTimeZone.UTC)
            val startTime = "${startDate.year}-${startDate.monthOfYear}-${startDate.dayOfMonth}T${startDate.hourOfDay}:00:00Z"
            val endTime = "${endDate.year}-${endDate.monthOfYear}-${endDate.dayOfMonth}T${endDate.hourOfDay}:00:00Z"
            val response = getTweetsPage(user.id, startTime, endTime, nextToken)
            responses.add(response)
            nextToken = response.meta.nextToken
        } while (nextToken != null)
        val mediaTypeMap =
            responses.mapNotNull { it.includes }.map { it.media }.flatten()
                .associate { it.mediaKey to TweetMediaType.fromServerName(it.type) }
        val threadsCount =
            responses.mapNotNull { it.data }.flatten().groupBy { it.conversationId }.mapValues { it.value.size }
        return responses.mapNotNull { it.data }.flatten().filter { shouldKeepTweet(it) }.map { dto ->
            Tweet(
                user,
                dto.id,
                dto.text,
                dto.publicMetrics.likeCount,
                dto.publicMetrics.replyCount,
                dto.publicMetrics.quoteCount,
                dto.publicMetrics.retweetCount,
                dto.entities?.hashtags?.map { it.tag },
                getMediaType(dto.attachments?.medialKeys, mediaTypeMap),
                threadsCount[dto.id]!!,
                dto.createdAt,
                getTimeAndDay(dto.createdAt)
            )
        }
    }

    private fun getTimeAndDay(createdAt: String): TimeAndDay {
        val date = DateTime(createdAt, DateTimeZone.UTC).toDateTime(dateTimeZone)
        val dayOfWeek = date.dayOfWeek
        val hour = date.hourOfDay
        return TimeAndDay(dayOfWeek, hour)
    }

    private fun shouldKeepTweet(tweet: TweetData): Boolean {
        return tweet.inReplyToUserId == null && tweet.lang != "en"
    }

    private fun getMediaType(medialKeys: List<String>?, mediaTypeMap: Map<String, TweetMediaType>): TweetMediaType {
        return if (medialKeys.isNullOrEmpty()) {
            TweetMediaType.None
        } else {
            val mapped = medialKeys.map { mediaTypeMap[it] }.sortedBy { it!!.ordinal }
            mapped.first()!!
        }
    }

    private suspend fun getTweetsPage(
        userId: String,
        startTime: String,
        endTime: String,
        nextToken: String?
    ): TweetsResponse {
        return client.get("https://api.twitter.com/2/users/$userId/tweets") {
            parameter("max_results", 100)
            parameter("exclude", "retweets,replies")
            parameter("start_time", startTime)
            parameter("end_time", endTime)
            parameter("media.fields", "type")
            parameter("expansions", "attachments.media_keys")
            parameter(
                "tweet.fields",
                "public_metrics,in_reply_to_user_id,created_at,entities,attachments,lang,conversation_id"
            )
            if (nextToken != null) {
                parameter("pagination_token", nextToken)
            }
        }
    }
}

private val dateTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("Asia/Jerusalem"))