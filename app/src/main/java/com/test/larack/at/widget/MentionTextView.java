package com.test.larack.at.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint("AppCompatCustomView")
public class MentionTextView extends TextView {

    public static final String TAG = "MentionTextView";

    public static final String DEFAULT_METION_TAG = "@";
    public static final String DEFAULT_MENTION_PATTERN = "@?\\{uid:.+?,nick:.*?\\}";
    public static final String DEFAULT_MENTION_FORMAT = "@{uid:%s,nick:%s}";
    public static final String DEFAULT_DISPLAY_PART_FIRST = "@";
    public static final int DEFAULT_DISPLAY_PART_GROUP = 2;
    public static final int DEFAULT_MENTION_TEXT_COLOR = Color.RED;

    private int mMetionBkColor = DEFAULT_MENTION_TEXT_COLOR;
    private MentionSpan.OnSpanClickListener mOnSpanClickListener;

    public MentionTextView(Context context) {
        super(context);
        init();
    }

    public MentionTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MentionTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

    }

    @Override
    public void setText(final CharSequence text, TextView.BufferType type) {
        super.setText(text, type);

    }

    public String getOrgText() {
        return getText().toString();
    }

    public SpannableStringBuilder getDisplaySpan() {
        return convertOrgToDisplay(getOrgText());
    }

    public SpannableStringBuilder convertOrgToDisplay(SpannableStringBuilder orgStr) {
        return analyseData(orgStr);
    }

    public SpannableStringBuilder convertOrgToDisplay(String orgStr) {
        SpannableStringBuilder ss = new SpannableStringBuilder(orgStr);
        SpannableStringBuilder ssb = analyseData(ss);
        return ssb;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
    }

    public void setOnSpanClickListener(MentionSpan.OnSpanClickListener onSpanClickListener) {
        mOnSpanClickListener = onSpanClickListener;
    }

    public int getMetionBkColor() {
        return mMetionBkColor;
    }

    public void setMetionBkColor(int metionBkColor) {
        this.mMetionBkColor = metionBkColor;
    }

    /**
     * 把输入的文字转换成发送给后台的数据,这里假定“@”相关的格式为<编号,名字>
     *
     * @param ss 输入框中的内容
     * @return 生成的数据
     */
    private String parseData(Spannable ss) {
        MentionSpan[] spans = ss.getSpans(0, ss.length(), MentionSpan.class);
        // 对span对象进行排序，在字符串靠前的排在前面
        int position;
//        for (int i = 0; i < spans.length; i++) {
//            int j = i + 1;
//            position = i;
//            MentionSpan temp = spans[i];
//            for (; j < spans.length; j++) {
//                if (ss.getSpanStart(spans[j]) < ss.getSpanStart(temp)) {
//                    temp = spans[j];
//                    position = j;
//                }
//            }
//            spans[position] = spans[i];
//            spans[i] = temp;
//        }

        String pattern = DEFAULT_MENTION_FORMAT;
        StringBuilder sb = new StringBuilder(ss);
        for (int i = spans.length; i > 0; i--) {
            int spanStart = ss.getSpanStart(spans[i - 1]);
            int spanEnd = ss.getSpanEnd(spans[i - 1]);
            sb.replace(spanStart, spanEnd, String.format(pattern, spans[i - 1].uid, spans[i - 1].nick));
        }
        return sb.toString();
    }

    /**
     * 把后台返回的数据处理进行处理, 替换成spannable字符串
     *
     * @param spannableString 后台返回的数据
     */
    private SpannableStringBuilder analyseData(SpannableStringBuilder spannableString) {
        String patternStr = DEFAULT_MENTION_PATTERN;
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(spannableString);
        if (matcher.find()) {
            String userId = matcher.group(1);
            String nickname = matcher.group(2);
            String atString = DEFAULT_DISPLAY_PART_FIRST + matcher.group(DEFAULT_DISPLAY_PART_GROUP) + " ";
            spannableString.replace(matcher.start(), matcher.end(), atString);
            MentionSpan span = new MentionSpan(userId, nickname, mMetionBkColor, mOnSpanClickListener);
            spannableString.setSpan(span, matcher.start(), matcher.start() + atString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            analyseData(spannableString);
        }
        return spannableString;
    }

}
