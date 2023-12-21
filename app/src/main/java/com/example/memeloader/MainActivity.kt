package com.example.memeloader
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import com.example.memeloader.ui.theme.MemeLoaderTheme
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import androidx.lifecycle.viewModelScope
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemeLoaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MemeListScreen()
                }
            }
        }
    }
}

@Composable
fun MemeListScreen(viewModel: MemeViewModel = viewModel()) {
    val meme = viewModel.meme.value
    MemeList(meme, viewModel::loadNewMeme)
}

@Composable
fun MemeList(meme: Meme?, onRefresh: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.weight(1f))

        if (meme != null) {
            Text(
                text = meme.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.Center)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Image(
                    painter = rememberImagePainter(meme.url),
                    contentDescription = meme.name,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            // Handle the case when there is no meme to display
            Text(
                text = "No meme found.",
                modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            Text("Generate new meme")
        }
    }
}


class MemeViewModel : ViewModel() {
    private val repository = MemeRepository()
    private val _meme = mutableStateOf<Meme?>(null)
    val meme: State<Meme?> = _meme

    init {
        loadNewMeme()
    }

    fun loadNewMeme() {
        viewModelScope.launch {
            try {
                _meme.value = repository.getRandomMeme()
            } catch (e: Exception) {
                // Handle exceptions
            }
        }
    }
}

class MemeRepository {
    private val api = Retrofit.Builder()
        .baseUrl("https://api.imgflip.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ImgflipApiService::class.java)

    suspend fun getRandomMeme(): Meme {
        val response = api.getMemes()

        if (response.isSuccessful) {
            val data = response.body()?.data
            if (data != null && data.memes.isNotEmpty()) {
                // Get a random meme
                val randomIndex = Random.nextInt(data.memes.size)
                val memeJson = data.memes[randomIndex]
                val id = memeJson.id
                val name = memeJson.name
                val url = memeJson.url
                val width = memeJson.width
                val height = memeJson.height
                val boxCount = memeJson.boxCount
                return Meme(id, name, url, width, height, boxCount)
            }
        }

        throw Exception("No memes found in the API response.")
    }
}

interface ImgflipApiService {
    @GET("get_memes")
    suspend fun getMemes(): retrofit2.Response<MemeResponse>
}

data class Meme(
    val id: String,
    val name: String,
    val url: String,
    val width: Int,
    val height: Int,
    val boxCount: Int
)

data class MemeResponse(val data: MemeData)

data class MemeData(val memes: List<Meme>)

@Preview(showBackground = true)
@Composable
fun MemeListPreview() {
    MemeLoaderTheme {
    }
}
