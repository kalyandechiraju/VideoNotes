package com.kalyan.videonotes.util;

import android.net.Uri;

/**
 * Copyright (c) 2017 Codelight Studios
 * Created by kalyandechiraju on 18/07/17.
 */

public class UriUtil {
    public static String getVideoTag(String text) {
        //URL url = new URL(text);
        if (text.startsWith("http://") || text.startsWith("https://")) {
            Uri uri = Uri.parse(text);
            return uri.getQueryParameter("v");
        } else {
            // Assuming its a tag
            return text;
        }
    }

//    public static void main(String[] args) {
//        String video = "https://www.youtube.com/watch?v=zroqGVwZlWU";
//        System.out.println(UriUtil.getVideoTag(video));
//    }
}
