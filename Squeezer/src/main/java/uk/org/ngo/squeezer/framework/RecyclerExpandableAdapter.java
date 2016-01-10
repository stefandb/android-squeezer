package uk.org.ngo.squeezer.framework;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bignerdranch.expandablerecyclerview.Adapter.ExpandableRecyclerAdapter;
import com.bignerdranch.expandablerecyclerview.Model.ParentObject;

import java.util.ArrayList;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.expandable.RecyclerItemViewHolder;
import uk.org.ngo.squeezer.framework.expandable.ParentHolder;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.ExpandableParentListItem;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.Song;

/**
 * Created by Stefan on 5-1-2016.
 */
public class RecyclerExpandableAdapter<Child extends Item> extends ExpandableRecyclerAdapter<ParentHolder, RecyclerItemViewHolder> {

    private final LayoutInflater mInflater;

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
        View view = mInflater.inflate(R.layout.list_item, viewGroup, false);
        return new RecyclerItemViewHolder(view);
    }

    @Override
    public void onBindParentViewHolder(ParentHolder parentHolder, int i, Object o) {
        ExpandableParentListItem crime = (ExpandableParentListItem) o;
        parentHolder.mCrimeTitleTextView.setText(crime.getTitle());
        parentHolder.mIcon.setImageResource(crime.getIcon());
        parentHolder.mItemCount.setText(crime.getItemCount());
    }

    @Override
    public void onBindChildViewHolder(RecyclerItemViewHolder childHolder, int i, Object o) {
        Child childObject = null;
        if(o instanceof Song){
            childObject = (Child) o;
        }else if(o instanceof Album){
            childObject = (Child) o;
        }else if(o instanceof Artist){
            childObject = (Child) o;
        }else if(o instanceof Genre){
            childObject = (Child) o;
        }else{
            new Exception(o.getClass().toString() + " is a not coorect search type class");
        }

//        childHolder.text1.setText(childObject.getText1());
//        childHolder.text2.setText(childObject.getText2());
//        childHolder.icon.setImageResource(childObject.getImage());
    }

    public List<ParentObject> getParentItems(){
        return mParentItemList;
    }

    public <T extends Item> void setChildItems(String ClassType, List<T> items){
        for(ParentObject parent: mParentItemList){
            ExpandableParentListItem ParentItem = (ExpandableParentListItem) parent;
            String loopClass= ParentItem.getItemClassName();

            String searchClassName = String.valueOf(ClassType.substring(ClassType.lastIndexOf('.') + 1)).toLowerCase().trim().toString();
            String currentClassName = String.valueOf(loopClass.substring(loopClass.lastIndexOf('.') + 1)).toLowerCase().trim().toString();

            Log.d("check", "nieuw");
            Log.d("check", searchClassName);
            Log.d("check", currentClassName);
            Log.d("check", String.valueOf(currentClassName.contains(searchClassName)));
            Log.d("check", "eind");


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

    public void setViewHolderInstance(){

    }
}