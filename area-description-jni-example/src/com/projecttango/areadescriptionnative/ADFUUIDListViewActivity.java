/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.projecttango.areadescriptionnative;

import java.io.File;
import java.util.Arrays;

import com.projecttango.areadescriptionnative.SetADFNameDialog.SetNameAndUUIDCommunicator;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class lets you manage ADFs between this class's Application Package folder 
 * and API private space. This show cases mainly three things:
 * Import, Export, Delete an ADF file from API private space to any known and accessible file path.
 *
 */
public class ADFUUIDListViewActivity extends Activity implements SetNameAndUUIDCommunicator{
  private static final String INTENT_CLASSPACKAGE = "com.projecttango.tango";
  private static final String INTENT_REQUESTPERMISSION_CLASSNAME = "com.google.atap.tango.RequestPermissionActivity";
  private static final String INTENT_IMPORTEXPORT_CLASSNAME = "com.google.atap.tango.RequestImportExportActivity";

  // startActivityForResult requires a code number.
  public static final int TANGO_INTENT_ACTIVITYCODE = 1129;
  private static final String EXTRA_KEY_SOURCEUUID = "SOURCE_UUID";
  private static final String EXTRA_KEY_DESTINATIONFILE = "DESTINATION_FILE";
  private static final String EXTRA_KEY_SOURCEFILE = "SOURCE_FILE";
  public static final String EXTRA_KEY_DESTINATIONUUID = "DESTINATION_UUID";

  private ADFDataSource mADFDataSource;
  private ListView mUUIDListView,mAppSpaceUUIDListView;
  ADFUUIDArrayAdapter mADFAdapter,mAppSpaceADFAdapter;
  String[] mUUIDList,mUUIDNames,mAppSpaceUUIDList,mAppSpaceUUIDNames;
  String[] mAPISpaceMenuStrings,mAppSpaceMenuStrings;
  String mAppSpaceADFFolder;

