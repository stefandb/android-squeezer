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

package uk.org.ngo.squeezer.framework;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.widget.Toolbar;
//import android.support.v7.app.ActionBar;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import uk.org.ngo.squeezer.IconRowAdapter;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.RandomplayActivity;
import uk.org.ngo.squeezer.SettingsActivity;
import uk.org.ngo.squeezer.VolumePanel;
import uk.org.ngo.squeezer.dialog.AboutDialog;
import uk.org.ngo.squeezer.dialog.TipsDialog;
import uk.org.ngo.squeezer.itemlist.AlarmsActivity;
import uk.org.ngo.squeezer.itemlist.AlbumListActivity;
import uk.org.ngo.squeezer.itemlist.ApplicationListActivity;
import uk.org.ngo.squeezer.itemlist.ArtistListActivity;
import uk.org.ngo.squeezer.itemlist.FavoriteListActivity;
import uk.org.ngo.squeezer.itemlist.GenreListActivity;
import uk.org.ngo.squeezer.itemlist.MusicFolderListActivity;
import uk.org.ngo.squeezer.itemlist.PlayerListActivity;
import uk.org.ngo.squeezer.itemlist.PlaylistsActivity;
import uk.org.ngo.squeezer.itemlist.RadioListActivity;
import uk.org.ngo.squeezer.itemlist.SongListActivity;
import uk.org.ngo.squeezer.itemlist.YearListActivity;
import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.PlayerVolume;
import uk.org.ngo.squeezer.util.ImageFetcher;
import uk.org.ngo.squeezer.util.SqueezePlayer;
import uk.org.ngo.squeezer.util.ThemeManager;

/**
 * Common base class for all activities in Squeezer.
 *
 * @author Kurt Aaholst
 */
public abstract class BaseActivity extends AppCompatActivity implements HasUiThread {

    @Nullable
    private ISqueezeService mService = null;

    private final ThemeManager mTheme = new ThemeManager();
    private int mThemeId = mTheme.getDefaultTheme().mThemeId;

    /** Records whether the activity has registered on the service's event bus. */
    private boolean mRegisteredOnEventBus;

    private final Handler uiThreadHandler = new Handler() {
    };

    private SqueezePlayer squeezePlayer;

    /** Option menu volume control entry. */
    @Nullable
    private MenuItem mMenuItemVolume;

    /** Whether volume changes should be ignored. */
    private boolean mIgnoreVolumeChange;

    /** Volume control panel. */
    @Nullable
    private VolumePanel mVolumePanel;

    //save our header or result
    protected AccountHeader navigationDrawerHeader = null;
    protected Drawer navigationDrawer = null;

    protected String getTag() {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : getTag");
        return getClass().getSimpleName();
    }

    protected Bundle _savedInstanceState = null;

    /**
     * @return The squeezeservice, or null if not bound
     */
    @Nullable
    public ISqueezeService getService() {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : getService");
        return mService;
    }

    public int getThemeId() {
        return mThemeId;
    }

    private boolean mCanFavorites = false;

    private boolean mCanMusicfolder = false;

    private boolean mCanMyApps = false;

    private boolean mCanRandomplay = false;

