package com.example.simpleledger.domain.model

data class Category(
    val id: String,
    val label: String,
    val type: TransactionType,
)

object Categories {
    val expenseFood = Category("expense.food", "饮食", TransactionType.EXPENSE)
    val expenseTransport = Category("expense.transport", "交通", TransactionType.EXPENSE)
    val expenseShopping = Category("expense.shopping", "购物", TransactionType.EXPENSE)
    val expenseHousing = Category("expense.housing", "居住", TransactionType.EXPENSE)
    val expenseEntertainment = Category("expense.entertainment", "娱乐", TransactionType.EXPENSE)
    val expenseHealthcare = Category("expense.healthcare", "医疗", TransactionType.EXPENSE)
    val expenseOther = Category("expense.other", "其他", TransactionType.EXPENSE)

    // Keep the former category readable for existing databases and backups, but do not offer it for new entries.
    private val legacyExpenseEducation = Category("expense.education", "教育", TransactionType.EXPENSE)

    val incomeSalary = Category("income.salary", "工资", TransactionType.INCOME)
    val incomeBonus = Category("income.bonus", "奖金", TransactionType.INCOME)
    val incomeSideJob = Category("income.side_job", "兼职", TransactionType.INCOME)
    val incomeReimbursement = Category("income.reimbursement", "报销", TransactionType.INCOME)
    val incomeOther = Category("income.other", "其他", TransactionType.INCOME)

    private val all: List<Category> = listOf(
        expenseFood,
        expenseTransport,
        expenseShopping,
        expenseHousing,
        expenseEntertainment,
        expenseHealthcare,
        expenseOther,
        legacyExpenseEducation,
        incomeSalary,
        incomeBonus,
        incomeSideJob,
        incomeReimbursement,
        incomeOther,
    )

    private val byId = all.associateBy(Category::id)
    private val selectable = all.filterNot { it.id == legacyExpenseEducation.id }

    fun find(id: String): Category? = byId[id]

    fun forType(type: TransactionType): List<Category> = selectable.filter { it.type == type }
}
