package com.bourke.finch.common;

import android.graphics.drawable.Drawable;

import android.os.AsyncTask;

import android.util.Log;

import com.bourke.finch.lazylist.Utils;
import com.bourke.finch.BaseFinchActivity;
import com.bourke.finch.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.List;

import twitter4j.IDs;

import twitter4j.Paging;

import twitter4j.ProfileImage;

import twitter4j.ResponseList;

import twitter4j.Twitter;

import twitter4j.TwitterException;

import twitter4j.User;

/*
 * Generic AsyncTask used to make API calls in a background thread.
 *
 * Pattern adapted from
 * http://jyro.blogspot.com/2009/11/android-asynctask-template.html
 *
 * AsyncTask<executeParams, publishProgressParams, resultType>
 */

public class TwitterTask extends
        AsyncTask<TwitterTaskParams, Object, TwitterTaskParams> {

    public static final String TAG = "Finch/TwitterTask";

    public static final int SHOW_USER = 0;
    public static final int GET_HOME_TIMELINE = 1;
    public static final int GET_PROFILE_IMAGE = 2;
    public static final int GET_USER_TIMELINE = 3;
    public static final int GET_FOLLOWING_IDS = 4;
    public static final int GET_MENTIONS = 5;
    public static final int LOOKUP_USERS = 6;
    public static final int CREATE_FAVORITE = 7;
    public static final int DESTROY_FAVORITE = 8;

    private TwitterTaskParams mParams;

    private Twitter mTwitter;

    private TwitterTaskCallback mCallback;

    public TwitterTask(TwitterTaskParams params, TwitterTaskCallback callback,
            Twitter twitter) {
        mParams = params;
        mCallback = callback;
        mTwitter = twitter;
    }

    @Override
    protected void onPreExecute() {
        BaseFinchActivity app = (BaseFinchActivity)mParams.data[0];
        app.showProgressIcon(true);
    }

    @Override
    public TwitterTaskParams doInBackground(TwitterTaskParams... params) {

        TwitterTaskParams payload = mParams;
        int taskType = mParams.taskType;

        switch(taskType) {

            case SHOW_USER:
                Log.d(TAG, "Running taskType SHOW_USER");
                User user = null;
                try {
                    Object userId = payload.data[1];
                    if (userId instanceof Long) {
                        user = mTwitter.showUser((Long)userId);
                    } else if (userId instanceof String) {
                        user = mTwitter.showUser((String)userId);
                    } else {
                        Log.e(TAG, "Error: TwitterTask.SHOW_USER called with "+
                                userId.getClass().getName());
                    }
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                payload.result = user;
                break;

            case GET_HOME_TIMELINE:
                Log.d(TAG, "Running taskType GET_HOME_TIMELINE");
                List<twitter4j.Status> homeTimeline = null;
                Paging page = (Paging)payload.data[3];
                try {
                    homeTimeline = mTwitter.getHomeTimeline(page);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                payload.result = homeTimeline;
                break;

            case GET_PROFILE_IMAGE:
                Log.d(TAG, "Running taskType GET_PROFILE_IMAGE");
                Drawable bitmap = null;
                File tempFile = null;
                try {
                    BaseFinchActivity a = (BaseFinchActivity)payload.data[0];
                    File cacheDir = a.getCacheDir();
                    String p_screenName = (String)payload.data[1];

                    ProfileImage.ImageSize imageSize =
                        (ProfileImage.ImageSize)payload.data[2];
                    ProfileImage p = mTwitter.getProfileImage(
                            p_screenName, imageSize);
                    String profileImageUrl = p.getURL();

                    tempFile = new File(cacheDir, "profile_" + p_screenName);
                    URL imageUrl = new URL(p.getURL());
                    HttpURLConnection conn =
                        (HttpURLConnection)imageUrl.openConnection();
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(30000);
                    conn.setInstanceFollowRedirects(true);
                    InputStream is = conn.getInputStream();
                    OutputStream os = new FileOutputStream(tempFile);
                    Utils.CopyStream(is, os);
                    os.close();
                    bitmap = Drawable.createFromPath(tempFile.toString());
                } catch (TwitterException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                payload.result = tempFile.toString();
                break;

            case GET_USER_TIMELINE:
                Log.d(TAG, "Running taskType GET_USER_TIMELINE");
                List<twitter4j.Status> userTimeLine = null;
                String u_screenName = (String)payload.data[1];
                try {
                    userTimeLine = mTwitter.getUserTimeline(u_screenName);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                payload.result = userTimeLine;
                break;

            case GET_FOLLOWING_IDS:
                Log.d(TAG, "Running taskType GET_FOLLOWING_IDS");
                IDs ids = null;
                String f_screenName = (String)payload.data[1];
                try {
                    long cursor = -1; // begin paging
                    ids = mTwitter.getFollowersIDs(f_screenName, -1);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                payload.result = ids;
                break;

            case LOOKUP_USERS:
                Log.d(TAG, "Running taskType LOOKUP_USERS");
                ResponseList users = null;
                long[] l_ids = (long[])payload.data[1];
                try {
                    users = mTwitter.lookupUsers(l_ids);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                payload.result = users;
                break;

            case GET_MENTIONS:
                Log.d(TAG, "Running taskType GET_MENTIONS");
                List<twitter4j.Status> mentions = null;
                Paging mentions_page = (Paging)payload.data[3];
                try {
                    mentions = mTwitter.getMentions(mentions_page);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                payload.result = mentions;
                break;

            case CREATE_FAVORITE:
                Log.d(TAG, "Running taskType CREATE_FAVORITE");
                twitter4j.Status createdFavorite = null;
                Long createFavId = (Long)payload.data[1];
                try {
                    createdFavorite = mTwitter.createFavorite(createFavId);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                payload.result = createdFavorite;
                break;

            case DESTROY_FAVORITE:
                Log.d(TAG, "Running taskType DESTROY_FAVORITE");
                twitter4j.Status destroyedFavorite = null;
                Long destroyFavId = (Long)payload.data[1];
                try {
                    destroyedFavorite = mTwitter.destroyFavorite(destroyFavId);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                payload.result = destroyedFavorite;
                break;
        }

        return payload;
    }

    @Override
    public void onPostExecute(TwitterTaskParams payload) {
        BaseFinchActivity app = (BaseFinchActivity)mParams.data[0];
        app.showProgressIcon(false);

        if (payload == null || payload.result == null) {
            Log.e(TAG, "payload is null, returning");
            return;
        }
        mCallback.onSuccess(payload);
    }

}