    /**
     * Use this to post Runnables to work off thread
     */
    @Override
    public Handler getUIThreadHandler() {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : getUIThreadHandler");
        return uiThreadHandler;
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity - ServiceConnection : onServiceConnected");
            mService = (ISqueezeService) binder;
            BaseActivity.this.onServiceConnected(mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity - ServiceConnection : onServiceDisconnected");
            mService = null;
        }
    };

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {

        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onCreate");
        _savedInstanceState = savedInstanceState;
        super.onCreate(savedInstanceState);

        mTheme.onCreate(this);
        createPlayerHeader();
//        ActionBar actionBar = getSupportActionBar();

//        actionBar.setIcon(R.drawable.ic_launcher);
        bindService(new Intent(this, SqueezeService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        Log.d(getTag(), "did bindService; serviceStub = " + getService());

    }

    @Override
    public void setTheme(int resId) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : setTheme");
        super.setTheme(resId);
        mThemeId = resId;
    }

    @Override
    public void onResume() {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onResume");
        super.onResume();

        mTheme.onResume(this);

        if (mService != null) {
            maybeRegisterOnEventBus(mService);
        }

        mVolumePanel = new VolumePanel(this);

        // If SqueezePlayer is installed, start it
        // TODO Only when connected (or at least serveraddress is saved)
        if (SqueezePlayer.hasSqueezePlayer(this) && new Preferences(this).controlSqueezePlayer()) {
            squeezePlayer = new SqueezePlayer(this);
        }

        // Ensure that any image fetching tasks started by this activity do not finish prematurely.
        ImageFetcher.getInstance(this).setExitTasksEarly(false);
    }

    @Override
    public void onPause() {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onPause");
        // At least some Samsung devices call onPause without ensuring that onResume is called
        // first, per https://code.google.com/p/android/issues/detail?id=74464, so mVolumePanel
        // may be null on those devices.
        if (mVolumePanel != null) {
            mVolumePanel.dismiss();
            mVolumePanel = null;
        }

        if (squeezePlayer != null) {
            squeezePlayer.stopControllingSqueezePlayer();
            squeezePlayer = null;
        }
        if (mRegisteredOnEventBus) {
            // If we are not bound to the service, it's process is no longer
            // running, so the callbacks are already cleaned up.
            if (mService != null) {
                mService.getEventBus().unregister(this);
                mService.cancelItemListRequests(this);
                mService.cancelSubscriptions(this);
            }
            mRegisteredOnEventBus = false;
        }

        // Ensure that any pending image fetching tasks are unpaused, and finish quickly.
        ImageFetcher imageFetcher = ImageFetcher.getInstance(this);
        imageFetcher.setExitTasksEarly(true);
        imageFetcher.setPauseWork(false);

        super.onPause();
    }

    /**
     * Clear the image memory cache if memory gets low.
     */
    @Override
    public void onLowMemory() {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onLowMemory");
        ImageFetcher.onLowMemory();
    }

    @Override
    public void onDestroy() {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onDestroy");
        super.onDestroy();
        unbindService(serviceConnection);
    }

    /** Fix for https://code.google.com/p/android/issues/detail?id=63570. */
    private boolean mIsRestoredToTop;

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onNewIntent");
        super.onNewIntent(intent);
        if ((intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) > 0) {
            mIsRestoredToTop = true;
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void finish() {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : finish");
        super.finish();
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !isTaskRoot()
                && mIsRestoredToTop) {
            // 4.4.2 platform issues for FLAG_ACTIVITY_REORDER_TO_FRONT,
            // reordered activity back press will go to home unexpectedly,
            // Workaround: move reordered activity current task to front when it's finished.
            ActivityManager tasksManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            tasksManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
        }
    }

    /**
     * Performs any actions necessary after the service has been connected. Derived classes
     * should call through to the base class.
     * <ul>
     *     <li>Invalidates the options menu so that menu items can be adjusted based on
     *     the state of the service connection.</li>
     *     <li>Ensures that callbacks are registered.</li>
     * </ul>
     *
     * @param service The connection to the bound service.
     */
    @CallSuper
    protected void onServiceConnected(@NonNull ISqueezeService service) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onServiceConnected");
        supportInvalidateOptionsMenu();
        maybeRegisterOnEventBus(service);
        addPlayersToMenu(service);
    }

