package sg.rp.geeks.leoapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.*;
import greendroid.app.GDActivity;
import greendroid.widget.GDActionBarItem;
import greendroid.widget.LoaderActionBarItem;
import greendroid.widget.GDActionBarItem.Type;
import sg.rp.geeks.leoapp.adapter.SectionedAdapter;
import sg.rp.geeks.leoapp.connection.BaseServer;
import sg.rp.geeks.leoapp.connection.PyroServer;
import sg.rp.geeks.leoapp.item.GradeSlot;
import sg.rp.geeks.leoapp.item.ModuleSlot;
import sg.rp.geeks.leoapp.item.UTSlot;
import sg.rp.geeks.leoapp.widget.TitleFlowIndicator;
import sg.rp.geeks.leoapp.widget.TitleProvider;
import sg.rp.geeks.leoapp.widget.ViewFlow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class TimetableActivity extends GDActivity {
    private final Handler mHandler = new Handler();

    /*private Animation slideLeftIn;
    private Animation slideLeftOut;
    private Animation slideRightIn;
    private Animation slideRightOut;
    private ViewFlipper vfTimetable;*/

    private ViewFlow vfTimetable;

    private PyroServer server;

    private ArrayList<GradeSlot> mRecentUTGrades;
    private ArrayList<GradeSlot> mRecentGrades;
    private ArrayList<ModuleSlot> mClasses;
    private ArrayList<UTSlot> mUTs;

    private TimetableViewsAdapter mTimetableViewsAdapter;

    private SectionedAdapter mSectionedRecentGradesAdapter;
    private SectionedAdapter mSectionedRecentUTGradesAdapter;
    private SectionedAdapter mSectionedClassesAdapter;
    private SectionedAdapter mSectionedUTsAdapter;

    private BaseAdapter[] mAdapters = new BaseAdapter[4];

    private SharedPreferences prefs;
    String username = "", password = "";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize views
        setGDActionBarContentView(R.layout.timetable_activity);
        addActionBarItem(Type.Refresh, R.id.action_bar_refresh);

        // dirty way: hide home button
        getGDActionBar().findViewById(R.id.gd_action_bar_home_item).setVisibility(View.GONE);
        ((ViewGroup) getGDActionBar().findViewById(R.id.gd_action_bar_home_item).getParent()).getChildAt(1).setVisibility(View.GONE);

        // initialize objects
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        mRecentGrades = new ArrayList<GradeSlot>();
        mSectionedRecentGradesAdapter = new SectionedAdapter() {
            protected View getHeaderView(String caption, int index, int count, View convertView, ViewGroup parent) {
                TextView result = (TextView) convertView;

                if (convertView == null) {
                    result = (TextView) getLayoutInflater().inflate(R.layout.list_section_header, null);
                }
                result.setText(caption);

                return (result);
            }
        };

        mRecentUTGrades = new ArrayList<GradeSlot>();
        mSectionedRecentUTGradesAdapter = new SectionedAdapter() {
            protected View getHeaderView(String caption, int index, int count, View convertView, ViewGroup parent) {
                TextView result = (TextView) convertView;

                if (convertView == null) {
                    result = (TextView) getLayoutInflater().inflate(R.layout.list_section_header, null);
                }
                result.setText(caption);

                return (result);
            }
        };

        mClasses = new ArrayList<ModuleSlot>();
        mSectionedClassesAdapter = new SectionedAdapter() {
            protected View getHeaderView(String caption, int index, int count, View convertView, ViewGroup parent) {
                TextView result = (TextView) convertView;

                if (convertView == null) {
                    result = (TextView) getLayoutInflater().inflate(R.layout.list_section_header, null);
                }
                result.setText(caption);

                return (result);
            }
        };

        mUTs = new ArrayList<UTSlot>();
        mSectionedUTsAdapter = new SectionedAdapter() {
            protected View getHeaderView(String caption, int index, int count, View convertView, ViewGroup parent) {
                TextView result = (TextView) convertView;

                if (convertView == null) {
                    result = (TextView) getLayoutInflater().inflate(R.layout.list_section_header, null);
                }
                result.setText(caption);

                return (result);
            }
        };

        mAdapters[0] = mSectionedRecentUTGradesAdapter;
        mAdapters[1] = mSectionedRecentGradesAdapter;
        mAdapters[2] = mSectionedClassesAdapter;
        mAdapters[3] = mSectionedUTsAdapter;

        vfTimetable = (ViewFlow) findViewById(R.id.vf_timetable);
        mTimetableViewsAdapter = new TimetableViewsAdapter();
        vfTimetable.setAdapter(mTimetableViewsAdapter);

        TitleFlowIndicator indicator = (TitleFlowIndicator) findViewById(R.id.vf_timetable_indicator);
        indicator.setTitleProvider(mTimetableViewsAdapter);
        vfTimetable.setFlowIndicator(indicator);

        vfTimetable.setSelection(2); //Select the middle tab
    }

    @Override
    public void onResume() {
        super.onResume();

        String prefUsername = prefs.getString("pref_key_rp_username", "");
        String prefPassword = prefs.getString("pref_key_rp_password", "");

        if (prefUsername.equals("") || prefPassword.equals("")) {
            showPreferences();
            Toast.makeText(TimetableActivity.this, "Please enter a username and password", Toast.LENGTH_LONG).show();
        } else {
            if (!this.username.equals(prefUsername) || !this.password.equals(prefPassword)) {
                this.username = prefUsername;
                this.password = prefPassword;
                server = new PyroServer(this, this.username, this.password);
                reloadData();
            }
        }
    }

    @Override
    public boolean onHandleActionBarItemClick(GDActionBarItem item, int position) {
        switch (item.getItemId()) {
            case R.id.action_bar_refresh:
                final LoaderActionBarItem loaderItem = (LoaderActionBarItem) item;
                reloadData();
                break;

            default:
                return super.onHandleActionBarItemClick(item, position);
        }

        return true;
    }

    // Create Option Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Create and add our menu items
        MenuItem itemPreferences = menu.add(0, R.id.menu_preferences, Menu.NONE, "Preferences");

        // Set their icons
        itemPreferences.setIcon(android.R.drawable.ic_menu_preferences);

        return true;
    }

    // Handles Option Menu Selection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case (R.id.menu_preferences):
                showPreferences();
                break;
        }

        return true;
    }

    private void showPreferences() {
        Intent i = new Intent(TimetableActivity.this, PreferencesActivity.class);
        startActivity(i);
    }

    public void reloadData() {
        final LoaderActionBarItem loaderItem = ((LoaderActionBarItem) getGDActionBar().getItem(0));
        loaderItem.setLoading(true);


        server.getClasses(new BaseServer.Delegate() {
            public void connectionError(String error) {
                mHandler.post(new Runnable() {
                    public void run() {
                        new AlertDialog.Builder(TimetableActivity.this)
                                .setTitle("Error")
                                .setMessage("The server is down. Please try again later.")
                                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                    }
                                })
                                .show();
                    }
                });

                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        loaderItem.setLoading(false);
                    }
                }, 2000);
            }

            public void connectionEnded(String error, Object object) {
                if (object instanceof ArrayList) {
                    mClasses = (ArrayList<ModuleSlot>) object;
                    ArrayList<String> problems_title = new ArrayList<String>();
                    ArrayList<Problem> problems = new ArrayList<Problem>();
                    for (ModuleSlot m : mClasses) {
                        Problem problem;
                        if (!problems_title.contains("Problem " + m.getProblem())) {
                            problem = new Problem();
                            problem.name = "Problem " + m.getProblem();
                            problems.add(problem);
                            problems_title.add(problem.name);
                            Log.d("Regulus", "Added Problem " + m.getProblem());
                        } else {
                            problem = problems.get(problems_title.indexOf("Problem " + m.getProblem()));
                        }

                        Log.d("Regulus", "Added to problem " + problem.name + ": " + m.getTitle());
                        problem.classes.add(m);
                    }

                    mSectionedClassesAdapter.removeAllSections();
                    for (Problem problem : problems) {
                        Log.d("Regulus", "Added to adapter: " + problem.name);
                        ClassesAdapter classesAdapter = new ClassesAdapter(problem.classes);
                        mSectionedClassesAdapter.addSection(problem.name, classesAdapter);
                    }

                    mSectionedClassesAdapter.notifyDataSetChanged();
                    mTimetableViewsAdapter.notifyDataSetChanged();
                } else {
                    mHandler.post(new Runnable() {
                        public void run() {
                            new AlertDialog.Builder(TimetableActivity.this)
                                    .setTitle("Error")
                                    .setMessage("Please check your username or password. If error persist, please try again later.")
                                    .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            showPreferences();
                                        }
                                    })
                                    .show();
                        }
                    });
                }

                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        //loaderItem.setLoading(false);
                    }
                }, 2000);
            }
        });

        server.getUTs(new BaseServer.Delegate() {
            public void connectionError(String error) {

            }

            public void connectionEnded(String error, Object object) {
                if (object instanceof ArrayList) {
                    mUTs = (ArrayList<UTSlot>) object;
                    ArrayList<String> ut_title = new ArrayList<String>();
                    ArrayList<UT> uts = new ArrayList<UT>();
                    for (UTSlot m : mUTs) {
                        UT ut;
                        if (!ut_title.contains("Understanding Test " + m.getUT())) {
                            ut = new UT();
                            ut.name = "Understanding Test " + m.getUT();
                            uts.add(ut);
                            ut_title.add(ut.name);
                            Log.d("Regulus", "Added UT " + m.getUT());
                        } else {
                            ut = uts.get(ut_title.indexOf("Understanding Test " + m.getUT()));
                        }

                        Log.d("Regulus", "Added to UT " + ut.name + ": " + m.getTitle());
                        ut.uts.add(m);
                    }
                    Collections.reverse(uts);
                    mSectionedUTsAdapter.removeAllSections();
                    for (UT ut : uts) {
                        Log.d("Regulus", "Added to adapter: " + ut.name);
                        UTsAdapter uTsAdapter = new UTsAdapter(ut.uts);
                        mSectionedUTsAdapter.addSection(ut.name, uTsAdapter);
                    }

                    mSectionedUTsAdapter.notifyDataSetChanged();
                    mTimetableViewsAdapter.notifyDataSetChanged();
                }

                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        //loaderItem.setLoading(false);
                    }
                }, 2000);
            }
        });

        server.getRecetUTGrades(new BaseServer.Delegate() {
            public void connectionError(String error) {

            }

            public void connectionEnded(String error, Object object) {
                if (object instanceof ArrayList) {
                    mRecentUTGrades = (ArrayList<GradeSlot>) object;
                    ArrayList<String> problems_title = new ArrayList<String>();
                    ArrayList<Grade> grades = new ArrayList<Grade>();
                    Collections.reverse(mRecentUTGrades);
                    for (GradeSlot m : mRecentUTGrades) {
                        Grade grade;
                        if (!problems_title.contains("Understanding Test " + m.getProblem())) {
                            grade = new Grade();
                            grade.name = "Understanding Test " + m.getProblem();
                            grades.add(grade);
                            problems_title.add(grade.name);
                            Log.d("Regulus", "Added UT For Grades " + m.getProblem());
                        } else {
                            grade = grades.get(problems_title.indexOf("Understanding Test " + m.getProblem()));
                        }

                        Log.d("Regulus", "Added To UT For Grades " + grade.name + ": " + m.getModuleCode());
                        grade.grades.add(m);
                    }

                    mSectionedRecentUTGradesAdapter.removeAllSections();
                    for (Grade grade : grades) {
                        Log.d("Regulus", "Added To Adapter: " + grade.name);
                        GradesAdapter gradesAdapter = new GradesAdapter(grade.grades);
                        mSectionedRecentUTGradesAdapter.addSection(grade.name, gradesAdapter);
                    }

                    mSectionedRecentUTGradesAdapter.notifyDataSetChanged();
                    mTimetableViewsAdapter.notifyDataSetChanged();
                }

                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        //loaderItem.setLoading(false);
                    }
                }, 2000);
            }
        });

        server.getRecentGrades(new BaseServer.Delegate() {
            public void connectionError(String error) {

            }

            public void connectionEnded(String error, Object object) {
                if (object instanceof ArrayList) {
                    mRecentGrades = (ArrayList<GradeSlot>) object;
                    ArrayList<String> problems_title = new ArrayList<String>();
                    ArrayList<Grade> grades = new ArrayList<Grade>();
                    Collections.reverse(mRecentGrades);
                    for (GradeSlot m : mRecentGrades) {
                        Grade grade;
                        if (!problems_title.contains("Problem " + m.getProblem())) {
                            grade = new Grade();
                            grade.name = "Problem " + m.getProblem();
                            grades.add(grade);
                            problems_title.add(grade.name);
                            Log.d("Regulus", "Added Problem For Grades " + m.getProblem());
                        } else {
                            grade = grades.get(problems_title.indexOf("Problem " + m.getProblem()));
                        }

                        Log.d("Regulus", "Added To Problem For Grades " + grade.name + ": " + m.getModuleCode());
                        grade.grades.add(m);
                    }

                    mSectionedRecentGradesAdapter.removeAllSections();
                    for (Grade grade : grades) {
                        Log.d("Regulus", "Added To Adapter: " + grade.name);
                        GradesAdapter gradesAdapter = new GradesAdapter(grade.grades);
                        mSectionedRecentGradesAdapter.addSection(grade.name, gradesAdapter);
                    }

                    mSectionedRecentGradesAdapter.notifyDataSetChanged();
                    mTimetableViewsAdapter.notifyDataSetChanged();
                }

                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        //Using a temporary solution of setting the loader item only when grades are loaded
                        //Grades are usually the slowest to load since it has to get the module name
                        loaderItem.setLoading(false);
                    }
                }, 2000);
            }
        });


    }

    private class Grade {
        String name;
        ArrayList<GradeSlot> grades = new ArrayList<GradeSlot>();
    }

    public class GradesAdapter extends ArrayAdapter<GradeSlot> {
        ArrayList<GradeSlot> items;

        public GradesAdapter(Context context, int textViewResourceId, ArrayList<GradeSlot> items) {
            super(context, textViewResourceId, items);
        }

        public GradesAdapter(ArrayList<GradeSlot> items) {
            this(TimetableActivity.this, R.layout.grade_list_item, items);
            this.items = items;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.grade_list_item, null);
            }

            GradeSlot gradeSlot = items.get(position);

            ((TextView) convertView.findViewById(R.id.title)).setText(gradeSlot.getModuleCode());
            ((TextView) convertView.findViewById(R.id.grade_holder)).setTextColor(getResources().getColor(mapGradeToColor(gradeSlot.getGrade())));
            ((TextView) convertView.findViewById(R.id.grade_holder)).setText(gradeSlot.getGrade());

            return convertView;
        }
    }

    private class Problem {
        String name;
        ArrayList<ModuleSlot> classes = new ArrayList<ModuleSlot>();
    }

    public class ClassesAdapter extends ArrayAdapter<ModuleSlot> {
        ArrayList<ModuleSlot> items;

        public ClassesAdapter(Context context, int textViewResourceId, ArrayList<ModuleSlot> items) {
            super(context, textViewResourceId, items);
        }

        public ClassesAdapter(ArrayList<ModuleSlot> items) {
            this(TimetableActivity.this, R.layout.timetable_list_item, items);
            this.items = items;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.timetable_list_item, null);
            }

            ModuleSlot moduleSlot = items.get(position);

            ((TextView) convertView.findViewById(R.id.day)).setText(moduleSlot.getDay());
            ((TextView) convertView.findViewById(R.id.day_date)).setText(moduleSlot.getDay_date());
            ((TextView) convertView.findViewById(R.id.title)).setText(moduleSlot.getTitle());
            ((TextView) convertView.findViewById(R.id.subtitle1)).setText("Class - ");
            ((TextView) convertView.findViewById(R.id.subtitle2)).setText(moduleSlot.getVenue());
            ((TextView) convertView.findViewById(R.id.time_hhmm)).setText(convertTime(moduleSlot.getTime()));
            ((TextView) convertView.findViewById(R.id.time_ampm)).setText(getAmPm(moduleSlot.getTime()));

            return convertView;
        }
    }

    private class UT {
        String name;
        ArrayList<UTSlot> uts = new ArrayList<UTSlot>();
    }

    public class UTsAdapter extends ArrayAdapter<UTSlot> {
        ArrayList<UTSlot> items;

        public UTsAdapter(Context context, int textViewResourceId, ArrayList<UTSlot> items) {
            super(context, textViewResourceId, items);
        }

        public UTsAdapter(ArrayList<UTSlot> items) {
            this(TimetableActivity.this, R.layout.timetable_list_item, items);
            this.items = items;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.timetable_list_item, null);
            }

            UTSlot utSlot = items.get(position);

            ((TextView) convertView.findViewById(R.id.day)).setText(utSlot.getDay());
            ((TextView) convertView.findViewById(R.id.day_date)).setText(utSlot.getDay_date());
            ((TextView) convertView.findViewById(R.id.title)).setText(utSlot.getTitle());
            ((TextView) convertView.findViewById(R.id.subtitle1)).setText("Venue - ");
            ((TextView) convertView.findViewById(R.id.subtitle2)).setText(utSlot.getVenue());
            ((TextView) convertView.findViewById(R.id.time_hhmm)).setText(convertTime(utSlot.getTime()));
            ((TextView) convertView.findViewById(R.id.time_ampm)).setText(getAmPm(utSlot.getTime()));

            return convertView;
        }
    }

    public class TimetableViewsAdapter extends BaseAdapter implements TitleProvider {
        private final String[] titles = {"UT Grades", "Daily Grades", "Classes", "UT Schedule"};

        public int getCount() {
            return titles.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new ListView(TimetableActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
                //lp.setMargins(10, 10, 10, 10);
                convertView.setLayoutParams(lp);
            }

            Log.d("Regulus", "Assigning " + mAdapters[position]);
            ((ListView) convertView).setAdapter(mAdapters[position]);

            return convertView;
        }

        public String getTitle(int position) {
            return titles[position];
        }
    }

    //Helper method for time conversion
    private String getAmPm(String time) {
        String[] temp = time.split(":");
        String hour = temp[0];
        if(Integer.parseInt(hour) >= 12) {
            return "pm";
        }
        return "am";
    }

    private String convertTime(String time) {
        String[] temp = time.split(":");
        String hour = temp[0];
        if(Integer.parseInt(hour) > 12) {
            return String.valueOf(Integer.parseInt(hour)-12) + ":" + temp[1];
        }
        return time;
    }

    //Helper method for changing colors
    private int mapGradeToColor(String grade) {
        if (grade.equalsIgnoreCase("A")) {
            return R.color.A;
        }
        if (grade.equalsIgnoreCase("B")) {
            return R.color.B;
        }
        if (grade.equalsIgnoreCase("B+")) {
            return R.color.Bplus;
        }
        if (grade.equalsIgnoreCase("C")) {
            return R.color.C;
        }
        if (grade.equalsIgnoreCase("C+")) {
            return R.color.Cplus;
        }
        if (grade.equalsIgnoreCase("D")) {
            return R.color.D;
        }
        if (grade.equalsIgnoreCase("D+")) {
            return R.color.Dplus;
        }
        if (grade.equalsIgnoreCase("E")) {
            return R.color.E;
        }
        if (grade.equalsIgnoreCase("F")) {
            return R.color.F;
        }
        if (grade.equalsIgnoreCase("X")) {
            return R.color.X;
        }
        return 0;
    }
}
