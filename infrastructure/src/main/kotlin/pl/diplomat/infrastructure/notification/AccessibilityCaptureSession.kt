package pl.diplomat.infrastructure.notification

class AccessibilityCaptureSession {
    private var contactKey: String? = null
    private val knownFingerprints = linkedSetOf<String>()

    fun onScan(
        contactKey: String,
        candidates: List<WhatsAppNodeMessageExtractor.MessageCandidate>,
    ): List<WhatsAppNodeMessageExtractor.MessageCandidate> {
        val fingerprints = candidates.map { it.fingerprint(contactKey) }
        if (contactKey != this.contactKey) {
            this.contactKey = contactKey
            knownFingerprints.clear()
            knownFingerprints.addAll(fingerprints)
            return emptyList()
        }
        return candidates.filterIndexed { index, _ -> knownFingerprints.add(fingerprints[index]) }
    }
}
