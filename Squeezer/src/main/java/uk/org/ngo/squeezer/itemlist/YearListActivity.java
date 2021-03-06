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

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Year;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class YearListActivity extends BaseListActivity<Year> {

    @Override
    public ItemView<Year> createItemView() {
        return new YearView(this);
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        service.years(start, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavigationDrawer(savedInstanceState);
        if (savedInstanceState == null) {
            // set the selection to the item with the identifier 11
            navigationDrawer.setSelection(5, false);
        }
        getSupportActionBar().setTitle(R.string.home_item_years);
    }

    public static void show(Context context) {
        final Intent intent = new Intent(context, YearListActivity.class);
        context.startActivity(intent);
    }

}
