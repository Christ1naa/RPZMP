package ua.kpi.energyforecast.pw04

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import ua.kpi.energyforecast.pw04.data.DemoSites
import ua.kpi.energyforecast.pw04.logic.*
import ua.kpi.energyforecast.pw04.model.SiteReading
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class AndroidCollationTest {
    @Test fun ukrainianAlphabetWorksOnAndroidRuntimeAsWellAsJvm() {
        val input = listOf("Явір", "Ґанок", "Ганок", "Дім", "Єдність", "Енергія", "Житло", "Їжак", "Іскра", "Ирій", "Йод")
            .mapIndexed { i, name -> SiteReading(DemoSites.sites.first().copy(id = "$i", name = name), 1.0, 1.0, Instant.EPOCH) }
        assertEquals(listOf("Ганок", "Ґанок", "Дім", "Енергія", "Єдність", "Житло", "Ирій", "Іскра", "Їжак", "Йод", "Явір"),
            querySites(input, SiteQuery()).map { it.site.name })
    }
}
