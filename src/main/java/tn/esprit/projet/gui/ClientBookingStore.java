package tn.esprit.projet.gui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * In-memory store for hotel room reservations made during the session.
 * Singleton so all views share the same reservation list.
 */
public class ClientBookingStore {

    // ── Booking record ────────────────────────────────────────────────────────

    public static class Booking {
        public final String  id;
        public final String  roomNumber;
        public final String  roomType;
        public final String  hotelName;
        public final double  pricePerNight;
        public final int     nights;
        public final double  total;
        public final LocalDate checkIn;
        public final LocalDate checkOut;
        public String status; // EN_ATTENTE, CONFIRMEE, ANNULEE

        public Booking(String roomNumber, String roomType, String hotelName,
                       double pricePerNight, int nights, LocalDate checkIn) {
            this.id           = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            this.roomNumber   = roomNumber;
            this.roomType     = roomType;
            this.hotelName    = hotelName;
            this.pricePerNight= pricePerNight;
            this.nights       = Math.max(1, nights);
            this.total        = pricePerNight * this.nights;
            this.checkIn      = checkIn != null ? checkIn : LocalDate.now().plusDays(1);
            this.checkOut     = this.checkIn.plusDays(this.nights);
            this.status       = "EN_ATTENTE";
        }

        public String formattedCheckIn()  { return checkIn.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        public String formattedCheckOut() { return checkOut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        public String formattedTotal()    { return ClientUtils.formatPrice(total) + " DT"; }
        public String dateRange()         { return formattedCheckIn() + "  →  " + formattedCheckOut(); }
    }

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static final ClientBookingStore INSTANCE = new ClientBookingStore();
    private final List<Booking> bookings = new ArrayList<>();

    private ClientBookingStore() {}

    public static ClientBookingStore getInstance() { return INSTANCE; }

    public void add(Booking b)    { bookings.add(b); }
    public void remove(Booking b) { bookings.remove(b); }
    public List<Booking> all()    { return Collections.unmodifiableList(bookings); }
    public int count()            { return bookings.size(); }
}
