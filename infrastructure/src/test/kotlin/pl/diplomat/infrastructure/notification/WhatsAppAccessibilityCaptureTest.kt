package pl.diplomat.infrastructure.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppNodeMessageExtractorTest {

    @Test
    fun extractsTitleFromKnownViewId() {
        val nodes = listOf(
            node("Alice", viewId = "com.whatsapp:id/conversation_contact_name", top = 40, centerX = 200),
            node("hello", viewId = "com.whatsapp:id/message_text", top = 400, centerX = 100),
        )
        assertEquals("Alice", WhatsAppNodeMessageExtractor.extractConversationTitle(nodes))
    }

    @Test
    fun classifiesInboundAndOutboundByGeometry() {
        val nodes = listOf(
            node("Alice", viewId = "com.whatsapp:id/conversation_contact_name", top = 40, centerX = 200),
            node("from them", viewId = "com.whatsapp:id/message_text", top = 400, centerX = 120),
            node("from me", viewId = "com.whatsapp:id/message_text", top = 500, centerX = 700),
        )
        val messages = WhatsAppNodeMessageExtractor.extractMessages(
            nodes = nodes,
            screenWidth = 1080,
            conversationTitle = "Alice",
        )
        assertEquals(
            listOf(
                WhatsAppNodeMessageExtractor.MessageCandidate("from them", isOutgoing = false, occurrence = 0),
                WhatsAppNodeMessageExtractor.MessageCandidate("from me", isOutgoing = true, occurrence = 0),
            ),
            messages,
        )
    }

    @Test
    fun preservesDuplicateIdenticalBubblesWithOccurrence() {
        val nodes = listOf(
            node("Alice", viewId = "com.whatsapp:id/conversation_contact_name", top = 40, centerX = 200),
            node("ok", viewId = "com.whatsapp:id/message_text", top = 400, centerX = 120),
            node("ok", viewId = "com.whatsapp:id/message_text", top = 500, centerX = 120),
        )
        val messages = WhatsAppNodeMessageExtractor.extractMessages(nodes, 1080, "Alice")
        assertEquals(
            listOf(
                WhatsAppNodeMessageExtractor.MessageCandidate("ok", false, 0),
                WhatsAppNodeMessageExtractor.MessageCandidate("ok", false, 1),
            ),
            messages,
        )
    }

    @Test
    fun extractComposeTextPrefersEditableEntry() {
        val nodes = listOf(
            node("draft", viewId = "com.whatsapp:id/entry", isEditable = true, top = 1800, centerX = 500),
            node("bubble", viewId = "com.whatsapp:id/message_text", top = 500, centerX = 700),
        )
        assertEquals("draft", WhatsAppNodeMessageExtractor.extractComposeText(nodes))
    }

    @Test
    fun skipsChromeLabels() {
        val nodes = listOf(
            node("Alice", viewId = "com.whatsapp:id/conversation_contact_name", top = 40, centerX = 200),
            node("online", className = "android.widget.TextView", top = 80, centerX = 200),
            node("hi", viewId = "com.whatsapp:id/message_text", top = 400, centerX = 100),
        )
        val messages = WhatsAppNodeMessageExtractor.extractMessages(nodes, 1080, "Alice")
        assertEquals(listOf(WhatsAppNodeMessageExtractor.MessageCandidate("hi", false, 0)), messages)
    }

    @Test
    fun clearingUnsentDraftDoesNotCreateMessageCandidates() {
        val nodesWithDraft = listOf(
            node("Alice", viewId = "com.whatsapp:id/conversation_contact_name", top = 40, centerX = 200),
            node("draft", viewId = "com.whatsapp:id/entry", isEditable = true, top = 1800, centerX = 500),
        )
        val nodesCleared = listOf(
            node("Alice", viewId = "com.whatsapp:id/conversation_contact_name", top = 40, centerX = 200),
            node("", viewId = "com.whatsapp:id/entry", isEditable = true, top = 1800, centerX = 500),
        )
        assertEquals("draft", WhatsAppNodeMessageExtractor.extractComposeText(nodesWithDraft))
        assertEquals(null, WhatsAppNodeMessageExtractor.extractComposeText(nodesCleared))
        assertTrue(WhatsAppNodeMessageExtractor.extractMessages(nodesCleared, 1080, "Alice").isEmpty())
    }

    private fun node(
        text: String,
        className: String? = "android.widget.TextView",
        viewId: String? = null,
        centerX: Int = 0,
        top: Int = 0,
        bottom: Int = top + 60,
        isEditable: Boolean = false,
    ) = WhatsAppNodeMessageExtractor.NodeTextSnapshot(
        text = text,
        className = className,
        viewId = viewId,
        centerX = centerX,
        top = top,
        bottom = bottom,
        isEditable = isEditable,
    )
}

