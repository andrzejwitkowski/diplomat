package pl.diplomat.infrastructure.notification

class AccessibilityCaptureSession {
    private var contactKey: String? = null
    private val knownCounts = mutableMapOf<String, Int>()

    fun onScan(
        contactKey: String,
        candidates: List<WhatsAppNodeMessageExtractor.MessageCandidate>,
    ): List<WhatsAppNodeMessageExtractor.MessageCandidate> {
        if (contactKey != this.contactKey) {
            this.contactKey = contactKey
            knownCounts.clear()
            for (candidate in candidates) {
                val key = contentKey(candidate)
                knownCounts[key] = (knownCounts[key] ?: 0) + 1
            }
            return emptyList()
        }

        val fresh = mutableListOf<WhatsAppNodeMessageExtractor.MessageCandidate>()
        val seenThisScan = mutableMapOf<String, Int>()
        for (candidate in candidates) {
            val key = contentKey(candidate)
            val occurrence = seenThisScan[key] ?: 0
            seenThisScan[key] = occurrence + 1
            val known = knownCounts[key] ?: 0
            if (occurrence >= known) {
                fresh.add(candidate.copy(occurrence = occurrence))
                knownCounts[key] = occurrence + 1
            }
        }
        return fresh
    }

    private fun contentKey(candidate: WhatsAppNodeMessageExtractor.MessageCandidate): String =
        "${candidate.text}\u0000${candidate.isOutgoing}\u0000${candidate.isMediaOnly}"
}
