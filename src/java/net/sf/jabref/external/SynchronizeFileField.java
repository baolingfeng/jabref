package net.sf.jabref.external;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.gui.FileListEditor;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListEntryEditor;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This action goes through all selected entries in the BasePanel, and attempts to autoset the
 * given external file (pdf, ps, ...) based on the same algorithm used for the "Auto" button in
 * EntryEditor.
 */
public class SynchronizeFileField extends AbstractWorker {

    private String fieldName = GUIGlobals.FILE_FIELD;
    private BasePanel panel;
    private BibtexEntry[] sel = null;
    private SynchronizeFileField.OptionsDialog optDiag = null;

    Object[] brokenLinkOptions =
            {Globals.lang("Ignore"), Globals.lang("Assign new file"), Globals.lang("Clear field"),
                    Globals.lang("Quit synchronization")};

    private boolean goOn = true, autoSet = true, checkExisting = true;

    private int brokenLinks = 0, entriesChangedCount = 0;

    public SynchronizeFileField(BasePanel panel) {
        this.panel = panel;
    }

    public void init() {
        Collection<BibtexEntry> col = panel.database().getEntries();
        sel = new BibtexEntry[col.size()];
        sel = col.toArray(sel);

        // Ask about rules for the operation:
        if (optDiag == null)
            optDiag = new SynchronizeFileField.OptionsDialog(panel.frame(), fieldName);
        Util.placeDialog(optDiag, panel.frame());
        optDiag.setVisible(true);
        if (optDiag.canceled()) {
            goOn = false;
            return;
        }
        autoSet = !optDiag.autoSetNone.isSelected();
        checkExisting = optDiag.checkLinks.isSelected();

        panel.output(Globals.lang("Synchronizing %0 links...", fieldName.toUpperCase()));
    }

    public void run() {
        if (!goOn) {
            panel.output(Globals.lang("No entries selected."));
            return;
        }
        panel.frame().setProgressBarValue(0);
        panel.frame().setProgressBarVisible(true);
        int weightAutoSet = 10; // autoSet takes 10 (?) times longer than checkExisting
        int progressBarMax = (autoSet ? weightAutoSet * sel.length : 0)
                + (checkExisting ? sel.length : 0);
        panel.frame().setProgressBarMaximum(progressBarMax);
        int progress = 0;
        brokenLinks = 0;
        final NamedCompound ce = new NamedCompound(Globals.lang("Autoset %0 field", fieldName));

        //final OpenFileFilter off = Util.getFileFilterForField(fieldName);

        //ExternalFilePanel extPan = new ExternalFilePanel(fieldName, panel.metaData(), null, null, off);
        //FieldTextField editor = new FieldTextField(fieldName, "", false);

        // Find the default directory for this field type:
        String dir = panel.metaData().getFileDirectory(GUIGlobals.FILE_FIELD);
        Set<BibtexEntry> changedEntries = new HashSet<BibtexEntry>();

        // First we try to autoset fields
        if (autoSet) {
            Collection<BibtexEntry> entries = new ArrayList<BibtexEntry>();
            for (int i = 0; i < sel.length; i++) {
                entries.add(sel[i]);
            }
            // Start the autosetting process:

            Thread t = FileListEditor.autoSetLinks(entries, ce, changedEntries);
            // Wait for the autosetting to finish:
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            /*
                progress += weightAutoSet;
                panel.frame().setProgressBarValue(progress);

                Object old = sel[i].getField(fieldName);
                FileListTableModel tableModel = new FileListTableModel();
                if (old != null)
                    tableModel.setContent((String)old);
                Thread t = FileListEditor.autoSetLinks(sel[i], tableModel, null, null);

                if (!tableModel.getStringRepresentation().equals(old)) {
                    String toSet = tableModel.getStringRepresentation();
                    if (toSet.length() == 0)
                        toSet = null;
                    ce.addEdit(new UndoableFieldChange(sel[i], fieldName, old, toSet));
                    sel[i].setField(fieldName, toSet);
                    entriesChanged++;
                }
            }    */

        }
        progress += sel.length*weightAutoSet;
        panel.frame().setProgressBarValue(progress);
        //System.out.println("Done setting");
        // The following loop checks all external links that are already set.
        if (checkExisting) {
            mainLoop:
            for (int i = 0; i < sel.length; i++) {
                panel.frame().setProgressBarValue(progress++);
                final Object old = sel[i].getField(fieldName);
                // Check if a extension is set:
                if ((old != null) && !old.equals("")) {
                    FileListTableModel tableModel = new FileListTableModel();
                    tableModel.setContent((String)old);
                    for (int j=0; j<tableModel.getRowCount(); j++) {
                        FileListEntry flEntry = tableModel.getEntry(j);
                        // Get an absolute path representation:
                        File file = Util.expandFilename(flEntry.getLink(), new String[]{dir, "."});
                        if ((file == null) || !file.exists()) {
                            int answer = JOptionPane.showOptionDialog(panel.frame(),
                                Globals.lang("<HTML>Could not find file '%0'<BR>linked from entry '%1'</HTML>",
                                        new String[]{flEntry.getLink(), sel[i].getCiteKey()}),
                                Globals.lang("Broken link"),
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE, null, brokenLinkOptions, brokenLinkOptions[0]);
                            switch (answer) {
                                case 1:
                                    // Assign new file.
                                    FileListEntryEditor flEditor = new FileListEntryEditor
                                            (panel.frame(), flEntry, false, panel.metaData());
                                    flEditor.setVisible(true);
                                    break;
                                case 2:
                                    // Clear field
                                    tableModel.removeEntry(j);
                                    j--; // Step back in the iteration, because we removed an entry.
                                    break;
                                case 3:
                                    // Cancel
                                    break mainLoop;
                            }
                            brokenLinks++;
                        }

                    }
                    if (!tableModel.getStringRepresentation().equals(old)) {
                        // The table has been modified. Store the change:
                        String toSet = tableModel.getStringRepresentation();
                        if (toSet.length() == 0)
                            toSet = null;
                        ce.addEdit(new UndoableFieldChange(sel[i], fieldName, old,
                                toSet));
                        sel[i].setField(fieldName, toSet);
                        changedEntries.add(sel[i]);
                        //System.out.println("Changed to: "+tableModel.getStringRepresentation());
                    }


                }
            }
        }

        entriesChangedCount = changedEntries.size();
	//for (BibtexEntry entr : changedEntries)
	//    System.out.println(entr.getCiteKey());
        if (entriesChangedCount > 0) {
            // Add the undo edit:
            ce.end();
            panel.undoManager.addEdit(ce);
            
        }
    }


