package com.example.pete.newsapp;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

public class PaginatorHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private Context context;

    // View references
    private ProgressBar paginator_progress;

    PaginatorHolder(Context context, View itemView) {
        super(itemView);

        this.context = context;

        getViewReferences(itemView);

        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

    }

    private void getViewReferences(View itemView) {
        this.paginator_progress = itemView.findViewById(R.id.paginator_progress);
    }
}
