package uk.org.ngo.squeezer.framework;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bignerdranch.expandablerecyclerview.Adapter.ExpandableRecyclerAdapter;
import com.bignerdranch.expandablerecyclerview.Model.ParentObject;

import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.expandable.ChildHolder;
import uk.org.ngo.squeezer.framework.expandable.ParentHolder;
import uk.org.ngo.squeezer.model.ExpandableChildListItem;
import uk.org.ngo.squeezer.model.ExpandableParentListItem;

/**
 * Created by Stefan on 5-1-2016.
 */
public class RecyclerExpandableAdapter extends ExpandableRecyclerAdapter<ParentHolder, ChildHolder> {

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
    public ChildHolder onCreateChildViewHolder(ViewGroup viewGroup) {
        View view = mInflater.inflate(R.layout.list_item, viewGroup, false);
        return new ChildHolder(view);
    }

    @Override
    public void onBindParentViewHolder(ParentHolder parentHolder, int i, Object o) {
        ExpandableParentListItem crime = (ExpandableParentListItem) o;
        parentHolder.mCrimeTitleTextView.setText(crime.getTitle());
    }

    @Override
    public void onBindChildViewHolder(ChildHolder childHolder, int i, Object o) {
        ExpandableChildListItem crimeChild = (ExpandableChildListItem) o;
        childHolder.text1.setText(crimeChild.getText1());
        childHolder.text2.setText(crimeChild.getText2());
        childHolder.icon.setImageResource(crimeChild.getImage());
    }
}