package assets.saveSystem;

import java.util.List;

/**
 * Contract for reading and writing game history records.
 *
 * OOP Principles applied:
 *  - Abstraction    : callers depend on this interface, not on the concrete class
 *  - Open/Closed    : new storage backends (database, cloud) can be added by
 *                     implementing this interface without touching existing code
 *  - Dependency Inversion : high-level modules (game panels) depend on this
 *                           abstraction, not on file-system details
 */
interface GameHistoryRepository {

    /**
     * Persists a completed game record.
     *
     * @param record the finished match to save
     * @throws SaveSystemException if the record cannot be written
     */
    void save(GameRecord record) throws SaveSystemException;

    /**
     * Retrieves all saved records in chronological order (oldest first).
     *
     * @return immutable list of all records; empty if no history exists
     * @throws SaveSystemException if the history cannot be read
     */
    List<GameRecord> loadAll() throws SaveSystemException;

    /**
     * Retrieves the N most recent records (newest first).
     *
     * @param limit maximum number of records to return
     * @return list of up to {@code limit} records
     * @throws SaveSystemException if the history cannot be read
     */
    List<GameRecord> loadRecent(int limit) throws SaveSystemException;

    /**
     * Retrieves all records for a specific game mode.
     *
     * @param mode the mode to filter by
     * @return matching records in chronological order
     * @throws SaveSystemException if the history cannot be read
     */
    List<GameRecord> loadByMode(GameMode mode) throws SaveSystemException;

    /**
     * Removes all saved history (used for testing or a "clear history" feature).
     *
     * @throws SaveSystemException if the history cannot be cleared
     */
    void clearAll() throws SaveSystemException;
}