package com.tresmonos.calendar.model;

import java.util.List;

/**
 * Created by david on 8/20/14.
 */
public interface Resource {

    String getShortName();

    String getFullName();

    String getAccountName();

    String getEmail();

    List<Reservation> getReservations();


}
