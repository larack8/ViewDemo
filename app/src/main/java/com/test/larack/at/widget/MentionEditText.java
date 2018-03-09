package com.test.larack.at.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;
import android.widget.Toast;

import com.test.larack.at.data.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MentionEditText adds some useful features for mention string @{uid:1510313758623304,nick:三少爷}, such as highlight,
 * intelligent deletion, intelligent selection and '@' input detection, etc.
 *
 * @author jinqianli
 */
@SuppressLint("AppCompatCustomView")
public class MentionEditText extends AppCompatEditText {

    public static final String TAG = "AtEditText";

    public static final String DEFAULT_METION_TAG = "@";
    public static final String DEFAULT_MENTION_PATTERN = "@?\\{uid:.*?,nick:.*?\\}";
    public static final String DEFAULT_DISPLAY_PART_FIRST = "@";
    public static final int DEFAULT_DISPLAY_PART_GROUP = 2;

    public static final String pattern_Str = "@?\\{uid:.*?,nick:.*?\\}";
    public static final String pattern_format = "@{uid:%s,nick:%s}";

    private Map<String, Pattern> mPatternMap = new HashMap<>();
    private Runnable mAction;

    private int mMentionTextColor;

    private boolean mIsSelected;
    private MentionEditText.Range mLastSelectedRange;
    private List<MentionEditText.Range> mRangeArrayList;

    private MentionEditText.OnMentionInputListener mOnMentionInputListener;
    private MentionSpan.OnSpanClickListener mOnSpanClickListener;

    public MentionEditText(Context context) {
        super(context);
        init();
    }

