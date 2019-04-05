package com.marlaw.takeaways

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MenuItemAdapter(private val recordContext: Context) : BaseAdapter() {
    var records: List<MenuItem> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val currentOrder = mutableMapOf<String, Int>()

    val order: List<String>
        get() = currentOrder.filterValues { it > 0 }
            .map { orderItem -> List(orderItem.value) { orderItem.key } }
            .flatten()

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        val theView = if (view == null) {
            val recordInflator = recordContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            val theView = recordInflator.inflate(R.layout.menuitem, null)
            val newMenuItemViewHolder = MenuItemViewHolder(
                theView.findViewById(R.id.item_name),
                theView.findViewById(R.id.item_count),
                theView.findViewById(R.id.item_price)
            )
            val countAdapter = ArrayAdapter(
                recordContext,
                android.R.layout.simple_spinner_dropdown_item,
                IntRange(0, 10).toList().toTypedArray()
            )
            newMenuItemViewHolder.count.adapter = countAdapter
            theView.tag = newMenuItemViewHolder

            theView
        } else {
            view
        }

        val menuItemViewHolder = theView.tag as MenuItemViewHolder

        val menuItem = getItem(i)
        menuItemViewHolder.name.text = menuItem.name
        menuItemViewHolder.id = menuItem.id
        menuItemViewHolder.price.text = menuItem.price

        menuItemViewHolder.count.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentOrder.remove(menuItem.id)
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentOrder[menuItem.id] = position
            }
        }
        return theView
    }

    override fun getItem(i: Int) = records[i]

    override fun getItemId(i: Int) = 1L

    override fun getCount() = records.size
}

data class MenuItemViewHolder(
    val name: TextView,
    val count: Spinner,
    val price: TextView
) {
    var id: String? = null
}