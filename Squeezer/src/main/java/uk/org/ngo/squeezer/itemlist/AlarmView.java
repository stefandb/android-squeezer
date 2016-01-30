/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.expandable.RecyclerItemViewHolder;
import uk.org.ngo.squeezer.framework.recyclerViewListAdapter;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.model.AlarmPlaylist;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.util.CompoundButtonWrapper;
import uk.org.ngo.squeezer.widget.AnimationEndListener;
import uk.org.ngo.squeezer.widget.UndoBarController;

public class AlarmView extends BaseItemView<Alarm> {
    private static final int ANIMATION_DURATION = 300;

    private final AlarmsActivity mActivity;
    private final Resources mResources;
    private final int mColorSelected;
    private final float mDensity;
    private final List<AlarmPlaylist> mAlarmPlaylists = new ArrayList<>();

    public AlarmView(AlarmsActivity activity) {
        super(activity);
        mActivity = activity;
        mResources = activity.getResources();
        mColorSelected = mResources.getColor(getActivity().getAttributeValue(R.attr.alarm_dow_selected));
        mDensity = mResources.getDisplayMetrics().density;
    }

    public String getQuantityString(int quantity) {
        return null;
    }

    @Override
    public RecyclerItemViewHolder getAdapterView(RecyclerItemViewHolder viewHolder, int position, Alarm item) {
        RecyclerItemViewHolder view = getAdapterView(viewHolder);

        bindView(viewHolder, position, item);

        return view;
    }

    @Override
    public int getListItemLayout(){
        return R.layout.list_item_alarm;
    }

    private RecyclerItemViewHolder getAdapterView(final RecyclerItemViewHolder viewHolder) {
//        AlarmViewHolder currentViewHolder =
//                (convertView != null && convertView.getTag() instanceof AlarmViewHolder)
//                        ? (AlarmViewHolder) convertView.getTag()
//                        : null;

//        if (currentViewHolder == null) {
//            convertView = getLayoutInflater().inflate(R.layout.list_item_alarm, parent, false);
//            final View alarmView = convertView;


            viewHolder.setIs24HourFormat(DateFormat.is24HourFormat(getActivity()));
            viewHolder.setTimeFormat(viewHolder.is24HourFormat ? "%02d:%02d" : "%d:%02d");
            String[] amPmStrings = new DateFormatSymbols().getAmPmStrings();
            viewHolder.setAm(amPmStrings[0]);
            viewHolder.setPm(amPmStrings[1]);
            viewHolder.setTime(R.id.time);
            viewHolder.setAmPm(R.id.am_pm);
            viewHolder.getAmPm();
            viewHolder.getAmPm().setVisibility(viewHolder.is24HourFormat ? View.GONE : View.VISIBLE);

//        int height = viewHolder.getItemView().findViewById(R.id.controllall).getHeight();
//
//        Log.d("height header", String.valueOf(height));

            viewHolder.setEnabled(R.id.enabled).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (getActivity().getService() != null) {
                        viewHolder.getAlarm().setEnabled(isChecked);
                        getActivity().getService().alarmEnable(viewHolder.getAlarm().getId(), isChecked);
                    }
                }
            });

            viewHolder.setRepeat(new CompoundButtonWrapper((CompoundButton) viewHolder.getItemView().findViewById(R.id.repeat)));
            viewHolder.getRepeat().setOncheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (getActivity().getService() != null) {
                        viewHolder.getAlarm().setRepeat(b);
                        getActivity().getService().alarmRepeat(viewHolder.getAlarm().getId(), b);
                        viewHolder.getDowHolder().setVisibility(b ? View.VISIBLE : View.GONE);
                    }
                }
            });
            viewHolder.getRepeat().getButton().setText(ServerString.ALARM_ALARM_REPEAT.getLocalizedString());
            viewHolder.setDelete(R.id.delete);
            viewHolder.setPlaylist(R.id.playlist);
            viewHolder.setDowHolder(R.id.dow);
            for (int day = 0; day < 7; day++) {
                ViewGroup dowButton = (ViewGroup) viewHolder.getDowHolder().getChildAt(day);
                final int finalDay = day;
                dowButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getActivity().getService() != null) {
                            final Alarm alarm = viewHolder.getAlarm();
                            boolean wasChecked = alarm.isDayActive(finalDay);
                            if (wasChecked) {
                                alarm.clearDay(finalDay);
                                getActivity().getService().alarmRemoveDay(alarm.getId(), finalDay);
                            } else {
                                alarm.setDay(finalDay);
                                getActivity().getService().alarmAddDay(alarm.getId(), finalDay);
                            }
                            setDowText(viewHolder, finalDay);
                        }
                    }
                });
                viewHolder.setDowTexts(day, (TextView) dowButton.getChildAt(0));
