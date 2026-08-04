package com.walley.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.displayName

val InvestmentCategoryColors: Map<InvestmentCategory, Color> = mapOf(
    InvestmentCategory.STOCK to Color(0xFF1565C0),
    InvestmentCategory.ETF to Color(0xFF2E7D32),
    InvestmentCategory.BOND to Color(0xFF00838F),
    InvestmentCategory.PRECIOUS_METAL to Color(0xFFF9A825),
    InvestmentCategory.ENERGY_METAL to Color(0xFF6D4C41),
    InvestmentCategory.CRYPTO to Color(0xFF6A1B9A)
)

/** A small outlined chip labeling an investment's category — a lighter treatment than [EquityStatusBadge] since it's metadata, not a call to action. */
@Composable
fun InvestmentCategoryChip(category: InvestmentCategory, modifier: Modifier = Modifier) {
    val color = InvestmentCategoryColors.getValue(category)
    Text(
        text = category.displayName(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
