import data.domain.Tweet
import data.domain.engagementRate
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartUtils
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.labels.PieSectionLabelGenerator
import org.jfree.chart.labels.StandardPieSectionLabelGenerator
import org.jfree.chart.plot.DatasetRenderingOrder
import org.jfree.chart.plot.PiePlot
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.renderer.category.LineAndShapeRenderer
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.data.general.DefaultPieDataset
import org.joda.time.DateTimeConstants
import java.io.File
import java.text.DecimalFormat
import java.text.NumberFormat

class ChartsHelper {


    fun drawCharts(tweets: List<Tweet>) {
        drawTweetsEngagementPerThreadLengthChart(tweets)
        drawTweetsMediaPieChart(tweets)
        drawTweetsHashtagsPieChart(tweets)
        val byDay = tweets.groupBy { it.timeAndDay.dayOfWeek }
        drawTweetsEngagementPerDayChart(byDay[DateTimeConstants.FRIDAY]!!, "ימי שישי")
        drawTweetsEngagementPerDayChart(byDay[DateTimeConstants.SATURDAY]!!, "ימי שבת")
        val midweek =
            byDay[DateTimeConstants.SUNDAY]!! + byDay[DateTimeConstants.MONDAY]!! + byDay[DateTimeConstants.TUESDAY]!! + byDay[DateTimeConstants.WEDNESDAY]!! + byDay[DateTimeConstants.THURSDAY]!!
        drawTweetsEngagementPerDayChart(midweek, "ימי ראשון עד חמישי")
    }

    private fun drawTweetsMediaPieChart(tweets: List<Tweet>) {
        val mediaCountMap = tweets.groupBy { it.mediaType }.mapValues { it.value.size }
        val dataset = DefaultPieDataset<String>()
        mediaCountMap.forEach { (mediaType, count) ->
            dataset.setValue(mediaType.displayName, count)
        }
        val title = "התפלגות ציוצים לפי מדיה מצורפת"
        val chart = ChartFactory.createPieChart(
            title,   //Chart title
            dataset,
            false,
            true,
            false
        )
        val plot = chart.plot as PiePlot<*>
        val gen: PieSectionLabelGenerator = StandardPieSectionLabelGenerator(
            "{0}: {1} ({2})", NumberFormat.getInstance(), DecimalFormat("0%")
        )
        plot.labelGenerator = gen
        saveChart(chart, title)
    }

    private fun drawTweetsHashtagsPieChart(tweets: List<Tweet>) {
        val hashtagsCountMap =
            tweets.map { it.hashTags ?: emptyList() }.flatten().groupBy { it }.mapValues { it.value.size }
        val dataset = DefaultPieDataset<String>()
        hashtagsCountMap.entries.sortedByDescending { it.value }.take(20).forEach { (hashtag, count) ->
            dataset.setValue(hashtag, count)
        }
        val title = "מספר המופעים של 20 ההאשטגים המובילים"
        val chart = ChartFactory.createPieChart(
            title,   //Chart title
            dataset,
            false,
            true,
            false
        )
        val plot = chart.plot as PiePlot<*>
        val gen: PieSectionLabelGenerator = StandardPieSectionLabelGenerator(
            "{0}: {1}", NumberFormat.getInstance(), DecimalFormat("0%")
        )
        plot.labelGenerator = gen
        saveChart(chart, title)
    }

    private fun drawTweetsEngagementPerThreadLengthChart(tweets: List<Tweet>) {
        val tweetsByThreadLength =
            tweets.filter { it.threadLength > 2 }.groupBy { it.threadLength }.mapValues { entry ->
                val list = dropEnds(entry.value) { it.engagementRate }
                list.size.toDouble() to list.sumOf { it.engagementRate } / list.size
            }
        val tweetsFrequencyDataset = DefaultCategoryDataset()
        val engagementDataset = DefaultCategoryDataset()
        tweetsByThreadLength.entries.sortedBy { it.key }
            .forEach { entry ->
                val countAndAverageEngagement = entry.value
                val threadLength = entry.key
                tweetsFrequencyDataset.addValue(countAndAverageEngagement.first, "", threadLength)
                engagementDataset.addValue(countAndAverageEngagement.second, "", threadLength)
            }
        val title = "ניתוח ציוצים לפי אורך השרשור"
        val chart = ChartFactory.createBarChart(
            title,
            "אורך השרשור",
            "מספר הציוצים",
            tweetsFrequencyDataset,
            PlotOrientation.VERTICAL,
            false,
            false,
            false
        )
        val plot = chart.categoryPlot
        plot.setDataset(1, engagementDataset)
        plot.mapDatasetToRangeAxis(1, 1)
        val axis2 = NumberAxis("מדד היעילות הממוצע")
        plot.setRangeAxis(1, axis2)
        val renderer2 = LineAndShapeRenderer()
        plot.setRenderer(1, renderer2)
        plot.datasetRenderingOrder = DatasetRenderingOrder.FORWARD
        saveChart(chart, title)
    }

    private fun drawTweetsEngagementPerDayChart(tweets: List<Tweet>, dayName: String) {
        val tweetsByHour = tweets.groupBy { it.timeAndDay.hourOfDay }.mapValues { entry ->
            val list = dropEnds(entry.value) { it.engagementRate }
            list.size.toDouble() to list.sumOf { it.engagementRate } / list.size
        }
        val tweetsFrequencyDataset = DefaultCategoryDataset()
        val engagementDataset = DefaultCategoryDataset()
        for (hour in 0..23) {
            val paddedHour = hour.toString().padStart(2, '0')
            val countAndAverageEngagement = tweetsByHour[hour] ?: Pair(0.0, 0.0)
            tweetsFrequencyDataset.addValue(countAndAverageEngagement.first, "", paddedHour)
            engagementDataset.addValue(countAndAverageEngagement.second, "", paddedHour)
        }
        val title = "ניתוח ציוצים ל$dayName"
        val chart = ChartFactory.createBarChart(
            title,
            "שעת הציוץ",
            "מספר הציוצים",
            tweetsFrequencyDataset,
            PlotOrientation.VERTICAL,
            false,
            false,
            false
        )
        val plot = chart.categoryPlot
        plot.setDataset(1, engagementDataset)
        plot.mapDatasetToRangeAxis(1, 1)
        val axis2 = NumberAxis("מדד היעילות הממוצע")
        plot.setRangeAxis(1, axis2)
        val renderer2 = LineAndShapeRenderer()
        plot.setRenderer(1, renderer2)
        plot.datasetRenderingOrder = DatasetRenderingOrder.FORWARD
        saveChart(chart, title)
    }

    private fun dropEnds(tweets: List<Tweet>, selector: (Tweet) -> Double): List<Tweet> {
        val dropCount = (tweets.size * 0.01 * PERCENTAGE_TO_DROP).toInt()
        return tweets.sortedBy(selector).drop(dropCount).dropLast(dropCount)
    }

    private fun saveChart(chart: JFreeChart, filename: String) {
        val dir = File("charts")
        dir.mkdir()
        ChartUtils.saveChartAsJPEG(File(dir, "$filename.jpg"), chart, CHART_WIDTH, CHART_HEIGHT)
    }
}

private const val PERCENTAGE_TO_DROP = 5
private const val CHART_WIDTH = 640
private const val CHART_HEIGHT = 480