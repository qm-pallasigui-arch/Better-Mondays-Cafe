package persistence;

/**
 * Abstraction for storing and retrieving staff profile pictures.
 *
 * Implementations may persist to a database BLOB column, the local filesystem,
 * or any other backing store.  The raw image bytes (PNG / JPEG / GIF) are
 * treated as opaque blobs by the UI layer.
 */
public interface ProfilePictureRepository {

    /**
     * Persist (insert or replace) the profile picture for the given username.
     *
     * @param username  the staff account whose picture is being saved
     * @param imageData raw image bytes (PNG, JPEG, …); never {@code null}
     * @throws Exception if the write operation fails
     */
    void savePicture(String username, byte[] imageData) throws Exception;

    /**
     * Retrieve the profile picture for the given username.
     *
     * @param username the staff account whose picture is requested
     * @return raw image bytes, or {@code null} / empty array if none is stored
     * @throws Exception if the read operation fails
     */
    byte[] loadPicture(String username) throws Exception;

    /**
     * Remove the stored picture for the given username (e.g. when the account
     * is deleted).  Implementations should be a no-op if no picture exists.
     *
     * @param username the staff account whose picture should be removed
     * @throws Exception if the delete operation fails
     */
    void deletePicture(String username) throws Exception;
}
