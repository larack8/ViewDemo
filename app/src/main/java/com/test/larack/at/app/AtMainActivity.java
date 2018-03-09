package com.test.larack.at.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.test.larack.R;
import com.test.larack.at.data.User;
import com.test.larack.at.widget.AtEditText;
import com.test.larack.at.widget.MentionEditText;
import com.test.larack.at.widget.MentionTextView;

public class AtMainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = AtMainActivity.class.getSimpleName();
    private static final int REQUEST_USER_LIST = 1;
    private static final String TEST_NAME = "ddd@{uid:#003,nick:Venusaur}222@{uid:#005,nick:Charmeleon}hhh";

    private Context mContext;
    private AtEditText mMentionEditText;
    private MentionTextView mMentionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_at_main);
        mContext = AtMainActivity.this;

        mMentionEditText = (AtEditText) findViewById(R.id.et_input);
        mMentionTextView = (MentionTextView) findViewById(R.id.tv_show);

        findViewById(R.id.btn_at).setOnClickListener(this);
        findViewById(R.id.btn_preview_display).setOnClickListener(this);
        findViewById(R.id.btn_preview_data).setOnClickListener(this);
        findViewById(R.id.btn_mock_data).setOnClickListener(this);

        mMentionEditText.setText(mMentionEditText.convertOrgToDisplay(TEST_NAME));
        mMentionEditText.setSelection(mMentionEditText.getEditableText().toString().length());

//        mMentionEditText.setText(TEST_NAME);
        mMentionEditText.setOnMentionInputListener(new AtEditText.OnMentionInputListener() {
            @Override
            public void onMentionCharacterInput(String tag) {
                startAtActivity();
            }
        });

        mMentionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int maxText = 30;
                if (s.length() > maxText) {
                    Toast.makeText(AtMainActivity.this, "字数上限" + maxText + "字", Toast.LENGTH_SHORT).show();
                    mMentionEditText.setText(s.toString().substring(0, maxText));
                    mMentionTextView.setText(mMentionEditText.getText());
                    mMentionEditText.setSelection(mMentionEditText.getText().length());
                }
            }
        });

        mMentionTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMentionEditText.setText(mMentionTextView.getDisplaySpan().toString());
                mMentionEditText.setSelection(mMentionEditText.getText().length());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_USER_LIST && resultCode == RESULT_OK) {
//            User user = (User) data.getParcelableExtra(User.TAG);
//            StringBuilder sb = new StringBuilder();
//            sb.append(mMentionEditText.getText()).append("{uid:").append(user.getUserId()).append(",nick:").append(user.getNickname() + "}");
//            mMentionEditText.setText(sb.toString());

            mMentionEditText.insertAt((User) data.getParcelableExtra(User.TAG));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // 点击“@”
            case R.id.btn_at:
                startAtActivity();
                break;
            // 预览显示效果
            case R.id.btn_preview_display:
                showPreviewDisplay();
                break;
            // 预览数据
            case R.id.btn_preview_data:
                showPreviewData();
                break;
            // 模拟收到数据
            case R.id.btn_mock_data:
                showReceiveMockData();
                break;
            default:
                break;
        }
    }

    private void startAtActivity() {
        Intent userList = new Intent(mContext, UserListActivity.class);
        startActivityForResult(userList, REQUEST_USER_LIST);
    }

    private void showPreviewDisplay() {
        View view = getLayoutInflater().inflate(R.layout.dialog_at_preview, null);
        TextView tvDisplay = (TextView) view.findViewById(R.id.tv_display);
        TextView tvDescription = (TextView) view.findViewById(R.id.tv_description);
        tvDisplay.setText(mMentionEditText.getText());
        tvDisplay.setMovementMethod(LinkMovementMethod.getInstance());
        tvDescription.setText("预览在TextView中的显示效果,每个“@”对象都可点击");
        AlertDialog dialog = new AlertDialog.Builder(mContext).setView(view)
                .setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
    }

    private void showPreviewData() {
        View view = getLayoutInflater().inflate(R.layout.dialog_at_preview, null);
        TextView tvDisplay = (TextView) view.findViewById(R.id.tv_display);
        TextView tvDescription = (TextView) view.findViewById(R.id.tv_description);
        tvDisplay.setText(mMentionEditText.getOrgText());
        tvDescription.setText("发送给后台的数据,假定“@”对象的格式为@{uid:编号,nick:名称}");
        AlertDialog dialog = new AlertDialog.Builder(mContext).setView(view)
                .setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
    }

    private void showReceiveMockData() {
        String mockData = "ddd@{uid:#003,nick:Venusaur}222@{uid:#005,nick:Charmeleon}hhh";
        View view = getLayoutInflater().inflate(R.layout.dialog_at_preview, null);
        TextView tvDisplay = (TextView) view.findViewById(R.id.tv_display);
        TextView tvDescription = (TextView) view.findViewById(R.id.tv_description);
        tvDisplay.setText(mMentionEditText.convertOrgToDisplay(new SpannableStringBuilder(mockData)));
        tvDisplay.setMovementMethod(LinkMovementMethod.getInstance());
        tvDescription.setText("模拟收到后台的数据: " + mockData);
        AlertDialog dialog = new AlertDialog.Builder(mContext).setView(view)
                .setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
    }
}
