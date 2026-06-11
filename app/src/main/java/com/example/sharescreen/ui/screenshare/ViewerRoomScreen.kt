package com.example.sharescreen.ui.screenshare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sharescreen.data.remote.websocket.ConnectionState
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun ViewerRoomScreen(
    navController: NavController,
    roomCode: String,
    viewerName: String,
    viewModel: ScreenShareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val remoteVideoTrack by viewModel.remoteVideoTrack.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var rendererRef by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    LaunchedEffect(roomCode, viewerName) {
        viewModel.connectAsViewer(roomCode, viewerName)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isRoomClosed) {
        if (uiState.isRoomClosed) navController.popBackStack()
    }

    // Attach/detach video track when it changes
    LaunchedEffect(remoteVideoTrack) {
        val renderer = rendererRef ?: return@LaunchedEffect
        remoteVideoTrack?.addSink(renderer)
    }

    DisposableEffect(Unit) {
        onDispose {
            rendererRef?.let { renderer ->
                remoteVideoTrack?.removeSink(renderer)
                renderer.release()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (remoteVideoTrack != null) {
            // Video stream
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).also { renderer ->
                        rendererRef = renderer
                        renderer.init(viewModel.eglBaseContext, null)
                        renderer.setMirror(false)
                        renderer.setEnableHardwareScaler(true)
                        remoteVideoTrack?.addSink(renderer)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Waiting state
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState.connectionState) {
                    is ConnectionState.Connected -> {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Text("Aguardando transmissão...", color = Color.White)
                    }
                    is ConnectionState.Connecting -> {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Text("Conectando...", color = Color.White)
                    }
                    is ConnectionState.Error -> {
                        Icon(
                            Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            (uiState.connectionState as ConnectionState.Error).message,
                            color = Color.Red
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Desconectado", color = Color.Gray)
                    }
                }
            }
        }

        // Back button (top-left overlay)
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("Sair")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
