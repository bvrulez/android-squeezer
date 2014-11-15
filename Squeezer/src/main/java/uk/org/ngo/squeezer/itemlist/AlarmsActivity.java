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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.DisabledItemListAdapter;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.model.AlarmPlaylist;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerPref;
import uk.org.ngo.squeezer.service.IServicePlayerPrefCallback;
import uk.org.ngo.squeezer.service.IServicePlayersCallback;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.util.CompoundButtonWrapper;
import uk.org.ngo.squeezer.widget.UndoBarController;

public class AlarmsActivity extends BaseListActivity<Alarm> {
    private Player player;
    private AlarmView alarmView;
    private CompoundButtonWrapper alarmsEnabledButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        alarmsEnabledButton = new CompoundButtonWrapper((CompoundButton) findViewById(R.id.alarms_enabled));
        findViewById(R.id.all_alarms_desc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(AlarmsActivity.this, ServerString.ALARM_ALARMS_ENABLED_DESC.getLocalizedString(), Toast.LENGTH_LONG).show();
            }
        });
        findViewById(R.id.add_alarm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerFragment.show(getSupportFragmentManager(), DateFormat.is24HourFormat(AlarmsActivity.this));
            }
        });
        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlarmsSettingsActivity.show(AlarmsActivity.this);
            }
        });

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        alarmsEnabledButton.setOncheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getService().playerPref(PlayerPref.alarmsEnabled, isChecked ? "1" : "0");
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        UndoBarController.hide(this);
    }

    public static void show(Activity context) {
        final Intent intent = new Intent(context, AlarmsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    @Override
    protected int getContentView() {
        return R.layout.item_list_player_alarms;
    }

    @Override
    public ItemView<Alarm> createItemView() {
        alarmView = new AlarmView(this);
        return alarmView;
    }

    @Override
    protected ItemAdapter<Alarm> createItemListAdapter(ItemView<Alarm> itemView) {
        return new DisabledItemListAdapter<Alarm>(itemView, getImageFetcher());
    }

    @Override
    protected void registerCallback() {
        super.registerCallback();
        player = getService().getActivePlayer();
        getService().registerPlayerPrefCallback(playerPrefCallback);
        getService().registerPlayersCallback(playersCallback);
        getService().alarmPlaylists(alarmPlaylistsCallback);
    }

    @Override
    protected void orderPage(int start) {
        getService().alarms(start, this);
        if (start == 0) {
            alarmsEnabledButton.setEnabled(false);
            getService().playerPref(PlayerPref.alarmsEnabled);
        }
    }

    private List<AlarmPlaylist> alarmPlaylists = new ArrayList<AlarmPlaylist>();
    private final IServiceItemListCallback<AlarmPlaylist> alarmPlaylistsCallback = new IServiceItemListCallback<AlarmPlaylist>() {
        @Override
        public void onItemsReceived(final int count, final int start, Map<String, String> parameters, final List<AlarmPlaylist> items, Class<AlarmPlaylist> dataType) {
            alarmPlaylists.addAll(items);
            if (start + items.size() >= count) {
                getUIThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        alarmView.setAlarmPlaylists(alarmPlaylists);
                        getItemAdapter().notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public Object getClient() {
            return AlarmsActivity.this;
        }
    };

    private final IServicePlayerPrefCallback playerPrefCallback = new IServicePlayerPrefCallback() {
        @Override
        public void onPlayerPrefReceived(final PlayerPref playerPref, final String value) {
            getUIThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (playerPref == PlayerPref.alarmsEnabled) {
                        alarmsEnabledButton.setEnabled(true);
                        alarmsEnabledButton.setChecked(Integer.valueOf(value) > 0);
                    }
                }
            });
        }

        @Override
        public Object getClient() {
            return AlarmsActivity.this;
        }
    };

    private final IServicePlayersCallback playersCallback = new IServicePlayersCallback() {
        @Override
        public void onPlayersChanged(final List<Player> players, final Player activePlayer) {
            if (activePlayer != null && !activePlayer.equals(player)) {
                getUIThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        player = activePlayer;
                        clearAndReOrderItems();
                    }
                });
            }
        }

        @Override
        public Object getClient() {
            return AlarmsActivity.this;
        }
    };


    public static class TimePickerFragment extends TimePickerDialog implements TimePickerDialog.OnTimeSetListener {
        BaseListActivity activity;

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            activity = (BaseListActivity) getActivity();
            setOnTimeSetListener(this);
            return super.onCreateDialog(savedInstanceState);
        }

        public static void show(FragmentManager manager, boolean is24HourMode) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerFragment fragment = new TimePickerFragment();
            fragment.initialize(fragment, hour, minute, is24HourMode);
            fragment.setThemeDark(true);
            fragment.show(manager, TimePickerFragment.class.getSimpleName());
        }

        @Override
        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
            activity.getService().alarmAdd((hourOfDay * 60 + minute) * 60);
            // TODO add to list and animate the new alarm in
            activity.clearAndReOrderItems();
        }
    }

}
