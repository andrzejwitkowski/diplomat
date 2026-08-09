package pl.diplomat.infrastructure.notification

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.launch
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.infrastructure.DiplomatServiceLocator
import pl.diplomat.infrastructure.debug.DevLog
import pl.diplomat.infrastructure.service.DiplomatForegroundService
import pl.diplomat.usecase.RawIncomingMessage

class DiplomatWhatsAppAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val session = AccessibilityCaptureSession()
    private var debounceRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        DiplomatForegroundService.startSafely(this)
        DevLog.log("A11Y", "service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        if (!NotificationParser.isWhatsAppPackage(packageName)) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            -> scheduleScan()
        }
    }

    override fun onInterrupt() {
        debounceRunnable?.let { handler.removeCallbacks(it) }
        debounceRunnable = null
    }

    private fun scheduleScan() {
        debounceRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable { scanActiveWindow() }
        debounceRunnable = runnable
        handler.postDelayed(runnable, DEBOUNCE_MS)
    }

    private fun scanActiveWindow() {
        val root = rootInActiveWindow ?: return
        try {
            val packageName = root.packageName?.toString()
            if (packageName == null || !NotificationParser.isWhatsAppPackage(packageName)) {
                return
            }
            val snapshots = ArrayList<WhatsAppNodeMessageExtractor.NodeTextSnapshot>(64)
            collectSnapshots(root, snapshots)
            processSnapshots(snapshots)
        } catch (e: RuntimeException) {
            DevLog.log("A11Y", "scan failed: ${e.javaClass.simpleName}")
        } finally {
            root.recycle()
        }
    }

    private fun processSnapshots(snapshots: List<WhatsAppNodeMessageExtractor.NodeTextSnapshot>) {
        val title = WhatsAppNodeMessageExtractor.extractConversationTitle(snapshots)
        if (title.isNullOrBlank()) return

        val scanMillis = System.currentTimeMillis()
        val candidates = WhatsAppNodeMessageExtractor.extractMessages(
            nodes = snapshots,
            screenWidth = resources.displayMetrics.widthPixels,
            conversationTitle = title,
            referenceMillis = scanMillis,
        )
        val fresh = session.onScan(title, candidates)
        if (fresh.isEmpty()) return

        val locator = application as? DiplomatServiceLocator ?: run {
            DevLog.log("ERROR", "application is not DiplomatServiceLocator")
            return
        }

        locator.applicationScope.launch {
            for (candidate in fresh) {
                val content = resolveAccessibilityContent(locator, candidate) ?: continue
                val timestamp = candidate.timestampMillis ?: scanMillis
                DevLog.log(
                    "A11Y",
                    "emit outgoing=${candidate.isOutgoing} ts=$timestamp textLen=${candidate.text.length}",
                )
                locator.dispatchCapturedMessage(
                    RawIncomingMessage(
                        senderPhone = title,
                        content = content,
                        timestamp = timestamp,
                        sourceApp = MessageSourceApp.WHATSAPP,
                        notificationKey = "a11y:${candidate.fingerprint(title)}",
                        isOutgoing = candidate.isOutgoing,
                    ),
                    logTag = "A11Y",
                )
            }
        }
    }

    private fun collectSnapshots(
        node: AccessibilityNodeInfo,
        out: MutableList<WhatsAppNodeMessageExtractor.NodeTextSnapshot>,
    ) {
        val text = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        val content = text.ifBlank { desc }
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (content.isNotBlank() || node.isEditable) {
            out.add(
                WhatsAppNodeMessageExtractor.NodeTextSnapshot(
                    text = content,
                    className = node.className?.toString(),
                    viewId = node.viewIdResourceName,
                    centerX = bounds.centerX(),
                    top = bounds.top,
                    bottom = bounds.bottom,
                    isEditable = node.isEditable,
                ),
            )
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                collectSnapshots(child, out)
            } finally {
                child.recycle()
            }
        }
    }

    private fun resolveAccessibilityContent(
        locator: DiplomatServiceLocator,
        candidate: WhatsAppNodeMessageExtractor.MessageCandidate,
    ): MessageContent? {
        if (candidate.isMediaOnly) {
            return MessageContent.VisualOnly(VisualMediaKind.PHOTO)
        }
        return locator.notificationParser.resolveTextContent(candidate.text)
    }

    companion object {
        private const val DEBOUNCE_MS = 600L
    }
}
