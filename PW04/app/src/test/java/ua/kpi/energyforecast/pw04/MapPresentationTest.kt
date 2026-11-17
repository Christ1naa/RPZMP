package ua.kpi.energyforecast.pw04

import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.pw04.data.DemoSites
import ua.kpi.energyforecast.pw04.logic.*
import ua.kpi.energyforecast.pw04.model.*
import ua.kpi.energyforecast.pw04.ui.siteMarkers
import java.time.Instant

class MapPresentationTest {
    private val readings = DemoSites.sites.map { SiteReading(it, 1.0, 12.5, Instant.EPOCH) }

    @Test fun markersMatchVisibleIdsCoordinatesSelectionAndInfoWindowFields() {
        val visible = querySites(readings, SiteQuery(type = SiteType.OFFICE, status = SiteStatus.ACTIVE,
            minPowerKw = "30", maxPowerKw = "50"))
        val marker = siteMarkers(visible, "office-1").single()
        val site = DemoSites.sites.first { it.id == "office-1" }
        assertEquals(site.id, marker.id)
        assertEquals(site.position, marker.position)
        assertEquals(site.name, marker.title)
        assertTrue(marker.isSelected)
        assertTrue(marker.snippet.contains(site.type.label))
        assertTrue(marker.snippet.contains(site.status.label))
        assertTrue(marker.snippet.contains(site.description))
        assertTrue(marker.snippet.contains("12,50 кВт·год/добу"))
    }

    @Test fun sortingAndNewReadingsDoNotMoveMarkersOrChangeIds() {
        val before = siteMarkers(readings, "home-1").associateBy { it.id }
        val changed = querySites(readings.map { it.copy(forecastDailyKwh = 99.0) }, SiteQuery(primary = SiteSort.POWER, descending = true))
        val after = siteMarkers(changed, "home-1")
        assertEquals(changed.map { it.site.id }, after.map { it.id })
        after.forEach { marker -> assertEquals(before.getValue(marker.id).position, marker.position) }
        assertEquals(listOf("home-1"), after.filter { it.isSelected }.map { it.id })
    }

    @Test fun noMatchingObjectsOrHiddenSelectionDoNotLeaveGhostMarkers() {
        assertTrue(siteMarkers(emptyList(), "home-1").isEmpty())
        val visible = querySites(readings, SiteQuery(type = SiteType.OFFICE))
        val markers = siteMarkers(visible, "home-1")
        assertEquals(setOf("office-1", "office-2"), markers.map { it.id }.toSet())
        assertFalse(markers.any { it.isSelected })
    }
}
