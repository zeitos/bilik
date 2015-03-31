package com.tresmonos.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.BASE64EncoderStream;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

public class GMailOauthSender {
	private Session session;
	private String token;
	private String sender;

	public String getToken() {
		return token;
	}

	public GMailOauthSender(Activity ctx, String accountName) {
		super();
		initToken(ctx, accountName);
	}

	private void initToken(Activity ctx, String accountName) {
		AccountManager am = AccountManager.get(ctx);
		Account selectedAccount = null;
		for (Account account : am.getAccountsByType("com.google")) {
			if (accountName.equals(account.name)) {
				selectedAccount = account;
				break;
			}
		}
		if (selectedAccount == null) {
			Log.e("initToken", "Invalid account name: " + accountName);
		}
		sender = selectedAccount.name;
		am.getAuthToken(selectedAccount, "oauth2:https://mail.google.com/", null, ctx, new AccountManagerCallback<Bundle>(){
			@Override
			public void run(AccountManagerFuture<Bundle> result){
				try{
					Bundle bundle = result.getResult();
					token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
					Log.d("initToken callback", "token="+token);

				} catch (Exception e){
					Log.e("test", e.getMessage());
				}
			}
		}, null);
		Log.d("getToken", "token="+token);
	}

	private SMTPTransport connectToSmtp(String host, int port, String userEmail, boolean debug) throws Exception {

		Properties props = new Properties();
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.starttls.required", "true");
		props.put("mail.smtp.sasl.enable", "false");

		session = Session.getInstance(props);
		session.setDebug(debug);

		final URLName unusedUrlName = null;
		SMTPTransport transport = new SMTPTransport(session, unusedUrlName);
		// If the password is non-null, SMTP tries to do AUTH LOGIN.
		final String emptyPassword = null;

        /* enable if you use this code on an Activity (just for test) or use the AsyncTask
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
         */

		transport.connect(host, port, userEmail, emptyPassword);

		byte[] response = String.format("user=%s\1auth=Bearer %s\1\1",
				userEmail, token).getBytes();
		response = BASE64EncoderStream.encode(response);

		transport.issueCommand("AUTH XOAUTH2 " + new String(response), 235);

		return transport;
	}

	public synchronized void sendMail(String subject, String body, String recipients) throws Exception {
		SMTPTransport smtpTransport = connectToSmtp("smtp.gmail.com", 587,
				sender, true);

		MimeMessage message = new MimeMessage(session);
		DataHandler handler = new DataHandler(new ByteArrayDataSource(
				body.getBytes(), "text/plain"));
		message.setSender(new InternetAddress(sender));
		message.setSubject(subject);
		message.setDataHandler(handler);
		if (recipients.indexOf(',') > 0)
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(recipients));
		else
			message.setRecipient(Message.RecipientType.TO,
					new InternetAddress(recipients));
		smtpTransport.sendMessage(message, message.getAllRecipients());
	}

}