package buthod.tony.rentingmanager;

import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidGridAdapter;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Tony on 20/08/2017.
 */

public class CaldroidBookingFragment extends CaldroidFragment {
    public final static String BOOKING = "booking";

    public final static int
                FREE = 0,
                HALF_FULL = 1,
                FULL = 2;

    protected HashMap<Date, Integer> bookingDates = new HashMap<>();
    private Calendar mCalendar = Calendar.getInstance();

    public void setBookingForDate(Date date, int state) {
        bookingDates.put(date, state);
    }

    public void setWeekBookingForDate(Date date, int state) {
        mCalendar.setTime(date);
        for (int i=0; i<7; i++) {
            bookingDates.put(mCalendar.getTime(), state);
            mCalendar.add(Calendar.DATE, 1);
        }
    }

    public void clearBooking() {
        bookingDates.clear();
    }

    @Override
    public CaldroidGridAdapter getNewDatesGridAdapter(int month, int year) {
        extraData.put(BOOKING, bookingDates);
        return new CaldroidBookingAdapter(getActivity(), month, year,
                getCaldroidData(), extraData);
    }

}