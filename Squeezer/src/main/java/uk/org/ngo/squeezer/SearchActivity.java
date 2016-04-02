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
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.bignerdranch.expandablerecyclerview.Model.ParentObject;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.framework.RecyclerItemClickListener;
import uk.org.ngo.squeezer.itemlist.AlbumView;
import uk.org.ngo.squeezer.itemlist.ArtistView;
import uk.org.ngo.squeezer.itemlist.GenreView;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.itemlist.PluginItemListActivity;
import uk.org.ngo.squeezer.itemlist.SongView;
import uk.org.ngo.squeezer.itemlist.SongViewWithArt;
import uk.org.ngo.squeezer.model.ExpandableParentListItem;
import uk.org.ngo.squeezer.model.PluginItem;
import uk.org.ngo.squeezer.model.SearchType;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import android.support.v4.widget.SwipeRefreshLayout;

/**
 * @param <Child>
 * @param <K>
 */
public class SearchActivity<Child extends Item, K extends BaseItemView, T extends BaseItemView> extends ItemListActivity {

    private View loadingLabel;

    private RecyclerView resultsExpandableListView;

    private SearchAdapter searchResultsAdapter;

    private String searchString;

    private SearchAdapter mExpandableAdapter;

    ArrayList<SearchType> SearchTypes;

