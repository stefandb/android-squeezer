package uk.org.ngo.squeezer.framework;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.R;

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

    private List<T> mItems = new ArrayList<>();;
    private boolean mShowIcon;
    private OnItemClickListener mOnItemClickListener;


    public interface OnItemClickListener {
        void onItemClicked(Item simpleItem);
    }

    public recyclerViewListAdapter(Context context) {
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
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener == null) {
                    return;
                }

                mOnItemClickListener.onItemClicked(mItems.get(viewHolder.getAdapterPosition()));
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final SimpleHolder viewHolder, int i) {
        Item item = mItems.get(i);
        viewHolder.getTextView().setText(item.getName());

        if (!mShowIcon) {
            viewHolder.getImageView().setVisibility(View.GONE);
        }

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

    public static class SimpleHolder extends RecyclerView.ViewHolder {

        private TextView mTextView;
        private ImageView mImageView;

        public SimpleHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.text1);
            mImageView = (ImageView) itemView.findViewById(R.id.icon);
        }

        public TextView getTextView() {
            return mTextView;
        }

        public ImageView getImageView() {
            return mImageView;
        }
    }

}