    /**
     * Conditionally registers with the service's EventBus.
     * <p>
     * Registration can happen in {@link #onResume()} and {@link
     * #onServiceConnected(uk.org.ngo.squeezer.service.ISqueezeService)}, this ensures that it only
     * happens once.
     *
     * @param service The connection to the bound service.
     */
    private void maybeRegisterOnEventBus(@NonNull ISqueezeService service) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : maybeRegisterOnEventBus");
        if (!mRegisteredOnEventBus) {
            service.getEventBus().registerSticky(this);
            mRegisteredOnEventBus = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onCreateOptionsMenu");

        Log.d("onCreateOptionsMenu", "1");
        MenuInflater inflater = getMenuInflater();
        Log.d("onCreateOptionsMenu", "2");
        inflater.inflate(R.menu.base_activity, menu);
        Log.d("onCreateOptionsMenu", "3");
        mMenuItemVolume = menu.findItem(R.id.menu_item_volume);
        Log.d("onCreateOptionsMenu", "4");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onPrepareOptionsMenu");


        Log.d("onPrepareOptionsMenu", "1");
        boolean haveConnectedPlayers = isConnected() && mService != null
                && !mService.getConnectedPlayers().isEmpty();
        Log.d("onPrepareOptionsMenu", "2");
        if (mMenuItemVolume != null) {
            Log.d("onPrepareOptionsMenu", "3");
            mMenuItemVolume.setVisible(haveConnectedPlayers);
        }
        Log.d("onPrepareOptionsMenu", "4");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onOptionsItemSelected");
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (upIntent != null) {
                    if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                        TaskStackBuilder.create(this)
                                .addNextIntentWithParentStack(upIntent)
                                .startActivities();
                    } else {
                        upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        NavUtils.navigateUpTo(this, upIntent);
                    }
                } else {
                    SongListActivity.show(this);
                }
                return true;
            case R.id.menu_item_volume:
                // Show the volume dialog.
                if (mService != null) {
                    PlayerState playerState = mService.getPlayerState();
                    Player player = mService.getActivePlayer();

                    if (playerState != null  && mVolumePanel != null) {
                        mVolumePanel.postVolumeChanged(playerState.getCurrentVolume(),
                                player == null ? "" : player.getName());
                    }

                    return true;
                }
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Block searches, when we are not connected.
     */
    @Override
    public boolean onSearchRequested() {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onSearchRequested");
        if (!isConnected()) {
            return false;
        }
        return super.onSearchRequested();
    }

