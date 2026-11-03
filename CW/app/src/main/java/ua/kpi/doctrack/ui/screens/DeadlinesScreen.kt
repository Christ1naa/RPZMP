package ua.kpi.doctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlinx.coroutines.delay
import ua.kpi.doctrack.R
import ua.kpi.doctrack.model.DeadlineGroup
import ua.kpi.doctrack.model.DocumentRecord
import ua.kpi.doctrack.model.groupDeadlineRecords

@Composable
fun DeadlinesScreen(
    records: List<DocumentRecord>,
    onRecordClick: (DocumentRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var today by remember { mutableStateOf(LocalDate.now()) }
    // Refresh dates if this tab stays open across midnight or the device clock changes.
    LaunchedEffect(Unit) {
        while (true) {
            today = LocalDate.now()
            delay(60_000L)
        }
    }
    val sections = remember(records, today) { groupDeadlineRecords(records, today) }

    if (sections.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.deadlines_empty_title),
            description = stringResource(R.string.deadlines_empty_description),
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "title") {
                Text(
                    text = stringResource(R.string.nav_deadlines),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            item(key = "description") {
                Text(
                    text = stringResource(R.string.deadlines_description),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            sections.forEach { section ->
                item(key = "group_${section.group.name}") {
                    val labelRes = when (section.group) {
                        DeadlineGroup.PAST -> R.string.deadlines_group_past
                        DeadlineGroup.TODAY -> R.string.deadlines_group_today
                        DeadlineGroup.NEXT_SEVEN_DAYS -> R.string.deadlines_group_soon
                        DeadlineGroup.LATER -> R.string.deadlines_group_later
                    }
                    Text(
                        text = stringResource(
                            R.string.deadlines_group_count,
                            stringResource(labelRes), section.records.size
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(section.records, key = { "record_${it.id}" }) { record ->
                    RecordCard(
                        record = record,
                        onClick = { onRecordClick(record) },
                        today = today
                    )
                }
            }
        }
    }
}
