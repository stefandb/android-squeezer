package uk.org.ngo.squeezer.framework;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.util.CompoundButtonWrapper;

//import com.bignerdranch.android.materialcoordination.R;
//import com.bignerdranch.android.materialcoordination.model.SimpleItem;
//import com.bignerdranch.android.materialcoordination.util.FileLoader;
//import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stefan on 7-11-2015.
 */
public class recyclerViewListAdapter<T extends Item>  extends RecyclerView.Adapter<recyclerViewListAdapter.SimpleHolder> {

    /**
     * View logic for this adapter
     */
    private ItemView<T> mItemView;

    /**
     * List of items, possibly headed with an empty item.
     * <p>
     * As the items are received from SqueezeServer they will be inserted in the list.
     */
    private int count;

    private final SparseArray<T[]> pages = new SparseArray<T[]>();

    /**
     * This is set if the list shall start with an empty item.
     */
    protected boolean mEmptyItem = true;

    /**
     * Text to display before the items are received from SqueezeServer
     */
    protected String loadingText;

    /**
     * Number of elements to by fetched at a time
     */
    private int pageSize;


    private List<T> mItems = new ArrayList<>();;


    private boolean mShowIcon;
    private OnItemClickListener mOnItemClickListener;


    public interface OnItemClickListener {
        void onItemClicked(Item simpleItem);
    }

//    public recyclerViewListAdapter(Context context) {
////        this(FileLoader.loadSampleItems(context), null, true);
//    }

    public recyclerViewListAdapter(ItemView<T> itemView) {
        mItemView = itemView;
        mEmptyItem = true;
        loadingText = itemView.getActivity().getString(R.string.loading_text);
        pageSize = itemView.getActivity().getResources().getInteger(R.integer.PageSize);
        pages.clear();
    }

    public recyclerViewListAdapter(Context context, ItemView<T> itemView) {
        mItemView = itemView;
        mEmptyItem = true;
        loadingText = itemView.getActivity().getString(R.string.loading_text);
        pageSize = itemView.getActivity().getResources().getInteger(R.integer.PageSize);
        pages.clear();

//        this(FileLoader.loadSampleItems(context), null, true);
    }

    public recyclerViewListAdapter(List<T> items, OnItemClickListener onItemClickListener) {
        this(items, onItemClickListener, false);
    }

    private recyclerViewListAdapter(List<T> items, OnItemClickListener onItemClickListener, boolean showIcon) {
        mItems = items;
        mOnItemClickListener = onItemClickListener;
        mShowIcon = showIcon;
    }

    public void additems(int count, int start, List<T> items){
        for (int i=0; i<items.size(); i++) {
            int position = 0;
            if(mItems != null){
                position = mItems.size() + 1;
            }
            mItems.add(mItems.size(), items.get(i));
        }
        notifyDataSetChanged();
    }

    @Override
    public SimpleHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        final SimpleHolder viewHolder = new SimpleHolder(inflater.inflate(R.layout.list_item, viewGroup, false));
//        viewHolder.getItemView().setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mOnItemClickListener == null) {
//                    return;
//                }
//
//                mOnItemClickListener.onItemClicked(mItems.get(viewHolder.getAdapterPosition()));
//            }
//        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final SimpleHolder viewHolder, int position) {

        Log.d("item-debug", "position " + String.valueOf(position));
        Log.d("item-debug", "mItems " + mItems.get(position).toString());