    /*
     * Intercept hardware volume control keys to control Squeezeserver
     * volume.
     *
     * Change the volume when the key is depressed.  Suppress the keyUp
     * event, otherwise you get a notification beep as well as the volume
     * changing.
     */
    @Override
    @CallSuper
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onKeyDown");
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                return changeVolumeBy(+5);
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return changeVolumeBy(-5);
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    @CallSuper
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onKeyUp");
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private boolean changeVolumeBy(int delta) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : changeVolumeBy");
        ISqueezeService service = getService();
        if (service == null) {
            return false;
        }
        Log.v(getTag(), "Adjust volume by: " + delta);
        service.adjustVolumeBy(delta);
        return true;
    }

    public void onEvent(PlayerVolume event) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onEvent");
        if (!mIgnoreVolumeChange && mVolumePanel != null && event.player == mService.getActivePlayer()) {
            mVolumePanel.postVolumeChanged(event.volume, event.player.getName());
        }
    }

    public void setIgnoreVolumeChange(boolean ignoreVolumeChange) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : setIgnoreVolumeChange");
        mIgnoreVolumeChange = ignoreVolumeChange;
    }

    // Safe accessors

    public boolean canDownload() {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : canDownload");
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD);
    }

    public boolean isConnected() {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : isConnected");
        return mService != null && mService.isConnected();
    }

    public String getServerString(ServerString stringToken) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : getServerString");
        return ServerString.values()[stringToken.ordinal()].getLocalizedString();
    }

    // This section is just an easier way to call squeeze service

    public void play(PlaylistItem item) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : play");
        playlistControl(PLAYLIST_PLAY_NOW, item, R.string.ITEM_PLAYING);
    }

    public void add(PlaylistItem item) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : add");
        playlistControl(PLAYLIST_ADD_TO_END, item, R.string.ITEM_ADDED);
    }

    public void insert(PlaylistItem item) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : insert");
        playlistControl(PLAYLIST_PLAY_AFTER_CURRENT, item, R.string.ITEM_INSERTED);
    }

    private void playlistControl(@PlaylistControlCmd String cmd, PlaylistItem item, int resId){
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : playlistControl");
        if (mService == null) {
            return;
        }

        mService.playlistControl(cmd, item);
        Toast.makeText(this, getString(resId, item.getName()), Toast.LENGTH_SHORT).show();
    }

    /**
     * Initiate download of songs for the supplied item.
     *
     * @param item Song or item with songs to download
     * @see ISqueezeService#downloadItem(FilterItem)
     */
    public void downloadItem(FilterItem item) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : downloadItem");
        if (canDownload())
            mService.downloadItem(item);
        else
            Toast.makeText(this, R.string.DOWNLOAD_MANAGER_NEEDED, Toast.LENGTH_LONG).show();
    }

    @StringDef({PLAYLIST_PLAY_NOW, PLAYLIST_ADD_TO_END, PLAYLIST_PLAY_AFTER_CURRENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlaylistControlCmd {}
    public static final String PLAYLIST_PLAY_NOW = "load";
    public static final String PLAYLIST_ADD_TO_END = "add";
    public static final String PLAYLIST_PLAY_AFTER_CURRENT = "insert";

    /**
     * Look up an attribute resource styled for the current theme.
     *
     * @param attribute Attribute identifier to look up.
     * @return The resource identifier for the given attribute.
     */
    public int getAttributeValue(int attribute) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : getAttributeValue");
        TypedValue v = new TypedValue();
        getTheme().resolveAttribute(attribute, v, true);
        return v.resourceId;
    }

    public void NavigationDrawer(Bundle savedInstanceState){
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : NavigationDrawer");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ProfileDrawerItem profile5 = new ProfileDrawerItem().withName("Batman").withEmail("batman@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile5));

        //Create the drawer
        navigationDrawer = new DrawerBuilder().addDrawerItems()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(navigationDrawerHeader) //set the AccountHeader we created earlier for the header
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.home_item_songs).withIcon(FontAwesome.Icon.faw_music).withIdentifier(1).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_artists).withIcon(FontAwesome.Icon.faw_home).withIdentifier(2).withSelectable(false), //NEE
                        new PrimaryDrawerItem().withName(R.string.home_item_albums).withIcon(GoogleMaterial.Icon.gmd_album).withIdentifier(3).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_genres).withIcon(FontAwesome.Icon.faw_music).withIdentifier(4).withSelectable(false), //NEE
                        new PrimaryDrawerItem().withName(R.string.home_item_years).withIcon(FontAwesome.Icon.faw_music).withIdentifier(5).withSelectable(false), //NEE
                        new PrimaryDrawerItem().withName(R.string.home_item_new_music).withIcon(FontAwesome.Icon.faw_music).withIdentifier(6).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_random_mix).withIcon(FontAwesome.Icon.faw_random).withIdentifier(7).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_playlists).withIcon(FontAwesome.Icon.faw_music).withIdentifier(8).withSelectable(false), //NEE
                        new PrimaryDrawerItem().withName(R.string.home_item_music_folder).withIcon(FontAwesome.Icon.faw_folder).withIdentifier(9).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_radios).withIcon(GoogleMaterial.Icon.gmd_radio).withIdentifier(10).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_favorites).withIcon(GoogleMaterial.Icon.gmd_favorite).withIdentifier(11).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_my_apps).withIcon(GoogleMaterial.Icon.gmd_apps).withIdentifier(12).withSelectable(false),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName(ServerString.ALARM.getLocalizedString()).withIcon(FontAwesome.Icon.faw_clock_o).withIdentifier(20).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.menu_item_settings_label).withIcon(FontAwesome.Icon.faw_cog).withIdentifier(21).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.menu_item_about_label).withIcon(FontAwesome.Icon.faw_cog).withIdentifier(22).withSelectable(false)

