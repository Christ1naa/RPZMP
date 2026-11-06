package ua.kpi.doctrack.ui

import android.os.Bundle
import androidx.compose.runtime.saveable.listSaver
import java.time.LocalDate
import ua.kpi.doctrack.model.DocumentRecord
import ua.kpi.doctrack.model.RecordCurrency
import ua.kpi.doctrack.model.RecordType

/** Тимчасовий демонстраційний стан у контексті відтворення активності; «Room» замінить цей список. */
internal val recordListSaver = listSaver<List<DocumentRecord>, Bundle>(
    save = { records ->
        records.map { record ->
            Bundle().apply {
                putString("id", record.id)
                putString("title", record.title)
                putString("type", record.type.name)
                putString("deadline", record.deadline.toString())
                putString("issuer", record.issuer)
                putString("issuedOn", record.issuedOn?.toString())
                record.amountMinor?.let { putLong("amountMinor", it) }
                putString("currency", record.currency?.name)
                putBoolean("isPaid", record.isPaid)
                putString("note", record.note)
                putString("imagePath", record.imagePath)
                putString("recognizedText", record.recognizedText)
            }
        }
    },
    restore = { saved ->
        saved.map { bundle ->
            DocumentRecord(
                id = requireNotNull(bundle.getString("id")),
                title = requireNotNull(bundle.getString("title")),
                type = RecordType.valueOf(requireNotNull(bundle.getString("type"))),
                deadline = LocalDate.parse(bundle.getString("deadline")),
                issuer = bundle.getString("issuer"),
                issuedOn = bundle.getString("issuedOn")?.let { LocalDate.parse(it) },
                amountMinor = if (bundle.containsKey("amountMinor")) {
                    bundle.getLong("amountMinor")
                } else null,
                currency = bundle.getString("currency")?.let { RecordCurrency.valueOf(it) },
                isPaid = bundle.getBoolean("isPaid"),
                note = bundle.getString("note"),
                imagePath = bundle.getString("imagePath"),
                recognizedText = bundle.getString("recognizedText")
            )
        }
    }
)
