package com.test.larack.app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.test.larack.R;

import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private OldMentionEditText editText;
    private static final String TEST_NAME = "women@{uid:123,nick:三少爷";
    private static final String atTag = "at";
    public static final String AT_PATTERN_STR = "@?\\{uid:.*?,nick:.*?\\}";
    public static final Pattern AT_PATTERN = Pattern.compile(AT_PATTERN_STR);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);

        editText.setText(TEST_NAME);
//        List<String> mentionList = editText.getMentionList(true); //get a list of mention string
//        editText.setMentionTextColor(Color.RED); //optional, set highlight color of mention string
//        editText.setPattern(atTag, AT_PATTERN_STR); //optional, set regularExpression
        editText.setOnMentionInputListener(new OldMentionEditText.OnMentionInputListener() {
            @Override
            public void onMentionCharacterInput(String tag) {
                Toast.makeText(MainActivity.this, "" + atTag, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
