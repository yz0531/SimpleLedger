package com.example.simpleledger.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CategoryTest {
    @Test
    fun expensePickerUsesTheEightCurrentCategories() {
        assertEquals(
            listOf("饮食", "交通", "购物", "居住", "娱乐", "医疗", "旅行", "其他"),
            Categories.forType(TransactionType.EXPENSE).map(Category::label),
        )
    }

    @Test
    fun legacyEducationCategoryRemainsReadableForOldBackups() {
        assertNotNull(Categories.find("expense.education"))
    }
}
