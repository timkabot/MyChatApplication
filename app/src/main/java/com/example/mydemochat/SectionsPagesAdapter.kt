package com.example.mydemochat

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class SectionsPagesAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {
        return when(position){
            0 -> RequestsFragment()
            1 -> ChatsFragment()
            2 -> FriendsFragment()
            else -> Fragment()
        }
    }

    override fun  getPageTitle(position : Int) : CharSequence{
        return when (position) {
            0 -> "REQUESTS"
            1 -> "CHATS"
            2 -> "FRIENDS"
            else -> ""
        }
    }

    override fun getCount() = 3

}
