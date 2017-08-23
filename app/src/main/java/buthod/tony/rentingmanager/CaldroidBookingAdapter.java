package buthod.tony.rentingmanager;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidGridAdapter;
import com.roomorama.caldroid.CalendarHelper;
import com.roomorama.caldroid.CellView;

import java.util.Date;
import java.util.Map;

import hirondelle.date4j.DateTime;

/**
 * Created by Tony on 20/08/2017.
 */

public class CaldroidBookingAdapter extends CaldroidGridAdapter {

    public CaldroidBookingAdapter(Context context, int month, int year,
                                  Map<String, Object> caldroidData,
                                  Map<String, Object> extraData) {
        super(context, month, year, caldroidData, extraData);
    }

    @Override
    protected void customizeTextView(int position, CellView cellView) {
        // Get the padding of cell so that it can be restored later
        int topPadding = cellView.getPaddingTop();
        int leftPadding = cellView.getPaddingLeft();
        int bottomPadding = cellView.getPaddingBottom();
        int rightPadding = cellView.getPaddingRight();

        // Get dateTime of this cell
        DateTime dateTime = this.datetimeList.get(position);

        cellView.resetCustomStates();

        // Set default text and background color
        cellView.setBackgroundResource(R.drawable.cell_default_bg);
        cellView.setTextColor(defaultTextColorRes);
        // Set background depending on if the location is rented
        Map<Date, Integer> booking =
                (Map<Date, Integer>) extraData.get(CaldroidBookingFragment.BOOKING);
        Date date = CalendarHelper.convertDateTimeToDate(dateTime);
        Integer bookingState = null;
        if (booking != null && (bookingState = booking.get(date)) != null) {
            switch (bookingState) {
                case CaldroidBookingFragment.HALF_FULL:
                    cellView.setBackgroundResource(R.drawable.cell_half_full_bg);
                    break;
                case CaldroidBookingFragment.FULL:
                    cellView.setBackgroundResource(R.drawable.cell_full_bg);
                    break;
            }
        }

        if (dateTime.equals(getToday())) {
            cellView.addCustomState(CellView.STATE_TODAY);
        }

        // Set color of the dates in previous / next month
        if (dateTime.getMonth() != month) {
            cellView.addCustomState(CellView.STATE_PREV_NEXT_MONTH);
        }

        // Customize for disabled dates and date outside min/max dates
        if ((minDateTime != null && dateTime.lt(minDateTime))
                || (maxDateTime != null && dateTime.gt(maxDateTime))
                || (disableDates != null && disableDatesMap
                .containsKey(dateTime))) {

            cellView.addCustomState(CellView.STATE_DISABLED);
        }

        // Customize for selected dates
        if (selectedDates != null && selectedDatesMap.containsKey(dateTime)) {
            cellView.addCustomState(CellView.STATE_SELECTED);
        }

        cellView.refreshDrawableState();

        // Set text
        cellView.setText(String.valueOf(dateTime.getDay()));

        // Set custom color if required
        setCustomResources(dateTime, cellView, cellView);

        // Somehow after setBackgroundResource, the padding collapse.
        // This is to recover the padding
        cellView.setPadding(leftPadding, topPadding, rightPadding,
                bottomPadding);
    }
}
