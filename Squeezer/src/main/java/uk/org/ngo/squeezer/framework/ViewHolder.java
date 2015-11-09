package uk.org.ngo.squeezer.framework;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import uk.org.ngo.squeezer.R;

/**
 * Created by Stefan on 9-11-2015.
 */
//public class ViewHolder {
public interface ViewHolder<T extends Item> {

    public SimpleHolder(){}

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

}
