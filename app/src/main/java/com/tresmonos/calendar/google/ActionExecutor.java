package com.tresmonos.calendar.google;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.tresmonos.calendar.CalendarAuthenticatorProvider;
import com.tresmonos.calendar.model.Reservation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.greenrobot.event.EventBus;

public class ActionExecutor {

    private static final int REQUEST_AUTHORIZATION = 1;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    private final BlockingQueue<Pair<Action, ActionExecutorCallback>> pendingActions = Queues.newLinkedBlockingQueue();
    private volatile List<Pair<Action, ActionExecutorCallback>> processedActions = Lists.newCopyOnWriteArrayList();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private volatile boolean started = false;
    private Future<?> future = null;

    private static final EventBus eventBus = EventBus.getDefault();
    private static final String APPLICATION_NAME = "bilik";
    private static final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    private static final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    private final Context context;

    protected final CalendarAuthenticatorProvider authenticatorProvider;

    public ActionExecutor(Context context, CalendarAuthenticatorProvider authenticatorProvider) {
        this.authenticatorProvider = authenticatorProvider;
        this.context = context;
    }

    public <T> ActionExecutor execute(Action<T> action, ActionExecutorCallback<T> callback) {
        pendingActions.add(Pair.<Action, ActionExecutorCallback>create(action, callback));
        return this;
    }

    public <T> void executeNow(final Action<T> action, final ActionExecutorCallback<T> callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                executeWithCallback(action, callback);
            }
        });
    }

    private <T> void executeWithCallback(final Action<T> action, final ActionExecutorCallback<T> callback) {
	    try {
		    T result = executeTask(action);
		    callback.onExecutionFinished(true, result);
	    } catch (Throwable t) {
		    callback.onExecutionFinished(false, null);
	    }
    }

    private <T> T executeTask(final Action<T> action) {
        try {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(CalendarScopes.CALENDAR));
            credential.setSelectedAccountName(action.getAccountName());

            // Create Calendar client
            Calendar client = new Calendar.Builder(transport, jsonFactory, credential).setApplicationName(APPLICATION_NAME).build();
            return action.execute(client);
        } catch (GoogleJsonResponseException e) {
            // 404 Not Found would happen if user tries to declineReservationAsync an already deleted calendar
            if (e.getStatusCode() == 404) {
                return null;
            }
            Log.e(this.getClass().getName(), "GoogleJsonResponseException when running calendar Async action", e);
            eventBus.post(e);
	        throw Throwables.propagate(e);
        } catch (GooglePlayServicesAvailabilityIOException availabilityException) {
            eventBus.post(new GoogleCalendarServiceProvider.GoogleAuthenticationRequest(availabilityException.getConnectionStatusCode(), REQUEST_GOOGLE_PLAY_SERVICES));
	        throw Throwables.propagate(availabilityException);
        } catch (UserRecoverableAuthIOException userRecoverableException) {
            Log.e(this.getClass().getName(), "userRecoverableException", userRecoverableException);
            eventBus.post(new GoogleCalendarServiceProvider.GoogleAuthorizationRequest(userRecoverableException.getIntent(), REQUEST_AUTHORIZATION));
	        throw Throwables.propagate(userRecoverableException);
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "IOException when running calendar Async action", e);
            eventBus.post(e);
	        throw Throwables.propagate(e);
        }
    }

    public ActionExecutor start() {
        if (!started) {
            future = executorService.submit(new Runnable() {
                @Override
                public void run() {
                    while (started) {
                        Pair<Action, ActionExecutorCallback> processingAction = null;
                        try {
                            processingAction = pendingActions.take();
                            processedActions.add(processingAction);
                            executeWithCallback(processingAction.first, processingAction.second);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
	                        throw Throwables.propagate(e);
                        }
                    }
                }
            });
            started = true;
        }
        return this;
    }

    public ActionExecutor stop() {
        started = false;
        future.cancel(true);
        executorService.shutdown();
        pendingActions.clear();
        return this;
    }

    public List<Reservation> transformReservations(List<Reservation> reservations) {
        ImmutableList.Builder<Pair<Action, ActionExecutorCallback>> actionsBuilder = ImmutableList.builder();
        actionsBuilder.addAll(processedActions);
        actionsBuilder.addAll(pendingActions);

        ImmutableList<Pair<Action, ActionExecutorCallback>> actions = actionsBuilder.build();
        Log.d("ActionExecutor", "PENDING actions to process: " + actions.size());
        for (Pair<Action, ActionExecutorCallback> pair : actions) {
            Action action = pair.first;
            if (action.isProcessed(reservations))
                processedActions.remove(pair);
            else
                reservations = new ArrayList<Reservation>(action.transform(reservations));
        }
        return reservations;
    }

}
