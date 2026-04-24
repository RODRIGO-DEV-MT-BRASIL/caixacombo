package com.seucaixa.caixacombo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sistema de dimensões adaptativas para diferentes dispositivos:
 * - POS (SUNMI V1/V2): Telas grandes 14-15.6"
 * - Tablet: Telas médias 7-10"
 * - Mobile: Smartphones
 */

enum class DeviceType {
    POS,      // SUNMI V1/V2 - 14-15.6"
    TABLET,   // Tablets 7-10"
    MOBILE    // Smartphones
}

@Composable
fun rememberDeviceType(): DeviceType {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    return when {
        // SUNMI V1/V2: Telas grandes (mais de 1000dp de largura)
        screenWidth >= 1000.dp -> DeviceType.POS
        // Tablets/P2B: 400dp a 999dp (ajustado para incluir P2B)
        screenWidth >= 400.dp -> DeviceType.TABLET
        // Mobile: menos de 400dp
        else -> DeviceType.MOBILE
    }
}

/**
 * Dimensões do Checkout
 */
object CheckoutDimensions {
    @Composable
    fun carrinhoWidth(): Dp {
        return when (rememberDeviceType()) {
            DeviceType.POS -> 400.dp      // Carrinho maior no POS
            DeviceType.TABLET -> 320.dp
            DeviceType.MOBILE -> 0.dp     // Mobile usa bottom sheet
        }
    }
    
    @Composable
    fun produtoItemHeight(): Dp {
        return when (rememberDeviceType()) {
            DeviceType.POS -> 100.dp      // Itens grandes para touch fácil
            DeviceType.TABLET -> 80.dp
            DeviceType.MOBILE -> 64.dp
        }
    }
    
    @Composable
    fun botaoFinalizarHeight(): Dp {
        return when (rememberDeviceType()) {
            DeviceType.POS -> 80.dp
            DeviceType.TABLET -> 64.dp
            DeviceType.MOBILE -> 56.dp
        }
    }
    
    @Composable
    fun spacing(): Dp {
        return when (rememberDeviceType()) {
            DeviceType.POS -> 24.dp
            DeviceType.TABLET -> 16.dp
            DeviceType.MOBILE -> 8.dp
        }
    }
    
    @Composable
    fun padding(): Dp {
        return when (rememberDeviceType()) {
            DeviceType.POS -> 24.dp
            DeviceType.TABLET -> 16.dp
            DeviceType.MOBILE -> 12.dp
        }
    }
}

/**
 * Dimensões de Fontes
 */
object FontDimensions {
    @Composable
    fun precoGrande(): androidx.compose.ui.unit.TextUnit {
        return when (rememberDeviceType()) {
            DeviceType.POS -> 48.sp
            DeviceType.TABLET -> 36.sp
            DeviceType.MOBILE -> 28.sp
        }
    }
    
    @Composable
    fun precoMedio(): androidx.compose.ui.unit.TextUnit {
        return when (rememberDeviceType()) {
            DeviceType.POS -> 32.sp
            DeviceType.TABLET -> 24.sp
            DeviceType.MOBILE -> 20.sp
        }
    }
    
    @Composable
    fun tituloProduto(): androidx.compose.ui.unit.TextUnit {
        return when (rememberDeviceType()) {
            DeviceType.POS -> 24.sp
            DeviceType.TABLET -> 20.sp
            DeviceType.MOBILE -> 16.sp
        }
    }
    
    @Composable
    fun subtituloProduto(): androidx.compose.ui.unit.TextUnit {
        return when (rememberDeviceType()) {
            DeviceType.POS -> 18.sp
            DeviceType.TABLET -> 14.sp
            DeviceType.MOBILE -> 12.sp
        }
    }
    
    @Composable
    fun botaoTexto(): androidx.compose.ui.unit.TextUnit {
        return when (rememberDeviceType()) {
            DeviceType.POS -> 20.sp
            DeviceType.TABLET -> 16.sp
            DeviceType.MOBILE -> 14.sp
        }
    }
}
