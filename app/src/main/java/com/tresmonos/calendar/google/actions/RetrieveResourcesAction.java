package com.tresmonos.calendar.google.actions;

import android.content.Context;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Strings;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.tresmonos.calendar.CalendarAuthenticatorProvider;
import com.tresmonos.calendar.Configuration;
import com.tresmonos.calendar.google.GAction;
import com.tresmonos.calendar.model.RemoteResource;
import com.tresmonos.calendar.model.Reservation;
import com.tresmonos.calendar.model.Resource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RetrieveResourcesAction extends GAction<List<RemoteResource>> {

    private final Context context;
    private final Configuration configuration;
    private final String accountName;
    private final CalendarAuthenticatorProvider authenticatorProvider;
    private Map<String, Configuration.ParseResource> parseResourcesByResourceName = null;

    public RetrieveResourcesAction(Context context, CalendarAuthenticatorProvider authenticatorProvider, String accountName) {
        this.context = context;
        this.configuration = Configuration.getInstance(this.context);
        this.authenticatorProvider = authenticatorProvider;
        this.accountName = accountName;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public List<RemoteResource> execute(Calendar client) throws IOException {
        List<RemoteResource> reservations = Lists.newArrayList();
        String pageToken = null;
        do {
            CalendarList calendarList = client.calendarList().list().setPageToken(pageToken).execute();
            List<CalendarListEntry> items = calendarList.getItems();

            for (CalendarListEntry calendarListEntry : items) {
                reservations.add(createRemoteResource(calendarListEntry));
            }
            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);

        return reservations;
    }

    RemoteResource createRemoteResource(CalendarListEntry entry) throws IOException {
        String resourceId = entry.getId();
        String summary = entry.getSummary();
        Configuration.ParseResource parseResource = getParseResourcesByResourceName().get(summary);
        boolean managedByBilik = parseResource != null;
        return new RemoteResource(this.accountName,
                resourceId,
                summary,
                entry.getDescription(),
                managedByBilik ? parseResource.currentState : null,
                managedByBilik ? parseResource.lastUpdateDate : null,
                managedByBilik ? parseResource.nextEventDate : null,
                managedByBilik,
                managedByBilik ? parseResource.alternativeName : summary,
                managedByBilik ? parseResource.area : null);
    }

    protected Map<String, Configuration.ParseResource> getParseResourcesByResourceName() throws IOException {
        if (parseResourcesByResourceName == null) {
            parseResourcesByResourceName = Strings.isNullOrEmpty(accountName)
                    ? ImmutableMap.<String, Configuration.ParseResource>of()
                    : Maps.uniqueIndex(configuration.getResources(accountName), Configuration.ParseResource.RESOURCE_NAME_BY_PARSE_RESOURCE);
        }
        return parseResourcesByResourceName;
    }

    @Override
    public List<Reservation> transform(List reservations) {
        return reservations;
    }

    @Override
    public boolean isProcessed(List<Reservation> reservations) {
        return true;
    }
}
