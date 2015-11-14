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


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListAdapter;
import android.widget.ProgressBar;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.behavior.TransformingToolbarBehavior;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.util.ImageFetcher;
import uk.org.ngo.squeezer.util.RetainFragment;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A generic base class for an activity to list items of a particular SqueezeServer data type. The
 * data type is defined by the generic type argument, and must be an extension of {@link Item}. You
 * must provide an {@link ItemView} to provide the view logic used by this activity. This is done by
 * implementing {@link #createItemView()}.
 * <p>
 * When the activity is first created ({@link #onCreate(Bundle)}), an empty {@link ItemAdapter}
 * is created using the provided {@link ItemView}. See {@link ItemListActivity} for see details of
 * ordering and receiving of list items from SqueezeServer, and handling of item selection.
 *
 * @param <T> Denotes the class of the items this class should list
 *
 * @author Kurt Aaholst
 */
public abstract class BaseListActivity<T extends Item> extends ItemListActivity implements IServiceItemListCallback<T> {

    private static final String TAG = BaseListActivity.class.getName();

    /**
     * Tag for first visible position in mRetainFragment.
     */
    private static final String TAG_POSITION = "position";


    /**
     * Tag for itemAdapter in mRetainFragment.
     */
    public static final String TAG_ADAPTER = "adapter";

//    private AbsListView mListView;
    private RecyclerView mrecyclerView;

    private recyclerViewListAdapter<T> itemAdapter;
//    private recyclerViewListAdapter recycleritemAdapter;
    /**
     * Progress bar (spinning) while items are loading.
     */
    private ProgressBar loadingProgress;

    private View controls_container;

    /**
     * Fragment to retain information across the activity lifecycle.
     */
    private RetainFragment mRetainFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : onCreate");
        super.onCreate(savedInstanceState);

        mRetainFragment = RetainFragment.getInstance(TAG, getSupportFragmentManager());

        setContentView(getContentView());
//        mListView = checkNotNull((AbsListView) findViewById(R.id.item_list),
//                "getContentView() did not return a view containing R.id.item_list");

        mrecyclerView = checkNotNull((RecyclerView) findViewById(R.id.item_list),
                "getContentView() did not return a view containing R.id.item_list");
        mrecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadingProgress = checkNotNull((ProgressBar) findViewById(R.id.loading_progress),
                "getContentView() did not return a view containing R.id.loading_progress");

//        mrecyclerView.addOnItemTouchListener();
        mrecyclerView.setOnScrollListener(new RecyclerScrollListener());

        registerForContextMenu(mrecyclerView);
        mrecyclerView.setLongClickable(true);

        mrecyclerView.addOnItemTouchListener(
            new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    Log.d("debug", "klik position " + String.valueOf(position));
                    getItemAdapter().onItemSelected(position);
                }
            })
        );




//        mListView.setOnItemClickListener(new OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                getItemAdapter().onItemSelected(position);
//            }
//        });

//        mListView.setRecyclerListener(new RecyclerListener() {
//            @Override
//            public void onMovedToScrapHeap(View view) {
//                 Release strong reference when a view is recycled
//                final ImageView imageView = (ImageView) view.findViewById(R.id.icon);
//                if (imageView != null) {
//                    imageView.setImageBitmap(null);
//                }
//            }
//        });

//        mListView.setLongClickable(true);

        // Delegate context menu creation to the adapter.
//        mListView.setOnCreateContextMenuListener(getItemAdapter());
//
//        mListView.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                v.showContextMenu();
//                return false;
//            }
//        });

//        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//
//            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
//                                           int pos, long id) {
//                arg0.showContextMenu();
//
//                Log.d("click search", "baselistactivity | oncreate mlistview");
//                return true;
//            }
//
//        });



        controls_container = findViewById(R.id.controls_container);
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) controls_container.getLayoutParams();
        TransformingToolbarBehavior transformingToolbarBehavior = (TransformingToolbarBehavior) layoutParams.getBehavior();
        transformingToolbarBehavior.setToolbarChangeListener(new TransformingToolbarBehavior.ToolbarChangeListener() {
            @Override
            public void onToolbarCollapse() {
                hideToolbar();
            }

            @Override
            public void onToolbarShown() {
                showToolbar();
            }
        });
    }

    private void hideToolbar() {

        if (controls_container.getVisibility() == View.INVISIBLE) {
            return;
        }

        controls_container.setVisibility(View.INVISIBLE);

//        int cx = controls_container.getWidth() / 2;
//        int cy = controls_container.getHeight() / 2;
//
////        mFloatingActionButton.setVisibility(View.INVISIBLE);
////        mFloatingActionButton.setX(mBottomToolbar.getX() + (mBottomToolbar.getWidth() / 2) - (mFloatingActionButton.getWidth() / 2));
////        mFloatingActionButton.setY(mBottomToolbar.getY() + (mBottomToolbar.getHeight() / 2) - (mFloatingActionButton.getHeight() / 2));
//
//        int initialRadius = controls_container.getWidth();
////        int endRadius = mFloatingActionButton.getWidth();
//
//        Animator anim = ViewAnimationUtils.createCircularReveal(controls_container, cx, cy, initialRadius, initialRadius);
//
//        anim.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                super.onAnimationEnd(animation);
//                controls_container.setVisibility(View.INVISIBLE);
//
//                mFloatingActionButton.setVisibility(View.VISIBLE);
//                mFloatingActionButton.animate()
//                        .x(mInitialFabX)
//                        .y(mInitialFabY)
//                        .setInterpolator(new LinearOutSlowInInterpolator())
//                        .start();
//            }
//        });
//
//        anim.start();
    }

    private void showToolbar() {

        if (controls_container.getVisibility() != View.INVISIBLE) {
            return;
        }


        controls_container.setVisibility(View.VISIBLE);


//        float x = controls_container.getX() + (controls_container.getWidth() / 2) - (mFloatingActionButton.getWidth() / 2);
//        float y = controls_container.getY() + (controls_container.getHeight() / 2) - (mFloatingActionButton.getHeight() / 2);

//        mFloatingActionButton.animate()
//                .x(x)
//                .y(y)
//                .setInterpolator(new LinearOutSlowInInterpolator())
//                .withEndAction(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        int initialRadius = mFloatingActionButton.getWidth();
//                        int endRadius = mBottomToolbar.getWidth();
//
//                        mBottomToolbar.setVisibility(View.VISIBLE);
//                        mFloatingActionButton.setVisibility(View.INVISIBLE);
//
//                        int cx = mBottomToolbar.getWidth() / 2;
//                        int cy = mBottomToolbar.getHeight() / 2;
//                        Animator anim = ViewAnimationUtils.createCircularReveal(mBottomToolbar, cx, cy, initialRadius, endRadius);
//                        anim.addListener(new AnimatorListenerAdapter() {
//                            @Override
//                            public void onAnimationEnd(Animator animation) {
//                                super.onAnimationEnd(animation);
//                            }
//                        });
//
//                        anim.start();
//                    }
//                })
//                .start();
    }



    public void onEventMainThread(HandshakeComplete event) {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : onEventMainThread");
        maybeOrderVisiblePages(mrecyclerView);
        setAdapter();
    }

    /**
     * Returns the ID of a content view to be used by this list activity.
     * <p>
     * The content view must contain a {@link AbsListView} with the id {@literal item_list} and a
     * {@link ProgressBar} with the id {@literal loading_progress} in order to be valid.
     *
     * @return The ID
     */
    protected int getContentView() {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : getContentView");
        return R.layout.item_list;
    }

    /**
     * @return A new view logic to be used by this activity
     */
    abstract protected ItemView<T> createItemView();

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : onContextItemSelected");
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) menuItem.getMenuInfo();

        // If menuInfo is null we have a sub menu, we expect the adapter to have stored the position
        if (menuInfo == null)
            return itemAdapter.doItemContext(menuItem);
        else
            return itemAdapter.doItemContext(menuItem, menuInfo.position);
    }

    /**
     * Set our adapter on the list view.
     * <p>
     * This can't be done in {@link #onCreate(android.os.Bundle)} because getView might be called
     * before the handshake is complete, so we need to delay it.
     * <p>
     * However when we set the adapter after onCreate the list is scrolled to top, so we retain the
     * visible position.
     * <p>
     * Call this method after the handshake is complete.
     */
    private void setAdapter() {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : setAdapter");
        // setAdapter is not defined for AbsListView before API level 11, but
        // it is for concrete implementations, so we call it by reflection
        try {
            Method method = mrecyclerView.getClass().getMethod("setAdapter",  RecyclerView.Adapter.class);
            method.invoke(mrecyclerView, getItemAdapter());
        } catch (Exception e) {
            Log.e(getTag(), "Error calling 'setAdapter'", e);
        }

        Integer position = (Integer) mRetainFragment.get(TAG_POSITION);
        if (position != null) {
            //TODO-stefan functies ombouwen naar nieuwe logica en recylerview
            if (mrecyclerView instanceof RecyclerView) {
//                ((RecyclerView) mrecyclerView).setSelectionFromTop(position, 0);
            } else {
//                mrecyclerView.setSelection(position);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : onSaveInstanceState");
        super.onSaveInstanceState(outState);
        saveVisiblePosition();
    }

    /**
     * Store the first visible position of {@link #mrecyclerView}, in the {@link #mRetainFragment}, so
     * we can later retrieve it.
     *
     * @see android.widget.AbsListView#getFirstVisiblePosition()
     */
    private void saveVisiblePosition() {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : saveVisiblePosition");
        mRetainFragment.put(TAG_POSITION, mrecyclerView.getVerticalScrollbarPosition());
    }

    /**
     * @return The current {@link ItemAdapter}'s {@link ItemView}
     */
    public ItemView<T> getItemView() {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : getItemView");
        return getItemAdapter().getItemView();
    }

    /**
     * @return The current {@link ItemAdapter}, creating it if necessary.
     */
    public recyclerViewListAdapter<T> getItemAdapter() {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : getItemAdapter");
        if (itemAdapter == null) {
            //noinspection unchecked
            itemAdapter = (recyclerViewListAdapter<T>) mRetainFragment.get(TAG_ADAPTER);
            if (itemAdapter == null) {
                itemAdapter = createItemListAdapter(createItemView());
                mRetainFragment.put(TAG_ADAPTER, itemAdapter);
            } else {
                // We have just retained the item adapter, we need to create a new
                // item view logic, cause it holds a reference to the old activity
                itemAdapter.setItemView(createItemView());
                // Update views with the count from the retained item adapter
                itemAdapter.onCountUpdated();
            }
        }

        return itemAdapter;
    }

    @Override
    protected void clearItemAdapter() {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : clearItemAdapter");
        // TODO: This should be removed in favour of showing a progress spinner in the actionbar.
        mrecyclerView.setVisibility(View.GONE);
        loadingProgress.setVisibility(View.VISIBLE);

        getItemAdapter().clear();
    }

    /**
     * @return The {@link AbsListView} used by this activity
     */
    public RecyclerView getListView() {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : getListView");
        return mrecyclerView;
    }

    protected recyclerViewListAdapter<T> createItemListAdapter(ItemView<T> itemView) {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : createItemListAdapter");
        return new recyclerViewListAdapter<T>(this, itemView);
    }

    public void onItemsReceived(final int count, final int start, final List<T> items) {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : onItemsReceived");
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : onItemsReceived parameters");
        Log.d("function-debug", "onItemsReceived items " + items.toString());
        Log.d("function-debug", "onItemsReceived start " + start);
        Log.d("function-debug", "onItemsReceived count " + count);

        super.onItemsReceived(count, start, items.size());

        getUIThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                mrecyclerView.setVisibility(View.VISIBLE);
                loadingProgress.setVisibility(View.GONE);
                getItemAdapter().update(count, start, items);

//                recycleritemAdapter.additems(count, start, items);
            }
        });
    }

    @Override
    public void onItemsReceived(int count, int start, Map<String, String> parameters, List<T> items, Class<T> dataType) {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : onItemsReceived");
        onItemsReceived(count, start, items);
    }

    @Override
    public Object getClient() {
        Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity : getClient");
        return this;
    }



    protected class ScrollListener extends ItemListActivity.ScrollListener {

        ScrollListener() {
            super();
        }

        /**
         * Pauses cache disk fetches if the user is flinging the list, or if their finger is still
         * on the screen.
         */
        @Override
        public void onScrollStateChanged(AbsListView listView, int scrollState) {
            super.onScrollStateChanged(listView, scrollState);

            if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING ||
                    scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                ImageFetcher.getInstance(BaseListActivity.this).setPauseWork(true);
            } else {
                ImageFetcher.getInstance(BaseListActivity.this).setPauseWork(false);
            }
        }
    }

    protected class RecyclerScrollListener extends ItemListActivity.RecyclerScrollListener{

        public RecyclerScrollListener(){
            super();
            Log.d("function-debug", "uk.org.ngo.squeezer.framework BaseListActivity - RecyclerScrollListener : RecyclerScrollListener");
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_FLING ||
                    newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                ImageFetcher.getInstance(BaseListActivity.this).setPauseWork(true);
            } else {
                ImageFetcher.getInstance(BaseListActivity.this).setPauseWork(false);
            }
        }
    }

    public BaseListActivity<T> getActivity(){
        return BaseListActivity.this;
    }
}
