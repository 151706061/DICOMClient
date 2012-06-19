package edu.umro.dicom.common;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.ValueRepresentation;

import edu.umro.util.Log;
import edu.umro.util.UMROGUID;


/**
 * Represent a patient ID - GUID combination
 * for anonymization.  The pairs are used to
 * facilitate the proper re-using of GUIDs
 * necessary to consistent anonymization of
 * GUIDs.
 * 
 * @author irrer
 *
 */
class Guid {

    /** Non-anonymized patient ID. */
    private String patientId;

    /** Anonymized GUID. */
    private String guid;

    public Guid(String patientId, String guid) {
        this.patientId = patientId;
        this.guid = guid;
    }

    @Override
    public boolean equals(Object obj) {
        Guid other = (Guid)obj;
        return patientId.equals(other.patientId) && guid.equals(other.guid);
    }

    @Override
    public int hashCode() {
        return patientId.hashCode() ^ guid.hashCode();
    }
}


public class Anonymize {


    private static HashSet<String> patientList = new HashSet<String>();


    private static HashMap<Guid, String> guidHistory = new HashMap<Guid, String>();


    public synchronized static String makeUniquePatientId() {
        String patientId = null;
        while (patientId == null) {
            Random random = new Random();
            int r = random.nextInt() % 10000;
            r = (r < 0) ? (-r) : r;
            String text = "" + r;
            while (text.length() < 4) {
                text = "0" + text;
            }
            text = "ANON" + text;
            if (!patientList.contains(text)) {
                patientList.add(text);
                patientId = text;
            }
        }
        return patientId;
    }


    private static String establishNewPatientId(AttributeList attributeList) {
        Attribute patientAttribute = attributeList.get(TagFromName.PatientID);
        String patientId = null;
        if (patientAttribute != null) {
            patientId = patientAttribute.getSingleStringValueOrNull();
        }
        return (patientId == null) ? makeUniquePatientId() : patientId;
    }


    /**
     * Translate the given GUID into an anonymized one.  If the
     * same GUID is passed in, the same anonymized GUID will be
     * returned.
     * 
     * @param anonimizedPatientId anonymized (target) patient ID.
     * 
     * @param oldGuid Non-anonymized GUID.
     * 
     * @return Anonymized GUID that is being used instead
     * of the non-anonymized GUID.
     */
    private static synchronized String translateGuid(String anonimizedPatientId, String oldGuid) {

        String newGuid = guidHistory.get(new Guid(anonimizedPatientId, oldGuid));
        if (newGuid == null) {
            try {
                newGuid = UMROGUID.getUID();
                guidHistory.put(new Guid(anonimizedPatientId, oldGuid), newGuid);
            } catch (UnknownHostException e) {
                Log.get().logrb(Level.SEVERE, Anonymize.class.getCanonicalName(),
                        "translateGuid", null, "UnknownHostException Unable to generate new GUID", e);
            } catch (SocketException e) {
                Log.get().logrb(Level.SEVERE, Anonymize.class.getCanonicalName(),
                        "translateGuid", null, "SocketException Unable to generate new GUID", e);
            }
        }
        return newGuid;
    }


    private static void anonymizeNonSequenceAttribute(String patientId, Attribute attribute, Attribute replacement) {
        if (replacement != null) {
            AttributeTag tag = attribute.getTag();

            if (ValueRepresentation.isUniqueIdentifierVR(attribute.getVR())) {
                String oldGuid = attribute.getSingleStringValueOrNull();
                if (oldGuid != null) {
                    String newGuid = translateGuid(patientId, oldGuid);
                    try {
                        attribute.setValue(newGuid);
                    }
                    catch (DicomException e) {
                        ;
                    }
                }
            }

            else {

                try {
                    if (tag.equals(TagFromName.PatientID)) {
                        attribute.setValue(replacement.getSingleStringValueOrEmptyString());
                    }
                    else {
                        attribute.setValue(replacement.getSingleStringValueOrEmptyString());                        
                    }

                } catch (DicomException e) {
                    // If there is a problem, then just make the attribute empty
                    try {
                        attribute.removeValues();
                    }
                    catch (DicomException e1) {
                        ;
                    }
                }
            }
        }
    }


    @SuppressWarnings("unchecked")
    private static TreeMap<AttributeTag, Attribute> getAttributeListValues(AttributeList attributeList) {
        return (TreeMap<AttributeTag, Attribute>)attributeList;
    }


    /**
     * Perform anonymization recursively to accommodate sequence attributes.
     * 
     * @param patientId New patient ID.
     * 
     * @param attributeList Anonymize (modify) this.
     * 
     * @param replacementAttributeList Reference this for what is to be anonymized.
     */
    private static void anonymize(String patientId, AttributeList attributeList, AttributeList replacementAttributeList) {
        for (Attribute attribute : getAttributeListValues(attributeList).values()) {
            AttributeTag tag = attribute.getTag();
            if (attribute instanceof SequenceAttribute) {
                Iterator<?> si = ((SequenceAttribute)attribute).iterator();
                while (si.hasNext()) {
                    SequenceItem item = (SequenceItem)si.next();
                    anonymize(patientId, item.getAttributeList(), replacementAttributeList);
                }
            }
            else {
                Attribute replacement = replacementAttributeList.get(tag);
                if (replacement != null) {
                    anonymizeNonSequenceAttribute(patientId, attribute, replacement);
                }
            }
        }
    }


    /**
     * Anonymize the given DICOM object.  Values are replaced with corresponding values
     * in the replacement list.  All UIDs are replaced with newly constructed ones, and
     * are kept consistent, corresponding to the new PatientID.
     * 
     * The PatientID is required to be anonymized.  If the PatientID is not given in
     * the replacement list, then a new unique patient ID will be constructed and put
     * into the target attribute list.
     * 
     * @param attributeList Target object to be anonymized.
     * 
     * @param replacementAttributeList List of values to be written into the attributeList.
     */
    public static synchronized void anonymize(AttributeList attributeList, AttributeList replacementAttributeList) {
        anonymize(establishNewPatientId(replacementAttributeList), attributeList, replacementAttributeList);
    }
}
