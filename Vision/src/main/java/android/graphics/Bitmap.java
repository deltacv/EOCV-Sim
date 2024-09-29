/*
 * Copyright (c) 2023 Sebastian Erives
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

package android.graphics;

import org.jetbrains.skia.ColorAlphaType;
import org.jetbrains.skia.ColorType;
import org.jetbrains.skia.ImageInfo;

import java.io.Closeable;
import java.io.IOException;

public class Bitmap implements AutoCloseable {

    @Override
    public void close() throws IOException {
        theBitmap.close();
    }

    /**
     * Possible bitmap configurations. A bitmap configuration describes
     * how pixels are stored. This affects the quality (color depth) as
     * well as the ability to display transparent/translucent colors.
     */
    public enum Config {
        // these native values must match up with the enum in SkBitmap.h
        /**
         * Each pixel is stored as a single translucency (alpha) channel.
         * This is very useful to efficiently store masks for instance.
         * No color information is stored.
         * With this configuration, each pixel requires 1 byte of memory.
         */
        ALPHA_8(1),
        /**
         * Each pixel is stored on 2 bytes and only the RGB channels are
         * encoded: red is stored with 5 bits of precision (32 possible
         * values), green is stored with 6 bits of precision (64 possible
         * values) and blue is stored with 5 bits of precision.
         *
         * This configuration can produce slight visual artifacts depending
         * on the configuration of the source. For instance, without
         * dithering, the result might show a greenish tint. To get better
         * results dithering should be applied.
         *
         * This configuration may be useful when using opaque bitmaps
         * that do not require high color fidelity.
         *
         * <p>Use this formula to pack into 16 bits:</p>
         * <pre class="prettyprint">
         * short color = (R & 0x1f) << 11 | (G & 0x3f) << 5 | (B & 0x1f);
         * </pre>
         */
        RGB_565(3),
        /**
         * Each pixel is stored on 2 bytes. The three RGB color channels
         * and the alpha channel (translucency) are stored with a 4 bits
         * precision (16 possible values.)
         *
         * This configuration is mostly useful if the application needs
         * to store translucency information but also needs to save
         * memory.
         *
         * It is recommended to use {@link #ARGB_8888} instead of this
         * configuration.
         *
         * Note: as of {link android.os.Build.VERSION_CODES#KITKAT},
         * any bitmap created with this configuration will be created
         * using {@link #ARGB_8888} instead.
         *
         * @deprecated Because of the poor quality of this configuration,
         *             it is advised to use {@link #ARGB_8888} instead.
         */
        @Deprecated
        ARGB_4444(4),
        /**
         * Each pixel is stored on 4 bytes. Each channel (RGB and alpha
         * for translucency) is stored with 8 bits of precision (256
         * possible values.)
         *
         * This configuration is very flexible and offers the best
         * quality. It should be used whenever possible.
         *
         * <p>Use this formula to pack into 32 bits:</p>
         * <pre class="prettyprint">
         * int color = (A & 0xff) << 24 | (B & 0xff) << 16 | (G & 0xff) << 8 | (R & 0xff);
         * </pre>
         */
        ARGB_8888(5),
        /**
         * Each pixel is stored on 8 bytes. Each channel (RGB and alpha
         * for translucency) is stored as a
         * {@link android.util.Half half-precision floating point value}.
         *
         * This configuration is particularly suited for wide-gamut and
         * HDR content.
         *
         * <p>Use this formula to pack into 64 bits:</p>
         * <pre class="prettyprint">
         * long color = (A & 0xffff) << 48 | (B & 0xffff) << 32 | (G & 0xffff) << 16 | (R & 0xffff);
         * </pre>
         */
        RGBA_F16(6),
        /**
         * Special configuration, when bitmap is stored only in graphic memory.
         * Bitmaps in this configuration are always immutable.
         *
         * It is optimal for cases, when the only operation with the bitmap is to draw it on a
         * screen.
         */
        HARDWARE(7),
        /**
         * Each pixel is stored on 4 bytes. Each RGB channel is stored with 10 bits of precision
         * (1024 possible values). There is an additional alpha channel that is stored with 2 bits
         * of precision (4 possible values).
         *
         * This configuration is suited for wide-gamut and HDR content which does not require alpha
         * blending, such that the memory cost is the same as ARGB_8888 while enabling higher color
         * precision.
         *
         * <p>Use this formula to pack into 32 bits:</p>
         * <pre class="prettyprint">
         * int color = (A & 0x3) << 30 | (B & 0x3ff) << 20 | (G & 0x3ff) << 10 | (R & 0x3ff);
         * </pre>
         */
        RGBA_1010102(8);

        final int nativeInt;
        private static Config sConfigs[] = {
                null, ALPHA_8, null, RGB_565, ARGB_4444, ARGB_8888, RGBA_F16, HARDWARE, RGBA_1010102
        };
        Config(int ni) {
            this.nativeInt = ni;
        }

        static Config nativeToConfig(int ni) {
            return sConfigs[ni];
        }
    }

    private static ColorType configToColorType(Config config) {
        switch (config) {
            case ALPHA_8:
                return ColorType.ALPHA_8;
            case RGB_565:
                return ColorType.RGB_565;
            case ARGB_4444:
                return ColorType.ARGB_4444;
            case ARGB_8888:
                return ColorType.BGRA_8888;
            case RGBA_F16:
                return ColorType.RGBA_F16;
            case RGBA_1010102:
                return ColorType.RGBA_1010102;
            default:
                throw new IllegalArgumentException("Unknown config: " + config);
        }
    }

    private Config colorTypeToConfig(ColorType colorType) {
        switch (colorType) {
            case ALPHA_8:
                return Config.ALPHA_8;
            case RGB_565:
                return Config.RGB_565;
            case ARGB_4444:
                return Config.ARGB_4444;
            case BGRA_8888:
                return Config.ARGB_8888;
            case RGBA_F16:
                return Config.RGBA_F16;
            case RGBA_1010102:
                return Config.RGBA_1010102;
            default:
                throw new IllegalArgumentException("Unknown colorType: " + colorType);
        }
    }

    public static Bitmap createBitmap(int width, int height) {
        Bitmap bm = new Bitmap();
        bm.theBitmap.allocPixels(ImageInfo.Companion.makeS32(width, height, ColorAlphaType.PREMUL));

        return bm;
    }

    public static Bitmap createBitmap(int width, int height, Config config) {
        Bitmap bm = new Bitmap();
        bm.theBitmap.allocPixels(new ImageInfo(width, height, configToColorType(config), ColorAlphaType.PREMUL));

        bm.theBitmap.erase(0);

        return bm;
    }

    /**
     * Internal: theBitmap represents the underlying skiko Bitmap
     * This field is not present in native android.graphics
     */
    public final org.jetbrains.skia.Bitmap theBitmap;

    public Bitmap() {
        theBitmap = new org.jetbrains.skia.Bitmap();
    }

    public Bitmap(org.jetbrains.skia.Bitmap bm) {
        theBitmap = bm;
    }

    public int getWidth() {
        return theBitmap.getWidth();
    }

    public int getHeight() {
        return theBitmap.getHeight();
    }

    public Rect getBounds() {
        return new Rect(0, 0, getWidth(), getHeight());
    }

    public Config getConfig() {
        return colorTypeToConfig(theBitmap.getColorType());
    }

    public void recycle() {
        theBitmap.close();
    }
}