    public MentionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MentionEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new MentionEditText.HackInputConnection(super.onCreateInputConnection(outAttrs), true, this);
    }

    @Override
    public void setText(final CharSequence text, BufferType type) {
        super.setText(text, type);
        //hack, put the cursor at the end of text after calling setText() method
        if (mAction == null) {
            mAction = new Runnable() {
                @Override
                public void run() {
                    setSelection(getText().length());
                }
            };
        }
        post(mAction);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        colorMentionString();
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        //avoid infinite recursion after calling setSelection()
        if (mLastSelectedRange != null && mLastSelectedRange.isEqual(selStart, selEnd)) {
            return;
        }

        //if user cancel a selection of mention string, reset the state of 'mIsSelected'
        MentionEditText.Range closestRange = getRangeOfClosestMentionString(selStart, selEnd);
        if (closestRange != null && closestRange.to == selEnd) {
            mIsSelected = false;
        }

        MentionEditText.Range nearbyRange = getRangeOfNearbyMentionString(selStart, selEnd);
        //if there is no mention string nearby the cursor, just skip
        if (nearbyRange == null) {
            return;
        }

        //forbid cursor located in the mention string.
        if (selStart == selEnd) {
            setSelection(nearbyRange.getAnchorPosition(selStart));
        } else {
            if (selEnd < nearbyRange.to) {
                setSelection(selStart, nearbyRange.to);
            }
            if (selStart > nearbyRange.from) {
                setSelection(nearbyRange.from, selEnd);
            }
        }
    }

    public String getDisplayText() {
        return getText().toString();
    }

    public String getOrgText() {
        return parseData(getText());
    }

    public SpannableStringBuilder convertOrgToDisplay(SpannableStringBuilder orgStr) {
        return analyseData(orgStr);
    }


    /**
     * 在输入框光标处插入"@somebody"
     *
     * @param user 要插入的用户
     */
    public void insertAt1(final User user) {
        int start = getSelectionStart();
        if (getText().toString().equals(DEFAULT_METION_TAG)) {
            getText().insert(start, user.getNickname());
            start = start - 1;
        } else {
            getText().insert(start, DEFAULT_METION_TAG + user.getNickname());
        }
        MentionSpan span = new MentionSpan(user.getUserId(), user.getNickname(), mOnSpanClickListener);
        getText().setSpan(span, start, start + user.getNickname().length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public void insertAt(final User user) {
        if (user == null) {
            return;
        }

//        int start = getSelectionStart();
//        int end = start + s.length();
//        MentionSpan span = new MentionSpan(user.getUserId(), user.getNickname(), mMentionTextColor, mOnSpanClickListener);
        try {
            String s = getText() + "@{uid:" + user.getUserId() + ",nick:" + user.getNickname() + "}";
            setText(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * set regularExpression by tag
     *
     * @param pattern regularExpression
     */
    public void setPattern(String tag, String pattern) {
        mPatternMap.clear();
        addPattern(tag, pattern);
    }

    /**
     * add regularExpression by tag
     *
     * @param tag     set tag for regularExpression
     * @param pattern regularExpression
     */
    public void addPattern(String tag, String pattern) {
        mPatternMap.put(tag, Pattern.compile(pattern));
    }

    /**
     * set highlight color of mention string
     *
     * @param color value from 'getResources().getColor()' or 'Color.parseColor()' etc.
     */
    public void setMentionTextColor(int color) {
        mMentionTextColor = color;
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

        String pattern = pattern_format;
        StringBuilder sb = new StringBuilder(ss);
        for (int i = spans.length; i > 0; i--) {
            int spanStart = ss.getSpanStart(spans[i - 1]);
            int spanEnd = ss.getSpanEnd(spans[i - 1]);
            sb.replace(spanStart, spanEnd,
                    String.format(pattern, spans[i - 1].getUserId(), spans[i - 1].getNickname()));
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

        String patternStr = pattern_Str;
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(spannableString);
        if (matcher.find()) {
            Log.d(TAG, "Find match: " + matcher.group());
            String userId = matcher.group(1);
            String nickname = matcher.group(2);
            String atString = DEFAULT_DISPLAY_PART_FIRST + matcher.group(DEFAULT_DISPLAY_PART_GROUP);

            // 将< , >的格式替换为@ 的格式
            spannableString.replace(matcher.start(), matcher.end(), atString);

            MentionSpan span = new MentionSpan(userId, nickname, new MentionSpan.OnSpanClickListener() {
                @Override
                public void onSpanClick(String userId, String nickname) {
                    Toast.makeText(getContext(), "nickname: " + nickname, Toast.LENGTH_SHORT).show();
                }
            });
            spannableString.setSpan(span, matcher.start(), matcher.start() + atString.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            analyseData(spannableString);
        }

        return spannableString;
    }

    /**
     * get a list of mention string
     *
     * @param excludeMentionCharacter if true, return mention string with format like 'Andy' instead of "@Andy"
     * @return list of mention string
     */
    public List<String> getMentionList(boolean excludeMentionCharacter) {
        List<String> mentionList = new ArrayList<>();
        if (TextUtils.isEmpty(getText().toString())) {
            return mentionList;
        }
        for (Map.Entry<String, Pattern> entry : mPatternMap.entrySet()) {
            Matcher matcher = entry.getValue().matcher(getText().toString());
            while (matcher.find()) {
                String mentionText = matcher.group();
                //tailor the mention string, using the format likes 'Andy' instead of "@Andy"
                if (excludeMentionCharacter) {
                    //careful! 'Andy#' will be the result of '#Andy#' here
                    mentionText = mentionText.substring(1);
                }
                if (!mentionList.contains(mentionText)) {
                    mentionList.add(mentionText);
                }
            }
        }
        return mentionList;
    }

    /**
     * get a list of mention string by tag
     *
     * @param excludeMentionCharacter if true, return mention string with format like 'Andy' instead of "@Andy"
     * @return list of mention string
     */
    public List<String> getMentionList(String tag, boolean excludeMentionCharacter) {
        List<String> mentionList = new ArrayList<>();
        if (TextUtils.isEmpty(getText().toString())) {
            return mentionList;
        }
        for (Map.Entry<String, Pattern> entry : mPatternMap.entrySet()) {
            if (entry.getKey().equals(tag)) {
                Matcher matcher = entry.getValue().matcher(getText().toString());
                while (matcher.find()) {
                    String mentionText = matcher.group();
                    //tailor the mention string, using the format likes 'Andy' instead of "@Andy"
                    if (excludeMentionCharacter) {
                        //careful! 'Andy#' will be the result of '#Andy#' here
                        mentionText = mentionText.substring(1);
                    }
                    if (!mentionList.contains(mentionText)) {
                        mentionList.add(mentionText);
                    }
                }
                break;
            }
        }
        return mentionList;
    }

    /**
     * set listener for mention character('@')
     *
     * @param onMentionInputListener MentionEditText.OnMentionInputListener
     */
    public void setOnMentionInputListener(MentionEditText.OnMentionInputListener onMentionInputListener) {
        mOnMentionInputListener = onMentionInputListener;
    }

    private void init() {
        mRangeArrayList = new ArrayList<>(5);
        setPattern(DEFAULT_METION_TAG, DEFAULT_MENTION_PATTERN);
        mMentionTextColor = Color.RED;
        //disable suggestion
        setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        addTextChangedListener(new MentionEditText.MentionTextWatcher());
    }

    private void colorMentionString() {
        //reset state
        mIsSelected = false;
        if (mRangeArrayList != null) {
            mRangeArrayList.clear();
        }

        Editable spannableText = getText();
        if (spannableText == null || TextUtils.isEmpty(spannableText.toString())) {
            return;
        }

        //remove previous spans
        MentionSpan[] oldSpans = spannableText.getSpans(0, spannableText.length(), MentionSpan.class);
        for (MentionSpan oldSpan : oldSpans) {
            spannableText.removeSpan(oldSpan);
        }

        //find mention string and color it
        String text = spannableText.toString();
        for (Map.Entry<String, Pattern> entry : mPatternMap.entrySet()) {
            int lastMentionIndex = -1;
            Matcher matcher = entry.getValue().matcher(text);
            while (matcher.find()) {
                String mentionText = matcher.group();
                String userId = matcher.group(1);
                String nickname = matcher.group(2);
                String atString = DEFAULT_DISPLAY_PART_FIRST + matcher.group(DEFAULT_DISPLAY_PART_GROUP);
                int start;
                if (lastMentionIndex != -1) {
                    start = text.indexOf(mentionText, lastMentionIndex);
                } else {
                    start = text.indexOf(mentionText);
                }
                int end = start + mentionText.length();
                MentionSpan span = new MentionSpan(userId, nickname, mMentionTextColor, mOnSpanClickListener);
                spannableText.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                lastMentionIndex = end;
                //record all mention-string's position
                mRangeArrayList.add(new MentionEditText.Range(start, end));
            }
        }
    }

    private MentionEditText.Range getRangeOfClosestMentionString(int selStart, int selEnd) {
        if (mRangeArrayList == null) {
            return null;
        }
        for (MentionEditText.Range range : mRangeArrayList) {
            if (range.contains(selStart, selEnd)) {
                return range;
            }
        }
        return null;
    }

    private MentionEditText.Range getRangeOfNearbyMentionString(int selStart, int selEnd) {
        if (mRangeArrayList == null) {
            return null;
        }
        for (MentionEditText.Range range : mRangeArrayList) {
            if (range.isWrappedBy(selStart, selEnd)) {
                return range;
            }
        }
        return null;
    }

    //text watcher for mention character('@')
    private class MentionTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int index, int i1, int count) {
            if (count == 1 && !TextUtils.isEmpty(charSequence)) {
                char mentionChar = charSequence.toString().charAt(index);
                for (Map.Entry<String, Pattern> entry : mPatternMap.entrySet()) {
                    if (entry.getKey().equals(String.valueOf(mentionChar)) && mOnMentionInputListener != null) {
                        mOnMentionInputListener.onMentionCharacterInput(entry.getKey());
                        break;
                    }
                }
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    //handle the deletion action for mention string, such as '@test'
    private class HackInputConnection extends InputConnectionWrapper {
        private EditText editText;

        HackInputConnection(InputConnection target, boolean mutable, MentionEditText editText) {
            super(target, mutable);
            this.editText = editText;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                int selectionStart = editText.getSelectionStart();
                int selectionEnd = editText.getSelectionEnd();
                MentionEditText.Range closestRange = getRangeOfClosestMentionString(selectionStart, selectionEnd);
                if (closestRange == null) {
                    mIsSelected = false;
                    return super.sendKeyEvent(event);
                }
                //if mention string has been selected or the cursor is at the beginning of mention string, just use default action(delete)
                if (mIsSelected || selectionStart == closestRange.from) {
                    mIsSelected = false;
                    return super.sendKeyEvent(event);
                } else {
                    //select the mention string
                    mIsSelected = true;
                    mLastSelectedRange = closestRange;
                    setSelection(closestRange.to, closestRange.from);
                }
                return true;
            }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            if (beforeLength == 1 && afterLength == 0) {
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }

    //helper class to record the position of mention string in EditText
    private class Range {
        int from;
        int to;

        Range(int from, int to) {
            this.from = from;
            this.to = to;
        }

        boolean isWrappedBy(int start, int end) {
            return (start > from && start < to) || (end > from && end < to);
        }

        boolean contains(int start, int end) {
            return from <= start && to >= end;
        }

        boolean isEqual(int start, int end) {
            return (from == start && to == end) || (from == end && to == start);
        }

        int getAnchorPosition(int value) {
            if ((value - from) - (to - value) >= 0) {
                return to;
            } else {
                return from;
            }
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
