package org.team2635.scoutnetclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONObject;
import org.team2635.scoutnetclient.dialogs.UploadPromptDialog;
import org.team2635.scoutnetclient.fragments.AutonomousInfoFragment;
import org.team2635.scoutnetclient.fragments.FieldInfoFragment;
import org.team2635.scoutnetclient.fragments.MatchDefensesFragment;
import org.team2635.scoutnetclient.fragments.TeleopInfoFragment;
import org.team2635.scoutnetclient.utilities.DataManager;
import org.team2635.scoutnetclient.utilities.FieldPagerAdapter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class FieldInfoActivity extends AppCompatActivity implements UploadPromptDialog.NoticeDialogListener
{
    private static final String TAG = "FieldInfo";
    ViewPager viewpager;
    FieldPagerAdapter padapter = new FieldPagerAdapter(getSupportFragmentManager());

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fieldinfo);

        Toolbar toolbar =
                (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        viewpager = (ViewPager) findViewById(R.id.fieldPager);
        viewpager.setAdapter(padapter);
    }

    //TODO: Test fragment titles on action bar
    public void setActionBarTitle(String title)
    {
        getSupportActionBar().setSubtitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_save:
                saveData();
                return true;

            case R.id.action_submit:
                showDialog("uploadPrompt");
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void submitData()
    {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        final DataManager manager = new DataManager(sharedPref);

        String[] urls = manager.getURLArray();
        //TODO: Test address retrieval from settings
        final String address = sharedPref.getString("pref_key_server_ip", "");
        final String pageID = sharedPref.getString("pref_key_server_data_page", "");

        //TODO: Test new data post functionality
        for (final String s : urls)
        {
            Thread thread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    boolean success = true;
                    try
                    {

                        URL url = new URL("http://" + address + "/" + pageID + "?" + s);
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setDoOutput(true);
                        urlConnection.setRequestMethod("POST");
                        urlConnection.setRequestProperty("Content-Type", "application/json");
                        urlConnection.connect();

                        OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
                        out.write(s);
                        out.close();

                        try
                        {
                            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                            Log.d(TAG, in.toString());
                        } finally
                        {
                            urlConnection.disconnect();
                        }

                    } catch (IOException e)
                    {
                        Log.e(TAG, e.toString());
                        FieldInfoActivity.this.runOnUiThread(new Runnable()
                                                             {
                                                                 @Override
                                                                 public void run()
                                                                 {
                                                                     showDialog("uploadFailure");
                                                                 }
                                                             }

                        );
                        success = false;
                    } finally
                    {
                        if (success)
                        {
                            manager.clearData();
                            FieldInfoActivity.this.runOnUiThread(new Runnable()
                                                                 {
                                                                     @Override
                                                                     public void run()
                                                                     {
                                                                         showDialog("success");
                                                                     }
                                                                 }

                            );
                        }
                    }
                }
            });
            thread.start();

            Log.d(TAG, "Extracted and ran a URL! Data: " + s);
        }

        //Clears saved data sets from memory. Prevents duplicate uploads.
        //TODO: Implement checking from server


    }

    private void saveData()
    {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        DataManager manager = new DataManager(sharedPref);
        JSONObject jsonObject = new JSONObject();

        //TODO: Fix getting of scout name
        final String scoutName = sharedPref.getString("pref_key_scout_name", "");

        try
        {
            jsonObject.accumulate("DATATYPE", "matchData");
            jsonObject.accumulate("SCOUTNAME", scoutName);
        } catch (Exception e)
        {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        FieldInfoFragment fieldFrag = (FieldInfoFragment) padapter.getItem(0);
        AutonomousInfoFragment autoFrag = (AutonomousInfoFragment) padapter.getItem(1);
        TeleopInfoFragment teleFrag = (TeleopInfoFragment) padapter.getItem(2);
        MatchDefensesFragment defenseFrag = (MatchDefensesFragment) padapter.getItem(3);


        //Set page to field info
        viewpager.setCurrentItem(0, false);

        //Get data from field info
        String teamnum = fieldFrag.getTeamNum();
        String matchNum = fieldFrag.getMatchNum();
        String teamScore = fieldFrag.getTeamScore();
        String defenseRating = fieldFrag.getDefenseRating();

        try
        {
            jsonObject.accumulate("TEAMNNUM", teamnum);
            jsonObject.accumulate("MATCHNUM", matchNum);
            jsonObject.accumulate("TEAMSCORE", teamScore);
            jsonObject.accumulate("DEFENSERATING", defenseRating);
        } catch (Exception e)
        {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        //Set page to autonomous info
        viewpager.setCurrentItem(1, false);

        //Get info from autonomous fragment
        String autonomous = autoFrag.getAutonomous();
        String defenseCrossed = autoFrag.defenseCrossed();
        String autoHighScores = autoFrag.getHighScores();
        String ownBucketLifted = autoFrag.ownBucketLifted();
        String enemyBucketLifted = autoFrag.enemyBucketLifted();

        try
        {
            jsonObject.accumulate("DOESAUTO", autonomous);

            jsonObject.accumulate("DEFENSECROSSED", defenseCrossed);
            jsonObject.accumulate("OWNBUCKETLIFTED", ownBucketLifted);
            jsonObject.accumulate("ENEMYBUCKETLIFTED", enemyBucketLifted);
            jsonObject.accumulate("AUTOBUNNIES", autoHighScores);

        } catch (Exception e)
        {
            Log.d("InputStream", e.getLocalizedMessage());
        }


        //Set page to teleop fragment
        viewpager.setCurrentItem(2, false);

        //Get info from teleop fragment
        String teleHighScores = teleFrag.getHighScores();

        String didMalfunction = teleFrag.didMalfunction();
        String foundBunny = teleFrag.foundBunny();
        String stoleBunny = teleFrag.stoleBunny();
        String teleNotes = teleFrag.getNotes();

        try
        {
            jsonObject.accumulate("TELEHIGHSCORES", teleHighScores);
            jsonObject.accumulate("MALFUNCTION", didMalfunction);
            jsonObject.accumulate("FOUNDBUNNY", foundBunny);
            jsonObject.accumulate("STOLEBUNNY", stoleBunny);
            jsonObject.accumulate("TELENOTES", teleNotes);
        } catch (Exception e)
        {
            Log.d("InputStream", e.getLocalizedMessage());
        }


        //Set page to match defense
        viewpager.setCurrentItem(3, false);

        String story = defenseFrag.getStory();

        try{
            jsonObject.accumulate("SPECIALNOTES", story);
        } catch (Exception e){
            Log.d("InputStream", e.getLocalizedMessage());
        }


        manager.write(jsonObject.toString());

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        showDialog("dataSaved");
    }


    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
    @Override
    public void onDialogPositiveClick(DialogFragment uploadPromptDialog)
    {
        submitData();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment uploadPromptDialog)
    {
        // User touched the dialog's negative button
    }

    private void showDialog(String dialog)
    {
        switch (dialog)
        {
            case ("success"):
                Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                break;
            case ("uploadPrompt"):
                DialogFragment uploadPromptDialog = new UploadPromptDialog();
                uploadPromptDialog.show(getSupportFragmentManager(), "NoticeDialogFragment");
                break;
            case ("dataSaved"):
                Toast.makeText(getApplicationContext(), "Data Saved", Toast.LENGTH_SHORT).show();
                break;
            case ("uploadFailure"):
                Toast.makeText(getApplicationContext(), "Upload Failed", Toast.LENGTH_SHORT).show();
                break;
            default:
                Log.e(TAG, "Expected 'success' or 'uploadPrompt' for showDialog(), got " + dialog);
                break;
        }

    }
}
