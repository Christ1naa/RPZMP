package ua.kpi.doctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import ua.kpi.doctrack.R
import ua.kpi.doctrack.model.DeadlineGroup
import ua.kpi.doctrack.model.DocumentRecord
import ua.kpi.doctrack.model.groupDeadlineRecords

@Composable
fun DeadlinesScreen(
    records: List<DocumentRecord>,
    today: LocalDate,
    onRecordClick: (DocumentRecord) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    modifier = Modifier.semantics { heading() },
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
                        modifier = Modifier.semantics { heading() },
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
