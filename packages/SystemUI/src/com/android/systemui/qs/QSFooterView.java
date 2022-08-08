/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.systemui.qs;

import static android.app.StatusBarManager.DISABLE2_QUICK_SETTINGS;

import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.systemui.R;

/**
 * Footer of expanded Quick Settings, tiles page indicator, (optionally) build number and
 * {@link FooterActionsView}
 */
public class QSFooterView extends FrameLayout {
    private PageIndicator mPageIndicator;
    private TextView mBuildText;
    private View mActionsContainer;

    private LinearLayout mDataUsagePanel;

    protected TouchAnimator mFooterAnimator;

    private boolean mQsDisabled;
    private boolean mExpanded;
    private boolean mShowUsagePanel;
    private float mExpansionAmount;

    private boolean mShouldShowBuildText;

    private OnClickListener mExpandClickListener;

    private final Handler mHandler = new Handler();

    /*private final ContentObserver mDeveloperSettingsObserver = new ContentObserver(
            new Handler(mContext.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            setBuildText();
        }
    };*/

    public QSFooterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPageIndicator = findViewById(R.id.footer_page_indicator);
        mActionsContainer = requireViewById(R.id.qs_footer_actions);
        mBuildText = findViewById(R.id.build);
        mDataUsagePanel = findViewById(R.id.qs_data_usage);

        updateResources();
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        //setBuildText();
        mHandler.post(() -> {
            getInternetUsage();
        });
    }

    private void setBuildText() {
        if (mBuildText == null) return;
        mBuildText.setText(null);
        mShouldShowBuildText = false;
        mBuildText.setSelected(false);
    }

    private void getInternetUsage() {
        TextView internet_up = findViewById(R.id.internet_up);
        TextView internet_down = findViewById(R.id.internet_down);
        TextView mobile_up = findViewById(R.id.mobile_up);
        TextView mobile_down = findViewById(R.id.mobile_down);
        TextView wifi_up = findViewById(R.id.wifi_up);
        TextView wifi_down = findViewById(R.id.wifi_down);

        long mobileUpload = TrafficStats.getMobileTxBytes();
        long mobileDownload = TrafficStats.getMobileRxBytes();

        long totalDownload = TrafficStats.getTotalRxBytes();
        long totalUpload = TrafficStats.getTotalTxBytes();

        long wifiUpload = totalUpload - mobileUpload;
        long wifiDownload = totalDownload - mobileDownload;

        internet_down.setText("Download: " + BytesToMb(totalDownload) + "MB");
        internet_up.setText("Upload: " + BytesToMb(totalUpload) + "MB");

        mobile_down.setText("Download: " + BytesToMb(mobileDownload) + "MB");
        mobile_up.setText("Upload: " + BytesToMb(mobileUpload) + "MB");

        wifi_down.setText("Download: " + BytesToMb(wifiDownload) + "MB");
        wifi_up.setText("Upload: " + BytesToMb(wifiUpload) + "MB");
    }

    public long BytesToMb(long usage) {
        return usage / 1048567;
    }

    void updateExpansion() {
        setExpansion(mExpansionAmount);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    private void updateResources() {
        updateFooterAnimator();
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        lp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.qs_footers_margin_bottom);
        setLayoutParams(lp);
    }

    private void updateFooterAnimator() {
        mFooterAnimator = createFooterAnimator();
    }

    @Nullable
    private TouchAnimator createFooterAnimator() {
        TouchAnimator.Builder builder = new TouchAnimator.Builder()
                .addFloat(mActionsContainer, "alpha", 0, 1)
                .addFloat(mPageIndicator, "alpha", 0, 1)
                .addFloat(mBuildText, "alpha", 0, 1)
                .addFloat(mDataUsagePanel, "alpha", 0, 1)
                .setStartDelay(0.9f);
        return builder.build();
    }

    /** */
    public void setKeyguardShowing() {
        setExpansion(mExpansionAmount);
    }

    public void setExpandClickListener(OnClickListener onClickListener) {
        mExpandClickListener = onClickListener;
    }

    void setExpanded(boolean expanded) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        updateEverything();
    }

    /** */
    public void setExpansion(float headerExpansionFraction) {
        mExpansionAmount = headerExpansionFraction;
        if (mFooterAnimator != null) {
            mFooterAnimator.setPosition(headerExpansionFraction);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        /*mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED), false,
                mDeveloperSettingsObserver, UserHandle.USER_ALL);*/
    }

    @Override
    @VisibleForTesting
    public void onDetachedFromWindow() {
        //mContext.getContentResolver().unregisterContentObserver(mDeveloperSettingsObserver);
        super.onDetachedFromWindow();
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action == AccessibilityNodeInfo.ACTION_EXPAND) {
            if (mExpandClickListener != null) {
                mExpandClickListener.onClick(null);
                return true;
            }
        }
        return super.performAccessibilityAction(action, arguments);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
    }

    void disable(int state2) {
        final boolean disabled = (state2 & DISABLE2_QUICK_SETTINGS) != 0;
        if (disabled == mQsDisabled) return;
        mQsDisabled = disabled;
        updateEverything();
    }

    void updateEverything() {
        post(() -> {
            updateVisibilities();
            updateClickabilities();
            setClickable(false);
        });
    }

    private void updateClickabilities() {
        mBuildText.setLongClickable(mBuildText.getVisibility() == View.VISIBLE);
    }

    private void updateVisibilities() {
        mShowUsagePanel = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.QS_DATA_USAGE_PANEL, 0,
                UserHandle.USER_CURRENT) == 1;

        mBuildText.setVisibility(mExpanded && mShouldShowBuildText ? View.VISIBLE : View.INVISIBLE);

        if (mExpanded && mShowUsagePanel) {
            mDataUsagePanel.setVisibility(View.VISIBLE);
            getInternetUsage();
        } else {
            mDataUsagePanel.setVisibility(View.GONE);
        }
    }
}