package uk.org.ngo.squeezer;

import uk.org.ngo.squeezer.framework.BaseActivity;

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
            // set the selection to the item with the identifier 11
            navigationDrawer.setSelection(22, false);
        }

        getSupportActionBar().setTitle(R.string.menu_item_about_label);
    }

}
