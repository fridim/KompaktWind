package com.kompaktwind.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LanguageManagerTest {
    private lateinit var context: Context

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("kompaktwind_language", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @After fun teardown() {
        Locale.setDefault(Locale.US)
    }

    @Test fun `defaults to system and persists an override`() {
        val manager = LanguageManager(context)
        assertThat(manager.currentTag()).isNull()

        manager.setTag("fr")
        assertThat(LanguageManager(context).currentTag()).isEqualTo("fr")
        assertThat(manager.wrap(context).resources.configuration.locales[0].language)
            .isEqualTo("fr")

        manager.setTag(null)
        assertThat(manager.currentTag()).isNull()
    }
}
