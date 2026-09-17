package com.example.productiondisplay.ui.slideshow.Production;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.productiondisplay.R;

import java.util.ArrayList;
import java.util.List;

public class ModuleAdapter extends RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder> {

    private List<String> modules = new ArrayList<>();
    private final OnModuleClickListener listener;

    // Interface for click callbacks
    public interface OnModuleClickListener {
        void onModuleClick(String moduleName);
    }

    public ModuleAdapter(OnModuleClickListener listener) {
        this.listener = listener;
    }

    // Update list of modules dynamically
    public void setModules(List<String> moduleList) {
        if (moduleList != null) {
            this.modules = moduleList;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate button item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.module_item, parent, false);
        return new ModuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModuleViewHolder holder, int position) {
        String moduleName = modules.get(position);
        holder.moduleButton.setText(moduleName);

        // Click listener
        holder.moduleButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onModuleClick(moduleName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }

    static class ModuleViewHolder extends RecyclerView.ViewHolder {
        Button moduleButton;

        ModuleViewHolder(@NonNull View itemView) {
            super(itemView);
            moduleButton = itemView.findViewById(R.id.moduleButton);
        }
    }
}
