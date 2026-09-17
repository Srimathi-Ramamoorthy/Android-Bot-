package com.example.productiondisplay.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.productiondisplay.R;
import com.example.productiondisplay.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private ImageView logoImage, bannerImage;
    private TextView titleText, subtitleText;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        logoImage = view.findViewById(R.id.logo_image);
        bannerImage = view.findViewById(R.id.logo_image1);
        titleText = view.findViewById(R.id.title_text);
        subtitleText = view.findViewById(R.id.title_text1);

        // Optional: Set content or listeners here
        titleText.setText("Welcome to CRI PUMPS");
        subtitleText.setText("Choose menu to use");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}