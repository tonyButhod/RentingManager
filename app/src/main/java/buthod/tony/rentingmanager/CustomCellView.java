package buthod.tony.rentingmanager;

import android.content.Context;
import android.util.AttributeSet;
import com.roomorama.caldroid.CellView;

import java.util.ArrayList;

/**
 * Created by Tont on 3/30/15.
 */
public class CustomCellView extends CellView {

    public static final int STATE_FULL = R.attr.state_full;
    public static final int STATE_HALF_FULL = R.attr.state_half_full;
    public static final int STATE_PREV_FULL = R.attr.state_prev_full;
    public static final int STATE_PREV_HALF_FULL = R.attr.state_prev_half_full;
    public static final int STATE_DIFFERENT_TENANTS = R.attr.state_different_tenants;

    private ArrayList<Integer> customStates = new ArrayList<Integer>();

    public CustomCellView(Context context) {
        super(context);
    }

    public CustomCellView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public CustomCellView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, w, oldw, oldh);
    }

    private void init() {
        if (null == customStates) customStates = new ArrayList<Integer>();
    }

    public void resetCustomStates() {
        super.resetCustomStates();
        customStates.clear();
    }

    public void addCustomState(int state) {
        if (!customStates.contains(state)) {
            customStates.add(state);
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        init();
        int customStateSize = customStates.size();
        if (customStateSize > 0) {
            final int[] drawableState = super.onCreateDrawableState(extraSpace + customStateSize);
            int[] stateArray = new int[customStateSize];
            int i = 0;
            for (Integer state : customStates) {
                stateArray[i] = state;
                i++;
            }
            mergeDrawableStates(drawableState, stateArray);
            return drawableState;
        } else {
            return super.onCreateDrawableState(extraSpace);
        }
    }
}
