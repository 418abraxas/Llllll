package dev.findthink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScorerTest {
    private val apps = listOf(
        AppEntry("Maps", "a.maps"), AppEntry("Mapsly Pro", "b.mapsly"),
        AppEntry("Camera", "c.cam"), AppEntry("Clock", "d.clock"), AppEntry("Photo Maps", "e.pm"),
    )
    private val none = { _: String -> 0 }

    private fun labels(l: List<AppEntry>) = l.map { it.label }

    @Test fun exactBeatsPrefix() = assertEquals("Maps", Scorer.rank(apps, "maps", none).first().label)
    @Test fun partialMatches() = assertTrue(labels(Scorer.rank(apps, "cam", none)).contains("Camera"))
    @Test fun caseInsensitive() = assertEquals(labels(Scorer.rank(apps, "MAPS", none)), labels(Scorer.rank(apps, "maps", none)))
    @Test fun wordPrefixMatches() = assertTrue(labels(Scorer.rank(apps, "pro", none)).contains("Mapsly Pro"))
    @Test fun emptyQueryNoUsageIsEmpty() = assertTrue(Scorer.rank(apps, "", none).isEmpty())
    @Test fun emptyQueryReturnsMostUsed() =
        assertEquals(listOf("Clock"), labels(Scorer.rank(apps, "", { if (it == "d.clock") 3 else 0 })))
    @Test fun noMatch() = assertTrue(Scorer.rank(apps, "zzz", none).isEmpty())
    @Test fun equalScoresAreDeterministic() {
        val a = Scorer.rank(apps, "c", none)
        assertEquals(labels(a), labels(Scorer.rank(apps.reversed(), "c", none)))
    }
    @Test fun learnedPreferenceReorders() {
        val before = Scorer.rank(apps, "ma", none).first().label
        val after = Scorer.rank(apps, "ma", { if (it == "b.mapsly") 20 else 0 }).first().label
        assertEquals("Maps", before)
        assertEquals("Mapsly Pro", after)
    }
    @Test fun subsequenceMatches() = assertTrue(labels(Scorer.rank(apps, "cmr", none)).contains("Camera"))
}
