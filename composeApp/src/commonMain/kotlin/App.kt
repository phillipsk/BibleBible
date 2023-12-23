import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import email.kevinphillips.biblebible.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    val greeting = remember { mutableStateOf(Greeting().greet()) }
    val API_URL = "https://iq-bible.p.rapidapi.com/GetBooks"
    val scope = rememberCoroutineScope()
    val client = HttpClient {
//        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Logging) { logger = Logger.SIMPLE }
    }


    scope.launch(Dispatchers.IO) {
        try {
            val s: String = client.get(API_URL) {
                parameter("language", "english")
                header("X-RapidAPI-Key", BuildKonfig.API_KEY)
            }.body()
            greeting.value = s
        } catch (e: Exception) {
            // Handle exceptions here (e.g., network issues, API errors)
            println("Error: ${e.message}")
        } finally {
            client.close() // Close the HttpClient when done
        }
    }

    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource("compose-multiplatform.xml"), null)
                    Text("Compose: ${greeting.value}")
                }
            }
        }
    }
}