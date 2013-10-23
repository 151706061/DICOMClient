package edu.umro.dicom.client;

/*
 * Copyright 2012 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeFactory;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.ReadStrategy;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.SOPClassDescriptions;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.XMLRepresentationOfDicomObjectFactory;
import com.pixelmed.display.ConsumerFormatImageMaker;
import edu.umro.dicom.common.Anonymize;
import edu.umro.dicom.common.PACS;
import edu.umro.dicom.common.Util;
import edu.umro.util.Log;
import edu.umro.util.UMROException;
import edu.umro.util.Utility;
import edu.umro.util.XML;

/*
 import org.restlet.Client;
 import org.restlet.Request;
 import org.restlet.Response;
 import org.restlet.data.MediaType;
 import org.restlet.data.Method;
 import java.io.FileInputStream;
 import java.util.Set;
 import java.io.FileNotFoundException;
 import java.io.InputStream;
 import org.restlet.data.Protocol;
 import org.restlet.data.Reference;
 import org.restlet.data.Status;
 import org.restlet.representation.InputRepresentation;
 import org.restlet.representation.Representation;
 import org.restlet.resource.ResourceException;
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import com.pixelmed.dicom.DicomInputStream;
 import com.pixelmed.dicom.TransferSyntax;
 import com.pixelmed.dicom.DicomOutputStream;
 import com.pixelmed.dicom.AttributeTag;
 */

/**
 * Represent a DICOM series.
 * 
 * @author Jim Irrer irrer@umich.edu
 * 
 */

public class Series extends JPanel implements ActionListener, Runnable {

    /** Default ID. */
    private static final long serialVersionUID = 1L;

    /** Suffix for creating DICOM files. */
    private static final String DICOM_SUFFIX = ".DCM";

    /** Card layout identifier for preview slider mode. */
    static private final String CARD_SLIDER = "Slider";

    /** Card layout identifier for upload progress mode. */
    static private final String CARD_PROGRESS = "Progress";

    /** Tool tip for Upload All button. */
    private static final String UPLOAD_BUTTON_TOOLTIP_TEXT_ENABLED = "<html>Upload this series<br>to the selected<br>PACS server.</html>";

    /** Tool tip for Upload All button. */
    private static final String UPLOAD_BUTTON_TOOLTIP_TEXT_DISABLED = "<html>Log in and Select a PACS<br>server to enable uploading.</html>";

    /** Tool tip for Upload All button. */
    private static final String ANONYMIZE_BUTTON_TOOLTIP = "<html>AnonymizeGUI this series,<br>overwriting files.</html>";

    /** Maximum number of slices to buffer when uploading. */
    private static final int UPLOAD_BUFFER_SIZE = 25;

    /** DICOM value for Patient ID for the series. */
    private String patientID = null;

    /** DICOM value for Patient name for the series. */
    private String patientName = null;

    /** Description of series including key metadata values. */
    private String seriesSummary = null;

    /** True if this series has been anonymized. */
    private boolean isAnonymized = false;

    /** DICOM value for series number for the series. */
    private String seriesNumber = null;
    /** DICOM value for modality for the series. */
    private String modality = null;
    /** DICOM value for series description for the series. */
    private String seriesDescription = null;

    private static volatile Semaphore processLock = new Semaphore(1);

    /**
     * The next two sets of DICOM values are dates and times. Frequently, a
     * DICOM file is generated with one or another date or time field filled in,
     * but others left vacant. To get some date and time to show to the user,
     * several are examined to see if they are present in the hope that at least
     * one of them is. This situation is not ideal (all DICOM files should
     * always have a minimal set of values), but vendors will do what they will.
     */

    /** DICOM value for series date for the series. */
    private String seriesDate = null;
    /** DICOM value for content date for the series. */
    private String contentDate = null;
    /** DICOM value for acquisition date for the series. */
    private String acquisitionDate = null;
    /** DICOM value for instance creation date for the series. */
    private String instanceCreationDate = null;
    /** DICOM value for RT plan date for the series. */
    private String rtPlanDate = null;
    /** DICOM value for structure set date for the series. */
    private String structureSetDate = null;

    /** DICOM value for series time for the series. */
    private String seriesTime = null;
    /** DICOM value for content time for the series. */
    private String contentTime = null;
    /** DICOM value for acquisition time for the series. */
    private String acquisitionTime = null;
    /** DICOM value for instance creation time for the series. */
    private String instanceCreationTime = null;
    /** DICOM value for RT plan time for the series. */
    private String rtPlanTime = null;
    /** DICOM value for structure set time for the series. */
    private String structureSetTime = null;

    /** DICOM value for structure set time for the series. */
    private String seriesInstanceUID = null;

