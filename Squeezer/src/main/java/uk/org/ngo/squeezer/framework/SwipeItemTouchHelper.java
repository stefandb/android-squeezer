package uk.org.ngo.squeezer.framework;

import uk.org.ngo.squeezer.framework.expandable.RecyclerItemViewHolder;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import uk.org.ngo.squeezer.framework.expandable.RecyclerItemViewHolder;

/**
 * Created by Stefan on 16-2-2016.
 */
public class SwipeItemTouchHelper  extends ItemTouchHelper.Callback {

    private final recyclerViewListAdapter mAdapter;

    public SwipeItemTouchHelper(recyclerViewListAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        mAdapter.onItemMove(viewHolder.getAdapterPosition(), viewHolder.getAdapterPosition());
        return true;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        RecyclerItemViewHolder vh = (RecyclerItemViewHolder) viewHolder;
        mAdapter.onItemDismiss(vh.getAdapterPosition());
    }
}