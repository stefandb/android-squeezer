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

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;

import com.bignerdranch.expandablerecyclerview.Model.ParentObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.RecyclerExpandableAdapter;
import uk.org.ngo.squeezer.framework.expandable.CrimeLab;
import uk.org.ngo.squeezer.framework.expandable.ParentHolder;
import uk.org.ngo.squeezer.itemlist.AlbumView;
import uk.org.ngo.squeezer.itemlist.ArtistView;
import uk.org.ngo.squeezer.itemlist.GenreView;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.itemlist.SongView;
import uk.org.ngo.squeezer.itemlist.SongViewWithArt;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.ExpandableChildListItem;
import uk.org.ngo.squeezer.model.ExpandableParentListItem;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;

public class SearchActivity extends ItemListActivity {

    private View loadingLabel;

    private RecyclerView resultsExpandableListView;

    private SearchAdapter searchResultsAdapter;

    private String searchString;
    private RecyclerExpandableAdapter mExpandableAdapter;

    private final int[] groupIcons = {
            R.drawable.ic_songs, R.drawable.ic_albums, R.drawable.ic_artists, R.drawable.ic_genres
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_layout);

        loadingLabel = findViewById(R.id.loading_label);

        searchResultsAdapter = new SearchAdapter(this);
        resultsExpandableListView = (RecyclerView) findViewById(R.id.item_list);
        resultsExpandableListView.setLayoutManager(new LinearLayoutManager(this));


        RecyclerExpandableAdapter mExpandableAdapter = new RecyclerExpandableAdapter(this, generateCrimes());
        mExpandableAdapter.setCustomParentAnimationViewId(R.id.parent_list_item_expand_arrow);
        mExpandableAdapter.setParentClickableViewAnimationDefaultDuration();
        mExpandableAdapter.setParentAndIconExpandOnClick(true);
//        resultsExpandableListView.setOnChildClickListener(new OnChildClickListener() {
//            @Override
//            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
//                                        int childPosition, long id) {
//                searchResultsAdapter.onChildClick(groupPosition, childPosition);
//                return true;
//            }
//        });

        resultsExpandableListView.setAdapter(mExpandableAdapter);

//        resultsExpandableListView.setOnCreateContextMenuListener(searchResultsAdapter);
//        resultsExpandableListView.setOnScrollListener(new ScrollListener());

        handleIntent(getIntent());

        NavigationDrawer(savedInstanceState);

        getSupportActionBar().setTitle(R.string.menu_item_search_label);

        navigationDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private ArrayList<ParentObject> generateCrimes() {

        CrimeLab crimeLab = CrimeLab.get(this);

        ItemAdapter<?>[] adapters = {
            new ItemAdapter<Song>(new SongViewWithArt(this)),
            new ItemAdapter<Album>(new AlbumView(this)),
            new ItemAdapter<Artist>(new ArtistView(this)),
            new ItemAdapter<Genre>(new GenreView(this)),
        };

        String[] Titles = {"Songs","Albums","Artists","Genres"};

        //door de titels of adapters en de andere ophalen dat en een parent object aanmaken via functie in crimeLab




        ((SongViewWithArt) adapters[0].getItemView()).setDetails(
                SongView.DETAILS_DURATION | SongView.DETAILS_ALBUM | SongView.DETAILS_ARTIST);

        ((AlbumView) adapters[1].getItemView()).setDetails(
                AlbumView.DETAILS_ARTIST | AlbumView.DETAILS_YEAR);



        List<ExpandableParentListItem> crimes = crimeLab.getCrimes();
        ArrayList<ParentObject> parentObjects = new ArrayList<>();
        for (ExpandableParentListItem crime : crimes) {
            ArrayList<Object> childList = new ArrayList<>();
            childList.add(new ExpandableChildListItem(R.drawable.ic_years, "tekst", "tekst"));
            crime.setChildObjectList(childList);
            parentObjects.add(crime);
        }
        return parentObjects;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
//            doSearch(query);
        }
    }

//    @Override
    public final boolean onContextItemSelectedOLD(MenuItem menuItem) {
        Log.d("context-function-debug", "SearchActivity onContextItemSelected (item)");
        Log.d("click", String.valueOf(menuItem));
        if (getService() != null) {
            ExpandableListContextMenuInfo contextMenuInfo = (ExpandableListContextMenuInfo) menuItem
                    .getMenuInfo();
            long packedPosition = contextMenuInfo.packedPosition;
            int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
            int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
            if (ExpandableListView.getPackedPositionType(packedPosition)
                    == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                return searchResultsAdapter.doItemContext(menuItem, groupPosition, childPosition);
            }
        }
        return false;
    }

    /**
     * Performs the search now that the service connection is active.
     */
    @Override
    protected void onServiceConnected(@NonNull ISqueezeService service) {
        super.onServiceConnected(service);
        doSearch();
    }

    /**
     * Setting the list adapter will trigger a layout pass, which requires information from
     * the server.  Only do this after the handshake has completed.  When done, perform the
     * search.
     */
    public void onEventMainThread(HandshakeComplete event) {
//        resultsExpandableListView.setAdapter(searchResultsAdapter);
//        doSearch();
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
//        service.search(start, searchString, itemListCallback);
    }

    /**
     * Saves the search query, and attempts to query the service for <code>searchString</code>. If
     * the service binding has not completed yet then {@link #onEventMainThread(HandshakeComplete)}
     * will re-query for the saved search query.
     *
     * @param searchString The string to search fo.
     */
    private void doSearch(String searchString) {
        this.searchString = searchString;
        if (searchString != null && searchString.length() > 0 && getService() != null) {
            clearAndReOrderItems();
        }
    }

    @Override
    protected void clearItemAdapter() {
//        resultsExpandableListView.setVisibility(View.GONE);
//        loadingLabel.setVisibility(View.VISIBLE);
//        searchResultsAdapter.clear();
    }

    /**
     * Searches for the saved search query.
     */
    private void doSearch() {
        doSearch(searchString);
    }

    private final IServiceItemListCallback itemListCallback = new IServiceItemListCallback() {
        @Override
        public void onItemsReceived(final int count, final int start, Map parameters, final List items, final Class dataType) {
            SearchActivity.super.onItemsReceived(count, start, items.size());

            getUIThreadHandler().post(new Runnable() {
                @Override
                public void run() {
//                    searchResultsAdapter.updateItems(count, start, items, dataType);
                    loadingLabel.setVisibility(View.GONE);
//                    resultsExpandableListView.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        public Object getClient() {
            return SearchActivity.this;
        }
    };

}
