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

    private final LayoutInflater mInflater;
    private ArrayList<SearchType> searchTypes;
    private ItemView<Child> mItemView = null;

    private int position = 0;


    public RecyclerExpandableAdapter(Context context, List<ParentObject> parentItemList) {
        super(context, parentItemList);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public ParentHolder onCreateParentViewHolder(ViewGroup viewGroup) {
        View view = mInflater.inflate(R.layout.expandable_parent, viewGroup, false);
        return new ParentHolder(view);
    }

    @Override
    public RecyclerItemViewHolder onCreateChildViewHolder(ViewGroup viewGroup) {
        //TODO-stefan R.layout dynamisch maken
        View view = mInflater.inflate(R.layout.list_item, viewGroup, false);

        HashMap<String, K> enginesViews = new HashMap<String, K>();
        for (int i = 0; i < searchTypes.size(); i++) {
            enginesViews.put(searchTypes.get(i).getModelClassName(), (K) searchTypes.get(i).getViewBuilder());
        }
        RecyclerItemViewHolder viewHolderInstance = new RecyclerItemViewHolder(view, this);
        viewHolderInstance.setItemViews(enginesViews);

        return viewHolderInstance;
    }

    @Override
    public void onBindParentViewHolder(ParentHolder parentHolder, int i, Object o) {
        ExpandableParentListItem crime = (ExpandableParentListItem) o;
        parentHolder.mCrimeTitleTextView.setText(crime.getTitle());
        parentHolder.mIcon.setImageResource(crime.getIcon());
        parentHolder.mItemCount.setText(crime.getItemCount());
    }

    @Override
    public void onBindChildViewHolder(final RecyclerItemViewHolder childHolder, int i, Object o) {
        String ClassType = o.getClass().getName().toLowerCase().trim().toString();
        String searchClassName = String.valueOf(ClassType.substring(ClassType.lastIndexOf('.') + 1)).toLowerCase().trim().toString();
        for(SearchType engine: searchTypes) {
            if(engine.getModelClassName().toLowerCase().trim().toString().contains(searchClassName)){
                Child childObject = (Child) o;
                childHolder.setItem(childObject);

                engine.getViewBuilder().bindView(childHolder, childObject);
            }
        }
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

    public <T extends Item> void setChildItems(String ClassType, List<T> items){
        for(ParentObject parent: mParentItemList){
            ExpandableParentListItem ParentItem = (ExpandableParentListItem) parent;
            String loopClass= ParentItem.getItemClassName();

            String searchClassName = String.valueOf(ClassType.substring(ClassType.lastIndexOf('.') + 1)).toLowerCase().trim().toString();
            String currentClassName = String.valueOf(loopClass.substring(loopClass.lastIndexOf('.') + 1)).toLowerCase().trim().toString();

            if(currentClassName.contains(searchClassName)){
                Log.d("check", "Dit is goed");

                ParentItem.setItemCount(items.size());
                ArrayList<Object> childList = new ArrayList<>();

                for(T childItem: items) {

                    childList.add(childItem);
                }

                ParentItem.setChildObjectList(childList);
            }
        }

        notifyDataSetChanged();
    }

    public void setSearchEngines(ArrayList<SearchType> st){
        searchTypes = st;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * TODO-stefan code ombouwen van ItemAdapter
     */
    public boolean doItemContext(MenuItem menuItem, int position) {
        Child Item = (Child) mItemList.get(position);
        String Classname = Item.getClass().getName().toString().toLowerCase().trim();
        String searchClassName = String.valueOf(Classname.substring(Classname.lastIndexOf('.') + 1)).toLowerCase().trim().toString();

        for(SearchType engine: searchTypes) {
            if(engine.getModelClassName().toLowerCase().trim().toString().contains(searchClassName)){
                return  engine.getViewBuilder().doItemContext(menuItem, position, (Child) mItemList.get(position));
            }
        }
        return false;
    }
}