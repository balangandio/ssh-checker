package com.comxa.universo42.sshchecker.view;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;

import com.comxa.universo42.sshchecker.R;

import java.util.ArrayList;

public class SshListActivity extends FragmentActivity {
    public static final String INTENT_SSHS = "sshs";
    public static final String INTENT_SSH_ONS = "sshOns";
    public static final String INTENT_SSH_OFFS = "sshOffs";
    public static final String INTENT_SSH_ERROR = "sshError";

    private FragmentTabHost tabHost;
    private ArrayList<String> sshs;
    private ArrayList<String> sshOns;
    private ArrayList<String> sshOffs;
    private ArrayList<String> sshError;
    private boolean hasTab;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_sshs);

        tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        sshs = getIntent().getStringArrayListExtra(INTENT_SSHS);

        if (sshs != null) {
            makeTab("Tab0", getString(R.string.titleTabSshs) + " ("+sshs.size()+")", TabFragment.TabSshs.class);
        } else {
            sshOns = getIntent().getStringArrayListExtra(INTENT_SSH_ONS);
            if (sshOns != null)
                makeTab("Tab1", getString(R.string.titleTabOns) + " ("+sshOns.size()+")", TabFragment.TabSshOns.class);

            sshOffs = getIntent().getStringArrayListExtra(INTENT_SSH_OFFS);
            if (sshOffs != null)
                makeTab("Tab2", getString(R.string.titleTabOffs) + " ("+sshOffs.size()+")", TabFragment.TabSshOffs.class);

            sshError = getIntent().getStringArrayListExtra(INTENT_SSH_ERROR);
            if (sshError != null)
                makeTab("Tab3", getString(R.string.titleTabError) + " ("+sshError.size()+")", TabFragment.TabSshError.class);
        }

        if (!hasTab)
            makeTab("Tab4", getString(R.string.titleTabSshs), TabFragment.TabSshEmpty.class);
    }

    private void makeTab(String specName, String title, Class classe) {
        tabHost.addTab(tabHost.newTabSpec(specName).setIndicator(title), classe, null);
        hasTab = true;
    }

    public ArrayList<String> getSshs() {
        return this.sshs;
    }

    public ArrayList<String> getSshOns() {
        return this.sshOns;
    }

    public ArrayList<String> getSshOffs() {
        return this.sshOffs;

    }
    public ArrayList<String> getSshError() {
        return this.sshError;
    }
}
