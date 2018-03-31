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
    public final static String DIFFERENT_TENANTS = "differentTenants";

    public final static int
                FREE = 0,
                HALF_FULL = 1,
                FULL = 2;

    // For each date, contains the state of the date.
    // The state of the date can be FREE, HALF_FULL or FULL.
    protected HashMap<Date, Integer> bookingDates = new HashMap<>();
    // HashMap containing dates when tenants are changing.
    protected HashMap<Date, Boolean> differentTenantsDates = new HashMap<>();
    private Calendar mCalendar = Calendar.getInstance();

    /**
     * Add a booking. Put the corresponding state in the hash map "bookingDates".
     * @param date The starting date of the booking.
     * @param duration The duration of the booking.
     * @param state The state to put.
     */
    public void addBooking(Date date, int duration, int state) {
        mCalendar.setTime(date);
        for (int i=0; i<duration; i++) {
            bookingDates.put(mCalendar.getTime(), state);
            mCalendar.add(Calendar.DATE, 1);
        }
    }

    /**
     * Add a booking for one particular date. Put the state in the hash map "bookingDates".
     * @param date
     * @param state
     */
    public void addBooking(Date date, int state) {
        bookingDates.put(date, state);
    }

    /**
     * Add the information in the hash map that some tenants are leaving or arriving at a date.
     * @param date The arrival date.
     * @param duration The duration of the booking, used to know the leaving date.
     */
    public void addDifferentTenants(Date date, int duration) {
        // Add arrival date
        differentTenantsDates.put(date, true);
        // Add leaving date
        mCalendar.setTime(date);
        mCalendar.add(Calendar.DATE, duration);
        differentTenantsDates.put(mCalendar.getTime(), true);
    }

    public int getBookingState(Date date) {
        Integer state = bookingDates.get(date);
        return (state != null) ? state : 0;
    }

    public void clearBooking() {
        bookingDates.clear();
        differentTenantsDates.clear();
    }

    @Override
    public CaldroidGridAdapter getNewDatesGridAdapter(int month, int year) {
        extraData.put(BOOKING, bookingDates);
        extraData.put(DIFFERENT_TENANTS, differentTenantsDates);
        return new CaldroidBookingAdapter(getActivity(), month, year,
                getCaldroidData(), extraData);
    }

}