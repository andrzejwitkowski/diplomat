package pl.diplomat.domain.testsupport

import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact

class WhitelistedContactBuilder {
    private var id: Long = TestConstants.CONTACT_ID
    private var displayName: String = TestConstants.BOB_NAME
    private var phoneNumber: PhoneNumber = PhoneNumber(TestConstants.ALICE_PHONE_FORMATTED)
    private var avatarUri: String? = TestConstants.AVATAR_URI

    fun withId(value: Long) = apply { id = value }

    fun withDisplayName(value: String) = apply { displayName = value }

    fun withPhoneNumber(value: String) = apply { phoneNumber = PhoneNumber(value) }

    fun withPhoneNumber(value: PhoneNumber) = apply { phoneNumber = value }

    fun withAvatarUri(value: String?) = apply { avatarUri = value }

    fun withoutAvatar() = apply { avatarUri = null }

    fun build(): WhitelistedContact = WhitelistedContact(
        id = id,
        displayName = displayName,
        phoneNumber = phoneNumber,
        avatarUri = avatarUri,
    )
}

fun aWhitelistedContact(): WhitelistedContactBuilder = WhitelistedContactBuilder()
