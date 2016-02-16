package uk.org.ngo.squeezer.framework;

/**
 * Created by Stefan on 16-2-2016.
 */
public interface ItemTouchHelperAdapter {
    void onItemMove(int fromPosition, int toPosition);

    void onItemDismiss(int position);
}