    /**
     * Short human readable description of series constructed from its DICOM
     * values.
     */
    private JLabel summaryLabel = null;

    /** Button that user uses to initiate uploads and anonymization. */
    private JButton uploadAnonymizeButton = null;

    /**
     * Icon associated with upload button to indicate whether or not the series
     * has been uploaded.
     */
    private JLabel uploadButtonIcon = null;

    /** Shows preview. */
    private JButton previewButton = null;

    /** Shows progressBar of upload. */
    private JProgressBar progressBar = null;

    /** Panel that shows the progress bar or preview slider. */
    private JPanel previewProgressPanel = null;

    /** Layout that shows the progress bar or preview slider. */
    private CardLayout previewProgressLayout = null;

    private class InstanceList {
        private class Instance implements Comparable<Instance> {
            public final int instanceNumber;
            public String sopInstanceUID;
            public String fileName;

            public Instance(int instanceNumber, String sopInstanceUID, String fileName) {
                this.instanceNumber = instanceNumber;
                this.sopInstanceUID = sopInstanceUID;
                this.fileName = fileName;
            }

            @Override
            public int compareTo(Instance o) {
                return this.instanceNumber - o.instanceNumber;
            }
        }

        private ArrayList<Instance> instList = new ArrayList<Instance>();

        private HashSet<String> sopList = new HashSet<String>();
        private HashSet<String> fileList = new HashSet<String>();
        private ArrayList<String> sortedList = null;

        /**
         * Get the sortedList of file names in ascending order by instance
         * number.
         * 
         * @return List of file names.
         */
        public ArrayList<String> values() {
            if (sortedList == null) {
                Collections.sort(instList);
                sortedList = new ArrayList<String>();
                for (Instance inst : instList) {
                    sortedList.add(inst.fileName);
                }
            }
            return sortedList;
        }

        public int size() {
            return instList.size();
        }

        public String getFileName(int i) {
            return instList.get(i).fileName;
        }

        public String put(String fileName, AttributeList attributeList) {

            if (fileList.contains(fileName)) {
                return "The file " + fileName + " has already been loaded.";
            }

            String sopInstanceUID = attributeList.get(TagFromName.SOPInstanceUID).getSingleStringValueOrEmptyString();
            if (sopList.contains(sopInstanceUID)) {
                String oldFile = "";
                for (Instance instance : instList) {
                    if (instance.sopInstanceUID == sopInstanceUID) {
                        oldFile = instance.fileName;
                        break;
                    }
                }
                return "The SOP Instance UID " + sopInstanceUID + " was already loaded with from file " + oldFile + ", so ignoring file " + fileName;
            }

            Attribute instanceNumberAttr = attributeList.get(TagFromName.InstanceNumber);
            int instanceNumber = 0;
            if (instanceNumberAttr != null) {
                instanceNumber = instanceNumberAttr.getSingleIntegerValueOrDefault(0);
            }

            instList.add(new Instance(instanceNumber, sopInstanceUID, fileName));
            sopList.add(sopInstanceUID);
            fileList.add(fileName);
            sortedList = null;

            return null;
        }
    }

    /** List of file names for this series. */
    private InstanceList instanceList = new InstanceList();

    /** List of PACS AE titles to which this series has been uploaded. */
    private HashSet<String> aeTitleUploadList = new HashSet<String>();

