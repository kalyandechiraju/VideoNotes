package com.kalyan.videonotes;

import com.kalyan.videonotes.util.UriUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void valid_url() throws Exception {
        String video = "https://www.youtube.com/watch?v=zroqGVwZlWU";
        assertEquals(UriUtil.getVideoTag(video), "zroqGVwZlWU");
    }
}