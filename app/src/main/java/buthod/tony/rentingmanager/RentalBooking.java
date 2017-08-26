package buthod.tony.rentingmanager;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Tony on 19/08/2017.
 *
 * This class allows to store booking of a rent.
 * The rent can be divided in sub-rents that can be rented independently.
 */

public class RentalBooking {

    // The name of the whole rent.
    private String mWholeRent = null;
    // Contains sub-rents but also the whole rent.
    private HashMap<String, HashMap<Date, String>> mSubrents = null;
    private SparseArray<String> mIdMap = null;

    public RentalBooking() {
        mSubrents = new HashMap<>();
        mIdMap = new SparseArray<>();
    }

    public void addSubrent(String name, int id) {
        mSubrents.put(name, new HashMap<Date, String>());
        mIdMap.put(id, name);
        // The first rent added is set as the whole rent
        if (mWholeRent == null)
            mWholeRent = name;
    }

    public String getWholeRent() {
        return mWholeRent;
    }

    /**
     * Throw an IllegalArgumentException if the rent hasn't been added.
     */
    public void setWholeRent(String rent) {
        if (!mSubrents.containsKey(rent))
            throw new IllegalArgumentException();
        mWholeRent = rent;
    }

    public int getNumberSubrent() {
        return mSubrents.size();
    }

    public Iterable<String> getSubrentSet() {
        return mSubrents.keySet();
    }

    /**
     * Throw an error if the rent is not in mSubrents.
     */
    public Iterable<Map.Entry<Date,String>> getBookingEntrySet(String rent) {
        return mSubrents.get(rent).entrySet();
    }

    /**
     * Output tenants in a rent at a given date.
     * Check also tenants in sub-rents.
     * Map keys correspond to rents names, and values are tenants names.
     *
     * Throw an error if the rent is not in mSubrents.
     */
    public Map<String, String> getTenants(String rent, Date date) {
        HashMap<String, String> tenants = new HashMap<>();
        if (rent.equals(mWholeRent)) {
            for (Map.Entry<String, HashMap<Date, String>> entry : mSubrents.entrySet()) {
                String tenant = entry.getValue().get(date);
                if (tenant != null)
                    tenants.put(entry.getKey(), tenant);
            }
        }
        else {
            String tenant = mSubrents.get(rent).get(date);
            if (tenant != null)
                tenants.put(rent, tenant);
            tenant = mSubrents.get(mWholeRent).get(date);
            if (tenant != null)
                tenants.put(mWholeRent, tenant);
        }
        return tenants;
    }

    /**
     * Output the tenant in a rent at a given date.
     * If their is no tenant, null is output.
     * Doesn't check sub-rents.
     *
     * Throw an error if the rent is not in mSubrents.
     */
    public String getTenant(String rent, Date date) {
        return mSubrents.get(rent).get(date);
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

    /**
     * Remove a booking from a sub-rent or the whole rent.
     * @param rent The name of the sub-rent or the whole rent.
     * @param date The booking date. Corresponds to the saturday of the rented week.
     */
    public void removeBooking(String rent, Date date) {
        HashMap<Date, String> booking = mSubrents.get(rent);
        if (booking != null) {
            booking.remove(date);
        }
    }
    public void removeBooking(int id, Date date) {
        String rent = mIdMap.get(id);
        if (rent != null)
            removeBooking(rent, date);
    }

    /**
     * Return -1 if the rent is not in mIdMap.
     */
    public int getIdRent(String rent) {
        // Need to browse manually because SparseArray uses references for comparison.
        int idMapSize = mIdMap.size();
        for (int i=0; i<idMapSize; ++i) {
            if (mIdMap.valueAt(i).equals(rent))
                return mIdMap.keyAt(i);
        }
        return -1;
    }

    /**
     * Return the list of free rents for the given date.
     * This contains the whole rent if nothing is booked for this date,
     * and the free sub-rents.
     *
     * Throw an error if the rent is not in mSubrents.
     */
    public List<String> getFreeRentsForDate(Date date) {
        ArrayList<String> freeRents = new ArrayList<>();
        // Check if the whole rent is rented
        if (mSubrents.get(mWholeRent).containsKey(date)) {
            // The whole rent is rented, there are no free sub-rents
            return freeRents;
        }
        boolean oneBookingForDate = false;
        // Iterates on mIdMap to keep the same rent name order as RentActivity.
        for (int i=0; i<mIdMap.size(); i++) {
            String rent = mIdMap.valueAt(i);
            if (!rent.equals(mWholeRent)) {
                HashMap<Date, String> booking = mSubrents.get(rent);
                if (booking.containsKey(date)) {
                    // The sub-rent is rented for the given date
                    oneBookingForDate = true;
                } else {
                    // The sub-rent is free for the given date
                    freeRents.add(rent);
                }
            }
        }
        // If no sub-rent is rented for the given date, the whole rent can be rented
        if (!oneBookingForDate)
            freeRents.add(0, mWholeRent);
        return freeRents;
    }

    /**
     * Return the list of rents with a tenant for a given date.
     */
    public List<String> getBusyRentsForDate(Date date) {
        ArrayList<String> busyRents = new ArrayList<>();
        // Iterates on mIdMap to keep the same rent name order as RentActivity.
        for (int i=0; i<mIdMap.size(); i++) {
            String rent = mIdMap.valueAt(i);
            if (mSubrents.get(rent).containsKey(date)) {
                // The sub-rent is rented for the given date
                busyRents.add(rent);
            }
        }
        return busyRents;
    }
}
