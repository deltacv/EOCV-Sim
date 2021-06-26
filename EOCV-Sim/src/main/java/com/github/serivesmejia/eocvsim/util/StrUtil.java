/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.util;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StrUtil {

    public static final Pattern URL_PATTERN = Pattern.compile(
            "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
            Pattern.CASE_INSENSITIVE);

    public static String[] findUrlsInString(String str) {

        Matcher urlMatcher = URL_PATTERN.matcher(str);

        ArrayList<String> matches = new ArrayList<>();

        while(urlMatcher.find()) {
            String url = str.substring(urlMatcher.start(0),
                    urlMatcher.end(0));
            matches.add(url);
        }

        return matches.toArray(new String[0]);

    }

    public static String getFileBaseName(String fileName) {
        int index = fileName.lastIndexOf('.');
        if(index == -1)
            return fileName;
        else
            return fileName.substring(0, index);
    }

    public static String random() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String fromException(Throwable ex) {
        StringWriter writer = new StringWriter(256);
        ex.printStackTrace(new PrintWriter(writer));
        return writer.toString().trim();
    }

    public static String cutStringBy(String str, String by, int amount) {
        int truncateIndex = str.length();

        for(int i = 0 ; i < amount ; i++) {
            truncateIndex = str.lastIndexOf(by, truncateIndex - 1);
        }

        return str.substring(0, truncateIndex);
    }

}
