package com.kompaktwind.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private lateinit var scope: CoroutineScope
    private lateinit var settingsFile: File

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        settingsFile = File(ctx.cacheDir, "settings-${UUID.randomUUID()}.preferences_pb")
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope) { settingsFile }
        settings = SettingsManager(ctx, store)
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
        scope.cancel()
        settingsFile.delete()
    }

    @Test fun `missing tide key defaults off and other defaults hold`() = runBlocking {
        val s = settings.flow.first()
        assertThat(s.windUnit).isEqualTo(WindUnit.MS)
        assertThat(s.tempUnit).isEqualTo(TempUnit.C)
        assertThat(s.marineDisplay).isEqualTo(MarineDisplay.AUTO)
        assertThat(s.showTides).isFalse()
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
        settings.setShowTides(true)
        val s = settings.flow.first()
        assertThat(s.windUnit).isEqualTo(WindUnit.KN)
        assertThat(s.tempUnit).isEqualTo(TempUnit.F)
        assertThat(s.darkScreen).isTrue()
        assertThat(s.windColorMode).isEqualTo(WindColorMode.COLOR)
        assertThat(s.showTides).isTrue()
    }

    @Test fun `source can be disabled`() = runBlocking {
        settings.setSourceEnabled("openstreetmap.org", false)
        assertThat(settings.flow.first().enabledSources).doesNotContain("openstreetmap.org")
    }
}
