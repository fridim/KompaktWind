package com.kompaktwind.data.catalog

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CatalogModelsTest {
    @Test fun `lakes are not coastal`() {
        val spot = CatalogSpot(id = "lac", name = "Lac du Bourget", lat = 45.7, lon = 5.8)
        assertThat(spot.likelyCoastal()).isFalse()
    }

    @Test fun `coastal names are coastal`() {
        val spot = CatalogSpot(id = "plage", name = "Soulac - Plage", lat = 45.5, lon = -1.1)
        assertThat(spot.likelyCoastal()).isTrue()
    }

    @Test fun `coastal activities win over a lake name`() {
        val spot = CatalogSpot(
            id = "serre",
            name = "Lac de Serre-Ponçon",
            lat = 44.5,
            lon = 6.3,
            activities = listOf("kitesurfing")
        )
        assertThat(spot.likelyCoastal()).isTrue()
    }

    @Test fun `hint name is used when the spot has no name`() {
        val spot = CatalogSpot(
            id = "hint",
            name = "",
            lat = 43.5,
            lon = 3.9,
            hintName = "Étang du Ponant"
        )
        assertThat(spot.displayName()).isEqualTo("Étang du Ponant")
    }

    @Test fun `coordinates are the last display fallback`() {
        val spot = CatalogSpot(id = "coords", name = "", lat = 43.5, lon = 3.9)
        assertThat(spot.displayName()).isEqualTo("43.5000, 3.9000")
    }

    @Test fun `coastal town with an inland-looking name is coastal`() {
        val spot = CatalogSpot(
            id = "etang-sale",
            name = "L'Étang-Salé",
            lat = -21.27,
            lon = 55.36,
            activities = listOf("wing")
        )
        assertThat(spot.likelyCoastal()).isTrue()
    }
}
