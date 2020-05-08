package com.belkin.linker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class LinkToListItemAdapter extends RecyclerView.Adapter<LinkToListItemAdapter.ViewHolder> {

    private LayoutInflater inflater;
    private List<Link> links;
    private RecyclerItemTouchHelper.RecyclerItemTouchHelperListener listener;
    private Context context;

    LinkToListItemAdapter(Context context, List<Link> links, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener listener) {
        this.links = links;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public LinkToListItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LinkToListItemAdapter.ViewHolder holder, int position) {
        Link link = links.get(position);
        String imageUrl = link.getImageUrl();
        if (imageUrl != null) {
            Picasso.with(context).load(imageUrl).into(holder.imageView);
            holder.imageView.setVisibility(View.VISIBLE);
        } else {
            holder.imageView.setVisibility(View.INVISIBLE);
        }
        holder.datetimeView.setText(link.getDatetime());
        holder.hostView.setText(link.getHost());
        holder.headerView.setText(link.getHeader());
    }

    @Override
    public int getItemCount() {
        return links.size();
    }

    void removeItem(int position) {
        links.remove(position);
        notifyItemRemoved(position);
        // NOTE: don't call notifyDataSetChanged()
    }

    void restoreItem(Link item, int position) {
        links.add(position, item);
        notifyItemInserted(position);
    }

    void animateLoading(int pos) {
        //todo:

    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView imageView;
        final TextView datetimeView, hostView, headerView;
        final ConstraintLayout viewBackgroundDelete;
        final ConstraintLayout viewBackgroundShare;
        final ConstraintLayout viewForeground;

        ViewHolder(View view) {
            super(view);
            imageView = (ImageView) view.findViewById(R.id.image);
            hostView = (TextView) view.findViewById(R.id.host);
            headerView = (TextView) view.findViewById(R.id.header);
            datetimeView = (TextView) view.findViewById(R.id.datetime);
            viewBackgroundDelete = (ConstraintLayout) view.findViewById(R.id.item_background_delete);
            viewBackgroundShare = (ConstraintLayout) view.findViewById(R.id.item_background_share);
            viewForeground = (ConstraintLayout) view.findViewById(R.id.item_foreground);
            viewForeground.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.onClicked(this);
        }
    }


}