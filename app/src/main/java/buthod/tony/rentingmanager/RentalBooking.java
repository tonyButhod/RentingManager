package buthod.tony.rentingmanager;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Tony on 19/08/2017.
 *
 * This class allows to store bookings of a rent.
 * The rent can be divided in sub-rents that can be rented independently.
 */

public class RentalBooking {

    // The name of the whole rent.
    private String mWholeRent = null;
    // Contains sub-rents but also the whole rent.
    private HashMap<String, BookingPerMonth> mSubrents = null;
    // Map between rent id and rent name.
    private SparseArray<String> mIdMap = null;

    public RentalBooking() {
        mSubrents = new HashMap<>();
        mIdMap = new SparseArray<>();
    }

    public void clear() {
        mWholeRent = null;
        mSubrents.clear();
        mIdMap.clear();
    }

    public void addSubrent(String name, int id) {
        mSubrents.put(name, new BookingPerMonth());
        mIdMap.put(id, name);
        // The first rent added is set as the whole rent
        if (mWholeRent == null)
            mWholeRent = name;
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
    public Set<BookingInformation> getAllBookings(String rent) {
        return mSubrents.get(rent).getAllBookings();
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
            for (String subrent : mSubrents.keySet()) {
                BookingInformation booking = mSubrents.get(subrent).getBookingInformation(date);
                if (booking != null)
                    tenants.put(subrent, booking.tenant);
            }
        }
        else {
            BookingInformation booking = mSubrents.get(rent).getBookingInformation(date);
            if (booking != null)
                tenants.put(rent, booking.tenant);
            booking = mSubrents.get(mWholeRent).getBookingInformation(date);
            if (booking != null)
                tenants.put(mWholeRent, booking.tenant);
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
        BookingInformation info = mSubrents.get(rent).getBookingInformation(date);
        return (info!=null) ? info.tenant : null;
    }

    /**
     * Add a booking to a sub-rent or the whole rent.
     * @param rent The name of the sub-rent or the whole rent.
     * @param date The booking date.
     * @param duration The duration of the booking.
     * @param tenant The tenant.
     */
    public void addBooking(long id, String rent, Date date, int duration, String tenant) {
        BookingPerMonth booking = mSubrents.get(rent);
        if (booking != null) {
            booking.addBooking(id, date, duration, tenant);
        }
    }
    public void addBooking(long id, int rentId, Date date, int duration, String tenant) {
        String rent = mIdMap.get(rentId);
        if (rent != null)
            addBooking(id, rent, date, duration, tenant);
    }

    /**
     * Remove a booking from a sub-rent or the whole rent.
     * @param rent The name of the sub-rent or the whole rent.
     * @param date One date of the booking.
     */
    public void removeBooking(String rent, Date date) {
        BookingPerMonth booking = mSubrents.get(rent);
        if (booking != null) {
            booking.removeBooking(date);
        }
    }
    public void removeBooking(int id, Date date) {
        String rent = mIdMap.get(id);
        if (rent != null)
            removeBooking(rent, date);
    }

    public void modifyBooking(String rent, Date date, String newTenant) {
        BookingPerMonth booking = mSubrents.get(rent);
        if (booking != null) {
            booking.modifyBooking(date, newTenant);
        }
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
    public List<String> getFreeRentsForDate(Date date, int duration) {
        ArrayList<String> freeRents = new ArrayList<>();
        // Check if the whole rent is rented
        if (!mSubrents.get(mWholeRent).isFree(date, duration)) {
            // The whole rent is rented, there are no free sub-rents
            return freeRents;
        }
        boolean oneBookingForDate = false;
        // Iterates on mIdMap to keep the same rent name order as RentActivity.
        for (int i=0; i<mIdMap.size(); i++) {
            String rent = mIdMap.valueAt(i);
            if (!rent.equals(mWholeRent)) {
                BookingPerMonth booking = mSubrents.get(rent);
                if (!booking.isFree(date, duration)) {
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
            if (mSubrents.get(rent).getBookingInformation(date) != null) {
                // The sub-rent is rented for the given date
                busyRents.add(rent);
            }
        }
        return busyRents;
    }

    /**
     *
     * @param rent The rent name.
     * @param date One date of the booking.
     * @return Booking information if a booking exists at the given date, null otherwise.
     */
    public BookingInformation getBookingInformation(String rent, Date date) {
        BookingPerMonth bookings = mSubrents.get(rent);
        if (bookings != null) {
            return bookings.getBookingInformation(date);
        }
        return null;
    }

    /**
     * Class containing information of a booking.
     */
    public class BookingInformation {
        public long id;
        public Date date;
        public int duration;
        public String tenant;

        public BookingInformation(long id, Date date, int duration, String tenant) {
            this.id = id;
            this.date = date;
            this.duration = duration;
            this.tenant = tenant;
        }
    }

    /**
     * Class saving bookings of a rent.
     */
    public class BookingPerMonth {

        /* For better performances, bookings are saved per month in a sparse array.
        Every month contains a list of bookings. Its size doesn't exceed 6 in general.
         */
        private SparseArray<List<BookingInformation>> mBooking = null;

        public BookingPerMonth() {
            mBooking = new SparseArray<>();
        }

        /**
         *
         * @param year The year of the month.
         * @param month The number of the month.
         * @return The key of a month in mBooking.
         */
        private int getKey(int year, int month) {
            return year*100 + month;
        }

        /**
         * Add a booking in mBooking.
         * @param date The starting date of the booking.
         * @param duration The duration.
         * @param tenant The tenant name.
         */
        public void addBooking(long id, Date date, int duration, String tenant) {
            // Create a new object BookingInformation
            BookingInformation bookingInfo = new BookingInformation(id, date, duration, tenant);
            Calendar startDate = Calendar.getInstance();
            startDate.setTime(date);
            startDate.set(Calendar.DAY_OF_MONTH, 1);
            Calendar endDate = Calendar.getInstance();
            endDate.setTime(date);
            endDate.add(Calendar.DATE, duration-1);
            // Add this object to each month related to the booking
            while (startDate.compareTo(endDate) <= 0) {
                int year = startDate.get(Calendar.YEAR);
                int month = startDate.get(Calendar.MONTH);
                int key = getKey(year, month);
                List<BookingInformation> bookings = mBooking.get(key);
                if (bookings == null) {
                    bookings = new ArrayList<>();
                    mBooking.put(key, bookings);
                }
                bookings.add(bookingInfo);
                startDate.add(Calendar.MONTH, 1);
            }
        }

        /**
         *
         * @param date The starting date.
         * @param duration The duration.
         * @return true if the rent is free during this period, false otherwise.
         */
        public boolean isFree(Date date, int duration) {
            Calendar startDate = Calendar.getInstance();
            startDate.setTime(date);
            Calendar currDate = Calendar.getInstance();
            currDate.setTime(date);
            currDate.set(Calendar.DAY_OF_MONTH, 1);
            Calendar endDate = Calendar.getInstance();
            endDate.setTime(date);
            endDate.add(Calendar.DATE, duration-1);
            int currYear = currDate.get(Calendar.YEAR);
            int currMonth = currDate.get(Calendar.MONTH);
            int currKey = getKey(currYear, currMonth);
            int endYear = endDate.get(Calendar.YEAR);
            int endMonth = endDate.get(Calendar.MONTH);
            int endKey = getKey(endYear, endMonth);
            // Browse every month of mBooking related to the given period.
            while (currKey <= endKey) {
                List<BookingInformation> bookings = mBooking.get(currKey);
                // Check if every booking in mBooking doesn't intersect the given period
                if (bookings != null) {
                    Calendar bookingDate = Calendar.getInstance();
                    for (BookingInformation b : bookings) {
                        bookingDate.setTime(b.date);
                        if (bookingDate.compareTo(endDate) <= 0) {
                            bookingDate.add(Calendar.DATE, b.duration-1);
                            Date temp1 = bookingDate.getTime();
                            if (bookingDate.compareTo(startDate) >= 0) {
                                // The booking intersects the given period, so the rent is not free
                                return false;
                            }
                        }
                    }
                }
                currDate.add(Calendar.MONTH, 1);
                currYear = currDate.get(Calendar.YEAR);
                currMonth = currDate.get(Calendar.MONTH);
                currKey = getKey(currYear, currMonth);
            }
            return true;
        }

        /**
         *
         * @param date The date to get booking information
         * @return Booking information if a booking exists at the given date, null otherwise.
         */
        public BookingInformation getBookingInformation(Date date) {
            Calendar rentDate = Calendar.getInstance();
            rentDate.setTime(date);
            int year = rentDate.get(Calendar.YEAR);
            int month = rentDate.get(Calendar.MONTH);
            int key = getKey(year, month);
            List<BookingInformation> bookings = mBooking.get(key);
            // There is no booking for this month
            if (bookings == null)
                return null;
            Calendar bookingDate = Calendar.getInstance();
            // Iterate on bookings to look if a booking corresponds to the given date
            for (BookingInformation b : bookings) {
                bookingDate.setTime(b.date);
                if (bookingDate.compareTo(rentDate) <= 0) {
                    bookingDate.add(Calendar.DATE, b.duration);
                    if (bookingDate.compareTo(rentDate) > 0) {
                        return b;
                    }
                }
            }
            return null;
        }

        /**
         * Remove a booking at the given date.
         * If no booking corresponds to the date, nothing is done.
         * @param date One date of the booking.
         */
        public void removeBooking(Date date) {
            // First get the object BookingInformation
            Calendar startDate = Calendar.getInstance();
            startDate.setTime(date);
            startDate.set(Calendar.DAY_OF_MONTH, 1);
            // Get the booking information object corresponding to the date
            BookingInformation bookingInfo = getBookingInformation(date);
            if (bookingInfo != null) {
                boolean removed = true;
                // Remove booking information for each month of mBooking
                while (removed) {
                    int year = startDate.get(Calendar.YEAR);
                    int month = startDate.get(Calendar.MONTH);
                    int key = getKey(year, month);
                    List<BookingInformation> bookings = mBooking.get(key);
                    if (bookings != null)
                        removed = bookings.remove(bookingInfo);
                    else
                        removed = false;
                    startDate.add(Calendar.MONTH, 1);
                }
            }
        }

        /**
         * Modify the tenant of a booking at the given date.
         * If no booking corresponds to the date, nothing is done.
         * @param date One date of the booking.
         */
        public void modifyBooking(Date date, String newTenant) {
            // First get the object BookingInformation
            Calendar startDate = Calendar.getInstance();
            startDate.setTime(date);
            startDate.set(Calendar.DAY_OF_MONTH, 1);
            // Get the booking information object corresponding to the date
            BookingInformation bookingInfo = getBookingInformation(date);
            if (bookingInfo != null) {
                bookingInfo.tenant = newTenant;
            }
        }

        /**
         *
         * @return A set of every booking information contained in mBooking.
         */
        public Set<BookingInformation> getAllBookings() {
            Set<BookingInformation> bookingSet = new HashSet<>();
            for (int i = 0; i < mBooking.size(); i++) {
                List<BookingInformation> listBookings = mBooking.valueAt(i);
                for (BookingInformation b : listBookings) {
                    bookingSet.add(b);
                }
            }
            return bookingSet;
        }
    }
}
