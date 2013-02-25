package de.enaikoon.android.inviu.opencellidlibrary;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class TestActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		startService(new Intent(this, CellIDCollectionService.class));
	}
}
