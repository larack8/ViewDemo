package com.test.larack.at.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.test.larack.at.data.User;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * AtEditText adds some useful features for mention string @{uid:1510313758623304,nick:三少爷}, such as highlight,
 * intelligent deletion, intelligent selection and '@' input detection, etc.
 *
 * @author jinqianli
 */
@SuppressLint("AppCompatCustomView")
public class AtEditText extends EditText {

    public static final String TAG = "AtEditText";

    public static final String DEFAULT_METION_TAG = "@";
    public static final String DEFAULT_MENTION_PATTERN = "@?\\{uid:(.*?),nick:(.*?)\\}";
    public static final String DEFAULT_MENTION_FORMAT = "@{uid:%s,nick:%s}";
    public static final String DEFAULT_DISPLAY_PART_FIRST = "@";
    public static final int DEFAULT_DISPLAY_PART_GROUP = 2;
    public static final int DEFAULT_MENTION_TEXT_COLOR = Color.RED;

    private int mMetionBkColor = DEFAULT_MENTION_TEXT_COLOR;
    private OnMentionInputListener mOnMentionInputListener;
    private MentionSpan.OnSpanClickListener mOnSpanClickListener;

    public AtEditText(Context context) {
        super(context);
        init();
    }

    public AtEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AtEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        addTextChangedListener(new MentionTextWatcher());
    }

    @Override
    public void setText(final CharSequence text, BufferType type) {
        super.setText(text, type);
    }

    @Override
    public Editable getText() {
        return (Editable) super.getText();
    }

    public SpannableStringBuilder getDisplaySpan() {
        return convertOrgToDisplay(getOrgText());
    }

    public String getOrgText() {
        return parseData(getText());
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

    public void setOnMentionInputListener(OnMentionInputListener onMentionInputListener) {
        mOnMentionInputListener = onMentionInputListener;
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
     * 在输入框光标处插入"@somebody"
     *
     * @param user 要插入的用户
     */
    public void insertAt(final User user) {
        int start = getSelectionStart();
        getText().insert(start, DEFAULT_METION_TAG + user.nick);
        MentionSpan span = new MentionSpan(user.uid, user.nick, mMetionBkColor, mOnSpanClickListener);
        getText().setSpan(span, start, start + user.nick.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        for (int i = 0; i < spans.length; i++) {
            int j = i + 1;
            position = i;
            MentionSpan temp = spans[i];
            for (; j < spans.length; j++) {
                if (ss.getSpanStart(spans[j]) < ss.getSpanStart(temp)) {
                    temp = spans[j];
                    position = j;
                }
            }
            spans[position] = spans[i];
            spans[i] = temp;
        }

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
        // SpannableStringBuilder output = new SpannableStringBuilder(spannableString);

        String patternStr = DEFAULT_MENTION_PATTERN;
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(spannableString);
        if (matcher.find()) {
//            Log.d(TAG, "Find match: " + matcher.group());
            String userId = matcher.group(1);
            String nickname = matcher.group(2);
            String atString = DEFAULT_DISPLAY_PART_FIRST + matcher.group(DEFAULT_DISPLAY_PART_GROUP) + " ";

            // 将{ , }的格式替换为@ 的格式
            spannableString.replace(matcher.start(), matcher.end(), atString);

            MentionSpan span = new MentionSpan(userId, nickname, mMetionBkColor, mOnSpanClickListener);
            spannableString.setSpan(span, matcher.start(), matcher.start() + atString.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            analyseData(spannableString);
        }

        return spannableString;
    }

    //text watcher for mention character('@')
    private class MentionTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (count == 1 && !TextUtils.isEmpty(s)) {
                char mentionChar = s.toString().charAt(start);
                if (DEFAULT_METION_TAG.equals(String.valueOf(mentionChar)) && mOnMentionInputListener != null) {
                    mOnMentionInputListener.onMentionCharacterInput(String.valueOf(mentionChar));
                    setText(getEditableText().toString().subSequence(0, getText().toString().length() - 1));
                }
            }
            // 处理删除事件，在选中范围内的span都需要被删除
            int selectionStart = getSelectionStart();
            int selectionEnd = getSelectionEnd();
            MentionSpan[] spans = getText().getSpans(0, length(), MentionSpan.class);
            for (MentionSpan span : spans) {
                int spanStart = getText().getSpanStart(span);
                int spanEnd = getText().getSpanEnd(span);

                if (selectionStart > spanStart && selectionStart <= spanEnd) {
                    setSelection(spanStart, selectionEnd);
                    selectionStart = spanStart;
                }

                if (selectionEnd >= spanStart && selectionEnd < spanEnd) {
                    setSelection(selectionStart, spanEnd);
                    selectionEnd = spanEnd;
                }
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    /**
     * Listener for '@' character
     */
    public interface OnMentionInputListener {
        /**
         * call when '@' character is inserted into EditText
         */
        void onMentionCharacterInput(String tag);
    }
}
