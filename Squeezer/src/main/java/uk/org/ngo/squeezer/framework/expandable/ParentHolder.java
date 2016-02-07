package uk.org.ngo.squeezer.framework.expandable;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ViewHolder.ParentViewHolder;

import uk.org.ngo.squeezer.R;

/**
 * Created by Stefan on 6-1-2016.
 */
public class ParentHolder extends ParentViewHolder {

    public TextView mTitle;
    public TextView mSubTitle;
    public TextView mItemCount;

    public ImageButton mParentDropDownArrow;
    public ImageView mIcon;

    public ParentHolder(View itemView) {
        super(itemView);

        mTitle = (TextView) itemView.findViewById(R.id.text1);
        if(itemView.findViewById(R.id.text2) != null){
            mSubTitle = (TextView) itemView.findViewById(R.id.text2);
        }
        if(itemView.findViewById(R.id.itemcount) != null){
            mItemCount = (TextView) itemView.findViewById(R.id.itemcount);
        }

        mParentDropDownArrow = (ImageButton) itemView.findViewById(R.id.parent_list_item_expand_arrow);
        mIcon = (ImageView) itemView.findViewById(R.id.icon);

    }


}
