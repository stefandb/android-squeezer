/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
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
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import com.bignerdranch.expandablerecyclerview.Model.ParentObject;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.SearchAdapter;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.expandable.CrimeLab;
import uk.org.ngo.squeezer.itemlist.dialog.PlayerSyncDialog;
import uk.org.ngo.squeezer.model.ExpandableParentListItem;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.SearchType;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.PlayerStateChanged;
import uk.org.ngo.squeezer.service.event.PlayerVolume;

public class PlayerListActivity extends ItemListActivity implements
        PlayerSyncDialog.PlayerSyncDialogHost {
    private static final String CURRENT_PLAYER = "currentPlayer";

    private RecyclerView mResultsExpandableListView;

    private Player currentPlayer;
    private boolean mTrackingTouch;

    /** An update arrived while tracking touches. UI should be re-synced. */
    private boolean mUpdateWhileTracking = false;

    /** Map from player IDs to Players synced to that player ID. */
    private final Multimap<String, Player> mPlayerSyncGroups = HashMultimap.create();
    private PlayerListAdapter mExpandableAdapter;

    /**
     * Updates the adapter with the current players, and ensures that the list view is
     * expanded.
     */
    private void updateAndExpandPlayerList() {
        if (mResultsExpandableListView.getAdapter() == null) {
            return;
        }

        updateSyncGroups(getService().getPlayers(), getService().getActivePlayer());

        mExpandableAdapter.updatePlayers(mPlayerSyncGroups);

//        for (int i = 0; i < mResultsAdapter.getGroupCount(); i++) {
//            mResultsExpandableListView.expandGroup(i);
//        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.item_list_players);

        if (savedInstanceState != null)
            currentPlayer = savedInstanceState.getParcelable(CURRENT_PLAYER);

        mResultsExpandableListView = (RecyclerView) findViewById(R.id.item_list);
        mResultsExpandableListView.setLayoutManager(new LinearLayoutManager(this));

        mExpandableAdapter = new PlayerListAdapter(this, generateCrimes());
        mExpandableAdapter.setItemView(new PlayerView(this));
        mExpandableAdapter.setCustomParentAnimationViewId(R.id.parent_list_item_expand_arrow);
        mExpandableAdapter.setParentClickableViewAnimationDefaultDuration();
        mExpandableAdapter.setParentAndIconExpandOnClick(true);

        setIgnoreVolumeChange(true);

        NavigationDrawer(savedInstanceState);
        navigationDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.home_item_songs);
    }

    private ArrayList<ParentObject> generateCrimes() {
        CrimeLab crimeLab = CrimeLab.get(this);

        for (int i = 0; i < 4; i++) {
            ExpandableParentListItem crime = new ExpandableParentListItem();
            crime.setTitle(String.format("Groeps titel %d", i));
            crime.setSolved(i % 2 == 0);
            crimeLab.setCrime(crime);
        }

        List<ExpandableParentListItem> crimes = crimeLab.getCrimes();
        ArrayList<ParentObject> parentObjects = new ArrayList<>();
        for (ExpandableParentListItem crime : crimes) {
            ArrayList<Object> childList = new ArrayList<>();

            crime.setChildObjectList(childList);
            parentObjects.add(crime);
        }
        return parentObjects;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d("XXX-player", "playerlistactivity - onSaveInstanceState");
        outState.putParcelable(CURRENT_PLAYER, currentPlayer);
        super.onSaveInstanceState(outState);
    }

