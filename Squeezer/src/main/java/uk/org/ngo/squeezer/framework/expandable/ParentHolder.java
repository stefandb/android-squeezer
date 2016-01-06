package uk.org.ngo.squeezer.framework.expandable;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ViewHolder.ParentViewHolder;

import uk.org.ngo.squeezer.R;

/**
 * Created by Stefan on 6-1-2016.
 */
public class ParentHolder extends ParentViewHolder {

    public TextView mCrimeTitleTextView;
    public ImageButton mParentDropDownArrow;

    public ParentHolder(View itemView) {
        super(itemView);

        mCrimeTitleTextView = (TextView) itemView.findViewById(R.id.text1);
        mParentDropDownArrow = (ImageButton) itemView.findViewById(R.id.parent_list_item_expand_arrow);
    }


}
