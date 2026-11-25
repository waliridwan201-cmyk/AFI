package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LedgerCellBorder
import com.example.ui.theme.SlateBarBg
import com.example.ui.theme.SlateBarText
import com.example.ui.theme.SlateDivider
import com.example.ui.theme.SlateToolbarBg
import com.example.util.formatLedgerNumber

/**
 * Dark slate TopAppBar matching Reference Images 1 & 2.
 */
@Composable
fun SlateTopHeaderBar(
    title: String,
    onBack: () -> Unit,
    backTag: String,
    trailingAction: (@Composable RowScope.() -> Unit)? = null
) {
    Surface(
        color = SlateBarBg,
        contentColor = SlateBarText,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(backTag)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = SlateBarText
                )
            }
            Text(
                text = title,
                color = SlateBarText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
            if (trailingAction != null) {
                trailingAction()
            }
        }
    }
}

/**
 * 5-icon dark slate toolbar matching Reference Image 1:
 * [Party Picker] [Product/Cart] [Payment/Options Menu] [Share/Print] [Save]
 */
@Composable
fun PosSlateActionToolbar(
    tagPrefix: String,
    partyContentDescription: String,
    onSelectPartyClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onSharePrintClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = SlateDivider)
        Surface(
            color = SlateToolbarBg,
            contentColor = SlateBarText,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarIconAction(
                    icon = Icons.Default.Badge,
                    contentDescription = partyContentDescription,
                    tag = "${tagPrefix}_toolbar_party",
                    onClick = onSelectPartyClick
                )
                ToolbarIconAction(
                    icon = Icons.Default.ShoppingCart,
                    contentDescription = "Tambah Produk ke Daftar",
                    tag = "${tagPrefix}_toolbar_cart",
                    onClick = onAddProductClick
                )
                ToolbarIconAction(
                    icon = Icons.Default.Menu,
                    contentDescription = "Opsi Pembayaran & Kas",
                    tag = "${tagPrefix}_toolbar_menu",
                    onClick = onOptionsClick
                )
                ToolbarIconAction(
                    icon = Icons.Default.Share,
                    contentDescription = "Cetak / Bagikan Struk",
                    tag = "${tagPrefix}_toolbar_print",
                    onClick = onSharePrintClick
                )
                ToolbarIconAction(
                    icon = Icons.Default.Save,
                    contentDescription = "Simpan Transaksi",
                    tag = "${tagPrefix}_toolbar_save",
                    onClick = onSaveClick
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconAction(
    icon: ImageVector,
    contentDescription: String,
    tag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = SlateBarText,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Compact hairline key-value row matching Image 1 metadata rows (ID, Nomor, Tanggal, Customer/Supplier).
 */
@Composable
fun PosMetadataRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    tag: String? = null
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .then(if (tag != null) Modifier.testTag(tag) else Modifier)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = rowModifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color(0xFF6B7280)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                color = if (onClick != null) Color(0xFF1F2937) else Color(0xFF4B5563),
                fontWeight = if (onClick != null) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
        HorizontalDivider(thickness = 1.dp, color = LedgerCellBorder)
    }
}

/**
 * Table Header bar: NO | NAMA PRODUK | JML | NILAI matching Image 1.
 */
@Composable
fun PosLedgerTableHeader() {
    Surface(
        color = SlateToolbarBg,
        contentColor = SlateBarText,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NO",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateBarText,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(38.dp)
            )
            VerticalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.7f))
            Text(
                text = "NAMA PRODUK",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateBarText,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.7f))
            Text(
                text = "JML",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateBarText,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(58.dp)
            )
            VerticalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.7f))
            Text(
                text = "NILAI",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateBarText,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(104.dp)
            )
        }
    }
}

@Composable
fun PosLedgerTableRow(
    index: Int,
    productName: String,
    subtitle: String? = null,
    qtyText: String,
    amount: Double,
    subtitleColor: Color = Color(0xFF6B7280),
    onClick: () -> Unit,
    tag: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .heightIn(min = 42.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}",
                fontSize = 12.sp,
                color = Color(0xFF374151),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(38.dp)
                    .padding(vertical = 8.dp)
            )
            VerticalDivider(thickness = 1.dp, color = LedgerCellBorder, modifier = Modifier.fillMaxHeight())
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = productName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            VerticalDivider(thickness = 1.dp, color = LedgerCellBorder, modifier = Modifier.fillMaxHeight())
            Text(
                text = qtyText,
                fontSize = 12.sp,
                color = Color(0xFF111827),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(58.dp)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            )
            VerticalDivider(thickness = 1.dp, color = LedgerCellBorder, modifier = Modifier.fillMaxHeight())
            Text(
                text = formatLedgerNumber(amount),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .width(104.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
        HorizontalDivider(thickness = 1.dp, color = LedgerCellBorder)
    }
}

/**
 * Sticky Bottom Footer matching Image 1 (`TOTAL` left, numeric total right).
 */
@Composable
fun PosTotalFooterBar(
    totalAmount: Double,
    statusBadgeText: String? = null,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        color = SlateBarBg,
        contentColor = SlateBarText,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "TOTAL",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateBarText
                )
                if (!statusBadgeText.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = statusBadgeText,
                            fontSize = 11.sp,
                            color = SlateBarText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = formatLedgerNumber(totalAmount),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = SlateBarText
            )
        }
    }
}
