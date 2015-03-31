package com.tresmonos.calendar.model;

import android.util.Pair;

import com.google.common.base.Function;

import javax.annotation.Nullable;

/**
 * Utility methods
 */
public class Utils {

    public static <R, S> Function<Pair<R, S>, S> getFirstPairElement() {
        return new Function<Pair<R, S>, S>() {
            @Nullable
            @Override
            public S apply(Pair<R, S> input) {
                return input.second;
            }
        };
    }

    public static <R, S> Function<Pair<R, S>, S> getSecondPairElement() {
        return new Function<Pair<R, S>, S>() {
            @Nullable
            @Override
            public S apply(Pair<R, S> input) {
                return input.second;
            }
        };
    }

	public static int hash(Object ... objects) {
		int code = 0;
		for (Object object : objects)
			code ^= object != null ? object.hashCode() : 0;
		return code;
	}

	public static boolean equals(Object a, Object b) {
		if (a == null && b == null)
			return true;
		if (a == null || b == null)
			return false;
		return a.equals(b);
	}
}
