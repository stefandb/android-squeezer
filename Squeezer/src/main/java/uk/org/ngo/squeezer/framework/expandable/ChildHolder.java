package uk.org.ngo.squeezer.framework.expandable;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ViewHolder.ChildViewHolder;

import uk.org.ngo.squeezer.R;

/**
 * Created by Stefan on 6-1-2016.
 */
public class ChildHolder extends ChildViewHolder {

    public TextView text1;
    public TextView text2;
    public ImageView icon;


    public ChildHolder(View itemView) {
        super(itemView);

        text1 = (TextView) itemView.findViewById(R.id.text1);
        text2 = (TextView) itemView.findViewById(R.id.text2);
        icon = (ImageView) itemView.findViewById(R.id.icon);
    }
}