    /**
     * Find the first non-null value in a sortedList.
     * 
     * @param textList
     *            List of values.
     * 
     * @return the first non-null value, or null if all are null.
     */
    private String firstNonNull(String[] textList) {
        for (String text : textList) {
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    /**
     * Reset the summary label based on the current state of the series.
     */
    private void resetSummary() {
        String date = firstNonNull(new String[] { seriesDate, contentDate, acquisitionDate, instanceCreationDate, rtPlanDate, structureSetDate });
        String time = firstNonNull(new String[] { seriesTime, contentTime, acquisitionTime, instanceCreationTime, rtPlanTime, structureSetTime });

        String seriesSummary = "         ";
        seriesSummary += (seriesNumber == null) ? "" : " " + seriesNumber;
        seriesSummary += (modality == null) ? " No modality" : " " + modality;
        seriesSummary += (seriesDescription == null) ? "" : " " + seriesDescription;
        seriesSummary += (date == null) ? "" : " " + date;
        seriesSummary += (time == null) ? "" : " " + time;

        seriesSummary += "    Files: " + instanceList.size();

        seriesSummary += "   ";
        summaryLabel.setText(seriesSummary);
    }

    private JComponent buildPreviewButton() {
        previewButton = new JButton("Preview");
        previewButton.setToolTipText("<html>Preview file</html>");
        previewButton.addActionListener(this);
        JPanel previewButtonPanel = new JPanel();
        previewButtonPanel.setLayout(new BorderLayout());
        previewButtonPanel.add(previewButton, BorderLayout.CENTER);
        previewButtonPanel.add(new JLabel(), BorderLayout.EAST);
        return previewButtonPanel;
    }

    private JComponent buildUploadAnonymizeButton() {
        uploadAnonymizeButton = new JButton("Upload");
        uploadAnonymizeButton.addActionListener(this);
        uploadAnonymizeButton.setToolTipText("<html>Upload this series to<br>the given PACS server.</html>");
        uploadButtonIcon = new JLabel(PreDefinedIcons.getEmpty());
        JPanel uploadButtonPanel = new JPanel();
        uploadButtonPanel.add(uploadAnonymizeButton);
        uploadButtonPanel.add(uploadButtonIcon);
        uploadButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        return uploadButtonPanel;
    }

    private JComponent buildButtonPanel() {
        JPanel panel = new JPanel();
        panel.add(buildPreviewButton());
        panel.add(buildUploadAnonymizeButton());
        return panel;
    }

    private JComponent buildProgressBar() {
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(1);
        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout());
        progressPanel.add(progressBar, BorderLayout.CENTER);

        return progressPanel;
    }

    private JComponent buildEast() {
        previewProgressPanel = new JPanel();
        previewProgressLayout = new CardLayout();
        previewProgressPanel.setLayout(previewProgressLayout);
        previewProgressPanel.add(buildButtonPanel(), CARD_SLIDER);
        previewProgressPanel.add(buildProgressBar(), CARD_PROGRESS);
        previewProgressLayout.show(previewProgressPanel, CARD_SLIDER);
        return previewProgressPanel;
    }

    private JComponent buildSummaryLabel() {
        summaryLabel = new JLabel();
        summaryLabel.setFont(DicomClient.FONT_MEDIUM);
        return summaryLabel;
    }

    private void buildGui() {
        setLayout(new BorderLayout());

        add(buildSummaryLabel(), BorderLayout.CENTER);
        add(buildEast(), BorderLayout.EAST);

        int gap = 8;
        setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, 0));
    }

    /**
     * Build a GUI component that displays and controls the uploading of a
     * single series.
     * 
     * @param fileName
     *            Name of DICOM file from which the series originated.
     * 
     * @param attributeList
     *            Parsed version of DICOM file contents.
     */
    public Series(String fileName, AttributeList attributeList) {

        // For the first file of every series, read the entire attribute
        // list and then update the anonymizing list.  This ensures that
        // the anonymizing list contains a superset of all attributes used
        // by the DICOM files that have been loaded.  The only problem that
        // would arise is if a subsequent file in a series contained a
        // new type of DICOM attribute that was not in the first file of
        // the series (which is unlikely), and the user wanted to specify
        // custom anonymization for that type (again, unlikely).  The user
        // could get around this by previewing slice in the series, which
        // will read each file in it's entirety and update the anonymizing
        // list.  Correcting this error would make the initial loading of
        // DICOM files much slower (~ 10x), and so would annoy users more than
        // make them happy.  Possibly in the future a background thread will
        // be started to read each file in its entirety while the user is
        // staring at the screen, but there's not a big benefit to it.
        try {
            AttributeList al = new AttributeList();
            al.read(fileName);
            attributeList = al;
        }
        catch (Exception e) {
            Log.get().warning("Error reading DICOM file " + fileName + " .  The file may not be processed properly. :  " + e);
        }
        
        AnonymizeGUI.getInstance().updateTagList(attributeList);

        patientID = Util.getAttributeValue(attributeList, TagFromName.PatientID);
        patientID = (patientID == null) ? "No patient id" : patientID;
        patientName = Util.getAttributeValue(attributeList, TagFromName.PatientName);
        patientName = (patientName == null) ? "No patient name" : patientName;

        modality = Util.getAttributeValue(attributeList, TagFromName.Modality);
        if (modality == null) {
            modality = SOPClassDescriptions.getAbbreviationFromUID(Util.getAttributeValue(attributeList, TagFromName.MediaStorageSOPClassUID));
        }
        modality = (modality == null) ? "No modality" : modality;
        seriesDescription = Util.getAttributeValue(attributeList, TagFromName.SeriesDescription);

        // try lots of ways to get the date, because some machines fill in some
        // dates and others fill in others.
        seriesDate = Util.getAttributeValue(attributeList, TagFromName.SeriesDate);
        contentDate = Util.getAttributeValue(attributeList, TagFromName.ContentDate);
        acquisitionDate = Util.getAttributeValue(attributeList, TagFromName.AcquisitionDate);
        instanceCreationDate = Util.getAttributeValue(attributeList, TagFromName.InstanceCreationDate);
        rtPlanDate = Util.getAttributeValue(attributeList, TagFromName.RTPlanDate);
        structureSetDate = Util.getAttributeValue(attributeList, TagFromName.StructureSetDate);

        // try lots of times also.
        seriesTime = Util.getAttributeValue(attributeList, TagFromName.SeriesTime);
        contentTime = Util.getAttributeValue(attributeList, TagFromName.ContentTime);
        acquisitionTime = Util.getAttributeValue(attributeList, TagFromName.AcquisitionTime);
        instanceCreationTime = Util.getAttributeValue(attributeList, TagFromName.InstanceCreationTime);
        rtPlanTime = Util.getAttributeValue(attributeList, TagFromName.RTPlanTime);
        structureSetTime = Util.getAttributeValue(attributeList, TagFromName.StructureSetTime);

        instanceCreationDate = Util.getAttributeValue(attributeList, TagFromName.InstanceCreationDate);
        instanceCreationTime = Util.getAttributeValue(attributeList, TagFromName.InstanceCreationTime);
        seriesInstanceUID = Util.getAttributeValue(attributeList, TagFromName.SeriesInstanceUID);
        seriesInstanceUID = (seriesInstanceUID == null) ? "" : seriesInstanceUID;

        buildGui();
        resetSummary();
        addFile(fileName, attributeList);
        seriesSummary = getSeriesSummary();
        Log.get().info("Added series " + seriesSummary);
    }

    /**
     * Get the currently selected (by the user) AE title.
     * 
     * @return The currently selected (by the user) AE title
     */
    private String getSelectedAeTitle() {
        return DicomClient.getInstance().getSelectedAeTitle();
    }

    /**
     * Set the progress bar to zero.
     */
    public void zeroProgressBar() {
        setProgress(0);
    }

    /**
     * Set the progress bar to the given value.
     * 
     * @param value
     *            Value to set the progress bar to.
     */
    private void setProgress(int value) {
        previewProgressLayout.show(previewProgressPanel, CARD_PROGRESS);
        progressBar.setValue(value);
        Graphics graphics = previewProgressPanel.getGraphics();
        previewProgressPanel.paintAll(graphics);
    }

    /**
     * Set the icon to reflect whether this series has been anonymized or
     * uploaded to the currently selected PACS.
     * 
     * @return True if it has been uploaded, false if not.
     */
    public boolean setProcessedStatus() {
        Color foregroundColor = (DicomClient.getInstance().getPreview().getPreviewedSeries() == this) ? Color.GREEN : DicomClient.COLOR_FONT;
        previewButton.setForeground(foregroundColor);

        if (DicomClient.getInstance().getAnonymizeMode()) {
            uploadButtonIcon.setIcon(isAnonymized ? PreDefinedIcons.getOk() : PreDefinedIcons.getEmpty());
            uploadAnonymizeButton.setToolTipText(ANONYMIZE_BUTTON_TOOLTIP);
            uploadAnonymizeButton.setText("Anonymize");
            uploadAnonymizeButton.setEnabled(true);
            return false;
        }
        else {
            String aeTitle = DicomClient.getInstance().getSelectedAeTitle();
            boolean isUploaded = aeTitleUploadList.contains(aeTitle);
            Icon icon = isUploaded ? PreDefinedIcons.getOk() : PreDefinedIcons.getEmpty();
            uploadButtonIcon.setIcon(icon);
            boolean enabled = DicomClient.getInstance().uploadEnabled();
            uploadAnonymizeButton.setEnabled(enabled);
            uploadAnonymizeButton.setToolTipText(enabled ? UPLOAD_BUTTON_TOOLTIP_TEXT_ENABLED : UPLOAD_BUTTON_TOOLTIP_TEXT_DISABLED);
            uploadAnonymizeButton.setText("Upload");
            return isUploaded;
        }
    }

    /**
     * Upload the given list of attribute lists to the current PACS.
     * 
     * @param description
     *            Description of this series.
     * 
     * @param attrListList
     *            Content to upload.
     * 
     * @return True on success.
     */
    private void pushDicom(AttributeList[] attrListList) {
        try {
            processLock.acquireUninterruptibly();
            PACS pacs = DicomClient.getInstance().getCurrentPacs();
            DicomPush dicomPush = new DicomPush(pacs, attrListList);
            String pushError = dicomPush.push();
            if (pushError != null) {
                processOk = false;
                String message = "Problem uploading files for " + seriesSummary + " : " + pushError;
                Log.get().warning(message);
                DicomClient.getInstance().showMessage(message);
                new Alert(message, "Upload Error");
            }
            return;
        }
        catch (Exception e) {
            String message = "Problem uploading series " + seriesSummary + " . Remaining uploads for this series are being aborted.  Exception: " + e;
            DicomClient.getInstance().showMessage(message);
        }
        finally {
            processLock.release();
        }
        processOk = false;

        /*
         * boolean ok = false; Response response = null; try { Client client =
         * new Client(Protocol.HTTPS); Request request = new Request(Method.PUT,
         * new Reference(urlText));
         * DicomClient.getInstance().setChallengeResponse(request);
         * Representation representation = new InputRepresentation(inputStream,
         * MediaType.APPLICATION_OCTET_STREAM);
         * request.setEntity(representation); request.setMethod(Method.PUT);
         * long start = System.currentTimeMillis(); response =
         * client.handle(request); representation.release(); Status status =
         * response.getStatus(); long elapsed = System.currentTimeMillis() -
         * start; Log.get().info("Elapsed upload time in ms: " + elapsed);
         * 
         * if (status.equals(Status.SUCCESS_OK)) { ok = true; } else { String
         * message =
         * "Problem uploading series.  Remaining uploads for this series are being aborted.\n"
         * + "    Server status: " + status + " : " +
         * response.getEntityAsText();
         * DicomClient.getInstance().showMessage(message); } } catch
         * (ResourceException ex) { String message =
         * "Problem uploading series to " + urlText +
         * " .  Remaining uploads for this series are being aborted. : " + ex;
         * if (response != null) { message += "\n    Server status: " +
         * response.getStatus() + " : " + response.getEntityAsText(); }
         * DicomClient.getInstance().showMessage(message);
         * Log.get().severe(message); } catch (Exception e) { String message =
         * "Unexpected problem while uploading series to " + urlText +
         * ".  Remaining uploads for this series are being aborted." + e; if
         * (response != null) { message += "\n    Server status: " +
         * response.getStatus() + " : " + response.getEntityAsText(); }
         * DicomClient.getInstance().showMessage(message);
         * Log.get().severe(message); } return ok;
         */
    }

    public void run() {
        if (DicomClient.getInstance().getAnonymizeMode()) {
            anonymizeSeries();
        }
        else {
            uploadSeries();
        }
        DicomClient.getInstance().setProcessedStatus();
    }

    /**
     * Perform either anonymization or upload on this series depending on the
     * mode.
     */
    public void processSeries() {
        if (!processOk) return;
        if (DicomClient.inCommandLineMode()) {
            run();
        }
        else {
            Thread thread = new Thread(this);
            thread.start();
        }
        /*
         * try { thread.join(); } catch (InterruptedException e) {
         * Log.get().warning
         * ("Unexpected error from background processing thread: " + e); }
         */
    }

    /**
     * Get the patient object containing this series.
     * 
     * @return The patient object containing this series.
     */
    private Patient getPatient() {
        Component pat = this;
        while (!(pat instanceof Patient)) {
            pat = pat.getParent();
        }
        return (Patient) pat;
    }

    /**
     * Get the sortedList of anonymization values for this series.
     * 
     * @return The sortedList of anonymization values for this series.
     * 
     * @throws DicomException
     *             On invalid PatientID or PatientName
     */
    private synchronized AttributeList getAnonymizingReplacementList() throws DicomException {
        AttributeList replacementAttributeList = AnonymizeGUI.getInstance().getAttributeList();

        Attribute patientId = AttributeFactory.newAttribute(TagFromName.PatientID);
        patientId.addValue(getPatient().getAnonymizePatientIdText());
        replacementAttributeList.put(patientId);

        Attribute patientName = AttributeFactory.newAttribute(TagFromName.PatientName);
        patientName.addValue(getPatient().getAnonymizePatientNameText());
        replacementAttributeList.put(patientName);

        return replacementAttributeList;
    }

    /**
     * Get a new the file to be written to.  The file should not already
     * exist, and it should represent the content of the attribute list.
     * Make any directories as required.
     * 
     * @param attributeList Content that will be written.
     * 
     * @return
     */
    static public File getNewFile(AttributeList attributeList) {
        File dir = DicomClient.getInstance().getDestination();

        if ((DicomClient.inCommandLineMode()) && (DicomClient.getOutputFile() != null)) {
            if (DicomClient.getInstance().getFileCount() > 1) {
                dir = DicomClient.getOutputFile();
            }
            else {
                return DicomClient.getOutputFile();
            }
        }

        String patientIdText = Util.getAttributeValue(attributeList, TagFromName.PatientID);
        String modalityText = Util.getAttributeValue(attributeList, TagFromName.Modality);
        String seriesNumberText = Util.getAttributeValue(attributeList, TagFromName.SeriesNumber);
        String instanceNumberText = Util.getAttributeValue(attributeList, TagFromName.InstanceNumber);

        while ((instanceNumberText != null) && (instanceNumberText.length() < 4)) {
            instanceNumberText = "0" + instanceNumberText;
        }

        String name = "";
        name += (patientIdText == null) ? "" : patientIdText;
        name += (modalityText == null) ? "" : ("_" + modalityText);
        name += (seriesNumberText == null) ? "" : ("_" + seriesNumberText);
        name += (instanceNumberText == null) ? "" : ("_" + instanceNumberText);

        name = name.replace(' ', '_');

        File file = new File(dir, name + DICOM_SUFFIX);
        int count = 1;
        while (file.exists()) {
            file = new File(dir, name + "_" + count + DICOM_SUFFIX);
            count++;
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }
        return file;
    }

    /**
     * If in command line mode, save the anonymized DICOM as text and, if
     * possible, as an image.
     * 
     * @param attributeList
     *            Anonymized DICOM.
     * 
     * @param file
     *            File where anonymized DICOM is to be written.
     */
    private void saveTextAndXmlAndImageFiles(AttributeList attributeList, File file) {
        if (DicomClient.inCommandLineMode()) {
            StringBuffer text = new StringBuffer();
            DicomClient.getInstance().getPreview().addTextAttributes(attributeList, text, 0, null);
            String fileName = file.getName();
            String textFileName = null;
            String imageFileName = null;
            String xmlFileName = null;
            File dir = (file.getParentFile() == null) ? new File(".") : file.getParentFile();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex == -1) {
                textFileName = fileName + ".TXT";
                imageFileName = fileName + ".PNG";
                xmlFileName = fileName + ".XML";
            }
            else {
                String baseName = fileName.substring(0, dotIndex);
                textFileName = baseName + ".TXT";
                imageFileName = baseName + ".PNG";
                xmlFileName = baseName + ".XML";
            }

            File textFile = new File(dir, textFileName);
            try {
                textFile.delete();
                textFile.createNewFile();
                Utility.writeFile(textFile, text.toString().getBytes());
            }
            catch (UMROException e) {
                System.err.println("Unable to write anonymized text file: " + e);
                e.printStackTrace();
            }
            catch (IOException e) {
                System.err.println("Unable to create anonymized text file " + textFile.getAbsolutePath() + " : " + e);
                e.printStackTrace();
            }

            if (SOPClass.isImageStorage(Attribute.getSingleStringValueOrEmptyString(attributeList,TagFromName.SOPClassUID))) {
                try {
                    BufferedImage image = ConsumerFormatImageMaker.makeEightBitImage(attributeList, 0);
                    File imageFile = new File(dir, imageFileName);
                    try {
                        ImageIO.write(image, "png", imageFile);
                        Log.get().info("Created image file " + imageFile.getAbsolutePath());
                    }
                    catch (IOException e) {
                        Log.get().warning("Unable to write image file as part of anonymization for file " + imageFile.getAbsolutePath() + " : " + e);
                    }
                }
                catch (Exception e) {
                    Log.get().info("Unable to convert to image file: " + e);
                }
            }

            try {
                Document document = new XMLRepresentationOfDicomObjectFactory().getDocument(attributeList);
                if (DicomClient.getReplaceControlCharacters()) {
                    XML.replaceControlCharacters(document, ' ');
                }
                String xmlText = XML.domToString(document);
                Utility.writeFile(new File(dir, xmlFileName), xmlText.getBytes());
            }
            catch (ParserConfigurationException e) {
                System.err.println("Unable to parse anonymized DICOM as XML: " + e);
                e.printStackTrace();
            }
            catch (UMROException e) {
                System.err.println("Unable to write anonymized XML file: " + e);
                e.printStackTrace();
            }

        }
    }

    /**
     * AnonymizeGUI this series and write the results to new files.
     */
    private synchronized void anonymizeSeries() {
        try {
            processLock.acquireUninterruptibly();
            if (!processOk) return;
            File newFile = null;
            int count = 0;
            int tries = 0;
            try {
                previewProgressLayout.show(previewProgressPanel, CARD_PROGRESS);
                zeroProgressBar();
                for (String fileName : instanceList.values()) {
                    tries++;
                    CustomAttributeList attributeList = new CustomAttributeList();
                    attributeList.read(fileName);

                    AnonymizeGUI.getInstance().updateTagList(attributeList);

                    Anonymize.anonymize(attributeList, getAnonymizingReplacementList());
                    FileMetaInformation.addFileMetaInformation(attributeList, Util.DEFAULT_TRANSFER_SYNTAX, "DICOMService");

                    // set up to put file in new directory
                    newFile = getNewFile(attributeList);
                    File parent = (newFile.getParentFile() == null) ? new File(".") : newFile.getParentFile();
                    parent.mkdirs();
                    attributeList.write(newFile, Util.DEFAULT_TRANSFER_SYNTAX, true, true);

                    saveTextAndXmlAndImageFiles(attributeList, newFile);
                    count++;
                    setProgress(count);
                    Log.get().info("Anonymized to file: " + newFile.getAbsolutePath());
                }
            }
            catch (DicomException e) {
                String msg = "DICOM error - unable to anonymize series : " + e;
                DicomClient.getInstance().showMessage(msg);
                Log.get().severe(msg);
                processOk = false;
                if (DicomClient.inCommandLineMode()) {
                    System.err.println(msg);
                    System.exit(1);
                }
            }
            catch (IOException e) {
                processOk = false;
                String msg = "File error - unable to anonymize series : " + e;
                DicomClient.getInstance().showMessage(msg);
                Log.get().severe(msg);
                // If there was an IO exception, then
                if (tries == 1) { // only tell user once per series
                    String ioMsg = "<html>Unable to write file\n\n    " + newFile.getAbsolutePath()
                            + "\n<p><br><p>It is possible that you do not have permission to write to this directory." + "\n<p><br><p>Technical details:<br>" + e + "\n</html>";
                    new Alert(ioMsg, "Unable to write file");
                }
                if (DicomClient.inCommandLineMode()) {
                    System.err.println(msg);
                    System.exit(1);
                }
            }
            finally {
                previewProgressLayout.show(previewProgressPanel, CARD_SLIDER);
            }
            if (count == instanceList.size()) {
                isAnonymized = true;
                setProcessedStatus();
            }
            else {
                String msg = "Unable to anonymize series " + toString() + " .  Only " + count + " slices of " + instanceList.size() + " were completed.";
                DicomClient.getInstance().showMessage(msg);
            }
        }
        finally {
            processLock.release();
        }
    }

    private boolean uploadKoFile() {
        if (DicomClient.getInstance().getKoManifestPolicy()) {
            try {
                Log.get().info("Sending KO file for series " + seriesSummary);
                KeyObject keyObject = new KeyObject(seriesSummary, instanceList.values());
                pushDicom(new AttributeList[] { keyObject });
                if (!processOk) {
                    String msg = "Could not upload Key Object file for series";
                    Log.get().info(msg);
                    DicomClient.getInstance().showMessage(msg + seriesSummary);
                    return false;
                }
            }
            catch (Exception e) {
                Log.get().info("Upload failed.  Could not generate Key Object File for series: " + e);
                return false;
            }
        }
        else {
            Log.get().info("Not sending KO file for series " + seriesSummary);
        }
        return true;
    }

    private String getSeriesSummary() {
        String seriesSum = "";
        seriesSummary += (patientID == null) ? "" : " " + patientID;
        seriesSummary += (patientName == null) ? "" : " " + patientName;
        seriesSummary += (seriesNumber == null) ? "" : " " + seriesNumber;
        seriesSummary += (modality == null) ? "" : " " + modality;
        seriesSummary += (seriesDescription == null) ? "" : " " + seriesDescription;
        return seriesSum;
    }

    /**
     * Upload the entire series to the currently selected PACS. If any of the
     * files in the series have a problem uploading, then fail with a message to
     * the user. If the entire upload succeeds, then mark the series as
     * uploaded.
     */
    private void uploadSeries() {
        if (!processOk) return;
        String aeTitle = getSelectedAeTitle();
        zeroProgressBar();
        int fileIndex = 0;
        if (aeTitle == null) {
            String message = "No valid PACS selected.";
            Log.get().warning(message);
            DicomClient.getInstance().showMessage(message);
            return;
        }
        try {
            Log.get().info("Starting upload of " + seriesSummary);

            if (!uploadKoFile()) return;

            AttributeList[] tempList = new AttributeList[instanceList.size()];
            while ((fileIndex < instanceList.size()) && (processOk)) {
                int f = 0;
                while ((fileIndex < instanceList.size()) && processOk && (f < UPLOAD_BUFFER_SIZE)) {
                    String fileName = instanceList.getFileName(fileIndex);
                    AttributeList attributeList = new AttributeList();
                    try {
                        attributeList.read(fileName);
                    }
                    catch (Exception e) {
                        String msg = seriesSummary + " Unable to read DICOM file " + fileName + " : " + e;
                        Log.get().warning(msg);
                        DicomClient.getInstance().showMessage(msg);
                        new Alert(msg, "Upload Failure");
                        processOk = false;
                        return;
                    }
                    tempList[f] = attributeList;
                    f++;
                    fileIndex++;
                }
                AttributeList[] attrListList = null;
                if (tempList.length == f)
                    attrListList = tempList;
                else {
                    attrListList = new AttributeList[f];
                    for (int ff = 0; ff < f; ff++)
                        attrListList[ff] = tempList[ff];
                }

                pushDicom(attrListList);
                if (processOk) {
                    Log.get().info("Uploaded file to PACS " + aeTitle);
                    setProgress(fileIndex);
                    DicomClient.getInstance().incrementUploadCount(attrListList.length);
                }
            }

            if (processOk) {
                aeTitleUploadList.add(aeTitle);
                DicomClient.getInstance().setProcessedStatus();
            }
        }
        catch (Exception e) {
            Log.get().severe("Unexpected error in edu.umro.dicom.client.Series.uploadSeries: " + e);
            e.printStackTrace();
        }
        finally {
            previewProgressLayout.show(previewProgressPanel, CARD_SLIDER);
        }

        Log.get().info("Uploaded " + fileIndex + " of " + instanceList.size() + " files.");
    }

    /*
     * @SuppressWarnings("unchecked") private void
     * addAnonymousAttributes(AttributeList attributeList) { AttributeList
     * anonList = ClientConfig.getInstance().getAnonymizingReplacementList();
     * AttributeList newAttrs = new AttributeList();
     * 
     * for (AttributeTag tag : (Set<AttributeTag>)attributeList.keySet()) { if
     * ((anonList.get(tag) != null) && (newAttrs.get(tag) == null)) {
     * newAttrs.put(anonList.get(tag)); } }
     * AnonymizeGUI.getInstance().updateTagList(newAttrs); }
     */

    /**
     * Add the given file to this series.
     * 
     * @param fileName
     *            The file to add.
     * 
     * @param attributeList
     *            Parsed version of the DICOM file contents.
     */
    public void addFile(String fileName, AttributeList attributeList) {
        AnonymizeGUI.getInstance().updateTagList(attributeList);
        String msg = instanceList.put(fileName, attributeList);
        progressBar.setMaximum(instanceList.size());
        if (msg == null) {
            resetSummary();
        }
        else {
            DicomClient.getInstance().showMessage(msg);
        }
        if (DicomClient.getInstance().getPreview().getPreviewedSeries() == this) {
            int sliceNumber = instanceList.size() / 2;
            sliceNumber = (sliceNumber < 1) ? 1 : sliceNumber;
            showPreview(sliceNumber);
        }
    }

    /**
     * Get the series instance UID.
     * 
     * @return The series instance UID.
     */
    public String getSeriesInstanceUID() {
        return seriesInstanceUID;
    }

    /**
     * Determine if this series is equal to another. Two series are considered
     * equal if they have the same series instance UID.
     */
    public boolean equals(Object other) {
        return (other instanceof Series) && seriesInstanceUID.equals(((Series) other).seriesInstanceUID);
    }

    public static volatile boolean processOk = true;

    @Override
    public void actionPerformed(ActionEvent ev) {
        processOk = true;
        if (ev.getSource() == uploadAnonymizeButton) {
            if (DicomClient.getInstance().getAnonymizeMode()) {
                if (DicomClient.getInstance().ensureAnonymizeDirectoryExists()) {
                    processSeries();
                }
            }
            else {
                processSeries();
            }
        }

        if (ev.getSource() == previewButton) {
            int slice = instanceList.size() / 2;
            slice = (slice < 1) ? 1 : slice;
            showPreview(slice);
        }
    }

    @Override
    public String toString() {
        return summaryLabel.getText();
    }

    /**
     * Get the title to be displayed on the preview window.
     * 
     * @param sliceNumber
     *            Current slice being displayed.
     * 
     * @return Orienting description of preview.
     */
    public String getPreviewTitle(int sliceNumber) {
        return getDescription() + "   " + sliceNumber + " / " + instanceList.size();
    }

    /**
     * Get a description of this series.
     * 
     * @return A description of this series.
     */
    public String getDescription() {
        StringBuffer title = new StringBuffer();

        title.append((patientID == null) ? "" : "  " + patientID);
        title.append((patientName == null) ? "" : "  " + patientName);
        title.append((seriesNumber == null) ? "" : "  " + seriesNumber);
        title.append((modality == null) ? "" : "  " + modality);
        title.append((seriesDescription == null) ? "" : "  " + seriesDescription);
        return title.toString();
    }

    /**
     * Show the current slice in the previewer.
     */
    public synchronized void showPreview(int sliceNumber) {
        Preview preview = DicomClient.getInstance().getPreview();
        String fileName = instanceList.values().get(sliceNumber - 1);
        preview.showDicom(this, getPreviewTitle(sliceNumber), sliceNumber, getFileNameList().size(), fileName);
    }

    /**
     * Get the sortedList of file names.
     * 
     * @return List of file names.
     */
    public Collection<String> getFileNameList() {
        return instanceList.values();
    }

}
