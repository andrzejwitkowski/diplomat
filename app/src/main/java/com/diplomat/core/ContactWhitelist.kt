package com.diplomat.core

import com.diplomat.domain.model.MessageSource

/**
 * Decides which notifications Diplomat is allowed to capture.
 *
 * Filtering happens on two axes:
 *  - source package must be a known messaging app ([MessageSource]);
 *  - sender must match the contact whitelist, unless the whitelist is empty
 *    (empty means "capture everything from supported apps").
 *
 * In a full build the sender set would be user-managed and persisted; here it
 * is an in-memory placeholder so the listener has a real filter to apply.
 */
object ContactWhitelist {

    private val allowedSenders = mutableSetOf<String>()

    val allowedPackages: Set<String> =
        MessageSource.entries
            .filter { it != MessageSource.UNKNOWN }
            .map { it.packageName }
            .toSet()

    fun allowsPackage(packageName: String): Boolean = packageName in allowedPackages

    fun allowsSender(sender: String): Boolean =
        allowedSenders.isEmpty() || allowedSenders.any { it.equals(sender, ignoreCase = true) }

    fun allows(packageName: String, sender: String): Boolean =
        allowsPackage(packageName) && allowsSender(sender)

    fun setSenders(senders: Collection<String>) {
        allowedSenders.clear()
        allowedSenders.addAll(senders)
    }
}
