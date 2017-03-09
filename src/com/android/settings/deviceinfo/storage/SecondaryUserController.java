/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo.storage;

import android.content.Context;
import android.content.pm.UserInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.SparseArray;

import com.android.settings.Utils;
import com.android.settings.applications.UserManagerWrapper;
import com.android.settings.core.PreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * SecondaryUserController controls the preferences on the Storage screen which had to do with
 * secondary users.
 */
public class SecondaryUserController extends PreferenceController implements
        StorageAsyncLoader.ResultHandler {
    // PreferenceGroupKey to try to add our preference onto.
    private static final String TARGET_PREFERENCE_GROUP_KEY = "pref_secondary_users";
    private static final String PREFERENCE_KEY_BASE = "pref_user_";
    private static final int USER_PROFILE_INSERTION_LOCATION = 6;
    private static final int SIZE_NOT_SET = -1;

    private @NonNull UserInfo mUser;
    private @Nullable StorageItemPreferenceAlternate mStoragePreference;
    private long mSize;

    /**
     * Adds the appropriate controllers to a controller list for handling all secondary users on
     * a device.
     * @param context Context for initializing the preference controllers.
     * @param userManager UserManagerWrapper for figuring out which controllers to add.
     */
    public static List<PreferenceController> getSecondaryUserControllers(
            Context context, UserManagerWrapper userManager) {
        List<PreferenceController> controllers = new ArrayList<>();
        UserInfo primaryUser = userManager.getPrimaryUser();
        boolean addedUser = false;
        List<UserInfo> infos = userManager.getUsers();
        for (int i = 0, size = infos.size(); i < size; i++) {
            UserInfo info = infos.get(i);
            if (info.equals(primaryUser)) {
                continue;
            }

            if (info == null || Utils.isProfileOf(primaryUser, info)) {
                controllers.add(new UserProfileController(context, info,
                        USER_PROFILE_INSERTION_LOCATION));
                continue;
            }

            controllers.add(new SecondaryUserController(context, info));
            addedUser = true;
        }

        if (!addedUser) {
            controllers.add(new NoSecondaryUserController(context));
        }
        return controllers;
    }

    /**
     * Constructor for a given secondary user.
     * @param context Context to initialize the underlying {@link PreferenceController}.
     * @param info {@link UserInfo} for the secondary user which this controllers covers.
     */
    @VisibleForTesting
    SecondaryUserController(Context context, @NonNull UserInfo info) {
        super(context);
        mUser = info;
        mSize = SIZE_NOT_SET;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        if (mStoragePreference == null) {
            mStoragePreference = new StorageItemPreferenceAlternate(screen.getContext());

            PreferenceGroup group =
                    (PreferenceGroup) screen.findPreference(TARGET_PREFERENCE_GROUP_KEY);
            mStoragePreference.setTitle(mUser.name);
            mStoragePreference.setKey(PREFERENCE_KEY_BASE + mUser.id);
            if (mSize != SIZE_NOT_SET) {
                mStoragePreference.setStorageSize(mSize);
            }
            group.setVisible(true);
            group.addPreference(mStoragePreference);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mStoragePreference != null ? mStoragePreference.getKey() : null;
    }

    /**
     * Returns the user for which this is the secondary user controller.
     */
    @NonNull
    public UserInfo getUser() {
        return mUser;
    }

    /**
     * Sets the size for the preference.
     * @param size Size in bytes.
     */
    public void setSize(long size) {
        mSize = size;
        if (mStoragePreference != null) {
            mStoragePreference.setStorageSize(mSize);
        }
    }

    public void handleResult(SparseArray<StorageAsyncLoader.AppsStorageResult> stats) {
        int userId = getUser().id;
        StorageAsyncLoader.AppsStorageResult result = stats.get(userId);
        if (result != null) {
            setSize(result.externalStats.totalBytes);
        }
    }

    private static class NoSecondaryUserController extends PreferenceController {
        public NoSecondaryUserController(Context context) {
            super(context);
        }

        @Override
        public void displayPreference(PreferenceScreen screen) {
            PreferenceGroup group =
                    (PreferenceGroup) screen.findPreference(TARGET_PREFERENCE_GROUP_KEY);
            if (group == null) {
                return;
            }
            screen.removePreference(group);
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String getPreferenceKey() {
            return null;
        }

    }
}