    public void update() {
        if (!goOn)
            return;

        panel.output(Globals.lang("Finished synchronizing %0 links. Entries changed%c %1.",
                new String[]{fieldName.toUpperCase(), String.valueOf(entriesChangedCount)}));
        panel.frame().setProgressBarVisible(false);
        if (entriesChangedCount > 0) {
            panel.markBaseChanged();
        }
    }

    static class OptionsDialog extends JDialog {
        JRadioButton autoSetUnset, autoSetAll, autoSetNone;
        JCheckBox checkLinks;
        JButton ok = new JButton(Globals.lang("Ok")),
                cancel = new JButton(Globals.lang("Cancel"));
        JLabel description;
        private boolean canceled = true;
        private String fieldName;

        public OptionsDialog(JFrame parent, String fieldName) {
            super(parent, Globals.lang("Synchronize %0 links", fieldName.toUpperCase()), true);
            final String fn = Globals.lang("file");
            this.fieldName = fieldName;
            ok.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    canceled = false;
                    dispose();
                }
            });

            Action closeAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            };


            cancel.addActionListener(closeAction);

            InputMap im = cancel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = cancel.getActionMap();
            im.put(Globals.prefs.getKey("Close dialog"), "close");
            am.put("close", closeAction);

            autoSetUnset = new JRadioButton(Globals.lang("Autoset %0 links. Do not overwrite existing links.", fn), true);
            autoSetAll = new JRadioButton(Globals.lang("Autoset %0 links. Allow overwriting existing links.", fn), false);
            autoSetNone = new JRadioButton(Globals.lang("Do not autoset"), false);
            checkLinks = new JCheckBox(Globals.lang("Check existing %0 links", fn), true);
            ButtonGroup bg = new ButtonGroup();
            bg.add(autoSetUnset);
            bg.add(autoSetNone);
            bg.add(autoSetAll);
            FormLayout layout = new FormLayout("fill:pref", "");
            DefaultFormBuilder builder = new DefaultFormBuilder(layout);
            description = new JLabel("<HTML>" +
                    Globals.lang(//"This function helps you keep your external %0 links up-to-date." +
                            "Attempt to autoset %0 links for your entries. Autoset works if "
                                    + "a %0 file in your %0 directory or a subdirectory<BR>is named identically to an entry's BibTeX key, plus extension.", fn)
                    + "</HTML>");
            //            name.setVerticalAlignment(JLabel.TOP);
            builder.appendSeparator(Globals.lang("Autoset"));
            builder.append(description);
            builder.nextLine();
            builder.append(autoSetUnset);
            builder.nextLine();
            builder.append(autoSetAll);
            builder.nextLine();
            builder.append(autoSetNone);
            builder.nextLine();
            builder.appendSeparator(Globals.lang("Check links"));

            description = new JLabel("<HTML>" +
                    Globals.lang("This makes JabRef look up each %0 extension and check if the file exists. If not, you will "
                            + "be given options<BR>to resolve the problem.", fn)
                    + "</HTML>");
            builder.append(description);
            builder.nextLine();
            builder.append(checkLinks);
            builder.nextLine();
            builder.appendSeparator();


            JPanel main = builder.getPanel();
            main.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            ButtonBarBuilder bb = new ButtonBarBuilder();
            bb.addGlue();
            bb.addGridded(ok);
            bb.addGridded(cancel);
            bb.addGlue();
            getContentPane().add(main, BorderLayout.CENTER);
            getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

            pack();
        }

        public void setVisible(boolean visible) {
            if (visible)
                canceled = true;

            String dir = Globals.prefs.get(fieldName + "Directory");
            if ((dir == null) || (dir.trim().length() == 0)) {

                autoSetNone.setSelected(true);
                autoSetNone.setEnabled(false);
                autoSetAll.setEnabled(false);
                autoSetUnset.setEnabled(false);
            } else {
                autoSetNone.setEnabled(true);
                autoSetAll.setEnabled(true);
                autoSetUnset.setEnabled(true);
            }

            new FocusRequester(ok);
            super.setVisible(visible);

        }

        public boolean canceled() {
            return canceled;
        }
    }
}