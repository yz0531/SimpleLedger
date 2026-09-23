package com.example.simpleledger.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoConfirmDatePickerDialog(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMillis = runCatching {
        LocalDate.parse(initialDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }.getOrNull()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = null,
        initialDisplayedMonthMillis = initialMillis,
    )

    LaunchedEffect(state.selectedDateMillis) {
        state.selectedDateMillis?.let { millis ->
            onDateSelected(
                Instant.ofEpochMilli(millis)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .toString(),
            )
        }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    ) {
        DatePicker(state = state)
    }
}
