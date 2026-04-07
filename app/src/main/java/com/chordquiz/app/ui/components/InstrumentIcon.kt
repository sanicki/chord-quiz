package com.chordquiz.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chordquiz.app.R

@Composable
fun InstrumentIcon(
    instrumentId: String,
    modifier: Modifier = Modifier.size(48.dp, 64.dp)
) {
    val drawableRes = when (instrumentId) {
        "guitar_standard" -> R.drawable.ic_instrument_guitar
        "ukulele_soprano" -> R.drawable.ic_instrument_ukulele
        "bass_standard"   -> R.drawable.ic_instrument_bass
        "banjo_5string"   -> R.drawable.ic_instrument_banjo
        else              -> R.drawable.ic_instrument_guitar
    }
    Image(
        painter = painterResource(drawableRes),
        contentDescription = null,
        modifier = modifier
    )
}
