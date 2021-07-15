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

package uk.org.ngo.squeezer.service;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.model.MenuStatusMessage;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.event.ActivePlayerChanged;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.PlayersChanged;

public class ConnectionState {

    private static final String TAG = "ConnectionState";

    ConnectionState(@NonNull EventBus eventBus) {
        mEventBus = eventBus;
        mHomeMenuHandling = new HomeMenuHandling(eventBus);
    }

    private final EventBus mEventBus;
    private final HomeMenuHandling mHomeMenuHandling;

    public final static String MEDIA_DIRS = "mediadirs";

    // Connection state machine
    @IntDef({MANUAL_DISCONNECT, DISCONNECTED, CONNECTION_STARTED, CONNECTION_FAILED, CONNECTION_COMPLETED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConnectionStates {}
    /** User disconnected */
    public static final int MANUAL_DISCONNECT = 0;
    /** Ordinarily disconnected from the server. */
    public static final int DISCONNECTED = 1;
    /** A connection has been started. */
    public static final int CONNECTION_STARTED = 2;
    /** The connection to the server did not complete. */
    public static final int CONNECTION_FAILED = 3;
    /** The connection to the server completed, the handshake can start. */
    public static final int CONNECTION_COMPLETED = 4;

    @ConnectionStates
    private volatile int mConnectionState = DISCONNECTED;

    /** Milliseconds since boot of latest auto connect */
    private volatile long autoConnect;

    /** Minimum milliseconds between automatic connection */
    private static final long AUTO_CONNECT_INTERVAL = 60_000;

    /** Map Player IDs to the {@link uk.org.ngo.squeezer.model.Player} with that ID. */
    private final Map<String, Player> mPlayers = new ConcurrentHashMap<>();

    /** The active player (the player to which commands are sent by default). */
    private final AtomicReference<Player> mActivePlayer = new AtomicReference<>();

    private final AtomicReference<String> serverVersion = new AtomicReference<>();

    private final AtomicReference<String[]> mediaDirs = new AtomicReference<>();

    public boolean canAutoConnect() {
        return (mConnectionState == DISCONNECTED || mConnectionState == CONNECTION_FAILED)
                && ((SystemClock.elapsedRealtime() - autoConnect) > AUTO_CONNECT_INTERVAL);
    }

    public void setAutoConnect() {
        this.autoConnect = SystemClock.elapsedRealtime();
    }

    /**
     * Sets a new connection state, and posts a sticky
     * {@link uk.org.ngo.squeezer.service.event.ConnectionChanged} event with the new state.
     *
     * @param connectionState The new connection state.
     */
    void setConnectionState(@ConnectionStates int connectionState) {
        Log.i(TAG, "setConnectionState(" + mConnectionState + " => " + connectionState + ")");
        updateConnectionState(connectionState);
        mEventBus.postSticky(new ConnectionChanged(connectionState));
    }

    void setConnectionError(ConnectionError connectionError) {
        Log.i(TAG, "setConnectionError(" + mConnectionState + " => " + connectionError.name() + ")");
        updateConnectionState(CONNECTION_FAILED);
        mEventBus.postSticky(new ConnectionChanged(connectionError));
    }

    private void updateConnectionState(@ConnectionStates int connectionState) {
        // Clear data if we were previously connected
        if (isConnected() && !isConnected(connectionState)) {
            mEventBus.removeAllStickyEvents();
            setServerVersion(null);
            mPlayers.clear();
            setActivePlayer(null);
        }
        mConnectionState = connectionState;
    }

    public void setPlayers(Map<String, Player> players) {
        mPlayers.clear();
        mPlayers.putAll(players);
        mEventBus.postSticky(new PlayersChanged());
    }

    Player getPlayer(String playerId) {
        return mPlayers.get(playerId);
    }

    public Map<String, Player> getPlayers() {
        return mPlayers;
    }

    public Player getActivePlayer() {
        return mActivePlayer.get();
    }

    void setActivePlayer(Player player) {
        mActivePlayer.set(player);
        mEventBus.post(new ActivePlayerChanged(player));
    }

    void setServerVersion(String version) {
        if (Util.atomicReferenceUpdated(serverVersion, version)) {
            if (version != null && mConnectionState == CONNECTION_COMPLETED) {
                HandshakeComplete event = new HandshakeComplete(getServerVersion());
                Log.i(TAG, "Handshake complete: " + event);
                mEventBus.postSticky(event);
            }
        }
    }

    void setMediaDirs(String[] mediaDirs) {
        this.mediaDirs.set(mediaDirs);
    }

    public HomeMenuHandling getHomeMenuHandling() {
        return mHomeMenuHandling;
    }

//    For menu updates sent from LMS, handling of archived nodes needs testing!
    void menuStatusEvent(MenuStatusMessage event) {
        if (event.playerId.equals(getActivePlayer().getId())) {
            mHomeMenuHandling.handleMenuStatusEvent(event);
        }
    }

    Map<String, Set<String>> itemsInFolders = new HashMap<>();
    Map<String, Set<String>> playeditemsInFolders = new HashMap<>();

//
    public void addToSetOfIDs(String folderID, Set<String> stringSetOfFifty) {
//        called from SlimDelegate: mClient.getConnectionState().addToSetOfIDs(stringSetOfFifty);
        Log.d(TAG, "addToSetOfIDs: BEN with stringSetOfFifty.size(): " + stringSetOfFifty.size());
        BiFunction<Set<String>, Set<String>, Set<String>> biFunction = (set1, set2) -> set1 == null ? set1 : Stream.concat(set1.stream(), set2.stream())
                .collect(Collectors.toSet());
        itemsInFolders.merge(folderID, stringSetOfFifty, biFunction);
        Log.d(TAG, "addToSetOfIDs: BEN itemsInFolders folderID: " + folderID + " with size: " + itemsInFolders.get(folderID).size());
    }

    public void addToSetOfPlayedIDs(String folderID, Set<String> playedTracks) {
        Log.d(TAG, "addToSetOfPlayedIDs: BEN add this set of tracks to set of played: " + playedTracks);
        BiFunction<Set<String>, Set<String>, Set<String>> biFunction = (set1, set2) -> set1 == null ? set1 : Stream.concat(set1.stream(), set2.stream()).collect(Collectors.toSet());
        playeditemsInFolders.merge(folderID, playedTracks, biFunction);
        Log.d(TAG, "addToSetOfPlayedIDs: BEN played tracks");
    }


    String getServerVersion() {
        return serverVersion.get();
    }

    String[] getMediaDirs() {
        return mediaDirs.get();
    }

    /**
     * @return True if the socket connection to the server has completed.
     */
    boolean isConnected() {
        return isConnected(mConnectionState);
    }

    /**
     * @return True if the socket connection to the server has completed.
     */
    static boolean isConnected(@ConnectionStates int connectionState) {
        return connectionState == CONNECTION_COMPLETED;
    }

    /**
     * @return True if the socket connection to the server has started, but not yet
     *     completed (successfully or unsuccessfully).
     */
    boolean isConnectInProgress() {
        return isConnectInProgress(mConnectionState);
    }

    /**
     * @return True if the socket connection to the server has started, but not yet
     *     completed (successfully or unsuccessfully).
     */
    static boolean isConnectInProgress(@ConnectionStates int connectionState) {
        return connectionState == CONNECTION_STARTED;
    }

    @NonNull
    @Override
    public String toString() {
        return "ConnectionState{" +
                "mConnectionState=" + mConnectionState +
                ", serverVersion=" + serverVersion +
                '}';
    }
}
