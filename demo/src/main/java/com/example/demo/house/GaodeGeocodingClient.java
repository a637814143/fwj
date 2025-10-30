package com.example.demo.house;

/**
 * Retained as an empty placeholder so that any stale classpath entries referencing
 * the old Gaode geocoding client no longer trigger Spring bean creation errors.
 * <p>
 * The original implementation has been deleted along with all map functionality.
 * This shell ensures that incremental builds replacing the previous component do
 * not accidentally fail when the application context is refreshed.
 */
public final class GaodeGeocodingClient {
    private GaodeGeocodingClient() {
        throw new UnsupportedOperationException("Gaode geocoding has been removed from the application.");
    }
}
