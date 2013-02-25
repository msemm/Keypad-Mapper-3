/**************************************************************************
 * Copyright
 *
 * $Id: HideCursorEditText.java 2 2012-12-07 08:13:11Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/view/HideCursorEditText.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * 
 */
public class HideCursorEditText extends EditText {

    public interface EditTextImeBackListener {
        public abstract void onImeBack(HideCursorEditText editText, String text);
    }

    private EditTextImeBackListener onImeBack;

    public HideCursorEditText(Context context) {
        super(context);
    }

    public HideCursorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HideCursorEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (onImeBack != null) {
                onImeBack.onImeBack(this, this.getText().toString());
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * 
     * @param listener
     */
    public void setOnEditTextImeBackListener(EditTextImeBackListener listener) {
        onImeBack = listener;
    }

}
