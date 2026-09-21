package com.example.simpleledger.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.TransactionType

@Composable
fun CategoryPicker(
    type: TransactionType,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = remember(type) { Categories.forType(type) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(3).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowCategories.forEach { category ->
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = category.id == selectedId,
                        onClick = { onSelected(category.id) },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = categoryIcon(category.id),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = category.label,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 5.dp),
                                )
                            }
                        },
                    )
                }
                repeat(3 - rowCategories.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