        T item = mItems.get(position);
//        Log.d("item-debug", "item " + String.valueOf(item));
//        if (!mItems.equals(null)) {
            mItemView.getAdapterView(viewHolder, position, item);
//        }
//
//        mItemView.getAdapterView(viewHolder, (position == 0 && mEmptyItem ? "" : loadingText));


//        Item item = mItems.get(i);
//        viewHolder.getTextView().setText(item.getName());
//
//        if (!mShowIcon) {
//            viewHolder.getImageView().setVisibility(View.GONE);
//        }

//        Context context = viewHolder.itemView.getContext();
//        Picasso.with(context)
//                .load(item.getIconUrl())
//                .into(viewHolder.getImageView());
    }

    @Override
    public int getItemCount() {
        if(mItems != null){
            return mItems.size();
        }
       return 0;
    }

    /**
     * Dit op een zelfde techniek als de itemview logica met het override van verschillende functies in de klas.
     * Waardoor dit opeens heel dynamisch word. en anders geimplementeerd kan worden. (hoop ik)
     */
    public static class SimpleHolder extends RecyclerView.ViewHolder {

        private TextView text1;
        private TextView text2;
        private ImageView icon;
        private SeekBar volumeBar;

        private View itemView;
        private TextView volumeValue;

        int position;
        public boolean is24HourFormat;
        String timeFormat;
        String am;
        String pm;
        Alarm alarm;
        TextView time;
        TextView amPm;
        CompoundButtonWrapper enabled;
        CompoundButtonWrapper repeat;
        ImageView delete;
        Spinner playlist;
        LinearLayout dowHolder;
        final TextView[] dowTexts = new TextView[7];

        public SimpleHolder(){
            super(null);
        }

        public SimpleHolder(View itemView) {
            super(itemView);

            text1 = (TextView) itemView.findViewById(R.id.text1);
            text2 = (TextView) itemView.findViewById(R.id.text2);
            icon = (ImageView) itemView.findViewById(R.id.icon);
        }

        public ImageView getIcon() {
            return icon;
        }

        public TextView getText1() {
            return text1;
        }

        public TextView getText2() {
            return text2;
        }

        //TODO-stefan als tijdelijke oplossing hier alle noldijke velden van de templates zetten

        public SeekBar getVolumeBar() {
            return volumeBar;
        }

        public void setVolumeBar(int volumeBar) {
            this.volumeBar = (SeekBar) itemView.findViewById(volumeBar);
        }

        public TextView getVolumeValue() {
            return volumeValue;
        }

        public void setVolumeValue(int volumeValue) {
            this.volumeValue = (TextView) itemView.findViewById(volumeValue);
        }

        public View getItemView() {
            return itemView;
        }

        public boolean is24HourFormat() {
            return is24HourFormat;
        }

        public void setIs24HourFormat(boolean is24HourFormat) {
            this.is24HourFormat = is24HourFormat;
        }

        public String getTimeFormat() {
            return timeFormat;
        }

        public void setTimeFormat(String timeFormat) {
            this.timeFormat = timeFormat;
        }

        public String getAm() {
            return am;
        }

        public void setAm(String am) {
            this.am = am;
        }

        public String getPm() {
            return pm;
        }

        public void setPm(String pm) {
            this.pm = pm;
        }

        public Alarm getAlarm() {
            return alarm;
        }

        public void setAlarm(Alarm alarm) {
            this.alarm = alarm;
        }

        public TextView getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = (TextView) itemView.findViewById(time);
        }

        public TextView getAmPm() {
            return amPm;
        }

        public void setAmPm(int amPm) {
            this.amPm = (TextView) itemView.findViewById(amPm);
        }

        public CompoundButtonWrapper getEnabled() {
            return enabled;
        }

        public void setEnabled(CompoundButtonWrapper enabled) {
            this.enabled = enabled;
        }

        public CompoundButtonWrapper getRepeat() {
            return repeat;
        }

        public void setRepeat(CompoundButtonWrapper repeat) {
            this.repeat = repeat;
        }

        public ImageView getDelete() {
            return delete;
        }

        public void setDelete(int delete) {
            this.delete = (ImageView) itemView.findViewById(delete);
        }

        public Spinner getPlaylist() {
            return playlist;
        }

        public void setPlaylist(int playlist) {
            this.playlist = (Spinner) itemView.findViewById(playlist);
        }

        public LinearLayout getDowHolder() {
            return dowHolder;
        }

        public void setDowHolder(int dowHolder) {
            this.dowHolder = (LinearLayout) itemView.findViewById(dowHolder);
        }

        public TextView[] getDowTexts() {
            return dowTexts;
        }

        public void setDowTexts(int position, TextView text) {
            this.dowTexts[position] = text;
        }



        public void setDowHolder(LinearLayout dowHolder) {
            this.dowHolder = dowHolder;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getPositionInt(){
            return position;
        }
    }





    /**
     * TODO-stefan code ombouwen van ItemAdapter
     */
    public boolean doItemContext(MenuItem menuItem, int position) {
        return mItemView.doItemContext(menuItem, position, getItem(position));
    }

    public boolean doItemContext(MenuItem menuItem) {
        return mItemView.doItemContext(menuItem);
    }

    public T getItem(int position) {
        T item = getPage(position)[position % pageSize];
        if (item == null) {
            if (mEmptyItem) {
                position--;
            }
            getActivity().maybeOrderPage(pageNumber(position) * pageSize);
        }
        return item;
    }

    public ItemListActivity getActivity() {
        return mItemView.getActivity();
    }

    /**
     * Update the contents of the items in this list.
     * <p>
     * The size of the list of items is automatically adjusted if necessary, to obey the given
     * parameters.
     *
     * @param count Number of items as reported by SqueezeServer.
     * @param start The start position of items in this update.
     * @param items New items to insert in the main list
     */
    public void update(int count, int start, List<T> items) {
        int offset = (mEmptyItem ? 1 : 0);
        count += offset;
        start += offset;
        if (count == 0 || count != getCount()) {
            this.count = count;
            onCountUpdated();
        }
        setItems(start, items);

        notifyDataSetChanged();
    }

    private void setItems(int start, List<T> items) {
        T[] page = getPage(start);
        int offset = start % pageSize;
        start--;
        for (T item : items) {
            if (offset >= pageSize) {
//                start += offset;
//                page = getPage(start);
//                offset = 0;
            }
            mItems.add(start, item); //  page[offset++] = item;
            start++;
        }

        notifyDataSetChanged();
    }

    private T[] getPage(int position) {
        int pageNumber = pageNumber(position);
        T[] page = pages.get(pageNumber);
        if (page == null) {
            pages.put(pageNumber, page = arrayInstance(pageSize));
        }
        return page;
    }

    private int pageNumber(int position) {
        return position / pageSize;
    }

    public int getCount() {
        return mItems.size();
    }

    /**
     * Removes all items from this adapter leaving it empty.
     */
    public void clear() {
        this.count = (mEmptyItem ? 1 : 0);
        pages.clear();
    }

    public ItemView<T> getItemView() {
        return mItemView;
    }

    public void setItemView(ItemView<T> itemView) {
        mItemView = itemView;
    }

    /**
     * Called when the number of items in the list changes. The default implementation is empty.
     */
    protected void onCountUpdated() {
    }

    protected T[] arrayInstance(int size) {
        return mItemView.getCreator().newArray(size);
    }


    /**
     * TODO-stefan ombouwen naar swipe to delete
     * Remove the item at the specified position, update the count and notify the change.
     */
    public void removeItem(int position) {
        T[] page = getPage(position);
        int offset = position % pageSize;
        while (position++ <= count) {
            if (offset == pageSize - 1) {
                T[] nextPage = getPage(position);
                page[offset] = nextPage[0];
                offset = 0;
                page = nextPage;
            } else {
                page[offset] = page[offset+1];
                offset++;
            }
        }

        count--;
        onCountUpdated();
        notifyDataSetChanged();
    }

    /**
     * TODO-stefan generieke functie van maken
     * @param position
     * @param item
     */
    public void insertItem(int position, T item) {
        int n = count;
        T[] page = getPage(n);
        int offset = n % pageSize;
        while (n-- > position) {
            if (offset == 0) {
                T[] nextPage = getPage(n);
                offset = pageSize - 1;
                page[0] = nextPage[offset];
                page = nextPage;
            } else {
                page[offset] = page[offset-1];
                offset--;
            }
        }
        mItems.add(position, item); // = item;

        count++;
        onCountUpdated();
        notifyDataSetChanged();
    }

    public String getQuantityString(int size) {
        return mItemView.getQuantityString(size);
    }

    public void setItem(int position, T item) {
        mItems.add(position, item);

//        getPage(position)[position % pageSize] = item;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        return convertView;
//        T item = getItem(position);
//        if (item != null) {
//            return mItemView.getAdapterView(convertView, position, item);
//        }

//        return mItemView.getAdapterView(convertView, (position == 0 && mEmptyItem ? "" : loadingText));
    }

    public void onItemSelected(int position) {
        T item = getItem(position);
        if (item != null && item.getId() != null) {
            mItemView.onItemSelected(position, item);
        }
    }

}