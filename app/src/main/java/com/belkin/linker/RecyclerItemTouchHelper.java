package com.belkin.linker;

import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerItemTouchHelper extends ItemTouchHelper.SimpleCallback {

    private final static String CLASS_LOG_TAG = "RecyclerItemTouchHelper";

    private RecyclerItemTouchHelperListener listener;

    RecyclerItemTouchHelper(int dragDirs, int swipeDirs, RecyclerItemTouchHelperListener listener) {
        super(dragDirs, swipeDirs);
        this.listener = listener;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return true;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (viewHolder != null) {
            final View foregroundView = ((LinkToListItemAdapter.ViewHolder) viewHolder).viewForeground;
            Log.d(CLASS_LOG_TAG, "onSelectedChanged");
            getDefaultUIUtil().onSelected(foregroundView);
        }
    }

    @Override
    public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
        final View foregroundView = ((LinkToListItemAdapter.ViewHolder) viewHolder).viewForeground;
        Log.d(CLASS_LOG_TAG, "onChildDrawOver");
        getDefaultUIUtil().onDrawOver(c, recyclerView, foregroundView, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        final View foregroundView = ((LinkToListItemAdapter.ViewHolder) viewHolder).viewForeground;
        Log.d(CLASS_LOG_TAG, "clearView");
        getDefaultUIUtil().clearView(foregroundView);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        final View foregroundView = ((LinkToListItemAdapter.ViewHolder) viewHolder).viewForeground;
        final View backgroundDeleteView = ((LinkToListItemAdapter.ViewHolder) viewHolder).viewBackgroundDelete;
        final View backgroundShareView = ((LinkToListItemAdapter.ViewHolder) viewHolder).viewBackgroundShare;
        if (dX < 0) {
            backgroundDeleteView.setVisibility(View.VISIBLE);
            backgroundShareView.setVisibility(View.INVISIBLE);
        } else {
            backgroundDeleteView.setVisibility(View.INVISIBLE);
            backgroundShareView.setVisibility(View.VISIBLE);
        }
        Log.d(CLASS_LOG_TAG, "onChildDraw");
        getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        Log.d(CLASS_LOG_TAG, "onSwiped");
        listener.onSwiped(viewHolder, direction, viewHolder.getAdapterPosition());
    }

    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    public interface RecyclerItemTouchHelperListener {
        void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position);
        void onClicked(RecyclerView.ViewHolder viewHolder);
    }
}