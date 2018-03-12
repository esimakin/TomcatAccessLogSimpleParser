import java.io.File
import java.io.FileFilter
import java.nio.file.Files
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

val regex: Regex = Regex("^-\\s(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\s\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\s-\\s-\\s\\[(.*)]\\s\"(.+?)\"\\s\\d+\\s(\\d+|-)\\s\"(.+?)\".*$")
val fullDateFormatStr: String = "dd/MMM/yyyy:HH:mm:ss ZZZZ"
val onlyDateFormatStr: String = "dd MMM yyyy"
val fileDateFormatStr: String = "yyyy-MM-dd"

data class LogEntry(val date: Date, val remotehost: String, val requestFirstLine: String, val userAgent: String)

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Please, supply the path to the Tomcat logs directory")
        return
    }

    val logDir = File(args[0])
    if (logDir.isFile || !logDir.canRead()) {
        println("The logs directory is incorrect")
        return
    }

    val entries: Map<File, List<LogEntry>> = logDir
            .listFiles(FileFilter { if (it != null) it.name.contains(Regex("access\\.")) else false })
            .map { file ->
                file to Files.readAllLines(file.toPath())
                        .map { it.toLogEntry() }
                        .filterNotNull()
                        .filter { it.requestFirstLine.contains("/showcase-marketing/page.js") }
            }
            .filter { it.second.isNotEmpty() }
            .toMap()

    entries.keys.toSortedSet().forEach { file ->
        val fileDateStr: String? = if (file.name.length == "access.0000-00-00.log".length) {
            file.name.substring("access.".length, file.name.length - "log".length)
        } else null
        val date: Date? = try {
            SimpleDateFormat(fileDateFormatStr).parse(fileDateStr)
        } catch (ex: ParseException) { null }
        if (date != null) {
            val df = SimpleDateFormat(onlyDateFormatStr)
            println("DATE: ${df.format(date)}")
            println("IP address        | Req.")
            var counter: Int = 0
            entries[file]?.groupBy { it.remotehost }?.forEach {
                println(String.format("%-18s| %-4d", it.key, it.value.size))
                counter += it.value.size
            }
            println("TOTAL: $counter")
            println()
        }
    }
}


fun String.toLogEntry(): LogEntry? {
    if (this.isBlank()) {
        return null
    }

    val m = regex.toPattern().matcher(this)
    if (m.find()) {
        val df = SimpleDateFormat(fullDateFormatStr)
        return try {
            LogEntry(date = df.parse(m.group(2)),
                    remotehost = m.group(1),
                    requestFirstLine = m.group(3),
                    userAgent = m.group(5))
        } catch (ex: ParseException) {
            null
        }
    } else {
        return null
    }
}

