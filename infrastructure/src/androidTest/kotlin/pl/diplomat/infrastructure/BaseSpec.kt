package pl.diplomat.infrastructure

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.runner.RunWith
import pl.diplomat.infrastructure.notification.NotificationParser
import pl.diplomat.infrastructure.notification.VisualPlaceholderCatalog

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
}
