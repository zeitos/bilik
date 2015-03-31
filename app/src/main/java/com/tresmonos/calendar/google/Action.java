package com.tresmonos.calendar.google;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.calendar.Calendar;
import com.tresmonos.calendar.model.Reservation;

import java.io.IOException;
import java.util.List;

public interface Action<T> {

    String getAccountName();

    T execute(Calendar client) throws IOException;

    List<Reservation> transform(List<Reservation> reservation);

    boolean isProcessed(List<Reservation> reservations);
}
