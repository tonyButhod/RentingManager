package buthod.tony.rentingmanager;

import android.util.SparseArray;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tony on 19/08/2017.
 *
 * This class allows to store booking of a rent.
 * The rent can be divided in sub-rents that can be rented independently.
 */

public class RentalBooking {

    private String mRent = null;
    // Contains sub-rents but also the whole rent.
    private HashMap<String, HashMap<Date, String>> mSubrents = null;
    private SparseArray<String> mIdMap = null;

    /**
     * @param rent The name of the rent.
     */
    public RentalBooking(String rent) {
        mRent = rent;
        mSubrents = new HashMap<>();
        mIdMap = new SparseArray<>();
    }

    public String getRent() {
        return mRent;
    }

    public void addSubrent(String name, int id) {
        mSubrents.put(name, new HashMap<Date, String>());
        mIdMap.put(id, name);
    }

    public int getNumberSubrent() {
        return mSubrents.size();
    }

    public Iterable<String> getSubrentSet() {
        return mSubrents.keySet();
    }

    public Iterable<Map.Entry<Date,String>> getBookingEntrySet(String rent) {
        return mSubrents.get(rent).entrySet();
    }

    /**
     * Add a booking to a sub-rent or the whole rent.
     * @param rent The name of the sub-rent or the whole rent.
     * @param date The booking date. Corresponds to the saturday of the rented week.
     * @param tenant The tenant.
     */
    public void addBooking(String rent, Date date, String tenant) {
        HashMap<Date, String> booking = mSubrents.get(rent);
        if (booking != null) {
            booking.put(date, tenant);
        }
    }
    public void addBooking(int id, Date date, String tenant) {
        String rent = mIdMap.get(id);
        if (rent != null)
            addBooking(rent, date, tenant);
    }
}
