package com.jvanier.android.sendtocar.controllers.commands;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.Toast;

import com.jvanier.android.sendtocar.R;
import com.jvanier.android.sendtocar.common.Constants;
import com.jvanier.android.sendtocar.common.Log;

public class SendDebugLogToDeveloper implements Command {
	@Override
	public void perfrom(Context context) {
		String logContents = Log.readLog(context);
		Spanned logContentsEncoded = Html.fromHtml(TextUtils.htmlEncode(logContents).replace("\n", "<br>\n"));

		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/html");
		String aEmailList[] = { Constants.DEVELOPER_EMAIL };
		intent.putExtra(android.content.Intent.EXTRA_EMAIL, aEmailList);
		intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Send To Car Debug Log");
		intent.putExtra(android.content.Intent.EXTRA_TEXT, logContentsEncoded);

		try {
			context.startActivity(intent);
		} catch(ActivityNotFoundException e) {
			Toast toast = Toast.makeText(context, R.string.errorNoApp, Toast.LENGTH_SHORT);
			toast.show();
		}
	}
}
