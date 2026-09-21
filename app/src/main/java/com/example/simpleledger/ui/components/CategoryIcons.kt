package com.example.simpleledger.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DirectionsSubway
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryIcon(categoryId: String): ImageVector = when (categoryId) {
    "expense.food" -> Icons.Rounded.Restaurant
    "expense.transport" -> Icons.Rounded.DirectionsSubway
    "expense.shopping" -> Icons.Rounded.ShoppingBag
    "expense.housing" -> Icons.Rounded.Home
    "expense.entertainment" -> Icons.Rounded.Movie
    "expense.healthcare" -> Icons.Rounded.HealthAndSafety
    "expense.education" -> Icons.Rounded.School
    "income.salary" -> Icons.Rounded.Work
    "income.bonus" -> Icons.Rounded.CardGiftcard
    "income.side_job" -> Icons.Rounded.Storefront
    "income.reimbursement" -> Icons.AutoMirrored.Rounded.ReceiptLong
    "income.other" -> Icons.Rounded.Payments
    else -> Icons.Rounded.MoreHoriz
}
