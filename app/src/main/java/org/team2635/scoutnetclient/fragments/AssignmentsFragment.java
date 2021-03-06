package org.team2635.scoutnetclient.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.team2635.scoutnetclient.PitInfoActivity;
import org.team2635.scoutnetclient.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class AssignmentsFragment extends ListFragment implements AdapterView.OnItemClickListener
{
    //TODO: Implement saving assignment array
    //TODO: TEST clickable functionality for list elements
    //TODO: TEST Click functionality linking to data submission page
    //TODO: TEST Autofill team number in submission page based on list element

    //TODO: The pink panther's TODO list:
    //TODO: todo, todo todo todo todo toooodoooooo

    /** Called when the activity is first created. */
    @SuppressWarnings("unchecked")

    private int teamNumber = 0000;
    private final String TAG = "AssignmentsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setListAdapter(new ArrayAdapter(getContext(), R.layout.fragment_scouting_assignments, this.populate()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        return inflater.inflate(R.layout.fragment_teaminfo,container,false);
    }

    public void onItemClick(AdapterView parent, View v, int position, long id)
    {
        Intent intent = new Intent(getContext(), PitInfoActivity.class);
        //TODO: Get ID of clicked element
        intent.putExtra("TEAMNUMBER", teamNumber);
        startActivity(intent);
    }

    private ArrayList populate() {
        ArrayList items = new ArrayList();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String adress = sharedPref.getString("pref_key_server_ip", "");

        try {
            URL url = new URL
                    // TODO - change line below to your own domain
                    (adress);
            HttpURLConnection urlConnection =
                    (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();

            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(
                            urlConnection.getInputStream()));

            String next;
            while ((next = bufferedReader.readLine()) != null){
                JSONArray ja = new JSONArray(next);

                for (int i = 0; i < ja.length(); i++) {
                    JSONObject jo = (JSONObject) ja.get(i);
                    items.add(jo.getString("food_name"));
                }
            }
        } catch (JSONException | IOException e) {

            Log.e(TAG, e.toString());
        }
        return items;
    }


}
