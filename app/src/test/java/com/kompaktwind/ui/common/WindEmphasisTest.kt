package com.kompaktwind.ui.common

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WindEmphasisTest {
    @Test fun `color scale starts light and ends dark`() {
        assertThat(WindEmphasis.colorBackground(0.0)).isEqualTo(Color.White)
        assertThat(WindEmphasis.colorBackground(40.0)).isEqualTo(Color(0xFF0000FF))
    }

    @Test fun `color foreground contrasts with the background`() {
        assertThat(WindEmphasis.colorForeground(Color.White)).isEqualTo(Color.Black)
        assertThat(WindEmphasis.colorForeground(Color(0xFF0000FF))).isEqualTo(Color.White)
    }

    @Test fun `warm colors keep black foreground`() {
        assertThat(WindEmphasis.colorForeground(Color(0xFFFF322C))).isEqualTo(Color.Black)
        assertThat(WindEmphasis.colorForeground(Color(0xFFFF0AC8))).isEqualTo(Color.Black)
        assertThat(WindEmphasis.colorForeground(Color(0xFF9632FF))).isEqualTo(Color.White)
    }

    @Test fun `tiers use the home thresholds`() {
        assertThat(WindEmphasis.tier(4.8)).isEqualTo(0)
        assertThat(WindEmphasis.tier(4.9)).isEqualTo(1)
        assertThat(WindEmphasis.tier(7.7)).isEqualTo(2)
        assertThat(WindEmphasis.tier(12.86)).isEqualTo(3)
    }
}