  Activity thisActivity;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.uuid_listview);
    mAPISpaceMenuStrings = getResources().getStringArray(R.array.SetDialogMenuItemsAPISpace);
    mAppSpaceMenuStrings = getResources().getStringArray(R.array.SetDialogMenuItemsAppSpace);
    
    // Get API ADF ListView ready.
    mUUIDListView = (ListView) findViewById(R.id.uuidlistviewAPI);
    mADFDataSource = new ADFDataSource(this);
    mUUIDList =  mADFDataSource.getFullUUIDList();
    mUUIDNames = mADFDataSource.getUUIDNames();
    mADFAdapter = new ADFUUIDArrayAdapter(this, mUUIDList, mUUIDNames);
    mUUIDListView.setAdapter(mADFAdapter);
    registerForContextMenu(mUUIDListView);
    
    // Get apps space ADF list ready.
    mAppSpaceUUIDListView = (ListView) findViewById(R.id.uuidlistviewApplicationSpace);
    mAppSpaceADFFolder = getAppSpaceADFFolder();
    mAppSpaceUUIDList = getAppSpaceADFList();
    mAppSpaceADFAdapter = new ADFUUIDArrayAdapter(this, mAppSpaceUUIDList, null);
    mAppSpaceUUIDListView.setAdapter(mAppSpaceADFAdapter);
    registerForContextMenu(mAppSpaceUUIDListView);

    thisActivity = this;
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    if (v.getId() == R.id.uuidlistviewAPI) {
      AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
      
      menu.setHeaderTitle(mUUIDList[info.position]);
      menu.add(mAPISpaceMenuStrings[0]);
      menu.add(mAPISpaceMenuStrings[1]);
      menu.add(mAPISpaceMenuStrings[2]);
    }
    
    if (v.getId() == R.id.uuidlistviewApplicationSpace) {
      AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
      menu.setHeaderTitle(mAppSpaceUUIDList[info.position]);
      menu.add(mAppSpaceMenuStrings[0]);
      menu.add(mAppSpaceMenuStrings[1]);
    }
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    String itemName = (String) item.getTitle();
    // Delete the ADF from API storage and update the API ADF Listview.
    if (itemName.equals(mAPISpaceMenuStrings[0])) {
      showSetNameDialog(mUUIDList[info.position]);
    } 
    // Delete the ADF from API storage and update the API ADF Listview.
    else if (itemName.equals(mAPISpaceMenuStrings[1])) {
      mADFDataSource.deleteADFandUpdateList(mUUIDList[info.position]);
      // Update the API ADF Listview.
      mUUIDList = mADFDataSource.getFullUUIDList();
      mUUIDNames = mADFDataSource.getUUIDNames();
      mADFAdapter = new ADFUUIDArrayAdapter(this, mUUIDList, mUUIDNames);
      mUUIDListView.setAdapter(mADFAdapter);
    } 

    // Export the ADF into application package folder and update the Listview.
    else if (itemName.equals(mAPISpaceMenuStrings[2])) {
      Intent exportIntent = new Intent();
      exportIntent.setClassName(INTENT_CLASSPACKAGE, INTENT_IMPORTEXPORT_CLASSNAME);
      exportIntent.putExtra(EXTRA_KEY_SOURCEUUID, mUUIDList[info.position]);
      exportIntent.putExtra(EXTRA_KEY_DESTINATIONFILE, mAppSpaceADFFolder);
      thisActivity.startActivityForResult(exportIntent, TANGO_INTENT_ACTIVITYCODE);
    }
    
    // Delete an ADF from App space and update the App space ADF Listview.
    else if (itemName.equals(mAppSpaceMenuStrings[0])) {
      File file = new File(mAppSpaceADFFolder + File.separator + mAppSpaceUUIDList[info.position]);
      file.delete();
      // Update App space ADF ListView.
      mAppSpaceUUIDList = getAppSpaceADFList();
      mAppSpaceADFAdapter = new ADFUUIDArrayAdapter(this, mAppSpaceUUIDList, null);
      mAppSpaceUUIDListView.setAdapter(mAppSpaceADFAdapter);
    }
    
    // Import an ADF into API private Storage and update the API ADF Listview.
    else if (itemName.equals(mAppSpaceMenuStrings[1])) {
      String filepath = mAppSpaceADFFolder + File.separator + mAppSpaceUUIDList[info.position];

      Intent importIntent = new Intent();
      importIntent.setClassName(INTENT_CLASSPACKAGE, INTENT_IMPORTEXPORT_CLASSNAME);
      importIntent.putExtra(EXTRA_KEY_SOURCEFILE, filepath);
      thisActivity.startActivityForResult(importIntent, TANGO_INTENT_ACTIVITYCODE);
    }
    return true;
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Update App space ADF Listview.
    mAppSpaceUUIDList = getAppSpaceADFList();
    mAppSpaceADFAdapter = new ADFUUIDArrayAdapter(this, mAppSpaceUUIDList, null);
    mAppSpaceUUIDListView.setAdapter(mAppSpaceADFAdapter);
    
    // Update API ADF Listview.
    mUUIDList = mADFDataSource.getFullUUIDList();
    mUUIDNames = mADFDataSource.getUUIDNames();
    mADFAdapter = new ADFUUIDArrayAdapter(this, mUUIDList, mUUIDNames);
    mUUIDListView.setAdapter(mADFAdapter);
  }

  /* Returns maps storage location in the App package folder. 
   * Creates a folder called Maps, if it doesnt exist.
   */
  private String getAppSpaceADFFolder() {
    String mapsFolder = getFilesDir().getAbsolutePath() + File.separator + "Maps";
    File file = new File(mapsFolder);
    if (!file.exists())
      file.mkdirs();
    return mapsFolder;
  }
  
  /*
   * Returns the names of all ADFs in String array in the
   * files/maps folder.
   */
  private String[] getAppSpaceADFList() {
    File file = new File(mAppSpaceADFFolder);
    File[] ADFFileList = file.listFiles();
    String[] appSpaceADFList = new String[ADFFileList.length];
    for(int i=0; i<appSpaceADFList.length; ++i) {
      appSpaceADFList[i] = ADFFileList[i].getName();
    }
    Arrays.sort(appSpaceADFList);
    return appSpaceADFList;
  }
  
  private void showSetNameDialog(String mCurrentUUID) {
    Bundle bundle = new Bundle();
    String name = TangoJNINative.GetUUIDMetadataValue(mCurrentUUID, "name");
    if (name != null) {
      bundle.putString("name", name);
    }
    bundle.putString("id", mCurrentUUID);
    FragmentManager manager = getFragmentManager();
    SetADFNameDialog setADFNameDialog = new SetADFNameDialog();
    setADFNameDialog.setArguments(bundle);
    setADFNameDialog.show(manager, "ADFNameDialog");
  }

  @Override
  public void SetNameAndUUID(String name, String uuid) {
    TangoJNINative.SetUUIDMetadataValue(uuid, "name", name.length(), name);

    mUUIDList = mADFDataSource.getFullUUIDList();
    mUUIDNames = mADFDataSource.getUUIDNames();
    mADFAdapter = new ADFUUIDArrayAdapter(this, mUUIDList, mUUIDNames);
    mUUIDListView.setAdapter(mADFAdapter);
  }
}

/**
 * This is an adapter class which maps the ListView with 
 * a Data Source(Array of strings)
 *
 */
class ADFUUIDArrayAdapter extends ArrayAdapter<String> {
  Context mContext;
  private String[] mUUIDStringArray,mUUIDNamesStringArray;
  public ADFUUIDArrayAdapter(Context context,String[] uuids,String[] uuidNames) {
    super(context, R.layout.uuid_view, R.id.uuid, uuids);
    mContext = context;
    mUUIDStringArray = uuids;
    if (uuidNames != null) {
      mUUIDNamesStringArray = uuidNames;
    }
  }
  
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LayoutInflater inflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View row = inflator.inflate(R.layout.uuid_view, parent, false);
    TextView uuid = (TextView) row.findViewById(R.id.uuid);
    TextView uuidName = (TextView) row.findViewById(R.id.adfName);
    uuid.setText(mUUIDStringArray[position]);
    
    if (mUUIDNamesStringArray != null) {
      uuidName.setText(mUUIDNamesStringArray[position]);
    }
    else
      uuidName.setText("Metadata cannot be read");
    return row;
  }
}