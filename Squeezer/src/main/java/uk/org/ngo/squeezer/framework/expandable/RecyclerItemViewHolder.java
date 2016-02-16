package uk.org.ngo.squeezer.framework.expandable;

import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ViewHolder.ChildViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.framework.RecyclerExpandableAdapter;
import uk.org.ngo.squeezer.framework.recyclerViewListAdapter;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.util.CompoundButtonWrapper;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Dit op een zelfde techniek als de itemview logica met het override van verschillende functies in de klas.
 * Waardoor dit opeens heel dynamisch word. en anders geimplementeerd kan worden. (hoop ik)
 */
/**
 * Created by Stefan on 6-1-2016.
 */
public class RecyclerItemViewHolder<T extends Item, K extends BaseItemView> extends ChildViewHolder implements View.OnCreateContextMenuListener {

    private List<T> mItems = new ArrayList<>();
    /**
     * View logic for this adapter
     */
    private ItemView<T> mItemView = null;
    private HashMap<String, K> mItemViews = null;

    private RecyclerExpandableAdapter<?,?> mContextExpandableList = null;
    private RecyclerExpandableAdapter<?, ?> mContextList = null;

    public TextView text1;
    public TextView text2;
    public ImageView icon;

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
    Switch enabled;
    CompoundButtonWrapper repeat;
    ImageView delete;
    Spinner playlist;
    LinearLayout dowHolder;
    final TextView[] dowTexts = new TextView[7];
    private T mSingleItem;
    private recyclerViewListAdapter<T> adapter;

    private ImageView handleView;

    public RecyclerItemViewHolder(View v) {
        super(v);

        itemView = v;
        text1 = (TextView) v.findViewById(R.id.text1);
        text2 = (TextView) v.findViewById(R.id.text2);
        icon = (ImageView) v.findViewById(R.id.icon);
        handleView = (ImageView) itemView.findViewById(R.id.handle);

        itemView.setLongClickable(true);
        itemView.setOnCreateContextMenuListener(this);
    }

    public <Child extends Item, K extends BaseItemView> RecyclerItemViewHolder(View v, RecyclerExpandableAdapter<?, ?> childKRecyclerExpandableAdapter) {
        super(v);

        itemView = v;
        text1 = (TextView) v.findViewById(R.id.text1);
        text2 = (TextView) v.findViewById(R.id.text2);
        icon = (ImageView) v.findViewById(R.id.icon);
        handleView = (ImageView) itemView.findViewById(R.id.handle);

        itemView.setLongClickable(true);
        itemView.setOnCreateContextMenuListener(this);

        mContextList = childKRecyclerExpandableAdapter;
    }

    public RecyclerItemViewHolder(){
        super(null);
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

    //TODO-stefan sub class werkend maken
    public ChildViewHolder getHolder(){
        return mItemView.getViewHolderInstance(itemView);
    }


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

    public Switch getEnabled() {
        return this.enabled;
    }

    public Switch setEnabled(int enabled) {
        int a = itemView.getId();
        Object b = itemView.getTag();

        this.enabled = checkNotNull((Switch) itemView.findViewById(enabled),
                "setEnabled() did not return a view containing R.id.enabled");

        return this.enabled;
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




    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo adapterMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        final T selectedItem = (T) getItem(position);

        ItemView.ContextMenuInfo c;
        //int position, Item item, recyclerViewListAdapter<?> adapter, MenuInflater menuInflater
        if(adapter != null){
            c = new ItemView.ContextMenuInfo(position, selectedItem, adapter, getItemView(selectedItem).getActivity().getMenuInflater());
        }else{
            c = new ItemView.ContextMenuInfo(position, selectedItem, getItemView(selectedItem).getActivity().getMenuInflater());
        }

        if (selectedItem != null && selectedItem.getId() != null) {

            getItemView(selectedItem).onCreateContextMenu(menu, v, c);
        }
    }

    public boolean doItemContext(MenuItem menuItem, int position) {
        final T selectedItem = (T) getItem(position);
        return getItemView(selectedItem).doItemContext(menuItem, position, getItem(position));
    }

    public boolean doItemContext(MenuItem menuItem) {
        return mItemView.doItemContext(menuItem);
    }

    public void setItems(List<T> itemslist){
        mItems = itemslist;
    }

    public void setItem(T item){
        mSingleItem = item;
    }

    public T getItem(int position){
        if(mItems.size() == 0){
            return mSingleItem;
        }else{
            return mItems.get(position);
        }
    }

    private ItemView getItemView(T Item){
        if(mItemViews == null){
            return mItemView;
        }else {
//            Log.d("debug-class-name-itemview", Item.getClass().getSimpleName());
            return mItemViews.get(Item.getClass().getSimpleName());
        }
    }

    public void setItemView(ItemView<T> itemView){
        mItemView = itemView;
    }

    public void setItemViews(HashMap<String, K> enginesViews){
        mItemViews = enginesViews;
    }

    public void setAdapter(recyclerViewListAdapter<T> a) {
        adapter = a;
    }

    public ImageView getHandleView() {
        return handleView;
    }
}
