package pl.diplomat.infrastructure.persistence

data class ContactUnreadCountEntity(
    val contactId: Long,
    val unreadCount: Int,
)
