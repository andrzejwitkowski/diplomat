package pl.diplomat.infrastructure

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class BaseSpec {

    protected lateinit var context: Context
        private set

    @Before
    fun baseSetUp() {
        context = ApplicationProvider.getApplicationContext()
    }
}
