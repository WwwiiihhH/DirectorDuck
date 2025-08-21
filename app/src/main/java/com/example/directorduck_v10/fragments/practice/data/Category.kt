package com.example.directorduck_v10.fragments.practice.data

data class Category(
    val id: Int,
    val categoryCode: Int,
    val categoryName: String,
    val description: String? = null
)

data class Subcategory(
    val id: Int,
    val categoryId: Int,
    val subcategoryCode: String,
    val subcategoryName: String,
    val description: String? = null
)

// 用于RecyclerView的列表项
sealed class PracticeItem {
    data class CategoryItem(
        val category: Category,
        val isExpanded: Boolean = false,
        val subcategories: List<Subcategory> = emptyList()
    ) : PracticeItem()
    
    data class SubcategoryItem(
        val subcategory: Subcategory
    ) : PracticeItem()
}