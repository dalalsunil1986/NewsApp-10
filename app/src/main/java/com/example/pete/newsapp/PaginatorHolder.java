package com.example.pete.newsapp;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

class PaginatorHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    PaginatorHolder(Context context, View itemView) {
        super(itemView);

        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        // Could add code here to retry loading if it fails
    }
}
