package com.example.clientserver.fragment2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clientserver.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class fragment2 extends Fragment {

    public fragment2() {
        System.out.println("inside fragment2 constructor");
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_2, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager lm = new GridLayoutManager(requireActivity(), 2);
        RecyclerAdapter adapter = new RecyclerAdapter(this.getContext());

        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);
    }
}
