package com.tresmonos.calendar.model;

import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Area {
    private final static LoadingCache<String, Area> areas = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Area>() {
                @Override
                public Area load(String objectId) throws Exception {
                    return findArea(objectId);
                }
            });
    private static final String AREA_TABLE = "Area";
    private static final String AREA_OBJECT_ID_COLUMN = "objectId";
    private static final String AREA_NAME_COLUMN = "name";

    private final String objectId;
    private final String name;

    private Area(String objectId, String name) {
        this.objectId = objectId;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getObjectId() {
        return objectId;
    }

    private static Area findArea(String objectId) throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(AREA_TABLE);
        query.whereEqualTo(AREA_OBJECT_ID_COLUMN, objectId);
        List<ParseObject> areas = query.find();
        if (areas.isEmpty()) return null;
        return new Area(objectId, areas.get(0).get(AREA_NAME_COLUMN).toString());
    }

    public static Area getOrFindArea(String objectId) {
        try {
            return areas.get(objectId);
        } catch (ExecutionException e) {
            throw Throwables.propagate(e);
        }
    }

}
