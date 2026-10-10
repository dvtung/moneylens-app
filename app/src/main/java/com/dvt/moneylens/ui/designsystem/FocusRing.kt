package com.dvt.moneylens.ui.designsystem

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors

fun Modifier.focusRing(
    active: Boolean = true,
    cornerRadius: Dp = 12.dp,
    strokeWidth: Dp = 2.dp
): Modifier = composed {
    if (!active) this
    else {
        val amber = LocalMoneyLensColors.current.focus
        this.border(
            width = strokeWidth,
            color = amber,
            shape = RoundedCornerShape(cornerRadius)
        )
    }
}
