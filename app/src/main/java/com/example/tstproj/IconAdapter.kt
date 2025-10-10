package com.example.tstproj

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

class IconAdapter(private val context: Context, private val icons: List<Int>) : BaseAdapter() {

    override fun getCount(): Int = icons.size

    override fun getItem(position: Int): Any = icons[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView = if (convertView == null) {
            ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(100, 100)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(8, 8, 8, 8)
            }
        } else {
            convertView as ImageView
        }
        imageView.setImageResource(icons[position])
        return imageView
    }
}