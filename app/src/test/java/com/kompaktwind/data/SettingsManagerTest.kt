package com.kompaktwind.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsManagerTest {
    private lateinit var settings: SettingsManager

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.preferencesDataStoreFile("kompaktwind_settings").delete()
        settings = SettingsManager(ctx)
    }
    @After fun teardown() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.preferencesDataStoreFile("kompaktwind_settings").delete()
    }

    @Test fun `defaults are m_s celsius auto open-meteo 30min`() = runBlocking {
        val s = settings.flow.first()
        assertThat(s.windUnit).isEqualTo(WindUnit.MS)
        assertThat(s.tempUnit).isEqualTo(TempUnit.C)
        assertThat(s.marineDisplay).isEqualTo(MarineDisplay.AUTO)
        assertThat(s.providerId).isEqualTo("open-meteo")
        assertThat(s.cacheTtlMinutes).isEqualTo(30)
    }

    @Test fun `writes persist`() = runBlocking {
        settings.setWindUnit(WindUnit.KN)
        settings.setTempUnit(TempUnit.F)
        val s = settings.flow.first()
        assertThat(s.windUnit).isEqualTo(WindUnit.KN)
        assertThat(s.tempUnit).isEqualTo(TempUnit.F)
    }
}
