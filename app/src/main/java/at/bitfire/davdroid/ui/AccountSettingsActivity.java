/*
 * Copyright © 2013 – 2016 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.davdroid.ui;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.text.TextUtils;
import android.view.MenuItem;

import at.bitfire.davdroid.AccountSettings;
import at.bitfire.davdroid.InvalidAccountException;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.ui.setup.DetectConfigurationFragment;
import at.bitfire.davdroid.ui.setup.LoginCredentials;
import at.bitfire.davdroid.ui.setup.LoginCredentialsChangeFragment;
import at.bitfire.ical4android.TaskProvider;

public class AccountSettingsActivity extends AppCompatActivity {
    public final static String EXTRA_ACCOUNT = "account";

    private Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        account = getIntent().getParcelableExtra(EXTRA_ACCOUNT);
        setTitle(getString(R.string.settings_title, account.name));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, AccountSettingsFragment.instantiate(this, AccountSettingsFragment.class.getName(), getIntent().getExtras()))
                    .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, AccountActivity.class);
            intent.putExtra(AccountActivity.EXTRA_ACCOUNT, account);
            NavUtils.navigateUpTo(this, intent);
            return true;
        } else
            return false;
    }


    public static class AccountSettingsFragment extends PreferenceFragmentCompat implements LoaderManager.LoaderCallbacks<AccountSettings> {
        Account account;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            account = getArguments().getParcelable(EXTRA_ACCOUNT);

            getLoaderManager().initLoader(0, getArguments(), this);
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.settings_account);
        }

        @Override
        public Loader<AccountSettings> onCreateLoader(int id, Bundle args) {
            return new AccountSettingsLoader(getContext(), (Account)args.getParcelable(EXTRA_ACCOUNT));
        }

        @Override
        public void onLoadFinished(Loader<AccountSettings> loader, final AccountSettings settings) {
            if (settings == null) {
                getActivity().finish();
                return;
            }

            // category: authentication
            final EditTextPreference prefPassword = (EditTextPreference)findPreference("password");
            prefPassword.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    LoginCredentials credentials = newValue != null ? new LoginCredentials(account.name, (String) newValue) : null;
                    LoginCredentialsChangeFragment.newInstance(account, credentials).show(getFragmentManager(), null);
                    getLoaderManager().restartLoader(0, getArguments(), AccountSettingsFragment.this);
                    return false;
                }
            });

            // category: synchronization
            final ListPreference prefSyncContacts = (ListPreference)findPreference("sync_interval_contacts");
            final Long syncIntervalContacts = settings.getSyncInterval(ContactsContract.AUTHORITY);
            if (syncIntervalContacts != null) {
                prefSyncContacts.setValue(syncIntervalContacts.toString());
                if (syncIntervalContacts == AccountSettings.SYNC_INTERVAL_MANUALLY)
                    prefSyncContacts.setSummary(R.string.settings_sync_summary_manually);
                else
                    prefSyncContacts.setSummary(getString(R.string.settings_sync_summary_periodically, syncIntervalContacts / 60));
                prefSyncContacts.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        settings.setSyncInterval(ContactsContract.AUTHORITY, Long.parseLong((String)newValue));
                        return false;
                    }
                });
            } else {
                prefSyncContacts.setEnabled(false);
                prefSyncContacts.setSummary(R.string.settings_sync_summary_not_available);
            }

            final ListPreference prefSyncCalendars = (ListPreference)findPreference("sync_interval_calendars");
            final Long syncIntervalCalendars = settings.getSyncInterval(CalendarContract.AUTHORITY);
            if (syncIntervalCalendars != null) {
                prefSyncCalendars.setValue(syncIntervalCalendars.toString());
                if (syncIntervalCalendars == AccountSettings.SYNC_INTERVAL_MANUALLY)
                    prefSyncCalendars.setSummary(R.string.settings_sync_summary_manually);
                else
                    prefSyncCalendars.setSummary(getString(R.string.settings_sync_summary_periodically, syncIntervalCalendars / 60));
                prefSyncCalendars.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        settings.setSyncInterval(CalendarContract.AUTHORITY, Long.parseLong((String)newValue));
                        return false;
                    }
                });
            } else {
                prefSyncCalendars.setEnabled(false);
                prefSyncCalendars.setSummary(R.string.settings_sync_summary_not_available);
            }

            final ListPreference prefSyncTasks = (ListPreference)findPreference("sync_interval_tasks");
            final Long syncIntervalTasks = settings.getSyncInterval(TaskProvider.ProviderName.OpenTasks.authority);
            if (syncIntervalTasks != null) {
                prefSyncTasks.setValue(syncIntervalTasks.toString());
                if (syncIntervalTasks == AccountSettings.SYNC_INTERVAL_MANUALLY)
                    prefSyncTasks.setSummary(R.string.settings_sync_summary_manually);
                else
                    prefSyncTasks.setSummary(getString(R.string.settings_sync_summary_periodically, syncIntervalTasks / 60));
                prefSyncTasks.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        settings.setSyncInterval(TaskProvider.ProviderName.OpenTasks.authority, Long.parseLong((String)newValue));
                        return false;
                    }
                });
            } else {
                prefSyncTasks.setEnabled(false);
                prefSyncTasks.setSummary(R.string.settings_sync_summary_not_available);
            }

            final SwitchPreferenceCompat prefWifiOnly = (SwitchPreferenceCompat)findPreference("sync_wifi_only");
            prefWifiOnly.setChecked(settings.getSyncWifiOnly());
            prefWifiOnly.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object wifiOnly) {
                    settings.setSyncWiFiOnly((Boolean)wifiOnly);
                    getLoaderManager().restartLoader(0, getArguments(), AccountSettingsFragment.this);
                    return false;
                }
            });

            final EditTextPreference prefWifiOnlySSID = (EditTextPreference)findPreference("sync_wifi_only_ssid");
            final String onlySSID = settings.getSyncWifiOnlySSID();
            prefWifiOnlySSID.setText(onlySSID);
            if (onlySSID != null)
                prefWifiOnlySSID.setSummary(getString(R.string.settings_sync_wifi_only_ssid_on, onlySSID));
            else
                prefWifiOnlySSID.setSummary(R.string.settings_sync_wifi_only_ssid_off);
            prefWifiOnlySSID.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String ssid = (String)newValue;
                    settings.setSyncWifiOnlySSID(!TextUtils.isEmpty(ssid) ? ssid : null);
                    getLoaderManager().restartLoader(0, getArguments(), AccountSettingsFragment.this);
                    return false;
                }
            });
        }

        @Override
        public void onLoaderReset(Loader<AccountSettings> loader) {
        }

    }


    private static class AccountSettingsLoader extends AsyncTaskLoader<AccountSettings> implements SyncStatusObserver {

        final Account account;
        Object listenerHandle;

        public AccountSettingsLoader(Context context, Account account) {
            super(context);
            this.account = account;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();

            listenerHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, this);
        }

        @Override
        protected void onStopLoading() {
            ContentResolver.removeStatusChangeListener(listenerHandle);
        }

        @Override
        public AccountSettings loadInBackground() {
            AccountSettings settings;
            try {
                settings = new AccountSettings(getContext(), account);
            } catch(InvalidAccountException e) {
                return null;
            }
            return settings;
        }

        @Override
        public void onStatusChanged(int which) {
            forceLoad();
        }

    }

}
