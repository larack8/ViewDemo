package com.test.larack.at.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.test.larack.R;
import com.test.larack.at.data.User;

import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity {

    private ListView mLvUsers;
    private ArrayAdapter<User> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_at_user_list);

        mLvUsers = (ListView) findViewById(R.id.lv_users);
        mAdapter = new ArrayAdapter<>(UserListActivity.this, android.R.layout.simple_list_item_1);
        mLvUsers.setAdapter(mAdapter);
        mLvUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                User user = (User) parent.getAdapter().getItem(position);
                Intent data = new Intent();
                data.putExtra(User.TAG, user);
                setResult(RESULT_OK, data);
                finish();
            }
        });

        mAdapter.addAll(mockUpData());
    }

    private List<User> mockUpData() {
        List<User> users = new ArrayList<>();
        users.add(new User("504321021", "狐狸已化妖"));
        users.add(new User("908976841", "有心人"));
        users.add(new User("815404329", "Left"));
        users.add(new User("#004", "test4"));
        users.add(new User("#005", "test5"));
        users.add(new User("#006", "test6"));
        users.add(new User("#007", "test7"));
        users.add(new User("#008", "test8"));
        return users;
    }
}
