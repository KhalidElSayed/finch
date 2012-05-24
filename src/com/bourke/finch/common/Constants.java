package com.bourke.finch.common;

import android.net.Uri;

import com.bourke.finch.R;


public class Constants {

    /* Themes and colors */
    public static final int THEME_DARK = R.style.Theme_Finch;
    public static final int THEME_LIGHT = R.style.Theme_Finch_Light;
    public static final int COLOR_FINCH_YELLOW = 0xffffbb33;

    /* Fonts */
    public static final String ROBOTO_REGULAR = "fonts/Roboto-Regular.ttf";
    public static final String SHADOWS_INTO_LIGHT_REG =
        "fonts/ShadowsIntoLightTwo-Regular.ttf";

    public static final String PREF_TOKEN_DATA = "twitterTokens";
	public static final String PREF_ACCESS_TOKEN = "accessToken";
	public static final String PREF_ACCESS_TOKEN_SECRET = "accessTokenSecret";

    public static final String PREF_PROFILE_DATA = "profileData";
    public static final String PREF_PROFILE_IMAGE = "profileImage";
	public static final String PREF_SCREEN_NAME = "screenName";

    public static final String PREF_HOMETIMELINE_DATA = "hometimelineData";
    public static final String PREF_HOMETIMELINE_SINCEID =
        "hometimelineSinceId";
    public static final String PREF_HOMETIMELINE_PAGE = "hometimelinePage";
    public static final String PREF_HOMETIMELINE_POS = "hometimelinePos";

    public static final Uri SCREEN_NAME_URI = Uri.parse(
            "content://com.bourke.finch.provider/screenname");
    public static final String CALLBACK_URL = "finch-callback:///";
    public static final String KEY_URL = "finchURL";

    public static final int MAX_TWEET_LENGTH = 140;
}
