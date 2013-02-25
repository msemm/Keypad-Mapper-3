/**
 * 
 */
package de.enaikoon.android.inviu.opencellidlibrary;

import android.app.IntentService;
import android.content.Intent;

/**
 * @author fox
 * 
 */
public class UploadService extends IntentService
{

	public static final String XTRA_MAXPROGRESS = "maxProgressMade";
	public static final String XTRA_PROGRESS = "progressMade";
	public static final String XTRA_DONE = "done";
	public static final String XTRA_SUCCESS = "success";
	public static final String XTRA_FAILURE_MSG = "failure.message";
	public static final String BROADCAST = "uploadProgressMade";

	public UploadService()
	{
		super("Upload CellIDs");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	@Override
	protected void onHandleIntent(Intent intent)
	{
		try
		{
			boolean success = false;
			String errormsg = "";
			FileLog.writeToLog(getClass().getName() + ": STARTING");
			try
			{
				try
				{
					Uploader upl = new Uploader(CellIDCollectionService.getDatabase(getApplication()), UploadService.this, false) 
					{
						@Override
						public void onStatus(final int count, final int max)
						{

							FileLog.writeToLog(getClass().getName() + ": PROGRESS MADE " + count + "/" + max);
							Intent progress = new Intent(BROADCAST);
							progress.putExtra(XTRA_MAXPROGRESS, max);
							progress.putExtra(XTRA_PROGRESS, count);
							sendBroadcast(progress);
						}
					};
					upl.run();
					success = true;
				} catch (Exception e)
				{
					FileLog.writeExceptionToLog(e);
					errormsg = e.getMessage();
				}
			} finally
			{
				FileLog.writeToLog(getClass().getName() + ": DONE");
				Intent progress = new Intent(BROADCAST);
				progress.putExtra(XTRA_MAXPROGRESS, 0);
				progress.putExtra(XTRA_PROGRESS, 0);
				progress.putExtra(XTRA_DONE, true);
				progress.putExtra(XTRA_SUCCESS, success);
				if (!success)
				{
					progress.putExtra(XTRA_FAILURE_MSG, errormsg);
				}
				sendBroadcast(progress);
			}
		} catch (Exception e)
		{
			FileLog.writeExceptionToLog(e);
		}

	}

}
