import data.domain.Tweet
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import service.TwitterService
import java.io.File

fun main(args: Array<String>) {
    runBlocking {
        val bearerToken = args[0]
        val twitterListId = args[1]
        val allTweets = loadTweetsFromServer(bearerToken, twitterListId)
        saveTweetsToFile(allTweets)
        println("All tweets count: ${allTweets.size}")
        println("Most followers: ${allTweets.map { it.user }.maxByOrNull { it.followersCount }}")
        println(
            "Most tweets: ${
                allTweets.groupBy { it.user }.map { it.key to it.value.size }.maxByOrNull { it.second }
            }"
        )
        println("Most likes: ${allTweets.maxByOrNull { it.likeCount }}")
        println("Longest thread: " + allTweets.maxByOrNull { it.threadLength })
        println("Top tweets: " + allTweets.sortedByDescending { it.likeCount }.take(3).joinToString("\n"))
        ChartsHelper().drawCharts(allTweets)
    }
}

suspend fun loadTweetsFromServer(bearerToken: String, listId: String): List<Tweet> {
    val service = TwitterService(bearerToken)
    val members = service.getListMembers(listId)
    return members.map { service.getTweets(it, 12, 2021) }.flatten()
}

fun loadTweetsFromFile(): List<Tweet> {
    return Json.decodeFromString(File(FILENAME).readText())
}

fun saveTweetsToFile(tweets: List<Tweet>) {
    File(FILENAME).writeText(Json.encodeToString(tweets))
}

private const val FILENAME = "tweets.json"