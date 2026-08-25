package com.ganpati.vargani.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ganpati.vargani.R
import com.ganpati.vargani.core.theme.BadgeShape
import com.ganpati.vargani.core.theme.CardShape
import com.ganpati.vargani.core.theme.ErrorRed
import com.ganpati.vargani.core.theme.VarganiThemeExtras
import com.ganpati.vargani.core.utils.CurrencyUtils
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.domain.model.ExpenseCategory
import com.ganpati.vargani.domain.model.PaymentMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseCard(
    title: String,
    category: ExpenseCategory,
    amount: Double,
    paymentMode: PaymentMode,
    paidBy: String,
    dateEpochMillis: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val extendedColors = VarganiThemeExtras.extendedColors
    val paymentLabel = when (paymentMode) {
        PaymentMode.CASH -> stringResource(R.string.payment_cash)
        PaymentMode.UPI -> stringResource(R.string.payment_upi)
    }
    val paymentColor = when (paymentMode) {
        PaymentMode.CASH -> extendedColors.cash
        PaymentMode.UPI -> extendedColors.upi
    }
    val categoryLabel = categoryLabel(category)
    val initial = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(ErrorRed),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ErrorRed.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = CurrencyUtils.format(amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed,
                        )
                        Surface(shape = BadgeShape, color = paymentColor.copy(alpha = 0.14f)) {
                            Text(
                                text = paymentLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = paymentColor,
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = paidBy,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = DateTimeUtils.formatDate(dateEpochMillis),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun categoryLabel(category: ExpenseCategory): String = when (category) {
    ExpenseCategory.PUJA_ITEMS -> stringResource(R.string.expense_cat_puja)
    ExpenseCategory.DECORATION -> stringResource(R.string.expense_cat_decoration)
    ExpenseCategory.PRASAD -> stringResource(R.string.expense_cat_prasad)
    ExpenseCategory.SOUND_LIGHT -> stringResource(R.string.expense_cat_sound)
    ExpenseCategory.TRANSPORT -> stringResource(R.string.expense_cat_transport)
    ExpenseCategory.RENT -> stringResource(R.string.expense_cat_rent)
    ExpenseCategory.UTILITIES -> stringResource(R.string.expense_cat_utilities)
    ExpenseCategory.MISC -> stringResource(R.string.expense_cat_misc)
}
