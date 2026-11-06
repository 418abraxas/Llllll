package dev.findthink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiParseTest {
    @Test fun openApp() = assertEquals(Action.OpenApp("Maps"), AiActions.parseOne("""{"action":"open_app","app":"Maps"}"""))
    @Test fun searchStore() = assertEquals(Action.SearchStore("chess"), AiActions.parseOne("""{"action":"search_store","query":"chess"}"""))
    @Test fun setTimer() = assertEquals(Action.SetTimer(300, "tea"), AiActions.parseOne("""{"action":"set_timer","seconds":300,"label":"tea"}"""))
    @Test fun openUrl() = assertEquals(Action.OpenUrl("https://example.com/a"), AiActions.parseOne("""{"action":"open_url","url":"https://example.com/a"}"""))
    @Test fun unknownAction() = assertNull(AiActions.parseOne("""{"action":"delete_all"}"""))
    @Test fun missingArgs() = assertNull(AiActions.parseOne("""{"action":"open_app"}"""))
    @Test fun wrongTypes() {
        assertNull(AiActions.parseOne("""{"action":"set_timer","seconds":"60"}"""))
        assertNull(AiActions.parseOne("""{"action":"open_app","app":5}"""))
    }
    @Test fun malformedJson() = assertNull(AiActions.parseOne("{action: open_app"))
    @Test fun nestedStructures() {
        assertNull(AiActions.parseOne("""{"action":"open_app","app":{"x":1}}"""))
        assertNull(AiActions.parseOne("""{"action":{"a":1}}"""))
    }
    @Test fun invalidUrls() {
        for (u in listOf("http://example.com", "javascript:alert(1)", "file:///etc/passwd", "intent://x#Intent;end", "https://", "https://user:pw@evil.com", "not a url"))
            assertNull(u, AiActions.parseOne("""{"action":"open_url","url":"$u"}"""))
    }
    @Test fun timerRange() {
        assertNull(AiActions.parseOne("""{"action":"set_timer","seconds":0}"""))
        assertNull(AiActions.parseOne("""{"action":"set_timer","seconds":-5}"""))
        assertNull(AiActions.parseOne("""{"action":"set_timer","seconds":86401}"""))
        assertNull(AiActions.parseOne("""{"action":"set_timer","seconds":1.5}"""))
    }
    @Test fun multipleActionsAreCapped() {
        val t = "Sure.\n" + (1..5).joinToString("\n") { """ACTION: {"action":"open_app","app":"A$it"}""" }
        assertEquals(3, AiActions.parse(t).size)
    }
    @Test fun stripRemovesActionLines() =
        assertEquals("Hello", AiActions.stripActions("""Hello
ACTION: {"action":"open_app","app":"Maps"}"""))
    @Test fun payloadDefaultIsQueryOnly() {
        val p = AiClient.buildPayload("m", "hi", null)
        assertTrue(p.contains("hi") && !p.contains("Installed apps"))
    }
    @Test fun deltaParsing() {
        assertEquals("ab", AiClient.parseDelta("""{"choices":[{"delta":{"content":"ab"}}]}"""))
        assertEquals("", AiClient.parseDelta("""{"choices":[{"delta":{"content":null}}]}"""))
        assertEquals("", AiClient.parseDelta("garbage"))
    }
}
