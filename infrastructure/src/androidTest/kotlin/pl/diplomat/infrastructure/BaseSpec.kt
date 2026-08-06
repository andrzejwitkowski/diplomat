package pl.diplomat.infrastructure

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.runner.RunWith
import pl.diplomat.infrastructure.notification.NotificationParser
import pl.diplomat.infrastructure.notification.ParsedNotification
import pl.diplomat.infrastructure.notification.VisualPlaceholderCatalog
import pl.diplomat.infrastructure.testsupport.NotificationTestConstants

@RunWith(AndroidJUnit4::class)
abstract class BaseSpec {

    protected lateinit var context: Context
        private set

    protected lateinit var notificationParser: NotificationParser
        private set

    @Before
    fun baseSetUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationParser = NotificationParser(VisualPlaceholderCatalog.fromContext(context))
    }

    protected fun parseNotification(
        packageName: String = NotificationTestConstants.WHATSAPP_PACKAGE,
        extras: Bundle,
        postedAtMillis: Long,
        notificationKey: String = NotificationTestConstants.DEFAULT_NOTIFICATION_KEY,
    ): ParsedNotification? = notificationParser.parse(
        packageName = packageName,
        extras = extras,
        postedAtMillis = postedAtMillis,
        notificationKey = notificationKey,
    )
}
