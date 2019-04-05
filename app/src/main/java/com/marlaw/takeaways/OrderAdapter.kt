package com.marlaw.takeaways


import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

class OrderAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {



    override fun getItem(position: Int): Fragment? = when (position) {
        0 -> OrderFragment.newInstance()
        1 -> OrderFragment.newInstance()
        else -> null
    }

    override fun getPageTitle(position: Int): CharSequence = when (position) {
        0 -> "Order Menu"
        1 -> "Tab 2 Item"
        else -> ""
    }

    override fun getCount(): Int = 3
}