package com.example.findreal;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.navigation.NavigationView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.helper.HttpConnection;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    public static String email;
    private View header;
    private DrawerLayout drawerLayout;

    @RequiresApi(api = Build.VERSION_CODES.N)

    @Override
    protected void onResume(){
        super.onResume();
        Log.i("resume","resume");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        email = "flerika_kg@naver.com";

        if(email == null){
            Intent userdata = getIntent();
            email = userdata.getStringExtra("token");
            if(email == null){
                Toast.makeText(this, "Invalid Access", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        // Hide Navigation Bar - doesn't work well
        // getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        if(Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy((policy));
        }

        // initialize navigation menu
        initializeNavigation(email);

        // Initialize requests
        LinearLayout requests = (LinearLayout) findViewById(R.id.requests);
        requests.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, RequestListActivity.class);
//                startActivity(intent);
                Log.d(TAG, "changed to RequestListActivity");
            }
        });
        initializeRequests(email);

        // initialize News list
        try {
            initializeNewsList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeNavigation(String email) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView useremail = (TextView) headerView.findViewById(R.id.user_email);
        useremail.setText(email);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                menuItem.setChecked(false);
                drawerLayout.closeDrawers();
                Intent intent;

                switch (menuItem.getItemId())
                {
                    case R.id.menu_mypage:
                        intent = new Intent (MainActivity.this, MyPageActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.menu_info:
                        intent = new Intent (MainActivity.this, InformationActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.menu_services:
                        intent = new Intent (MainActivity.this, ServicesActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.menu_settings:
                        intent = new Intent (MainActivity.this, SettingsActivity.class);
                        startActivity(intent);
                        break;
//                    case R.id.menu_logout:
//                        intent = new Intent (MainActivity.this, LoginActivity.class);
//                        startActivity(intent);
//                        break;
                }
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void initializeRequests(String email) {
        // Requests
        TextView ongoingRequests   = (TextView) findViewById(R.id.ongoing_requests);
        TextView completedRequests = (TextView) findViewById(R.id.completed_requests);

        String url = "http://3.35.41.92:3000/requests";
        int[] result = sendPost(email, url);

        Integer numOfOngoingRequests = result[0];
        Integer numOfCompletedRequests = result[1];

        if (numOfOngoingRequests <= 1){
            ongoingRequests.setText(numOfOngoingRequests + " Ongoing Request");
        } else {
            ongoingRequests.setText(numOfOngoingRequests + " Ongoing Requests");
        }

        if (numOfCompletedRequests <= 1){
            completedRequests.setText(numOfCompletedRequests + " Completed Request");
        } else {
            completedRequests.setText(numOfCompletedRequests + " Completed Requests");
        }


        // PieChart
        PieChart requestPieChart = (PieChart)findViewById(R.id.requests_piechart);
        // Piechart for requests

        requestPieChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RequestListActivity.class);
                startActivity(intent);
            }
        });

        requestPieChart.setUsePercentValues(true);
        requestPieChart.getDescription().setEnabled(false);
        requestPieChart.getLegend().setEnabled(false);
        requestPieChart.setExtraOffsets(0,0,0,0);

        requestPieChart.setTouchEnabled(false);

        requestPieChart.setDrawHoleEnabled(false);
        requestPieChart.setHoleColor(Color.WHITE);
        requestPieChart.setTransparentCircleRadius(20f);
        ArrayList<PieEntry> yValues = new ArrayList<PieEntry>();

        yValues.add(new PieEntry(numOfCompletedRequests,""));
        yValues.add(new PieEntry(numOfOngoingRequests,""));

        PieDataSet dataSet = new PieDataSet(yValues, "");
        dataSet.setSliceSpace(0f);

        dataSet.setColors(new int[] {getResources().getColor(R.color.completed), getResources().getColor(R.color.ongoing)});

        PieData data = new PieData((dataSet));
        data.setValueTextSize(0f);

        requestPieChart.setData(data);
    }

    public static int[] sendPost(final String email, final String url) {
        final int[] result = new int[2];
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("email", email);

        final byte[] singInDataBytes = parseParameter(data);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL requestUrl = new URL(url);
                    StringBuffer res = new StringBuffer();

                    HttpURLConnection conn = (HttpURLConnection) requestUrl.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestProperty("Content-Length", String.valueOf(singInDataBytes.length));
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.write(singInDataBytes);

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG", conn.getResponseMessage());

                    int status = conn.getResponseCode();
                    if (status != 200) {
                        throw new IOException("Post failed");
                    } else {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            res.append(inputLine);
                        }
                        in.close();
                    }

                    conn.disconnect();

                    JSONObject response = new JSONObject(res.toString());
                    JSONArray doneArray = response.getJSONArray("done");
                    JSONArray progressArray = response.getJSONArray("progress");

                    result[0] = progressArray.length();
                    result[1] = doneArray.length();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        while (thread.isAlive()){}

        return result;
    }

    public static byte[] parseParameter(Map<String, Object> params) {
        StringBuilder postData = new StringBuilder();
        byte[] postDataBytes = null;
        try {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append("=");
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }

            postDataBytes = postData.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return postDataBytes;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public void initializeNewsList() throws IOException {
        final NewsListViewAdapter adapter;
        RecyclerView recyclerView = findViewById(R.id.news_listview);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new NewsListViewAdapter();
        adapter.setOnItemClickListener(new NewsListViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                NewsListViewItem articleData = (NewsListViewItem) adapter.getItem(position);
                Intent intent = new Intent(getApplicationContext(), NewsActivity.class);
                intent.putExtra("articleData", articleData);
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new NewsListViewDecoration(20)); // set border between news

        NewYorkTimesApiClass nyTimesAPI = new NewYorkTimesApiClass();
        List<ArticleInfo> autoLoadedArticleInfo = nyTimesAPI.loadArticleInfo();

        // add 3 article previews
        for (ArticleInfo articleInfo : autoLoadedArticleInfo) {
            Drawable thumbnailDrawable = new BitmapDrawable(getResources(), articleInfo.getThumbnailBitmap());
            adapter.addItem(thumbnailDrawable, articleInfo.getTitleStr(), articleInfo.getUrlStr(), articleInfo.getThumbnailUrlStr());
        }
    }
}
