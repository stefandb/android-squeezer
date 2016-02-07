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

package uk.org.ngo.squeezer.itemlist;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.PlaylistItem;
import uk.org.ngo.squeezer.framework.RecyclerExpandableAdapter;
import uk.org.ngo.squeezer.framework.expandable.ParentHolder;
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
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.SearchType;
import uk.org.ngo.squeezer.model.Song;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerListAdapter<Child extends Item, K extends BaseItemView> extends RecyclerExpandableAdapter {

    /** The last set of player sync groups that were provided. */
    private Multimap<String, Player> prevPlayerSyncGroups;

    public PlayerListAdapter(Context context, List parentItemList) {
        super(context, parentItemList);
    }

    @Override
    public ParentHolder onCreateParentViewHolder(ViewGroup viewGroup) {
        //TODO-stefan R.layout dynamisch maken
        View view = mInflater.inflate(R.layout.group_player, viewGroup, false);
        return new ParentHolder(view);
    }

    @Override
    public RecyclerItemViewHolder onCreateChildViewHolder(ViewGroup viewGroup) {
        //TODO-stefan R.layout dynamisch maken
        View view = mInflater.inflate(R.layout.list_item_player, viewGroup, false);

        RecyclerItemViewHolder viewHolderInstance = new RecyclerItemViewHolder(view, this);
        return viewHolderInstance;
    }

    @Override
    public void onBindParentViewHolder(ParentHolder parentHolder, int i, Object o) {
        ExpandableParentListItem crime = (ExpandableParentListItem) o;
        parentHolder.mTitle.setText(crime.getTitle());
        parentHolder.mSubTitle.setText(crime.getsubTitle());
    }

    @Override
    public void onBindChildViewHolder(final RecyclerItemViewHolder childHolder, int i, Object o) {

        childHolder.setItem((Child) o);
        mItemView.getAdapterView(childHolder, i, (Child) o);

        childHolder.setPosition(i);

        childHolder.getItemView().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setPosition(childHolder.getPosition());
                return false;
            }
        });
    }

    /**
     * TODO-stefan code ombouwen van ItemAdapter
     */
    public boolean doItemContext(MenuItem menuItem, int position) {
        Player Item = (Player) mItemList.get(position);
//        return  engine.getViewBuilder().doItemContext(menuItem, position, (Child) mItemList.get(position));
        return false;
    }



    /**
     * Sets the players in to the adapter.
     *
     * @param playerSyncGroups Multimap, mapping from the player ID of the syncmaster to the
     *     Players synced to that master. See
     *     {@link PlayerListActivity#updateSyncGroups(List, Player)} for how this map is
     *     generated.
     */
    public void updatePlayers(Multimap<String, Player> playerSyncGroups) {

        if (prevPlayerSyncGroups != null && prevPlayerSyncGroups.equals(playerSyncGroups)) {
            notifyDataSetChanged();
            return;
        }

        prevPlayerSyncGroups = HashMultimap.create(playerSyncGroups);
//        clear();



        ArrayList<ParentObject> parentObjects = new ArrayList<>();



        this.mParentItemList = parentObjects;
        this.mItemList = generateObjectList(parentObjects);

        notifyDataSetChanged();

    }

    private ArrayList<Object> generateObjectList(List<ParentObject> parentObjectList) {
        ArrayList objectList = new ArrayList();
        Iterator var3 = parentObjectList.iterator();

        while(var3.hasNext()) {
            ParentObject parentObject = (ParentObject)var3.next();
            objectList.add(parentObject);
        }

        return objectList;
    }

}