//    @Override
    public final boolean onContextItemSelectedOLD(MenuItem item) {
        /*
        if (getService() != null) {
            ExpandableListView.ExpandableListContextMenuInfo contextMenuInfo = (ExpandableListView.ExpandableListContextMenuInfo) item
                    .getMenuInfo();

            if (contextMenuInfo == null) {
                return mResultsAdapter.doItemContext(item);
            } else {

                long packedPosition = contextMenuInfo.packedPosition;
                int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
                int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
                if (ExpandableListView.getPackedPositionType(packedPosition)
                        == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                    return mResultsAdapter.doItemContext(item, groupPosition, childPosition);
                }
            }
        }
        */
        return false;
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        Log.d("XXX-player", "playerlistactivity - orderPage");
        // Do nothing -- the service has been tracking players from the time it
        // initially connected to the server.
    }

    public void onEventMainThread(HandshakeComplete event) {
        Log.d("XXX-player", "playerlistactivity - onEventMainThread 1 ");
            mResultsExpandableListView.setAdapter(mExpandableAdapter);
        updateAndExpandPlayerList();
    }

    public void onEventMainThread(PlayerStateChanged event) {
        Log.d("XXX-player", "playerlistactivity - onEventMainThread 2 ");
        if (!mTrackingTouch) {
            updateAndExpandPlayerList();
        } else {
            mUpdateWhileTracking = true;
        }
    }

    public void onEventMainThread(PlayerVolume event) {
        if (!mTrackingTouch) {
            mExpandableAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Builds the list of lists that is a sync group.
     *
     * @param players List of players.
     * @param activePlayer The currently active player.
     */
    public void updateSyncGroups(List<Player> players, Player activePlayer) {
        Map<String, Player> connectedPlayers = new HashMap<String, Player>();

        // Make a copy of the players we know about, ignoring unconnected ones.
        for (Player player : players) {
            if (!player.getConnected())
                continue;

            connectedPlayers.put(player.getId(), player);
        }

        mPlayerSyncGroups.clear();

        // Iterate over all the connected players to build the list of master players.
        for (Player player : connectedPlayers.values()) {
            String playerId = player.getId();
            PlayerState playerState = player.getPlayerState();
            String syncMaster = playerState.getSyncMaster();

            // If a player doesn't have a sync master then it's in a group of its own.
            if (syncMaster == null) {
                mPlayerSyncGroups.put(playerId, player);
                continue;
            }

            // If the master is this player then add itself and all the slaves.
            if (playerId.equals(syncMaster)) {
                mPlayerSyncGroups.put(playerId, player);
                continue;
            }

            // Must be a slave. Add it under the master. This might have already
            // happened (in the block above), but might not. For example, it's possible
            // to have a player that's a syncslave of an player that is not connected.
            mPlayerSyncGroups.put(syncMaster, player);
        }
    }

    @Override
    @NonNull
    public Multimap<String, Player> getPlayerSyncGroups() {
        Log.d("XXX-player", "playerlistactivity - Multimap");
        Log.d("XXX-player", "playerlistactivity - Multimap " + mPlayerSyncGroups.toString());

        return mPlayerSyncGroups;
    }

    public PlayerState getPlayerState(String id) {
        Log.d("XXX-player", "playerlistactivity - getPlayerState");
        return getService().getPlayerState(id);
    }

    @Override
    public Player getCurrentPlayer() {
        Log.d("XXX-player", "playerlistactivity - getCurrentPlayer");
        return currentPlayer;
    }
    public void setCurrentPlayer(Player currentPlayer) {
        Log.d("XXX-player", "playerlistactivity - setCurrentPlayer");
        this.currentPlayer = currentPlayer;
    }

    public void setTrackingTouch(boolean trackingTouch) {
        Log.d("XXX-player", "playerlistactivity - setTrackingTouch");
        mTrackingTouch = trackingTouch;
        if (!mTrackingTouch) {
            if (mUpdateWhileTracking) {
                mUpdateWhileTracking = false;
                updateAndExpandPlayerList();
            }
        }
    }

    public void playerRename(String newName) {
        Log.d("XXX-player", "playerlistactivity - playerRename");
        ISqueezeService service = getService();
        if (service == null) {
            return;
        }

        service.playerRename(currentPlayer, newName);
        this.currentPlayer.setName(newName);
        mExpandableAdapter.notifyDataSetChanged();
    }

    @Override
    protected void clearItemAdapter() {
        Log.d("XXX-player", "playerlistactivity - clearItemAdapter");
//        mExpandableAdapter.clear();
    }

    /**
     * Synchronises the slave player to the player with masterId.
     *
     * @param slave the player to sync.
     * @param masterId ID of the player to sync to.
     */
    @Override
    public void syncPlayerToPlayer(@NonNull Player slave, @NonNull String masterId) {
        Log.d("XXX-player", "playerlistactivity - syncPlayerToPlayer");
        getService().syncPlayerToPlayer(slave, masterId);
    }

    /**
     * Removes the player from any sync groups.
     *
     * @param player the player to be removed from sync groups.
     */
    @Override
    public void unsyncPlayer(@NonNull Player player) {
        Log.d("XXX-player", "playerlistactivity - unsyncPlayer");
        getService().unsyncPlayer(player);
    }

    public static void show(Context context) {
        Log.d("XXX-player", "playerlistactivity - show");
        final Intent intent = new Intent(context, PlayerListActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }
}
