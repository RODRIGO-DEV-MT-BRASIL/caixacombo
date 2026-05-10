package com.seucaixa.caixacombo.ui.components

import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Componente reutilizável para exibir imagem de produto.
 * Suporta URLs HTTP, data URIs (base64) e caminhos relativos do servidor.
 * O Coil não suporta data URIs nativamente, então base64 é decodificado manualmente.
 */
@Composable
fun ProdutoImagem(
    imagem: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    serverUrl: String = "",
    placeholderIcon: @Composable (() -> Unit)? = null
) {
    if (imagem == null || imagem.isBlank()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            if (placeholderIcon != null) {
                placeholderIcon()
            } else {
                Icon(
                    Icons.Default.Inventory,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    when {
        imagem.startsWith("data:image/") -> {
            // Base64 data URI - decodificar manualmente (Coil não suporta)
            val bitmap = remember(imagem) {
                try {
                    val base64Data = imagem.substringAfter("base64,")
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    android.util.Log.e("ProdutoImagem", "Erro ao decodificar base64: ${e.message}")
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale
                )
            } else {
                Box(modifier = modifier, contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        imagem.startsWith("http") -> {
            // URL completa - usar Coil
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imagem)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        else -> {
            // Caminho relativo - concatenar com URL do servidor
            val fullUrl = if (serverUrl.isNotEmpty()) "$serverUrl$imagem" else imagem
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fullUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
    }
}
