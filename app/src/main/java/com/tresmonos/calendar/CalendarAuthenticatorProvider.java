package com.tresmonos.calendar;

import android.content.Intent;

public interface CalendarAuthenticatorProvider {

    void requestAuthentication(int authInfo);

    void requestAuthorization(Intent intent);

}
