package org.wordpress.android.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;

import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsMetadata;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTrackerMixpanel;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.AnalyticsRailcar;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.ReaderPost;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsUtils {
    private static String BLOG_ID_KEY = "blog_id";
    private static String POST_ID_KEY = "post_id";
    private static String FEED_ID_KEY = "feed_id";
    private static String FEED_ITEM_ID_KEY = "feed_item_id";
    private static String IS_JETPACK_KEY = "is_jetpack";

    private static String RAILCAR_ID_KEY = "railcar";
    private static String RAILCAR_FETCH_ALG_KEY = "fetch_alg";
    private static String RAILCAR_FETCH_POSITION_KEY = "fetch_position";
    private static String RAILCAR_FETCH_LANG_KEY = "fetch_lang";
    private static String RAILCAR_FETCH_QUERY_KEY = "fetch_query_key";
    private static String RAILCAR_BLOG_ID_KEY = "rec_blog_id";
    private static String RAILCAR_POST_ID_KEY = "rec_post_id";
    private static String RAILCAR_FEED_ID_KEY = "rec_feed_id";
    private static String RAILCAR_FEED_ITEM_ID_KEY = "rec_feed_item_id";

    /**
     * Utility method to refresh mixpanel metadata.
     *
     * @param username WordPress.com username
     * @param email WordPress.com email address
     */
    public static void refreshMetadata(String username, String email) {
        AnalyticsMetadata metadata = new AnalyticsMetadata();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());

        metadata.setSessionCount(preferences.getInt(AnalyticsTrackerMixpanel.SESSION_COUNT, 0));
        metadata.setUserConnected(AccountHelper.isSignedIn());
        metadata.setWordPressComUser(AccountHelper.isSignedInWordPressDotCom());
        metadata.setJetpackUser(AccountHelper.isJetPackUser());
        metadata.setNumBlogs(WordPress.wpDB.getNumBlogs());
        metadata.setUsername(username);
        metadata.setEmail(email);

        AnalyticsTracker.refreshMetadata(metadata);
    }

    /**
     * Utility method to refresh mixpanel metadata.
     */
    public static void refreshMetadata() {
        AnalyticsMetadata metadata = new AnalyticsMetadata();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());

        metadata.setSessionCount(preferences.getInt(AnalyticsTrackerMixpanel.SESSION_COUNT, 0));
        metadata.setUserConnected(AccountHelper.isSignedIn());
        metadata.setWordPressComUser(AccountHelper.isSignedInWordPressDotCom());
        metadata.setJetpackUser(AccountHelper.isJetPackUser());
        metadata.setNumBlogs(WordPress.wpDB.getNumBlogs());
        metadata.setUsername(AccountHelper.getDefaultAccount().getUserName());
        metadata.setEmail(AccountHelper.getDefaultAccount().getEmail());

        AnalyticsTracker.refreshMetadata(metadata);
    }

    public static int getWordCount(String content) {
        String text = Html.fromHtml(content.replaceAll("<img[^>]*>", "")).toString();
        return text.split("\\s+").length;
    }

    /**
     * Bump Analytics for the passed Stat and CURRENT blog details into properties.
     *
     * @param stat The Stat to bump
     *
     */
    public static void trackWithCurrentBlogDetails(AnalyticsTracker.Stat stat) {
        trackWithCurrentBlogDetails(stat, null);
    }

    /**
     * Bump Analytics for the passed Stat and CURRENT blog details into properties.
     *
     * @param stat The Stat to bump
     * @param properties Properties to attach to the event
     *
     */
    public static void trackWithCurrentBlogDetails(AnalyticsTracker.Stat stat, Map<String, Object> properties) {
        trackWithBlogDetails(stat, WordPress.getCurrentBlog(), properties);
    }

    /**
     * Bump Analytics for the passed Stat and add blog details into properties.
     *
     * @param stat The Stat to bump
     * @param blog The blog object
     *
     */
    public static void trackWithBlogDetails(AnalyticsTracker.Stat stat, Blog blog) {
        trackWithBlogDetails(stat, blog, null);
    }

    /**
     * Bump Analytics for the passed Stat and add blog details into properties.
     *
     * @param stat The Stat to bump
     * @param blog The blog object
     * @param properties Properties to attach to the event
     *
     */
    public static void trackWithBlogDetails(AnalyticsTracker.Stat stat, Blog blog, Map<String, Object> properties) {
        if (blog == null || (!blog.isDotcomFlag() && !blog.isJetpackPowered())) {
            AppLog.w(AppLog.T.STATS, "The passed blog obj is null or it's not a wpcom or Jetpack. Tracking analytics without blog info");
            AnalyticsTracker.track(stat, properties);
            return;
        }

        String blogID = blog.getDotComBlogId();
        if (blogID != null) {
            if (properties == null) {
                properties = new HashMap<>();
            }
            properties.put(BLOG_ID_KEY, blogID);
            properties.put(IS_JETPACK_KEY, blog.isJetpackPowered());
        } else {
            // When the blog ID is null here does mean the blog is not hosted on wpcom.
            // It may be a Jetpack blog still in synch for options, or a self-hosted.
            // In both of these cases skip adding blog details into properties.
        }

        if (properties == null) {
            AnalyticsTracker.track(stat);
        } else {
            AnalyticsTracker.track(stat, properties);
        }
    }

    /**
     * Bump Analytics and add blog_id into properties
     *
     * @param stat The Stat to bump
     * @param blogID The REMOTE blog ID.
     *
     */
    public static void trackWithBlogDetails(AnalyticsTracker.Stat stat, Long blogID) {
        Map<String, Object> properties =  new HashMap<>();
        if (blogID != null) {
            properties.put(BLOG_ID_KEY, blogID);
        }
        AnalyticsTracker.track(stat, properties);
    }

    /**
     * Bump Analytics and add blog_id into properties
     *
     * @param stat The Stat to bump
     * @param blogID The REMOTE blog ID.
     *
     */
    public static void trackWithBlogDetails(AnalyticsTracker.Stat stat, String blogID) {
        try {
            Long remoteID = Long.parseLong(blogID);
            trackWithBlogDetails(stat, remoteID);
        } catch (NumberFormatException err) {
            AnalyticsTracker.track(stat);
        }
    }

    /**
     * Bump Analytics for a reader post
     *
     * @param stat The Stat to bump
     * @param post The reader post to track
     *
     */
    public static void trackWithReaderPostDetails(AnalyticsTracker.Stat stat, ReaderPost post) {
        if (post == null) return;

        // wpcom/jetpack posts should pass: feed_id, feed_item_id, blog_id, post_id, is_jetpack
        // RSS pass should pass: feed_id, feed_item_id, is_jetpack
        Map<String, Object> properties =  new HashMap<>();
        if (post.isWP() || post.isJetpack) {
            properties.put(BLOG_ID_KEY, post.blogId);
            properties.put(POST_ID_KEY, post.postId);
        }
        properties.put(FEED_ID_KEY, post.feedId);
        properties.put(FEED_ITEM_ID_KEY, post.feedItemId);
        properties.put(IS_JETPACK_KEY, post.isJetpack);
        AnalyticsTracker.track(stat, properties);
    }

    /**
     * Bump Analytics with a TrainTracks railcar
     *
     * @param stat The Stat to bump
     * @param railcar The railcar to track
     *
     */
    public static void trackWithRailcar(AnalyticsTracker.Stat stat, AnalyticsRailcar railcar) {
        if (railcar == null) return;

        Map<String, Object> properties =  new HashMap<>();

        properties.put(RAILCAR_ID_KEY, railcar.getRailcarId());

        properties.put(RAILCAR_FETCH_ALG_KEY, railcar.getFetchAlg());
        properties.put(RAILCAR_FETCH_POSITION_KEY, railcar.getFetchPosition());
        properties.put(RAILCAR_FETCH_LANG_KEY, railcar.getFetchLang());
        properties.put(RAILCAR_FETCH_QUERY_KEY, railcar.getFetchQuery());

        properties.put(RAILCAR_BLOG_ID_KEY, railcar.getBlogId());
        properties.put(RAILCAR_POST_ID_KEY, railcar.getPostId());
        properties.put(RAILCAR_FEED_ID_KEY, railcar.getFeedId());
        properties.put(RAILCAR_FEED_ITEM_ID_KEY, railcar.getFeedItemId());

        AnalyticsTracker.track(stat, properties);
    }
}