//                viewHolder.setDo[day] = (TextView) dowButton.getChildAt(0);
            }
            viewHolder.getDelete().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final AnimationSet animationSet = new AnimationSet(true);
                    animationSet.addAnimation(new ScaleAnimation(1F, 1F, 1F, 0.5F));
                    animationSet.addAnimation(new AlphaAnimation(1F, 0F));
                    animationSet.setDuration(ANIMATION_DURATION);
                    animationSet.setAnimationListener(new AnimationEndListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mActivity.getItemAdapter().removeItem(viewHolder.getPositionInt());
                            UndoBarController.show(getActivity(), ServerString.ALARM_DELETING.getLocalizedString(), new UndoListener(viewHolder.getPositionInt(), viewHolder.getAlarm()));
                        }
                    });

//                    alarmView.startAnimation(animationSet);
                }
            });
            viewHolder.getPlaylist().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    final AlarmPlaylist selectedAlarmPlaylist = mAlarmPlaylists.get(position);
                    final Alarm alarm = viewHolder.getAlarm();
                    if (getActivity().getService() != null &&
                            selectedAlarmPlaylist.getId() != null &&
                            !selectedAlarmPlaylist.getId().equals(alarm.getUrl())) {
                        alarm.setUrl(selectedAlarmPlaylist.getId());
                        getActivity().getService().alarmSetPlaylist(alarm.getId(), selectedAlarmPlaylist);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
//        }

        return viewHolder;
    }

    private void bindView(final RecyclerItemViewHolder viewHolder, final int position, final Alarm item) {
        long tod = item.getTod();
        int hour = (int) (tod / 3600);
        int minute = (int) ((tod / 60) % 60);
        int displayHour = hour;
        if (!viewHolder.is24HourFormat) {
            displayHour = displayHour % 12;
            if (displayHour == 0) displayHour = 12;
        }

        viewHolder.setPosition(position);
        viewHolder.setAlarm(item);
        viewHolder.getTime().setText(String.format(viewHolder.getTimeFormat(), displayHour, minute));
        viewHolder.getTime().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerFragment.show(getActivity().getSupportFragmentManager(), item, viewHolder.is24HourFormat(), getActivity().getThemeId() == R.style.AppTheme);
            }
        });
        viewHolder.getAmPm().setText(hour < 12 ? viewHolder.getAm() : viewHolder.getPm());
        viewHolder.getEnabled().setChecked(item.isEnabled());
        viewHolder.getRepeat().setChecked(item.isRepeat());
        if (!mAlarmPlaylists.isEmpty()) {
            viewHolder.getPlaylist().setAdapter(new AlarmPlaylistSpinnerAdapter());
            for (int i = 0; i < mAlarmPlaylists.size(); i++) {
                AlarmPlaylist alarmPlaylist = mAlarmPlaylists.get(i);
                if (alarmPlaylist.getId() != null && alarmPlaylist.getId().equals(item.getUrl())) {
                    viewHolder.getPlaylist().setSelection(i);
                    break;
                }
            }

        }

        viewHolder.getDowHolder().setVisibility(item.isRepeat() ? View.VISIBLE : View.GONE);
        for (int day = 0; day < 7; day++) {
            setDowText(viewHolder, day);
        }
    }

    private void setDowText(RecyclerItemViewHolder viewHolder, int day) {
        SpannableString text = new SpannableString(ServerString.getAlarmShortDayText(day));
        if (viewHolder.getAlarm().isDayActive(day)) {
            text.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), 0);
            text.setSpan(new ForegroundColorSpan(mColorSelected), 0, text.length(), 0);
            Drawable underline = mResources.getDrawable(R.drawable.underline);
            float textSize = (new Paint()).measureText(text.toString());
            underline.setBounds(0, 0, (int) (textSize * mDensity), (int) (1 * mDensity));
            viewHolder.getDowTexts()[day].setCompoundDrawables(null, null, null, underline);
        } else
            viewHolder.getDowTexts()[day].setCompoundDrawables(null, null, null, null);
        viewHolder.getDowTexts()[day].setText(text);
    }

    @Override
    public boolean isSelectable(Alarm item) {
        return false;
    }

    @Override
    public void onItemSelected(int index, Alarm item) {
        //TODO-stefan check
//        super.onItemSelected(index, item);
    }

    public void setAlarmPlaylists(List<AlarmPlaylist> alarmPlaylists) {
        String currentCategory = null;
        mAlarmPlaylists.clear();
        for (AlarmPlaylist alarmPlaylist : alarmPlaylists) {
            if (!alarmPlaylist.getCategory().equals(currentCategory)) {
                AlarmPlaylist categoryAlarmPlaylist = new AlarmPlaylist();
                categoryAlarmPlaylist.setCategory(alarmPlaylist.getCategory());
                mAlarmPlaylists.add(categoryAlarmPlaylist);
            }
            mAlarmPlaylists.add(alarmPlaylist);
            currentCategory = alarmPlaylist.getCategory();
        }
    }

    public static class TimePickerFragment extends TimePickerDialog implements TimePickerDialog.OnTimeSetListener {
        BaseListActivity activity;
        Alarm alarm;

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            activity = (BaseListActivity) getActivity();
            alarm = getArguments().getParcelable("alarm");
            setOnTimeSetListener(this);
            return super.onCreateDialog(savedInstanceState);
        }

        public static void show(FragmentManager manager, Alarm alarm, boolean is24HourFormat, boolean dark) {
            long tod = alarm.getTod();
            int hour = (int) (tod / 3600);
            int minute = (int) ((tod / 60) % 60);

            TimePickerFragment fragment = new TimePickerFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable("alarm", alarm);
            fragment.setArguments(bundle);
            fragment.initialize(fragment, hour, minute, is24HourFormat);
            fragment.setThemeDark(dark);
            fragment.show(manager, TimePickerFragment.class.getSimpleName());
        }

        @Override
        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
            if (activity.getService() != null) {
                int time = (hourOfDay * 60 + minute) * 60;
                alarm.setTod(time);
                activity.getService().alarmSetTime(alarm.getId(), time);
                activity.getItemAdapter().notifyDataSetChanged();
            }
        }
    }

    private class AlarmPlaylistSpinnerAdapter extends ArrayAdapter<AlarmPlaylist> {

        public AlarmPlaylistSpinnerAdapter() {
            super(getActivity(), android.R.layout.simple_spinner_dropdown_item, mAlarmPlaylists);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return (mAlarmPlaylists.get(position).getId() != null);
        }

        //TODO-stefan mischien toch fixen
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//           return Util.getSpinnerItemView(getActivity(), convertView, getItem(position).getName());
//        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (!isEnabled(position)) {
                FrameLayout view = (FrameLayout) getActivity().getLayoutInflater().inflate(R.layout.alarm_playlist_category_dropdown_item, parent, false);
                CheckedTextView spinnerItemView = (CheckedTextView) view.findViewById(R.id.text);
                spinnerItemView.setText(getItem(position).getCategory());
                spinnerItemView.setTypeface(spinnerItemView.getTypeface(), Typeface.BOLD);
                // Hide the checkmark for headings.
                spinnerItemView.setCheckMarkDrawable(new ColorDrawable(Color.TRANSPARENT));
                return view;
            } else {
                FrameLayout view = (FrameLayout) getActivity().getLayoutInflater().inflate(R.layout.alarm_playlist_dropdown_item, parent, false);
                TextView spinnerItemView = (TextView) view.findViewById(R.id.text);
                spinnerItemView.setText(getItem(position).getName());
                return view;
            }
        }
    }

    private class UndoListener implements UndoBarController.UndoListener {
        private final int position;
        private final Alarm alarm;

        public UndoListener(int position, Alarm alarm) {
            this.position = position;
            this.alarm = alarm;
        }

        @Override
        public void onUndo() {
            mActivity.getItemAdapter().insertItem(position, alarm);
        }

        @Override
        public void onDone() {
            if (mActivity.getService() != null) {
                mActivity.getService().alarmDelete(alarm.getId());
            }
        }
    }
}
