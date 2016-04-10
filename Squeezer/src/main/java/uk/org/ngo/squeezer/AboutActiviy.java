package uk.org.ngo.squeezer;

import uk.org.ngo.squeezer.framework.BaseActivity;

import com.mikepenz.aboutlibraries.LibTaskCallback;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.LibsConfiguration;
import com.mikepenz.aboutlibraries.entity.Library;
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * Created by Stefan on 7-2-2016.
 */
public class AboutActiviy extends BaseActivity {

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        NavigationDrawer(savedInstanceState);
        if (savedInstanceState == null) {
            navigationDrawer.setSelection(22, false);
        }

        getSupportActionBar().setTitle(R.string.menu_item_about_label);

//        .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)


        navigationDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //@style/AppTheme.Light.DarkActionBar
    }

    public LibTaskCallback getLibTaskCallback(){
        return libTaskCallback;
    }

    public LibsConfiguration.LibsUIListener getLibsUIListener(){
        return libsUIListener;
    }

    public LibsConfiguration.LibsListener getLibsListener(){
        return libsListener;
    }
    LibTaskCallback libTaskCallback = new LibTaskCallback() {
        @Override
        public void onLibTaskStarted() {
            Log.e("AboutLibraries", "started");
        }

        @Override
        public void onLibTaskFinished(FastItemAdapter fastItemAdapter) {
            Log.e("AboutLibraries", "finished");
        }
    };

    LibsConfiguration.LibsUIListener libsUIListener = new LibsConfiguration.LibsUIListener() {
        @Override
        public View preOnCreateView(View view) {
            return view;
        }

        @Override
        public View postOnCreateView(View view) {
            return view;
        }
    };

    LibsConfiguration.LibsListener libsListener = new LibsConfiguration.LibsListener() {
        @Override
        public void onIconClicked(View v) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/stefandb/android-squeezer"));
            startActivity(browserIntent);
        }

        @Override
        public boolean onLibraryAuthorClicked(View v, Library library) {
            return false;
        }

        @Override
        public boolean onLibraryContentClicked(View v, Library library) {
            return false;
        }

        @Override
        public boolean onLibraryBottomClicked(View v, Library library) {
            return false;
        }

        @Override
        public boolean onExtraClicked(View v, Libs.SpecialButton specialButton) {
            return false;
        }

        @Override
        public boolean onIconLongClicked(View v) {
            return false;
        }

        @Override
        public boolean onLibraryAuthorLongClicked(View v, Library library) {
            return false;
        }

        @Override
        public boolean onLibraryContentLongClicked(View v, Library library) {
            return false;
        }

        @Override
        public boolean onLibraryBottomLongClicked(View v, Library library) {
            return false;
        }
    };
}
