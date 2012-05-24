package com.bourke.finch;

import android.content.Context;

import android.os.Bundle;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.MenuItem;

import com.bourke.finch.common.FinchTwitterFactory;
import com.bourke.finch.common.TwitterTask;
import com.bourke.finch.common.TwitterTaskCallback;
import com.bourke.finch.common.TwitterTaskParams;
import com.bourke.finch.lazylist.LazyAdapter;

import java.util.ArrayList;
import java.util.List;

import twitter4j.auth.AccessToken;

import twitter4j.Paging;

import twitter4j.ResponseList;

import twitter4j.Status;

import twitter4j.Twitter;

import twitter4j.TwitterException;

import twitter4j.TwitterResponse;

public abstract class BaseTimelineFragment extends SherlockFragment
        implements OnScrollListener {

    protected String TAG = "Finch/BaseTimelineFragment";

    protected ListView mMainList;

    protected LazyAdapter mMainListAdapter;

    protected Twitter mTwitter;

    protected AccessToken mAccessToken;

    protected ActionMode mMode;

    protected Context mContext;

    protected boolean mLoadingPage = false;

    protected int mUnreadCount = 0;

    private int mNewestUnreadIndex = 0;

    private int mTwitterTaskType;

    private MainActivity mActivity;

    /* Update the unread display on scrolling every X items */
    private static final int UPDATE_UNREAD_COUNT_INTERVAL = 3;

    private static final int FETCH_LIMIT = 20;

    private List<TwitterResponse> mTimelineGap =
        new ArrayList<TwitterResponse>();

    public BaseTimelineFragment(int twitterTaskType) {
        mTwitterTaskType = twitterTaskType;

        switch (mTwitterTaskType) {
            case TwitterTask.GET_HOME_TIMELINE:
                TAG = "Finch/HomeTimelineFragment";
                break;
            case TwitterTask.GET_MENTIONS:
                TAG = "Finch/ConnectionsTimelineFragment";
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        setHasOptionsMenu(true);

        mActivity = (MainActivity)getSherlockActivity();
        mContext = mActivity.getApplicationContext();
        mTwitter = FinchTwitterFactory.getInstance(mContext).getTwitter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");

        RelativeLayout layout = (RelativeLayout)inflater
            .inflate(R.layout.standard_list_fragment, container, false);
        initMainList(layout);

        return layout;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisible, int visibleCount,
            int totalCount) {

        if (totalCount <= 0 || mLoadingPage) {
            return;
        }

        if (firstVisible <= mNewestUnreadIndex) {
           mNewestUnreadIndex = firstVisible;
           if (mNewestUnreadIndex % UPDATE_UNREAD_COUNT_INTERVAL == 0 ||
                   mNewestUnreadIndex == 0) {
               mUnreadCount = mNewestUnreadIndex;
               mActivity.updateUnreadDisplay();
           }
        }

        boolean loadMore = firstVisible + visibleCount >= totalCount;
        if (loadMore) {
            mLoadingPage = true;
            loadNextPage();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView v, int scrollState) {
        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
            //Log.i("a", "scrolling stopped...");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_compose:
                mActivity.showDialog();
                return true;
            case R.id.menu_refresh:
                refresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void refresh() {
        Log.d(TAG, "refresh()");

        final Paging page = new Paging();
        page.setCount(FETCH_LIMIT);

        final List<TwitterResponse> currentList = mMainListAdapter
            .getResponses();
        final int currentPosition = mMainList.getFirstVisiblePosition();
        mTimelineGap.clear();

        if (currentList != null && !currentList.isEmpty()) {
            Log.d(TAG, "current response list size = " + currentList.size());
            page.setSinceId(((Status)currentList.get(0)).getId());
            final TwitterTaskCallback taskCallback = new TwitterTaskCallback
                    <TwitterTaskParams, TwitterException>() {
                public void onSuccess(TwitterTaskParams payload) {
                    ResponseList res = (ResponseList)payload.result;
                    /* If we have made a request with maxId set, we need to
                     * drop the first response as maxId is inclusive */
                    if (page.getMaxId() > -1) {
                        Log.d(TAG, "maxId set, dropping first response");
                        res.remove(0);
                    }
                    if (res.size() > 0) {
                        Log.d(TAG, "fetched " + res.size() + " responses, " +
                                "appending to timeline gap list");
                        mTimelineGap.addAll(res);
                        page.setMaxId(((Status)mTimelineGap.get(
                                        mTimelineGap.size()-1)).getId());
                        Log.d(TAG, "Issuing GET with sinceId=" +
                                page.getSinceId() + ", maxId=" +
                                page.getMaxId());
                        // TODO: Add some sort of limit here to prevent getting
                        // rate limited if there's a *lot* to fetch. Also test
                        // what happens when we are rate limited
                        TwitterTaskParams taskParams = new TwitterTaskParams(
                                mTwitterTaskType, new Object[] {mActivity,
                                    mMainListAdapter, mMainList, page});
                        new TwitterTask(taskParams, this, mTwitter).execute();
                    } else {
                        Log.d(TAG, "fetched " + res.size() + " responses, " +
                                "prepending timeline gap list to adapter");
                        mMainListAdapter.prependResponses(mTimelineGap);
                        mMainListAdapter.notifyDataSetChanged();
                        int newListSize = mMainListAdapter.getResponses()
                            .size();
                        int newScrollPosition = (newListSize -
                                currentList.size()) + currentPosition;
                        mMainList.setSelection(newScrollPosition);

                        /* Update unread count display */
                        mNewestUnreadIndex = mTimelineGap.size()-1;
                        mUnreadCount += mTimelineGap.size();
                        mActivity.updateUnreadDisplay();
                    }
                }
                public void onFailure(TwitterException e) {
                    e.printStackTrace();
                }
            };
            Log.d(TAG, "Issuing GET with sinceId=" +
                    page.getSinceId() + ", maxId=" +
                    page.getMaxId());
            TwitterTaskParams taskParams = new TwitterTaskParams(
                    mTwitterTaskType, new Object[] {mActivity,
                        mMainListAdapter, mMainList, page});
            new TwitterTask(taskParams, taskCallback, mTwitter).execute();
        } else {
            Log.d(TAG, "currentList null or empty");
            /* Fetch first N tweets and add to list */
            TwitterTaskCallback taskCallback = new TwitterTaskCallback
                    <TwitterTaskParams, TwitterException>() {
                public void onSuccess(TwitterTaskParams payload) {
                    ResponseList res = (ResponseList)payload.result;
                    if (res.size() == 0) {
                        Log.d(TAG, "res.size() == 0, no action");
                        return;
                    }
                    /* Update list */
                    Log.d(TAG, "fetched " + res.size() + " responses, " +
                            "appending to adapter");
                    mMainListAdapter.appendResponses((ResponseList)res);
                    mMainListAdapter.notifyDataSetChanged();
                    /* Update unread count display */
                    mUnreadCount += res.size();
                    mActivity.updateUnreadDisplay();
                }
                public void onFailure(TwitterException e) {
                    e.printStackTrace();
                }
            };
            Log.d(TAG, "Issuing GET with sinceId=" +
                    page.getSinceId() + ", maxId=" +
                    page.getMaxId());
            TwitterTaskParams taskParams = new TwitterTaskParams(
                    mTwitterTaskType, new Object[] {mActivity,
                        mMainListAdapter, mMainList, page});
            new TwitterTask(taskParams, taskCallback, mTwitter).execute();
        }
    }

    protected void loadNextPage() {
        Log.d(TAG, "loadNextPage");

        List<TwitterResponse> currentList = mMainListAdapter.getResponses();
        if (currentList.isEmpty()) {
            Log.e(TAG, "mMainListAdapter.getResponses() is empty");
            return;
        }
        TwitterTaskCallback taskCallback = new TwitterTaskCallback
                <TwitterTaskParams, TwitterException>() {
            public void onSuccess(TwitterTaskParams payload) {
                ResponseList res =
                    (ResponseList)payload.result;
                if (res.size()-1 == 0) {  // -1 as maxId is inclusive
                    Log.d(TAG, "res.size()-1 == 0, no action");
                    return;
                }
                res.remove(0); // Avoid overlap with maxId
                mMainListAdapter.appendResponses(res);
                mMainListAdapter.notifyDataSetChanged();
                mLoadingPage = false;
            }
            public void onFailure(TwitterException e) {
                e.printStackTrace();
            }
        };
        Paging paging = new Paging();
        paging.setCount(FETCH_LIMIT);
        long maxId = ((Status)currentList.get(currentList.size()-1)).getId();
        paging.setMaxId(maxId);
        TwitterTaskParams taskParams = new TwitterTaskParams(mTwitterTaskType,
                new Object[] {mActivity, mMainListAdapter, mMainList, paging});
        new TwitterTask(taskParams, taskCallback, mTwitter).execute();
    }

    private void initMainList(ViewGroup layout) {
        mMainList = (ListView)layout.findViewById(R.id.list);
        mMainListAdapter = new LazyAdapter(mActivity);
        mMainList.setAdapter(mMainListAdapter);
        mMainList.setOnScrollListener(this);
        setupActionMode();
    }

    public int getUnreadCount() {
        return mUnreadCount;
    }

    public abstract void setupActionMode();
}
