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
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.google.common.collect.ImmutableList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.framework.RecyclerItemClickListener;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;

import static com.google.common.base.Preconditions.checkNotNull;

public class RandomplayActivity extends BaseActivity {

//    private ListView listView;
    private RecyclerView mrecyclerView;

    @StringDef({TRACKS, ALBUMS, CONTRIBUTORS, YEAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RandomplayType {}
    public static final String TRACKS = "tracks";
    public static final String ALBUMS = "albums";
    public static final String CONTRIBUTORS = "contributors";
    public static final String YEAR = "year";

    // Note: Must appear in the same order as R.array.randomplay_items
    ImmutableList<String> randomplayTypes = ImmutableList.of(
            TRACKS, ALBUMS, CONTRIBUTORS, YEAR);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_list);

        NavigationDrawer(savedInstanceState);
        if (savedInstanceState == null) {
            // set the selection to the item with the identifier 11
            navigationDrawer.setSelection(7, false);
        }
        getSupportActionBar().setTitle(R.string.home_item_random_mix);
        mrecyclerView = checkNotNull((RecyclerView) findViewById(R.id.item_list),
                "getContentView() did not return a view containing R.id.item_list");
        mrecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void onEventMainThread(HandshakeComplete event) {
        setRandomPlayList(getService());
    }

    private void setRandomPlayList(@NonNull final ISqueezeService service) {
        String[] values = getResources().getStringArray(R.array.randomplay_items);
        int[] icons = new int[values.length];
        Arrays.fill(icons, R.drawable.ic_random);

        // XXX: Implement the "Choose the genres that the random mix will be
        // drawn from" functionality.
        // icons[icons.length - 1] = R.drawable.ic_genres;
        mrecyclerView.setAdapter(new IconRowAdapter(this, values, icons));
        mrecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (position < randomplayTypes.size()) {
                            service.randomPlay(randomplayTypes.get(position));
                            NowPlayingActivity.show(RandomplayActivity.this);
                        }
                    }
            })
        );
    }

    static void show(Context context) {
        final Intent intent = new Intent(context, RandomplayActivity.class);
        context.startActivity(intent);
    }
}
