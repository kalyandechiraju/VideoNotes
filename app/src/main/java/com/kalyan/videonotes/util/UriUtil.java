package com.kalyan.videonotes.util;

import android.net.Uri;

/**
 * Created by kalyandechiraju on 18/07/17.
 */

public class UriUtil {
    public static String getVideoTag(String text) {
        if (text.startsWith("http://") || text.startsWith("https://")) {
            Uri uri = Uri.parse(text);
            return uri.getQueryParameter("v");
        } else {
            // Assuming its a tag
            return text;
        }
    }
}
