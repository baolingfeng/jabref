package net.sf.jabref.export;



import net.sf.jabref.*;
import net.sf.jabref.groups.*;
import net.sf.jabref.sql.SQLutil;
import java.util.Set;

/**
 * MySQLExport contributed by Lee Patton.
 */
public class MySQLExport extends ExportFormat {

    public MySQLExport() {
        super(Globals.lang("MySQL database"), "mysql", null, null, ".sql");
    }

    /**
     * First method called when user starts the export.
     * 
     * @param database
     *            The bibtex database from which to export.
     * @param file
     *            The filename to which the export should be writtten.
     * @param encoding
     *            The encoding to use.
     * @param keySet
     *            The set of IDs of the entries to export.
     * @throws java.lang.Exception
     *             If something goes wrong, feel free to throw an exception. The
     *             error message is shown to the user.
     */
    public void performExport(final BibtexDatabase database,
        final MetaData metaData, final String file, final String encoding,
        Set<String> keySet) throws Exception {

        SQLutil.exportDatabase(database, metaData, keySet, file);

    }


}