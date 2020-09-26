package com.example.clientserver.fragment1andItsRecyclerAdapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clientserver.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class fragment1 extends Fragment {

    public fragment1() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //if view.findViewById not work then use ((MainActivity) requireActivity()).findViewById();
        //start chatActivity if user clicks on any of cardView/image
        RecyclerView rv = view.findViewById(R.id.rv);
        RecyclerView.LayoutManager lm = new GridLayoutManager(requireActivity(), 2);
        RecyclerAdapter adapter = new RecyclerAdapter(requireActivity());
        RecyclerView.ItemAnimator animator;
        rv.setLayoutManager(lm);

        animator = new DefaultItemAnimator();
        animator.setAddDuration(1000);
        animator.setRemoveDuration(1000);
        rv.setItemAnimator(animator);

        rv.setAdapter(adapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