//                        new SwitchDrawerItem().withName("Switch").withIcon(Octicons.Icon.oct_tools).withChecked(true).withOnCheckedChangeListener(onCheckedChangeListener),
//                        new SwitchDrawerItem().withName("Switch2").withIcon(Octicons.Icon.oct_tools).withChecked(true).withOnCheckedChangeListener(onCheckedChangeListener),
//                        new ToggleDrawerItem().withName("Toggle").withIcon(Octicons.Icon.oct_tools).withChecked(true).withOnCheckedChangeListener(onCheckedChangeListener)
                ) // add the items we want to use with our Drawer
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        //check if the drawerItem is set.
                        //there are different reasons for the drawerItem to be null
                        //--> click on the header
                        //--> click on the footer
                        //those items don't contain a drawerItem

                        if (drawerItem != null) {
                            Intent intent = null;
                            if (drawerItem.getIdentifier() == 1) {
                                intent = new Intent(BaseActivity.this, SongListActivity.class);
                            } else if (drawerItem.getIdentifier() == 2) {
                                intent = new Intent(BaseActivity.this, ArtistListActivity.class);
                            } else if (drawerItem.getIdentifier() == 3) {
                                intent = new Intent(BaseActivity.this, AlbumListActivity.class);
                            } else if (drawerItem.getIdentifier() == 4) {
                                intent = new Intent(BaseActivity.this, GenreListActivity.class);
                            } else if (drawerItem.getIdentifier() == 5) {
                                intent = new Intent(BaseActivity.this, YearListActivity.class);
                            } else if (drawerItem.getIdentifier() == 6) {
                                intent = new Intent(BaseActivity.this, AlbumListActivity.class);
                                intent.putExtra(AlbumViewDialog.AlbumsSortOrder.class.getName(), AlbumViewDialog.AlbumsSortOrder.__new.name());
                            } else if (drawerItem.getIdentifier() == 7) {
                                intent = new Intent(BaseActivity.this, RandomplayActivity.class);
                            } else if (drawerItem.getIdentifier() == 8) {
                                intent = new Intent(BaseActivity.this, PlaylistsActivity.class);
                            } else if (drawerItem.getIdentifier() == 9) {
                                intent = new Intent(BaseActivity.this, MusicFolderListActivity.class);
                            } else if (drawerItem.getIdentifier() == 10) {
                                intent = new Intent(BaseActivity.this, RadioListActivity.class);
                            } else if (drawerItem.getIdentifier() == 11) {
                                intent = new Intent(BaseActivity.this, FavoriteListActivity.class);
                            } else if (drawerItem.getIdentifier() == 12) {
                                intent = new Intent(BaseActivity.this, ApplicationListActivity.class);
                            } else if (drawerItem.getIdentifier() == 20) {
                                intent = new Intent(BaseActivity.this, AlarmsActivity.class);
                            } else if (drawerItem.getIdentifier() == 21) {
                                intent = new Intent(BaseActivity.this, SettingsActivity.class);
                            } else if (drawerItem.getIdentifier() == 22) {
                                intent = new Intent(BaseActivity.this, AboutDialog.class);
                            }

                            if (intent != null) {
                                BaseActivity.this.startActivity(intent);
                            }
                        }

                        return false;
                    }
                })
                .withOnDrawerNavigationListener(new Drawer.OnDrawerNavigationListener() {
                    @Override
                    public boolean onNavigationClickListener(View clickedView) {
                        //this method is only called if the Arrow icon is shown. The hamburger is automatically managed by the MaterialDrawer
                        //if the back arrow is shown. close the activity
                        BaseActivity.this.finish();
                        //return true if we have consumed the event
                        return true;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .withShowDrawerOnFirstLaunch(true)
                .build();


        //only set the active selection or active profile if we do not recreate the activity
//        if (savedInstanceState == null) {
//             set the selection to the item with the identifier 11
//            navigationDrawer.setSelection(21, false);
//
//            set the active profile
//            headerResult.setActiveProfile(profile3);
//        }

        //navigationDrawer.updateBadge(4, new StringHolder(10 + ""));

    }

    private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
            Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onCheckedChanged");
            if (drawerItem instanceof Nameable) {
                Log.i("material-drawer", "DrawerItem: " + ((Nameable) drawerItem).getName() + " - toggleChecked: " + isChecked);
            } else {
                Log.i("material-drawer", "toggleChecked: " + isChecked);
            }
        }
    };

    public void onEventMainThread(HandshakeComplete event) {
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : onEventMainThread");
        /**
        int[] icons = new int[]{
                R.drawable.ic_artists,
                R.drawable.ic_albums, R.drawable.ic_songs,
                R.drawable.ic_genres, R.drawable.ic_years, R.drawable.ic_new_music,
                R.drawable.ic_music_folder, R.drawable.ic_random,
                R.drawable.ic_playlists, R.drawable.ic_internet_radio,
                R.drawable.ic_favorites, R.drawable.ic_my_apps
        };

        String[] items = getResources().getStringArray(R.array.home_items);

        if (getService() != null) {
            mCanFavorites = event.canFavourites;
            mCanMusicfolder = event.canMusicFolders;
            mCanMyApps = event.canMyApps;
            mCanRandomplay = event.canRandomPlay;
        }

        List<IconRowAdapter.IconRow> rows = new ArrayList<IconRowAdapter.IconRow>(MY_APPS + 1);
        for (int i = ARTISTS; i <= MY_APPS; i++) {
            if (i == MUSIC_FOLDER && !mCanMusicfolder) {
                continue;
            }

            if (i == RANDOM_MIX && !mCanRandomplay) {
                continue;
            }

            if (i == FAVORITES && !mCanFavorites) {
                continue;
            }

            if (i == MY_APPS && !mCanMyApps) {
                continue;
            }

            rows.add(new IconRowAdapter.IconRow(i, items[i], icons[i]));
        }

        listView.setAdapter(new IconRowAdapter(this, rows));
        listView.setOnItemClickListener(onHomeItemClick);

        // Show a tip about volume controls, if this is the first time this app
        // has run. TODO: Add more robust and general 'tips' functionality.
        PackageInfo pInfo;
        try {
            final SharedPreferences preferences = getSharedPreferences(Preferences.NAME,
                    0);

            pInfo = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_META_DATA);
            if (preferences.getLong("lastRunVersionCode", 0) == 0) {
                new TipsDialog().show(getSupportFragmentManager(), "TipsDialog");
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong("lastRunVersionCode", pInfo.versionCode);
                editor.commit();
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Nothing to do, don't crash.
        }
        */
    }

    private void createPlayerHeader(){
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : createPlayerHeader");
        navigationDrawerHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .addProfiles(
//                        profile5,
                        new ProfileSettingDrawerItem().withName("Manage Players").withIcon(GoogleMaterial.Icon.gmd_settings).withIdentifier(200)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {

                        //sample usage of the onProfileChanged listener
                        //if the clicked item has the identifier 1 add a new profile ;)
                        if (profile instanceof IDrawerItem && ((IDrawerItem) profile).getIdentifier() == 200) {
                            Intent intent = new Intent(BaseActivity.this, PlayerListActivity.class);
                            BaseActivity.this.startActivity(intent);
                        } else {
                            Log.d("profile-click", String.valueOf(profile.getIdentifier()));

                            List<Player> players = getService().getPlayers();
                            for (int i = 0; i < players.size(); i++) {
                                if ((int) players.get(i).getIdAsLong() == profile.getIdentifier() && (int) getService().getActivePlayer().getIdAsLong() != profile.getIdentifier()) {
                                    getService().setActivePlayer(players.get(i));
                                }
                            }
                        }
                        //false if you have not consumed the event and it should close the drawer
                        return false;
                    }
                })
                .build();
    }

    private void addPlayersToMenu(@NonNull ISqueezeService service){
        Log.d("function-debug", "uk.org.ngo.squeezer BaseActivity : addPlayersToMenu");
        // Create the AccountHeader

         Log.d("players", String.valueOf(service.getPlayers()));

         List<Player> players = service.getPlayers();
        if (_savedInstanceState == null) {
             for (int i = 0; i < players.size(); i++) {

                 String name = players.get(i).getName();
                 String ip = players.get(i).getIp();

                 TextDrawable image = TextDrawable.builder()
                         .beginConfig()
                            .width(380)  // width in px
                            .height(380) // height in px
                         .endConfig()
                         .buildRound(String.valueOf(name.charAt(0)), R.color.squeezer_main);

                 IProfile newProfile = new ProfileDrawerItem()
                         .withNameShown(true)
                         .withName(name)
                         .withEmail(ip)
                         .withIcon(image)
                         .withIdentifier((int) players.get(i).getIdAsLong());

                 if (navigationDrawerHeader.getProfiles() != null) {
                    //we know that there are 2 setting elements. set the new profile above them ;)
                    navigationDrawerHeader.addProfile(newProfile, navigationDrawerHeader.getProfiles().size() - 1);
                 } else {
                    navigationDrawerHeader.addProfiles(newProfile);
                 }

                 if (players.get(i).getConnected()) {
                    navigationDrawerHeader.setActiveProfile(newProfile);
                 }
             }
        }
    }

}
