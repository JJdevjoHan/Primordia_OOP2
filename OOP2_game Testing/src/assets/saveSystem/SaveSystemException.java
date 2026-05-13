package assets.saveSystem;

/**
 * Thrown when the save system encounters an unrecoverable error
 * (e.g. disk full, permission denied, corrupted file).
 *
 * OOP Principle: Abstraction – callers catch one meaningful exception type
 *                instead of having to know which low-level IOException variant
 *                the implementation threw.
 */
public class SaveSystemException extends Exception {

    public SaveSystemException(String message) {
        super(message);
    }

    public SaveSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
