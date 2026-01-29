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

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A utility class for reflection operations.
 */
public class ReflectUtil {

    /**
     * Get a declared field from a class, including superclasses.
     * @param clazz the class to get the field from
     * @param fieldName the name of the field
     * @return the field
     */
    public static Field getDeclaredFieldDeep(Class<?> clazz, String fieldName) {
        Field field = null;

        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(fieldName);
            } catch (Exception ignored) { }
            clazz = clazz.getSuperclass();
        }

        return field;
    }

    /**
     * Check if a class has a superclass, efficiently.
     * Though it is kind of a hack... it works.
     *
     * @param clazz the class to check
     * @param superClass the superclass to check for
     * @return whether the class has the superclass
     */
    public static boolean hasSuperclass(Class<?> clazz, Class<?> superClass) {
        try {
            clazz.asSubclass(superClass);
            return true;
        } catch (ClassCastException ex) {
            return false;
        }
    }

    /**
     * Check if a class has an annotation.
     * @param clazz the class to check
     * @param annotation the annotation to check for
     * @return whether the class has the annotation
     */
    public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        for(Annotation ann : clazz.getAnnotations()) {
            if(ann.getClass() == annotation) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the type arguments from the extends declaration.
     * Another hack... but it works.
     * Reflection is a pain.
     * @param clazz the class to get the type arguments from
     * @return
     */
    public static Type[] getTypeArgumentsFrom(Class<?> clazz) {
        //get type argument
        Type sooper = clazz.getGenericSuperclass();
        return ((ParameterizedType)sooper).getActualTypeArguments();
    }

    /**
     * Wrap a primitive class to its wrapper class.
     * @param c the class to wrap
     * @param <T> the type of the class
     * @return the wrapped class
     */
    @SuppressWarnings("unchecked") // we know what we're doing lol
    public static <T> Class<T> wrap(Class<T> c) {
        return (Class<T>) MethodType.methodType(c).wrap().returnType();
    }

}
