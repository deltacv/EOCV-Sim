/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 *
 * Portions of this file are derived from the Android Open Source Project,
 * Copyright (C) 2006 The Android Open Source Project
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

package android.graphics;

import org.jetbrains.skia.Data;
import org.jetbrains.skia.FontMgr;

import java.io.IOException;

public class Typeface {

    public static Typeface DEFAULT = new Typeface(FontMgr.Companion.getDefault().makeFromData(loadDataFromResource("/fonts/Roboto-Regular.ttf"), 0));
    public static Typeface DEFAULT_BOLD = new Typeface(FontMgr.Companion.getDefault().makeFromData(loadDataFromResource("/fonts/Roboto-Bold.ttf"), 0));
    public static Typeface DEFAULT_ITALIC = new Typeface(FontMgr.Companion.getDefault().makeFromData(loadDataFromResource("/fonts/Roboto-Italic.ttf"), 0));

    /**
     * Internal: theTypeface represents the underlying skiko Typeface
     * This field is not present in native android.graphics
     */
    public org.jetbrains.skia.Typeface theTypeface;

    public Typeface(long ptr) {
        theTypeface = new org.jetbrains.skia.Typeface(ptr);
    }

    private Typeface(org.jetbrains.skia.Typeface typeface) {
        theTypeface = typeface;
    }

    public android.graphics.Rect getBounds() {
        return new Rect(
                (int) theTypeface.getBounds().getLeft(),
                (int) theTypeface.getBounds().getTop(),
                (int) theTypeface.getBounds().getRight(),
                (int) theTypeface.getBounds().getBottom()
        );
    }

    private static Data loadDataFromResource(String resource) {
        try {
            byte[] bytes = Typeface.class.getResourceAsStream(resource).readAllBytes();

            return Data.Companion.makeFromBytes(
                    bytes,
                    0, bytes.length
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load from resource: " + resource, e);
        }
    }

}