class AccessibilityCaptureSessionTest {

    @Test
    fun firstScanBaselinesWithoutEmitting() {
        val session = AccessibilityCaptureSession()
        val first = session.onScan(
            "Alice",
            listOf(
                WhatsAppNodeMessageExtractor.MessageCandidate("old", false),
                WhatsAppNodeMessageExtractor.MessageCandidate("older", true),
            ),
        )
        assertTrue(first.isEmpty())
    }

    @Test
    fun secondScanEmitsOnlyFreshBubbles() {
        val session = AccessibilityCaptureSession()
        val baseline = listOf(
            WhatsAppNodeMessageExtractor.MessageCandidate("old", false),
        )
        session.onScan("Alice", baseline)

        val fresh = session.onScan(
            "Alice",
            baseline + WhatsAppNodeMessageExtractor.MessageCandidate("new reply", true),
        )
        assertEquals(
            listOf(WhatsAppNodeMessageExtractor.MessageCandidate("new reply", true)),
            fresh,
        )
    }

    @Test
    fun conversationSwitchResetsBaseline() {
        val session = AccessibilityCaptureSession()
        session.onScan("Alice", listOf(WhatsAppNodeMessageExtractor.MessageCandidate("hi", false)))
        val afterSwitch = session.onScan(
            "Bob",
            listOf(WhatsAppNodeMessageExtractor.MessageCandidate("yo", false)),
        )
        assertTrue(afterSwitch.isEmpty())

        val next = session.onScan(
            "Bob",
            listOf(
                WhatsAppNodeMessageExtractor.MessageCandidate("yo", false),
                WhatsAppNodeMessageExtractor.MessageCandidate("later", true),
            ),
        )
        assertEquals(
            listOf(WhatsAppNodeMessageExtractor.MessageCandidate("later", true)),
            next,
        )
    }

    @Test
    fun emitsSecondIdenticalMessageAsFreshOccurrence() {
        val session = AccessibilityCaptureSession()
        session.onScan(
            "Alice",
            listOf(WhatsAppNodeMessageExtractor.MessageCandidate("ok", false)),
        )
        val fresh = session.onScan(
            "Alice",
            listOf(
                WhatsAppNodeMessageExtractor.MessageCandidate("ok", false),
                WhatsAppNodeMessageExtractor.MessageCandidate("ok", false),
            ),
        )
        assertEquals(
            listOf(WhatsAppNodeMessageExtractor.MessageCandidate("ok", false, occurrence = 1)),
            fresh,
        )
    }

    @Test
    fun scrollingAwayDoesNotReemitBaselineMessages() {
        val session = AccessibilityCaptureSession()
        session.onScan(
            "Alice",
            listOf(
                WhatsAppNodeMessageExtractor.MessageCandidate("ok", false),
                WhatsAppNodeMessageExtractor.MessageCandidate("ok", false),
            ),
        )
        val afterScroll = session.onScan(
            "Alice",
            listOf(WhatsAppNodeMessageExtractor.MessageCandidate("ok", false)),
        )
        assertTrue(afterScroll.isEmpty())
    }
}
