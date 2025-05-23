package com.example.flightbookingapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class FlightListActivity extends AppCompatActivity {
    private static final String LOG_TAG = FlightListActivity.class.getName();
    private FirebaseUser user;

    private FrameLayout redCircle;
    private TextView countTextView;
    private int cartItems = 0;
    private int gridNumber = 1;
    private Integer itemLimit = 5;

    // Member variables.
    private RecyclerView mRecyclerView;
    private ArrayList<FlightItem> mItemsData;
    private FlightItemAdapter mAdapter;

    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;

    private NotificationHelper mNotificationHelper;
    private AlarmManager mAlarmManager;
    private JobScheduler mJobScheduler;
    private boolean isMenuPrepared = false;
    private boolean viewRow = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_list);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            Log.d(LOG_TAG, "Authenticated user!");
        } else {
            Log.d(LOG_TAG, "Unauthenticated user!");
            finish();
        }

        // recycle view
        mRecyclerView = findViewById(R.id.recyclerView);
        // Set the Layout Manager.
        mRecyclerView.setLayoutManager(new GridLayoutManager(
                this, gridNumber));
        // Initialize the ArrayList that will contain the data.
        mItemsData = new ArrayList<>();
        // Initialize the adapter and set it to the RecyclerView.
        mAdapter = new FlightItemAdapter(this, mItemsData);
        mRecyclerView.setAdapter(mAdapter);

        mFirestore = FirebaseFirestore.getInstance();
        mItems = mFirestore.collection("Flights");
        queryData();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        this.registerReceiver(powerReceiver, filter);

        mNotificationHelper = new NotificationHelper(this);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mJobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        // setAlarmManager();
        setJobScheduler();
    }

    BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();

            if (intentAction == null)
                return;

            switch (intentAction) {
                case Intent.ACTION_POWER_CONNECTED:
                    itemLimit = 10;
                    queryData();
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    itemLimit = 5;
                    queryData();
                    break;
            }
        }
    };

    private void initializeData() {
        // Get the resources from the XML file.
        String[] itemsList = getResources()
                .getStringArray(R.array.flight_names);
        String[] itemsInfo = getResources()
                .getStringArray(R.array.flight_desc);
        String[] itemsPrice = getResources()
                .getStringArray(R.array.flight_price);
        TypedArray itemsImageResources =
                getResources().obtainTypedArray(R.array.flight_images);
        TypedArray itemRate = getResources().obtainTypedArray(R.array.flight_rates);

        // Create the ArrayList of Sports objects with the titles and
        // information about each sport.
        for (int i = 0; i < itemsList.length; i++) {
            mItems.add(new FlightItem(
                    itemsList[i],
                    itemsInfo[i],
                    itemsPrice[i],
                    itemRate.getFloat(i, 0),
                    itemsImageResources.getResourceId(i, 0),
                    0));
        }

        // Recycle the typed array.
        itemsImageResources.recycle();
    }

    private void queryData() {
        mItemsData.clear();
        mItems.orderBy("cartedCount", Query.Direction.DESCENDING).limit(itemLimit).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        FlightItem item = document.toObject(FlightItem.class);
                        item.setId(document.getId());
                        mItemsData.add(item);
                    }

                    if (mItemsData.size() == 0) {
                        initializeData();
                        queryData();
                    }

                    // Notify the adapter of the change.
                    mAdapter.notifyDataSetChanged();
                });
    }

    private void queryDataByRating() {
        mItemsData.clear();
        mItems.orderBy("ratedInfo", Query.Direction.ASCENDING).limit(itemLimit).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        FlightItem item = document.toObject(FlightItem.class);
                        item.setId(document.getId());
                        mItemsData.add(item);
                    }
                    mAdapter.notifyDataSetChanged();
                });
    }

    private void queryDataByName() {
        mItemsData.clear();
        mItems.orderBy("name", Query.Direction.ASCENDING).limit(itemLimit).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        FlightItem item = document.toObject(FlightItem.class);
                        item.setId(document.getId());
                        mItemsData.add(item);
                    }
                    mAdapter.notifyDataSetChanged();
                });
    }


    public void deleteItem(FlightItem item) {
        DocumentReference ref = mItems.document(item._getId());
        ref.delete()
                .addOnSuccessListener(success -> {
                    Log.d(LOG_TAG, "Item is successfully deleted: " + item._getId());
                })
                .addOnFailureListener(fail -> {
                    Toast.makeText(this, "Item " + item._getId() + " cannot be deleted.", Toast.LENGTH_LONG).show();
                });

        queryData();
        mNotificationHelper.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Inflate the menu
        getMenuInflater().inflate(R.menu.flight_list_menu, menu);

        // Find the search menu item
        MenuItem menuItem = menu.findItem(R.id.search_bar);

        // Get the action view, which is the SearchView
        SearchView searchView = (SearchView) menuItem.getActionView();

        // Ensure the search view is set up properly
        if (searchView != null) {
            // Set the query text listener to perform the search
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    // This is called when the search button is clicked (optional)
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // This is called when the search query is updated
                    Log.d(LOG_TAG, "Query text: " + newText);
                    if (mAdapter != null) {
                        mAdapter.getFilter().filter(newText); // Filter the adapter based on the text
                    }
                    return false;
                }
            });
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.log_out_button) {
            Log.d(LOG_TAG, "Logout clicked!");
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;
        } else if (id == R.id.settings_button) {
            Log.d(LOG_TAG, "Setting clicked!");
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;
        } else if (id == R.id.cart) {
            Log.d(LOG_TAG, "Cart clicked!");
            return true;
        }else if (id == R.id.sort_by_rating) {
            Log.d(LOG_TAG, "Sort by rating clicked!");
            queryDataByRating();
            return true;
        } else if (id == R.id.sort_by_name) {
            Log.d(LOG_TAG, "Sort by name clicked!");
            queryDataByName();
            return true;
        }else if (id == R.id.profile_button) {
            Log.d(LOG_TAG, "Profile clicked!");
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            return true;
        }else if (id == R.id.view_selector) {
            if (viewRow) {
                changeSpanCount(item, R.drawable.ic_view_grid, 1);
            } else {
                changeSpanCount(item, R.drawable.ic_view_row, 2);
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    private void changeSpanCount(MenuItem item, int drawableId, int spanCount) {
        viewRow = !viewRow;
        item.setIcon(drawableId);
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.setSpanCount(spanCount);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        isMenuPrepared = true;

        final MenuItem alertMenuItem = menu.findItem(R.id.cart);
        FrameLayout rootView = (FrameLayout) alertMenuItem.getActionView();

        assert rootView != null;
        redCircle = rootView.findViewById(R.id.view_alert_red_circle);
        countTextView = rootView.findViewById(R.id.view_alert_count_textview);

        rootView.setOnClickListener(v -> onOptionsItemSelected(alertMenuItem));
        return super.onPrepareOptionsMenu(menu);
    }

    public void updateAlertIcon(FlightItem item) {
        if (!isMenuPrepared) {
            // If the user is not logged in, redirect to login
            Toast.makeText(this, "You can't book a flight without logging in.", Toast.LENGTH_LONG).show();

            // Redirect to the login screen (this is just an example, replace with your actual login activity)
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();  // Optional: If you want to close the current activity

            return;
        }
        cartItems = (cartItems + 1);
        if (countTextView != null) {
            if (cartItems > 0) {
                countTextView.setText(String.valueOf(cartItems));
            } else {
                countTextView.setText("");
            }
        } else {
            Log.e("FlightListActivity", "countTextView is null in updateAlertIcon");
        }

        redCircle.setVisibility((cartItems > 0) ? VISIBLE : GONE);

        mItems.document(item._getId()).update("cartedCount", item.getCartedCount() + 1)
                .addOnFailureListener(fail -> {
                    Toast.makeText(this, "Item " + item._getId() + " cannot be changed.", Toast.LENGTH_LONG).show();
                });

        mNotificationHelper.send(item.getName());
        queryData();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(powerReceiver);

    }

    private void setAlarmManager() {
        long repeatInterval = 60000;
        long triggerTime = SystemClock.elapsedRealtime() + repeatInterval;

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mAlarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                repeatInterval,
                pendingIntent);


        mAlarmManager.cancel(pendingIntent);
    }

    private void setJobScheduler() {
        // SeekBar, Switch, RadioButton
        int networkType = JobInfo.NETWORK_TYPE_UNMETERED;
        boolean isDeviceCharging = true;
        int hardDeadline = 5000;

        ComponentName serviceName = new ComponentName(getPackageName(), NotificationJobService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceName)
                .setRequiredNetworkType(networkType)
                .setRequiresCharging(isDeviceCharging)
                .setOverrideDeadline(hardDeadline);

        JobInfo jobInfo = builder.build();
        mJobScheduler.schedule(jobInfo);

    }
}