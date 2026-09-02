package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MemoryGameScreen()
            }
        }
    }
}

// --- Data Models ---
data class MemoryCard(
    val id: Int,
    val icon: ImageVector,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

// --- ViewModel ---
class MemoryGameViewModel : ViewModel() {
    private val icons = listOf(
        Icons.Filled.Star, Icons.Filled.Favorite, Icons.Filled.Face, Icons.Filled.Home,
        Icons.Filled.Build, Icons.Filled.Call, Icons.Filled.Email, Icons.Filled.CheckCircle
    )
    
    private val _cards = MutableStateFlow<List<MemoryCard>>(emptyList())
    val cards = _cards.asStateFlow()

    private val _moves = MutableStateFlow(0)
    val moves = _moves.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver = _isGameOver.asStateFlow()

    private var firstSelectedCardIndex: Int? = null
    private var isProcessing = false

    init {
        resetGame()
    }

    fun resetGame() {
        val shuffledIcons = (icons + icons).shuffled()
        _cards.value = shuffledIcons.mapIndexed { index, icon ->
            MemoryCard(id = index, icon = icon)
        }
        _moves.value = 0
        _isGameOver.value = false
        firstSelectedCardIndex = null
        isProcessing = false
    }

    fun onCardClicked(index: Int) {
        if (isProcessing) return
        val card = _cards.value[index]
        if (card.isFaceUp || card.isMatched) return

        // Flip card up
        _cards.value = _cards.value.toMutableList().apply {
            this[index] = card.copy(isFaceUp = true)
        }

        if (firstSelectedCardIndex == null) {
            firstSelectedCardIndex = index
        } else {
            // Second card flipped
            _moves.value += 1
            isProcessing = true
            val firstIndex = firstSelectedCardIndex!!
            val firstCard = _cards.value[firstIndex]

            viewModelScope.launch {
                delay(800) // Let user see the card

                if (firstCard.icon == card.icon) {
                    // Match found
                    _cards.value = _cards.value.toMutableList().apply {
                        this[firstIndex] = this[firstIndex].copy(isMatched = true)
                        this[index] = this[index].copy(isMatched = true)
                    }
                    if (_cards.value.all { it.isMatched }) {
                        _isGameOver.value = true
                    }
                } else {
                    // No match, flip back
                    _cards.value = _cards.value.toMutableList().apply {
                        this[firstIndex] = this[firstIndex].copy(isFaceUp = false)
                        this[index] = this[index].copy(isFaceUp = false)
                    }
                }

                firstSelectedCardIndex = null
                isProcessing = false
            }
        }
    }
}

// --- UI Components ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryGameScreen(viewModel: MemoryGameViewModel = viewModel()) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val moves by viewModel.moves.collectAsStateWithLifecycle()
    val isGameOver by viewModel.isGameOver.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("تحدي الذاكرة", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Score Board
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الحركات: $moves",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = { viewModel.resetGame() }) {
                    Text("إعادة اللعب")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Game Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).widthIn(max = 600.dp)
            ) {
                items(cards.size) { index ->
                    val card = cards[index]
                    MemoryCardView(card = card) {
                        viewModel.onCardClicked(index)
                    }
                }
            }

            // Game Over overlay / alert
            if (isGameOver) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("لقد فزت! 🎉") },
                    text = { Text("أنهيت اللعبة في $moves حركات. هل تريد اللعب مرة أخرى؟") },
                    confirmButton = {
                        Button(onClick = { viewModel.resetGame() }) {
                            Text("العب مرة أخرى")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MemoryCardView(card: MemoryCard, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFaceUp || card.isMatched) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "flip"
    )
    val isBackVisible = rotation <= 90f

    val backgroundColor = if (card.isMatched) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else if (!isBackVisible) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(enabled = !card.isMatched && !card.isFaceUp) { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (card.isMatched) 0.dp else 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!isBackVisible) {
                // Card Face (Icon) - rotate it back 180 so it's not mirrored
                Icon(
                    imageVector = card.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer { rotationY = 180f },
                    tint = if (card.isMatched) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                           else MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                // Card Back
                Icon(
                    imageVector = Icons.Filled.Lock, 
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                )
            }
        }
    }
}
