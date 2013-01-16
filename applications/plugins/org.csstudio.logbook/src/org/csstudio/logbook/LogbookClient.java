package org.csstudio.logbook;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * The interface for the logbook client.
 * 
 * @author shroffk
 * 
 */
public interface LogbookClient {

	/**
	 * List all the logbooks
	 * 
	 * @return a collection of all the logbooks available on the service
	 * @throws Exception
	 */
	public Collection<Logbook> listLogbooks() throws Exception;

	/**
	 * Lists all the Tags
	 * 
	 * @return a collection of all the available tags on the service
	 * @throws Exception
	 */
	public Collection<Tag> listTags() throws Exception;

	/**
	 * Lists all Properties
	 * 
	 * @return a collection of all the available properties on the service
	 * @throws Exception
	 */
	public Collection<Property> listProperties() throws Exception;

	/**
	 * Lists all the attachments associated with the <tt>logId</tt>
	 * 
	 * @param logId
	 * @return a collection of all the attachments attachmed to the log
	 *         identified by <tt>logId</tt>
	 * @throws Exception
	 */
	public Collection<Attachment> listAttachments(Object logId)
			throws Exception;

	/**
	 * Get an inputStream to the attachment with name
	 * <tt>attachmentFileName</tt> on log <tt>logId</tt>
	 * 
	 * @param logId
	 * @param attachment
	 * @return InputStream for the attachment
	 * @throws Exception
	 */
	public InputStream getAttachment(Object logId, String attachmentFileName)
			throws Exception;

	/**
	 * Find the logEntry with Id <tt>logId</tt>
	 * 
	 * @param logId
	 * @return return the logEntry with if <tt>logId</tt>
	 * @throws Exception
	 */
	public LogEntry findLogEntry(Object logId) throws Exception;

	/**
	 * Find all the logentries with match the search criteria specified in the
	 * <tt>findAttributeMap</tt>
	 * 
	 * @return a collection of LogEntry
	 * @throws Exception
	 */
	public Collection<LogEntry> findLogEntries(
			Map<String, String> findAttributeMap) throws Exception;

	/**
	 * Create the logEntry <tt>logEntry</tt>
	 * 
	 * @param logEntry
	 * @return the successfully created logEntry
	 * @throws Exception
	 */
	public LogEntry createLogEntry(LogEntry logEntry) throws Exception;

	/**
	 * Update the logEntry
	 * 
	 * @param logEntry
	 *            - the new updated logEntry to replace the existing logEntry.
	 * @return the successfully updated logEntry
	 * @throws Exception
	 */
	public LogEntry updateLogEntry(LogEntry logEntry) throws Exception;

	/**
	 * Update a collection of logEntries
	 * 
	 * @param logEntires
	 * @throws Exception
	 */
	public void updateLogEntries(Collection<LogEntry> logEntires)
			throws Exception;

	/**
	 * Attach the file to the log identified by <tt>logId</tt>
	 * 
	 * @param logId
	 * @param file
	 * @param attachment
	 * @return the Attachment describing the successfully attached file.
	 * @throws Exception
	 */
	public Attachment addAttachment(Object logId, InputStream file, String name)
			throws Exception;
}