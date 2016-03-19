/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.Model.ParentObject;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.PlaylistItem;
import uk.org.ngo.squeezer.framework.RecyclerExpandableAdapter;
import uk.org.ngo.squeezer.framework.expandable.RecyclerItemViewHolder;
import uk.org.ngo.squeezer.itemlist.AlbumView;
import uk.org.ngo.squeezer.itemlist.ArtistView;
import uk.org.ngo.squeezer.itemlist.GenreView;
import uk.org.ngo.squeezer.itemlist.SongView;
import uk.org.ngo.squeezer.itemlist.SongViewWithArt;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.ExpandableParentListItem;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.SearchType;
import uk.org.ngo.squeezer.model.Song;

public class SearchAdapter<Child extends Item, K extends BaseItemView> extends RecyclerExpandableAdapter {

    private ArrayList<SearchType> searchTypes;

    public SearchAdapter(Context context, List parentItemList) {
        super(context, parentItemList);
    }

    @Override
    public RecyclerItemViewHolder onCreateChildViewHolder(ViewGroup viewGroup) {
        HashMap<String, K> enginesViews = new HashMap<String, K>();
        for (int i = 0; i < searchTypes.size(); i++) {
            enginesViews.put(searchTypes.get(i).getModelClassName(), (K) searchTypes.get(i).getViewBuilder());
        }
        //TODO-stefan layout dynamisch maken
        View view = mInflater.inflate(R.layout.list_item, viewGroup, false);

        RecyclerItemViewHolder viewHolderInstance = new RecyclerItemViewHolder(view, this);
        viewHolderInstance.setItemViews(enginesViews);

        return viewHolderInstance;
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

    public void setSearchEngines(ArrayList<SearchType> st){
        searchTypes = st;
    }

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

    public <T extends Item> void setChildItems(String ClassType, List<T> items){
        List parentItems = getParentItems();
        for(Object parent: parentItems){
            ExpandableParentListItem ParentItem = (ExpandableParentListItem) parent;
            String loopClass= ParentItem.getItemClassName();

            String searchClassName = String.valueOf(ClassType.substring(ClassType.lastIndexOf('.') + 1)).toLowerCase().trim().toString();
            String currentClassName = String.valueOf(loopClass.substring(loopClass.lastIndexOf('.') + 1)).toLowerCase().trim().toString();

            if(currentClassName.contains(searchClassName)){
                ParentItem.setItemCount(items.size());
                ArrayList<Object> childList = new ArrayList<>();

                //TODO-stefan controle toevoegen of het item al bestaat zo ja vervangen
                for(T childItem: items) {
                    childList.add(childItem);
                }
                ParentItem.setChildObjectList(childList);
            }
        }

        notifyDataSetChanged();
    }
}
