package ua.kpi.doctrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ua.kpi.doctrack.ui.DocTrackApp
import ua.kpi.doctrack.ui.theme.DocTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DocTrackTheme {
                DocTrackApp()
            }
        }
    }
}