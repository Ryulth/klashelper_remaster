package com.ryulth.klashelper.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;


import com.ryulth.klashelper.MainActivity;
import com.ryulth.klashelper.R;
import com.ryulth.klashelper.adapter.AssignmentsViewAdapter;
import com.ryulth.klashelper.api.AssignmentApi;
import com.ryulth.klashelper.api.SemesterApi;
import com.ryulth.klashelper.database.AssignmentRepository;
import com.ryulth.klashelper.model.User;
import com.ryulth.klashelper.pojo.model.Assignment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryulth.klashelper.pojo.request.AssignmentRequest;
import com.ryulth.klashelper.service.AssignmentService;
import com.ryulth.klashelper.service.SimpleAssignmentService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class AssignmentActivity extends AppCompatActivity {
    private User user;
    private List<Assignment> assignments = new ArrayList<>();
    private List<Assignment> homeworks = new ArrayList<>();
    private List<Assignment> lectures = new ArrayList<>();
    private List<Assignment> notes = new ArrayList<>();
    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MyHandler myHandler;
    private BottomNavigationView navigation;
    private Toolbar toolbar;
    private long lastTimeBackPressed; //뒤로가기 버튼이 클릭된 시간
    private String tableName;
    private String selectSemester;
    private String semesters;
    private Spinner spinner;
    private AssignmentService assignmentService;
    private List<String> semesterCodes;

    private static class MyHandler extends Handler {
        AssignmentActivity activity;

        public MyHandler(AssignmentActivity activity) {
            this.activity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            activity.mappingAssignments();
            //TODO 리스트 비교 할지 말지 고민중
            if (msg.arg1 == 1) {
                Toast.makeText(activity, "자동 업데이트 완료", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(activity, "수동 업데이트 완료", Toast.LENGTH_LONG).show();
            }
            activity.swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment);
        Intent intent = getIntent();
        this.user = (User) intent.getSerializableExtra("userInfoIntent");
        this.navigation = (BottomNavigationView) findViewById(R.id.navigation);
        this.myHandler = new MyHandler(this);
        this.listView = (ListView) findViewById(R.id.navigation_assignment);
        this.toolbar = (Toolbar) findViewById(R.id.toolbarAssignment);
        this.toolbar.setTitle("");
        this.spinner = (Spinner) findViewById(R.id.spinnerSemester);
        this.assignmentService = new SimpleAssignmentService(getApplicationContext());
        this.semesters = assignmentService.getSemesters(user);
        this.semesterCodes = new ArrayList<>(Arrays.asList(semesters.split(",")));//your data here
        this.setSupportActionBar(toolbar);
        this.addItemsToSpinner();

        //this.aSwitch.setOnCheckedChangeListener(this);
        /*TODO 첫 자동업데이트 고민
        if (firstLogin) {
            firstLogin = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getAssignments();
                    Message msg = myHandler.obtainMessage();
                    msg.arg1=1;
                    myHandler.sendMessage(msg);
                }
            }).start();
        }*/
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // Your code here

                        swipeRefreshLayout.setRefreshing(true);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                assignments = assignmentService.getAssignment(selectSemester, user);
                                assignments = assignmentService.loadAssignment(tableName);
                                Message msg = myHandler.obtainMessage();
                                myHandler.sendMessage(msg);

                            }
                        }).start();
                    }
                });

        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(android.R.color.holo_blue_bright),
                getResources().getColor(android.R.color.holo_green_light),
                getResources().getColor(android.R.color.holo_orange_light),
                getResources().getColor(android.R.color.holo_red_light)
        );
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Toast.makeText(getApplicationContext(), "로그아웃띠", Toast.LENGTH_LONG).show();
                logout();
                return true;

            case R.id.action_sidebar:
                Toast.makeText(getApplicationContext(), "메뉴 버튼 클릭됨", Toast.LENGTH_LONG).show();
                return true;
        }
        return true;
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_assignment:
                    setHomeworks();
                    return true;
                case R.id.navigation_lecture:
                    setLectures();
                    return true;
                case R.id.navigation_notes:
                    setNotes();
                    return true;
            }
            return false;
        }
    };

    private void mappingAssignments() {
        this.homeworks.clear();
        this.lectures.clear();
        this.notes.clear();
        for (Assignment assignment : assignments) {
            switch (assignment.getWorkType()) {
                case "0":
                    this.homeworks.add(assignment);
                    break;
                case "1":
                    this.lectures.add(assignment);
                    break;
                case "2":
                    this.notes.add(assignment);
                    break;
            }
        }
        switch (navigation.getSelectedItemId()) {
            case R.id.navigation_assignment:
                this.setHomeworks();
                break;
            case R.id.navigation_lecture:
                this.setLectures();
                break;
            case R.id.navigation_notes:
                this.setNotes();
                break;
        }
    }


    public void addItemsToSpinner() {
        List<String> semesterNames =matchingSemesterName();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, semesterNames);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinner.setAdapter(adapter);
        this.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View v,
                                       int position, long id) {
                // On selecting a spinner item
                //String item = adapter.getItemAtPosition(position).toString();
                String item = semesterCodes.get(position);
                selectSemester = item;
                tableName = "a" + user.getId() + "_" + selectSemester;
                try {
                    assignments = assignmentService.loadAssignment(tableName);
                    mappingAssignments();

                } catch (Exception ignore) {
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });

    }
    private List<String > matchingSemesterName(){
        List<String> semesterNames = new ArrayList<>();
        for (String semesterCode : semesterCodes) {
            String[] temp = semesterCode.split("_");
            switch (temp[1]) {
                case "10":
                    temp[1] = "1학기";
                    break;
                case "15":
                    temp[1] = "여름학기";
                    break;
                case "20":
                    temp[1] = "2학기";
                    break;
                case "25":
                    temp[1] = "겨울학기";
                    break;
                default:
                    break;
            }
            semesterNames.add(temp[0]+"년 "+temp[1]);
        }
        return semesterNames;
    }
    @Override
    public void onBackPressed() {
        //2초 이내에 뒤로가기 버튼을 재 클릭 시 앱 종료
        if (System.currentTimeMillis() - lastTimeBackPressed < 2000) {
            this.finishAffinity();
            return;
        }
        //'뒤로' 버튼 한번 클릭 시 메시지
        Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
        lastTimeBackPressed = System.currentTimeMillis();
    }

    private void setHomeworks() {
        AssignmentsViewAdapter assignmentsViewAdapter = new AssignmentsViewAdapter();
        assignmentsViewAdapter.setAssignments(homeworks);
        assignmentsViewAdapter.setTableName(tableName);
        listView.setAdapter(assignmentsViewAdapter);
    }

    private void setLectures() {
        AssignmentsViewAdapter assignmentsViewAdapter = new AssignmentsViewAdapter();
        assignmentsViewAdapter.setAssignments(lectures);
        assignmentsViewAdapter.setTableName(tableName);
        listView.setAdapter(assignmentsViewAdapter);
    }

    private void setNotes() {
        AssignmentsViewAdapter assignmentsViewAdapter = new AssignmentsViewAdapter();
        assignmentsViewAdapter.setAssignments(notes);
        assignmentsViewAdapter.setTableName(tableName);
        listView.setAdapter(assignmentsViewAdapter);
    }

    private void logout() {
        SharedPreferences userInfoFile = getSharedPreferences("userInfoFile", MODE_PRIVATE);
        SharedPreferences.Editor editor = userInfoFile.edit();
        editor.putBoolean("isRemember", false);
        editor.commit();
        Intent intentHome = new Intent(this, MainActivity.class);
        intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentHome.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intentHome);
    }

}