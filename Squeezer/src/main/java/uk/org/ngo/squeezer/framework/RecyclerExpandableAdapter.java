package uk.org.ngo.squeezer.framework;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bignerdranch.expandablerecyclerview.Adapter.ExpandableRecyclerAdapter;
import com.bignerdranch.expandablerecyclerview.Model.ParentObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.expandable.RecyclerItemViewHolder;
import uk.org.ngo.squeezer.framework.expandable.ParentHolder;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.ExpandableParentListItem;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.SearchType;
import uk.org.ngo.squeezer.model.Song;

/**
 * Created by Stefan on 5-1-2016.
 */
public class RecyclerExpandableAdapter<Child extends Item, K extends BaseItemView> extends ExpandableRecyclerAdapter<ParentHolder, RecyclerItemViewHolder> {

    protected final LayoutInflater mInflater;
    protected ItemView<Child> mItemView = null;

    private int position = 0;

    public RecyclerExpandableAdapter(Context context, List<ParentObject> parentItemList) {
        super(context, parentItemList);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public ParentHolder onCreateParentViewHolder(ViewGroup viewGroup) {
        //TODO-stefan R.layout dynamisch maken
        View view = mInflater.inflate(R.layout.expandable_parent, viewGroup, false);
        return new ParentHolder(view);
    }

    @Override
    public RecyclerItemViewHolder onCreateChildViewHolder(ViewGroup viewGroup) {
        //TODO-stefan R.layout dynamisch maken
        View view = mInflater.inflate(R.layout.list_item, viewGroup, false);
        RecyclerItemViewHolder viewHolderInstance = new RecyclerItemViewHolder(view, this);
        return viewHolderInstance;
    }

    @Override
    public void onBindParentViewHolder(ParentHolder parentHolder, int i, Object o) {
        ExpandableParentListItem crime = (ExpandableParentListItem) o;
        parentHolder.mCrimeTitleTextView.setText(crime.getTitle());
        parentHolder.mIcon.setImageResource(crime.getIcon());

        int size = crime.getChildObjectList().size();
        parentHolder.mItemCount.setText(String.valueOf(size));
    }

    @Override
    public void onBindChildViewHolder(final RecyclerItemViewHolder childHolder, int i, Object o) {
        //TODO-stefan een default implementatie maken
        childHolder.setPosition(i);

        childHolder.getItemView().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setPosition(childHolder.getPosition());
                return false;
            }
        });
    }

    public List<ParentObject> getParentItems(){
        return mParentItemList;
    }

    public List<Object> getItemList(){
        return mItemList;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean doItemContext(MenuItem menuItem, int position) {
        return false;
    }

    public void clear() {
        mItemList.clear();
        mParentItemList.clear();
        notifyDataSetChanged();
    }

    public void setItemView(ItemView<Child> view){
        mItemView = view;
    }
}