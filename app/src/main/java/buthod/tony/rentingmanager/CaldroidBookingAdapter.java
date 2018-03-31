package buthod.tony.rentingmanager;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.roomorama.caldroid.CaldroidGridAdapter;
import com.roomorama.caldroid.CalendarHelper;
import com.roomorama.caldroid.CellView;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import hirondelle.date4j.DateTime;

/**
 * Created by Tony on 20/08/2017.
 */

public class CaldroidBookingAdapter extends CaldroidGridAdapter {

    private Map<Date, Integer> mBooking = null;
    private Map<Date, Boolean> mDifferentTenants = null;

    public CaldroidBookingAdapter(Context context, int month, int year,
                                  Map<String, Object> caldroidData,
                                  Map<String, Object> extraData) {
        super(context, month, year, caldroidData, extraData);
        // Get extra data
        mBooking = (Map<Date, Integer>) extraData.get(CaldroidBookingFragment.BOOKING);
        mDifferentTenants = (Map<Date, Boolean>) extraData.get(CaldroidBookingFragment.DIFFERENT_TENANTS);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CustomCellView cellView;

        // For reuse
        if (convertView == null) {
            cellView = (CustomCellView) localInflater.inflate(R.layout.custom_cell_view, parent, false);
        } else {
            cellView = (CustomCellView) convertView;
        }

        customizeCustomCellView(position, cellView);

        return cellView;
    }

    protected void customizeCustomCellView(int position, CustomCellView cellView) {
        // Get the padding of cell so that it can be restored later
        int topPadding = cellView.getPaddingTop();
        int leftPadding = cellView.getPaddingLeft();
        int bottomPadding = cellView.getPaddingBottom();
        int rightPadding = cellView.getPaddingRight();

        // Get dateTime of this cell
        DateTime dateTime = this.datetimeList.get(position);

        cellView.resetCustomStates();

        // Set default text and background color
        cellView.setBackgroundResource(R.drawable.custom_cell_view);
        cellView.setTextColor(defaultTextColorRes);

        // Get the date of the cell view
        Date date = CalendarHelper.convertDateTimeToDate(dateTime);

        // Customize background colors depending on the booking
        if (mBooking != null) {
            // Get the previous date of the cell view
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DATE, -1);
            Date dateBefore = calendar.getTime();
            // Get booking information corresponding to 2 previous dates
            Integer bookingState = mBooking.get(date);
            Integer bookingStateDateBefore = mBooking.get(dateBefore);
            // Set default value to 0 in case of null
            if (bookingState == null) bookingState = 0;
            if (bookingStateDateBefore == null) bookingStateDateBefore = 0;
            // Update customized attributes in the Cell view.
            switch (bookingState) {
                case CaldroidBookingFragment.HALF_FULL:
                    cellView.addCustomState(CustomCellView.STATE_HALF_FULL);
                    break;
                case CaldroidBookingFragment.FULL:
                    cellView.addCustomState(CustomCellView.STATE_FULL);
                    break;
            }
            switch (bookingStateDateBefore) {
                case CaldroidBookingFragment.HALF_FULL:
                    cellView.addCustomState(CustomCellView.STATE_PREV_HALF_FULL);
                    break;
                case CaldroidBookingFragment.FULL:
                    cellView.addCustomState(CustomCellView.STATE_PREV_FULL);
                    break;
            }
        }

        // Add a separation line if the tenants or different from one date to the next one.
        if (mDifferentTenants != null) {
            Boolean isDifferentTenants = mDifferentTenants.get(date);
            if (isDifferentTenants != null && isDifferentTenants) {
                cellView.addCustomState(CustomCellView.STATE_DIFFERENT_TENANTS);
            }
        }

        // Add a blue border if it is the date of today
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
