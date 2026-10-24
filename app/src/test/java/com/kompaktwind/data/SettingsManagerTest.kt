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
        runBlocking {
            settings.setWindUnit(WindUnit.MS)
            settings.setTempUnit(TempUnit.C)
            settings.setMarineDisplay(MarineDisplay.AUTO)
            settings.setCacheTtlMinutes(30)
            settings.setDarkScreen(false)
            settings.setWindColorMode(WindColorMode.MONO)
            settings.setSourceEnabled("openstreetmap.org", true)
        }
    }
    @After fun teardown() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.preferencesDataStoreFile("kompaktwind_settings").delete()
    }

    @Test fun `defaults are m_s celsius auto 30min light screen`() = runBlocking {
        val s = settings.flow.first()
        assertThat(s.windUnit).isEqualTo(WindUnit.MS)
        assertThat(s.tempUnit).isEqualTo(TempUnit.C)
        assertThat(s.marineDisplay).isEqualTo(MarineDisplay.AUTO)
        assertThat(s.cacheTtlMinutes).isEqualTo(30)
        assertThat(s.darkScreen).isFalse()
        assertThat(s.windColorMode).isEqualTo(WindColorMode.MONO)
        assertThat(s.enabledSources).contains("openstreetmap.org")
    }

    @Test fun `writes persist`() = runBlocking {
        settings.setWindUnit(WindUnit.KN)
        settings.setTempUnit(TempUnit.F)
        settings.setDarkScreen(true)
        settings.setWindColorMode(WindColorMode.COLOR)
        val s = settings.flow.first()
        assertThat(s.windUnit).isEqualTo(WindUnit.KN)
        assertThat(s.tempUnit).isEqualTo(TempUnit.F)
        assertThat(s.darkScreen).isTrue()
        assertThat(s.windColorMode).isEqualTo(WindColorMode.COLOR)
    }

    @Test fun `source can be disabled`() = runBlocking {
        settings.setSourceEnabled("openstreetmap.org", false)
        assertThat(settings.flow.first().enabledSources).doesNotContain("openstreetmap.org")
    }
}
