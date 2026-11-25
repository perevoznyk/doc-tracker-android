package com.example.doctracker.data

import androidx.room.*
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

@Entity(tableName = "categories", indices = [Index("name", unique = true)])
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "documents",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("categoryId")]
)
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val validFrom: LocalDate,
    val validUntil: LocalDate,
    /** Encoded set of [ReminderOption] names, e.g. "MONTH_1;ON_DAY". */
    val reminders: String = "",
    /** Absolute path of a photo copied into the app's private storage (optional). */
    val photoPath: String? = null
)

class Converters {
    @TypeConverter fun toDate(v: String?): LocalDate? = v?.let(LocalDate::parse)
    @TypeConverter fun fromDate(d: LocalDate?): String? = d?.toString()
}

enum class ReminderOption(val label: String, val before: Period) {
    ON_DAY("On expiration day", Period.ZERO),
    WEEK_1("1 week before", Period.ofWeeks(1)),
    MONTH_1("1 month before", Period.ofMonths(1)),
    MONTH_2("2 months before", Period.ofMonths(2)),
    MONTH_3("3 months before", Period.ofMonths(3)),
    MONTH_6("6 months before", Period.ofMonths(6));

    fun triggerDate(validUntil: LocalDate): LocalDate = validUntil.minus(before)

    companion object {
        fun parse(s: String): Set<ReminderOption> =
            s.split(';', ',').mapNotNull { runCatching { valueOf(it.trim()) }.getOrNull() }.toSet()

        fun encode(set: Set<ReminderOption>): String =
            entries.filter { it in set }.joinToString(";") { it.name }
    }
}

data class Validity(val percent: Int, val text: String, val expired: Boolean)

fun Document.validity(today: LocalDate = LocalDate.now()): Validity {
    val total = ChronoUnit.DAYS.between(validFrom, validUntil).coerceAtLeast(1)
    val elapsed = ChronoUnit.DAYS.between(validFrom, today)
    val percent = (elapsed * 100 / total).toInt().coerceIn(0, 100)
    val expired = today.isAfter(validUntil)
    val text = when {
        expired -> "Expired ${humanPeriod(Period.between(validUntil, today))} ago."
        today == validUntil -> "Expires today."
        else -> "Expires in ${humanPeriod(Period.between(today, validUntil))}."
    }
    return Validity(percent, text, expired)
}

private fun humanPeriod(p: Period): String {
    fun unit(n: Int, name: String) = "$n $name${if (n == 1) "" else "s"}"
    val parts = buildList {
        if (p.years > 0) add(unit(p.years, "year"))
        if (p.months > 0) add(unit(p.months, "month"))
        if (p.days > 0) add(unit(p.days, "day"))
    }
    return when (parts.size) {
        0 -> "0 days"
        1 -> parts[0]
        else -> parts.dropLast(1).joinToString(", ") + " and " + parts.last()
    }
}

fun LocalDate.pretty(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
