package com.thomashofmann.xposed.lib;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Paypal {
    public static Intent createDonationIntent(Context context,String emailAddress, String subject, String currencyCode) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("https").authority("www.paypal.com").path("cgi-bin/webscr");
        uriBuilder.appendQueryParameter("cmd", "_donations");

        uriBuilder.appendQueryParameter("business", emailAddress);
        uriBuilder.appendQueryParameter("item_name", subject);
        uriBuilder.appendQueryParameter("no_note", "1");
        uriBuilder.appendQueryParameter("no_shipping", "1");
        uriBuilder.appendQueryParameter("currency_code", currencyCode);
        Uri payPalUri = uriBuilder.build();

        Intent donateIntent = new Intent(Intent.ACTION_VIEW, payPalUri);
        donateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return donateIntent;
    }
}
