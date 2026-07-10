package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.isSpendingLimit
import java.math.BigDecimal
import java.math.RoundingMode

private val GoalGreen = Color(0xFF2E7D32)

/**
 * Shows the configured target % of disposable income for [section] (if any), plus how far
 * [actualPercent] sits from it:
 * - Fixed/Other costs (spending ceilings): over target warns, at/under target checks off.
 * - Savings/Investments (goals): any deviation from target, over or under, warns — since the
 *   percentages across every section have to add up, overshooting one still means another section
 *   got shortchanged, so it's just as worth flagging as falling short of the goal.
 */
@Composable
fun CategoryTargetIndicator(
    section: BudgetSectionType,
    actualPercent: BigDecimal?,
    targetPercent: BigDecimal?,
    modifier: Modifier = Modifier
) {
    if (targetPercent == null) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Target: ${formatPercent(targetPercent)}% of disposable income",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actualPercent != null) {
            val diff = actualPercent.setScale(1, RoundingMode.HALF_UP) - targetPercent.setScale(1, RoundingMode.HALF_UP)
            val isOver = diff.signum() > 0
            val isUnder = diff.signum() < 0
            val isWarning = if (section.isSpendingLimit) isOver else (isOver || isUnder)
            val message = when {
                !isOver && !isUnder -> "On target"
                isOver -> "${formatPercent(diff)}% over target allocation"
                else -> "${formatPercent(diff.abs())}% below target allocation"
            }
            val color = if (isWarning) MaterialTheme.colorScheme.error else GoalGreen

            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isWarning) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(message, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }
}

/** Renders a percentage without a trailing ".0" for whole numbers, otherwise with 1 decimal place. */
private fun formatPercent(value: BigDecimal): String {
    val rounded = value.setScale(1, RoundingMode.HALF_UP)
    val whole = rounded.setScale(0, RoundingMode.HALF_UP)
    return if (rounded.compareTo(whole) == 0) whole.toPlainString() else rounded.toPlainString()
}
