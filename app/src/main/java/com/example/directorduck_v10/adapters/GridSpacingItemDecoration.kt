package com.example.directorduck_v10.adapters

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacingH: Int,      // 横向间距(px)
    private val spacingV: Int,      // 纵向间距(px)  ← 你想加大就是改这个
    private val includeEdge: Boolean = true
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        val column = position % spanCount

        if (includeEdge) {
            outRect.left = spacingH - column * spacingH / spanCount
            outRect.right = (column + 1) * spacingH / spanCount

            outRect.top = if (position < spanCount) spacingV else spacingV / 2
            outRect.bottom = spacingV / 2
        } else {
            outRect.left = column * spacingH / spanCount
            outRect.right = spacingH - (column + 1) * spacingH / spanCount

            if (position >= spanCount) outRect.top = spacingV
        }
    }
}
