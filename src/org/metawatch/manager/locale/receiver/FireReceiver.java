/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.metawatch.manager.locale.receiver;

import java.io.IOException;
import java.io.InputStream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.Log;

import org.metawatch.manager.locale.Constants;
import org.metawatch.manager.locale.bundle.BundleScrubber;
import org.metawatch.manager.locale.bundle.PluginBundleManager;
import org.metawatch.manager.locale.ui.EditActivity;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 */
public final class FireReceiver extends BroadcastReceiver {

	static Typeface typeface = null; 
	
	/**
	 * @param context
	 *            {@inheritDoc}.
	 * @param intent
	 *            the incoming
	 *            {@link com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING}
	 *            Intent. This should contain the
	 *            {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} that was
	 *            saved by {@link EditActivity} and later broadcast by Locale.
	 */
	@Override
	public void onReceive(final Context context, final Intent intent)
	{
		/*
		 * Always be sure to be strict on input parameters! A malicious third-party app could always send an empty or otherwise
		 * malformed Intent. And since Locale applies settings in the background, the plug-in definitely shouldn't crash in the
		 * background.
		 */

		/*
		 * Locale guarantees that the Intent action will be ACTION_FIRE_SETTING
		 */
		if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction()))
		{
			if (Constants.IS_LOGGABLE)
			{
				Log.e(Constants.LOG_TAG, String.format("Received unexpected Intent action %s", intent.getAction())); //$NON-NLS-1$
			}
			return;
		}

		/*
		 * A hack to prevent a private serializable classloader attack
		 */
		BundleScrubber.scrub(intent);
		BundleScrubber.scrub(intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE));

		final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

		/*
		 * Final verification of the plug-in Bundle before firing the setting.
		 */
		if (PluginBundleManager.isBundleValid(bundle))
		{
			if (Constants.IS_LOGGABLE)
			{
				Log.d(Constants.LOG_TAG, "sending notification"); //$NON-NLS-1$
			}

			final String type = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_TYPE);

			if (type.equals("notification")) 
			{
				Intent broadcast = new Intent("org.metawatch.manager.NOTIFICATION");
				Bundle b = new Bundle();
				b.putString("title", bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_TITLE));
				b.putString("text", bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE));
				b.putInt("vibrate_on", 500);
				b.putInt("vibrate_off", 200);
				b.putInt("vibrate_cycles", 2);
				broadcast.putExtras(b);

				context.sendBroadcast(broadcast);
			}
			else if (type.equals("widget")) {
				final String icon = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_WIDGET_ICON);
				final String widgetId = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_WIDGET_ID);
				final String widgetLabel = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_WIDGET_LABEL);
				
				if (typeface==null) {
					typeface = Typeface.createFromAsset(context.getAssets(), "metawatch_8pt_5pxl_CAPS.ttf");
				}
				
				TextPaint paintSmall = new TextPaint();
				paintSmall.setColor(Color.BLACK);
				paintSmall.setTextSize(8);
				paintSmall.setTypeface(typeface);
				paintSmall.setTextAlign(Align.CENTER);
				
				// Create 16x16 widget
				{
					Bitmap iconBmp = loadBitmapFromAssets(context, icon+"_10.bmp");
					
					Bitmap bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.RGB_565);
					Canvas canvas = new Canvas(bitmap);
					canvas.drawColor(Color.WHITE);
					
					canvas.drawBitmap(iconBmp, 2, 0, null);					
					canvas.drawText(widgetLabel, 8, 15, paintSmall);
					
					Intent i = createUpdateIntent(bitmap, "localeMWM_"+widgetId+"_16_16", "Locale Plugin Widget (16x16)", 1);
					context.sendBroadcast(i);
				}
				
				// Create 24x32 widget
				{
					Bitmap iconBmp = loadBitmapFromAssets(context, icon+".bmp");
					
					Bitmap bitmap = Bitmap.createBitmap(24, 32, Bitmap.Config.RGB_565);
					Canvas canvas = new Canvas(bitmap);
					canvas.drawColor(Color.WHITE);
					
					canvas.drawBitmap(iconBmp, 0, 3, null);
					canvas.drawText(widgetLabel, 12, 30, paintSmall);
					
					Intent i = createUpdateIntent(bitmap, "localeMWM_"+widgetId+"_24_32", "Locale Plugin Widget (24x32)", 1);
					context.sendBroadcast(i);
				}
			}
			
		}
		else
		{
			if (Constants.IS_LOGGABLE)
			{
				Log.d(Constants.LOG_TAG, "bundle invalid"); //$NON-NLS-1$
			}
		}
	}
	
	private static Bitmap loadBitmapFromAssets(Context context, String path) {
		try {
			InputStream inputStream = context.getAssets().open(path);
	        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
	        inputStream.close();
	        //if (Preferences.logging) Log.d(MetaWatch.TAG, "ok");
	        return bitmap;
		} catch (IOException e) {
			//if (Preferences.logging) Log.d(MetaWatch.TAG, e.toString());
			return null;
		}
	}
	
	/**
	 * @param bitmap Widget image to send
	 * @param id ID of this widget - should be unique, and sensibly identify
	 *        the widget
	 * @param description User friendly widget name (will be displayed in the
	 * 		  widget picker)
	 * @param priority A value that indicates how important this widget is, for
	 * 		  use when deciding which widgets to discard.  Lower values are
	 *        more likely to be discarded.
	 * @return Filled-in intent, ready for broadcast.
	 */
	private static Intent createUpdateIntent(Bitmap bitmap, String id, String description, int priority) {
		int pixelArray[] = new int[bitmap.getWidth() * bitmap.getHeight()];
		bitmap.getPixels(pixelArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

		Intent intent = new Intent("org.metawatch.manager.WIDGET_UPDATE");
		Bundle b = new Bundle();
		b.putString("id", id);
		b.putString("desc", description);
		b.putInt("width", bitmap.getWidth());
		b.putInt("height", bitmap.getHeight());
		b.putInt("priority", priority);
		b.putIntArray("array", pixelArray);
		intent.putExtras(b);

		return intent;
	}
}