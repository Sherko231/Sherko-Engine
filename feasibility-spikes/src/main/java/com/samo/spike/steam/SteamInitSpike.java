package com.samo.spike.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamFriends;
import com.codedisaster.steamworks.SteamFriendsCallback;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamLibraryLoader;
import com.codedisaster.steamworks.SteamLibraryLoaderLwjgl3;
import com.codedisaster.steamworks.SteamNativeHandle;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUser;
import com.codedisaster.steamworks.SteamUserCallback;
import com.codedisaster.steamworks.SteamUserStats;
import com.codedisaster.steamworks.SteamUserStatsCallback;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SteamInitSpike {
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final long CALLBACK_PUMP_SLEEP_MILLIS = 50L;

    private SteamInitSpike() {

    }

    public static void main(String[] args) throws Exception {

        int timeoutSeconds = Integer.getInteger("spike.timeoutSeconds", DEFAULT_TIMEOUT_SECONDS);
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("spike.timeoutSeconds must be greater than zero");
        }

        SteamUser steamUser = null;
        SteamFriends steamFriends = null;
        SteamUserStats steamUserStats = null;
        boolean steamInitialized = false;
        AtomicBoolean callbackReceived = new AtomicBoolean(false);

        try {
            SteamLibraryLoader loader = new SteamLibraryLoaderLwjgl3();
            if (!SteamAPI.loadLibraries(loader)) {
                throw new IllegalStateException("Failed to load Steamworks4j native libraries. Ensure Steam is installed and the dependency natives can be loaded.");
            }

            SteamAPI.InitResult initResult = SteamAPI.initEx();
            if (initResult != SteamAPI.InitResult.OK) {
                throw new IllegalStateException("SteamAPI.initEx() failed with result: " + initResult + ". Make sure the Steam client is running and logged in.");
            }
            steamInitialized = true;

            steamUser = new SteamUser(new SteamUserCallback() {
            });
            steamFriends = new SteamFriends(new SteamFriendsCallback() {
            });
            steamUserStats = new SteamUserStats(new SteamUserStatsCallback() {
                @Override
                public void onNumberOfCurrentPlayersReceived(boolean success, int players) {

                    callbackReceived.set(true);
                    System.out.printf("Steam callback: onNumberOfCurrentPlayersReceived success=%s players=%d%n", success, players);

                }

                @Override
                public void onUserStatsReceived(long gameId, SteamID steamIDUser, SteamResult result) {

                    callbackReceived.set(true);
                    System.out.printf("Steam callback: onUserStatsReceived gameId=%d user=%s result=%s%n", gameId,
                        Long.toUnsignedString(SteamNativeHandle.getNativeHandle(steamIDUser)), result);

                }
            });

            SteamID steamID = steamUser.getSteamID();
            long nativeSteamId = SteamNativeHandle.getNativeHandle(steamID);
            String personaName = steamFriends.getPersonaName();

            System.out.println("Steam API initialized successfully.");
            System.out.println("Steam persona : " + personaName);
            System.out.println("Steam ID      : " + Long.toUnsignedString(nativeSteamId));
            System.out.println("Requesting asynchronous Steam callback...");

            steamUserStats.getNumberOfCurrentPlayers();

            long deadlineNanos = System.nanoTime() + timeoutSeconds * 1_000_000_000L;
            while (!callbackReceived.get() && System.nanoTime() < deadlineNanos) {
                SteamAPI.runCallbacks();
                Thread.sleep(CALLBACK_PUMP_SLEEP_MILLIS);
            }

            if (!callbackReceived.get()) {
                throw new IllegalStateException("Steam callback pump timed out after " + timeoutSeconds + " seconds");
            }
        } finally {
            if (steamUserStats != null) {
                steamUserStats.dispose();
            }
            if (steamFriends != null) {
                steamFriends.dispose();
            }
            if (steamUser != null) {
                steamUser.dispose();
            }
            if (steamInitialized) {
                SteamAPI.shutdown();
            }
        }

        System.out.println("P0-T07 passed: Steam initialized, identity printed, callback received, and shutdown completed.");

    }
}
