package com.example.clientserver;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.clientserver.fragment1andItsRecyclerAdapter.fragment1;
import com.example.clientserver.fragment2.fragment2;

public class viewPagerAdapter extends FragmentPagerAdapter {
    private int tabCount;

    viewPagerAdapter(@NonNull FragmentManager fm, int numberOfTabs) {
        super(fm, numberOfTabs);
        this.tabCount = numberOfTabs;
    }

    @NonNull
    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 1:
                return new fragment2();
            /*case 2:
                return new fragment3();*/
            case 0:
            default:
                return new fragment1();
        }
    }

    @Override
    public int getCount() {
        return tabCount;
    }

}
