package uk.org.ngo.squeezer.framework;

import uk.org.ngo.squeezer.framework.TouchHelpers.ItemTouchHelperViewHolder;
import uk.org.ngo.squeezer.framework.expandable.RecyclerItemViewHolder;

import android.graphics.Canvas;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;

import uk.org.ngo.squeezer.framework.expandable.RecyclerItemViewHolder;
import uk.org.ngo.squeezer.itemlist.PlaylistSongsActivity;
import uk.org.ngo.squeezer.model.Playlist;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.service.ISqueezeService;

/**
 * Created by Stefan on 16-2-2016.
 */
public class SwipeItemTouchHelper extends ItemTouchHelper.Callback {

    public static final float ALPHA_FULL = 1.0f;

    private ItemView<?> itemView;

    private BaseListActivity<?> activity;

    private final recyclerViewListAdapter mAdapter;

    private Playlist playlist = null;


    public <T extends Item> SwipeItemTouchHelper(recyclerViewListAdapter adapter, BaseListActivity<T> tBaseListActivity, ItemView<?> iv) {
        mAdapter = adapter;
        activity = tBaseListActivity;
        itemView = iv;
    }

    public <T extends Item> SwipeItemTouchHelper(recyclerViewListAdapter adapter, BaseListActivity<T> tBaseListActivity, ItemView<?> iv, Playlist pl) {
        mAdapter = adapter;
        activity = tBaseListActivity;
        itemView = iv;
        playlist = pl;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        Log.d("touchHelper", "SwipeItemTouchHelper -> getMovementFlags 29");

        final int dragFlags = itemView.getDragDirections();
        final int swipeFlags = itemView.getSwipeDirections();

        return makeMovementFlags(dragFlags, swipeFlags);


//        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
//            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
//            final int swipeFlags = 0;
//            return makeMovementFlags(dragFlags, swipeFlags);
//        } else {
//            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
//            final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
//            return makeMovementFlags(dragFlags, swipeFlags);
//        }
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        if (viewHolder.getItemViewType() != target.getItemViewType()) {
            return false;
        }

        RecyclerItemViewHolder vh = (RecyclerItemViewHolder) viewHolder;
        RecyclerItemViewHolder targetvh = (RecyclerItemViewHolder) target;

        // Notify the adapter of the move
        mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());

        ISqueezeService service = activity.getService();
        if (service == null) {
            return false;
        }

        if (playlist == null) {
            Log.d("touchHelper", "playlist null");
            service.playlistMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        } else {
            Log.d("touchHelper", "playlist != null");
            service.playlistsMove(playlist, viewHolder.getAdapterPosition(), target.getAdapterPosition());
        }

        return true;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        //TODO-stefan controleren via de View class of swipe aan staat en de richting
        return itemView.isSwipable();
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        Log.d("touchHelper", "SwipeItemTouchHelper -> onSwiped 109");
        mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        Log.d("touchHelper", "SwipeItemTouchHelper -> onChildDraw 115");
        //TODO-stefan controleren welke richting voor welke actie
        /**
         * http://stackoverflow.com/questions/30850494/confirmation-and-undo-removing-in-recyclerview
         * http://stackoverflow.com/questions/30549703/how-to-use-swipedismissbehavior-ondismisslistener-on-recyclerview/32071882
         * https://github.com/iPaulPro/Android-ItemTouchHelper-Demo/issues/11
         * http://blog.grafixartist.com/swipe-to-dismiss-recyclerview-itemtouchhelper/
         */


        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Fade out the view as it is swiped out of the parent's bounds
            final float alpha = ALPHA_FULL - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
            viewHolder.itemView.setAlpha(alpha);
            viewHolder.itemView.setTranslationX(dX);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {

        Log.d("touchHelper", "SwipeItemTouchHelper -> onSelectedChanged 81");
        Log.d("touchHelper", String.valueOf(actionState));
        // We only want the active item to change
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder instanceof ItemTouchHelperViewHolder) {
                // Let the view holder know that this item is being moved or dragged
                ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
                itemViewHolder.onItemSelected();
            }
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        Log.d("touchHelper", "SwipeItemTouchHelper -> clearView 96");

        viewHolder.itemView.setAlpha(ALPHA_FULL);

        if (viewHolder instanceof ItemTouchHelperViewHolder) {
            // Tell the view holder it's time to restore the idle state
            ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
            itemViewHolder.onItemClear();
        }
    }

    public void setPlaylist(Playlist pl){
        playlist = pl;
    }
}