    private SwipeRefreshLayout refreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_layout);
        loadingLabel = findViewById(R.id.loading_label);

        resultsExpandableListView = (RecyclerView) findViewById(R.id.item_list);
        resultsExpandableListView.setLayoutManager(new LinearLayoutManager(this));

        setSearchTypes();

        registerForContextMenu(resultsExpandableListView);
        resultsExpandableListView.setLongClickable(true);
        resultsExpandableListView.addOnItemTouchListener(ItemTouchListener());

        handleIntent(getIntent());

        NavigationDrawer(savedInstanceState);
        getSupportActionBar().setTitle(R.string.menu_item_search_label);
        navigationDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        refreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

            }
        });
    }

    private RecyclerView.OnItemTouchListener ItemTouchListener() {
        return new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (mExpandableAdapter.getItemList().get(position) instanceof ParentObject) {
                    ExpandableParentListItem parent = (ExpandableParentListItem) mExpandableAdapter.getParentItems().get(position);

                    int searchEngineId = parent.getSearchEngineId();
                    SearchType searchEngine = SearchTypes.get(searchEngineId);
                    searchEngine.toggleExpand();
                } else {
                    int index = 0;
                    SearchType engine = null;
                    for (SearchType search : SearchTypes) {
                        engine = search;
                        if (!search.isExpand()) {
                            ExpandableParentListItem parentItem = null;
                            for (Object parent : mExpandableAdapter.getParentItems()) {
                                parentItem = (ExpandableParentListItem) parent;
                                if (parentItem.getSearchEngineId() == index) {
                                    break;
                                }
                            }

                            if (position < parentItem.getItemCountint()) {
                                position += parentItem.getItemCountint();
                            } else {
                                break;
                            }
                        }
                        index++;
                    }

                    Object c = mExpandableAdapter.getItemList().get(position);
                    engine.getViewBuilder().onItemSelected(position, (Child) c);
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            ArrayList<Object> childList = new ArrayList<>();

            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d("Search-query", query);

            Bundle appData = getIntent().getBundleExtra(SearchManager.APP_DATA);

            Boolean Extra = false;
            String Type = "aa";
            BaseItemView View = null;
            PluginItem PluginItem = null;
            String Icon = "";
            PluginItemListActivity Class = null;
            if (appData != null) {
                Extra = appData.getBoolean("Extra", false);

                PluginItemListActivity pluginItemListActivity = new PluginItemListActivity();

                if(Extra){
                    String caller = appData.getString("caller");
                    ComponentName prev = this.getCallingActivity();

                    Type = appData.getString("Type", "");
                    Icon = appData.getString("Icon");

                    BaseItemView view = pluginItemListActivity.createItemViewSearch();

                    SearchType object = new SearchType(
                            appData.getString("Label", ""),
                            FontAwesome.Icon.faw_soundcloud,
                            view,
                            appData.getString("ClassType", "")
                    );
                    object.setPluginId(appData.getString("pluginId"));
                    object.setParentPluginId(appData.getString("parentPluginId"));
                    object.setCustom(true);
                    SearchTypes.add(object);
                }
            }

            SearchAdapter adapter = createListAdapter();

            doSearch(query);
        }
    }

    /**
     * @param menuItem
     * @return
     */
    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        if (getService() != null) {
            AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();

            int position = -1;
            try {
                position = mExpandableAdapter.getPosition();
            } catch (Exception e) {
                return super.onContextItemSelected(menuItem);
            }

            return mExpandableAdapter.doItemContext(menuItem, position);
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
        resultsExpandableListView.setAdapter(mExpandableAdapter);
        doSearch();
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        service.search(start, searchString, itemListCallback);

        SearchType Object = SearchTypes.get(SearchTypes.size() - 1);

        if(Object.getCustom() == true){
            service.searchPluginItems(1, Object.getPluginId(), Object.getParentPluginId(), searchString, itemListCallback);
        }

//        service.pluginItems(start, plugin, parent, search, this);
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
        Log.d("Search", this.searchString);
        if (searchString != null && searchString.length() > 0 && getService() != null) {
            clearAndReOrderItems();
        }
    }

    @Override
    protected void clearItemAdapter() {
        resultsExpandableListView.setVisibility(View.GONE);
        loadingLabel.setVisibility(View.VISIBLE);
        //TODO-stefan functie schrijven in adaper om het object te legen
//        mExpandableAdapter.clear();
    }

    /**
     * Searches for the saved search query.
     */
    private void doSearch() {
        doSearch(searchString);
    }

    private final IServiceItemListCallback itemListCallback = new IServiceItemListCallback() {
        @Override
        public void onItemsReceived(final int count, final int start, final Map parameters, final List items, final Class dataType) {
            SearchActivity.super.onItemsReceived(count, start, items.size());

            final String ModelClass = String.valueOf(String.valueOf(dataType).substring(String.valueOf(dataType).lastIndexOf('.') + 1)).toLowerCase().trim().toString();

            getUIThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    mExpandableAdapter.setChildItems(ModelClass, items);
                    loadingLabel.setVisibility(View.GONE);
                    resultsExpandableListView.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        public Object getClient() {
            return SearchActivity.this;
        }
    };

    private SearchAdapter createListAdapter(){
        mExpandableAdapter = new SearchAdapter(this, generateSearchEngines());
        mExpandableAdapter.setSearchEngines(SearchTypes);
        mExpandableAdapter.setCustomParentAnimationViewId(R.id.parent_list_item_expand_arrow);
        mExpandableAdapter.setParentClickableViewAnimationDefaultDuration();
        mExpandableAdapter.setParentAndIconExpandOnClick(true);
        return mExpandableAdapter;
    }

    private void setSearchTypes(){
        SearchTypes = new ArrayList<>();

        SearchTypes.add(new SearchType("Songs", FontAwesome.Icon.faw_music, new SongViewWithArt(this), "Song"));
        SearchTypes.add(new SearchType("Albums", GoogleMaterial.Icon.gmd_album, new AlbumView(this), "Album"));
        SearchTypes.add(new SearchType("Artists", FontAwesome.Icon.faw_home, new ArtistView(this), "Artist"));
        SearchTypes.add(new SearchType("Genres", FontAwesome.Icon.faw_music, new GenreView(this), "Genre"));

        ((SongViewWithArt) SearchTypes.get(0).getViewBuilder()).setDetails(SongView.DETAILS_DURATION | SongView.DETAILS_ALBUM | SongView.DETAILS_ARTIST);
        ((AlbumView) SearchTypes.get(1).getViewBuilder()).setDetails(AlbumView.DETAILS_ARTIST | AlbumView.DETAILS_YEAR);
    }

    public ArrayList getSearchTypes(){
        return SearchTypes;
    }

    private ArrayList<ParentObject> generateSearchEngines() {
        ArrayList<ParentObject> Objects = new ArrayList<>();

        int index = 0;
        for(SearchType search : SearchTypes) {
            ArrayList<Object> childList = new ArrayList<>();

            ExpandableParentListItem ParentObject = new ExpandableParentListItem();
            ParentObject.setTitle(search.getTitle());
            ParentObject.setIcon(search.getIconResourse());
            ParentObject.setItemClassName(search.getModelClassName());
            ParentObject.setSearchEngineId(index);
            ParentObject.setChildObjectList(childList);

            Objects.add(ParentObject);
            index++;
        }

        return Objects;
    }
}
