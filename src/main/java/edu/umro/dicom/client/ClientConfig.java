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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeFactory;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomDictionary;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.ValueRepresentation;

import edu.umro.util.Log;
import edu.umro.util.UMROException;
import edu.umro.util.Utility;
import edu.umro.util.XML;

/**
 * Get the configuration information.
 * 
 * @author Jim Irrer  irrer@umich.edu 
 *
 */
public class ClientConfig {

    /** Name of configuration file. */
    private static final String CONFIG_FILE_NAME = "DicomClientConfig.xml";

    /** List of all possible configuration files. */
    private static final String[] CONFIG_FILE_LIST = {
        System.getProperty("dicomclient.config"),
        CONFIG_FILE_NAME,
        "src\\main\\resources\\" + CONFIG_FILE_NAME
    };

    /** Instance of this object. */
    private volatile static ClientConfig clientConfig = null;

    /** Configuration information from file. */
    private volatile Document config = null;


    /** List of private tags. */
    private ArrayList<PrivateTag> privateTagList = null;


    /**
     * Read in the configuration for the client from the configuration file.  Try
     * all files on the list and use the first one that parses.
     */
    private void parseConfigFile() {
        for (String configFileName : CONFIG_FILE_LIST) {
            try {
                Log.get().info("Trying configuration file " + (new File(configFileName)).getAbsolutePath());
                config = XML.parseToDocument(Utility.readFile(new File(configFileName)));
            }
            catch (Exception e) {
                ;
            }
            if (config != null) {
                Log.get().info("Using configuration file " + (new File(configFileName)).getAbsolutePath());
                break;
            }
        }
        if (config == null) {
            Log.get().severe("Unable to read and parse any configuration file of: " + CONFIG_FILE_LIST);
        }
    }


    /**
     * Construct a configuration object.
     */
    public ClientConfig() {
        parseConfigFile();
    }


    /**
     * Get the base URL for the DICOM Service with any terminating /'s removed
     *  
     * @return Base URL for DICOM service, or null if not initialized.
     */
    public String getServerBaseUrl() {
        if (config != null) {
            try {
                return XML.getValue(config, "/DicomClientConfig/DicomServiceUrl/text()");
            }
            catch (UMROException e) {
                Log.get().logrb(Level.SEVERE, this.getClass().getCanonicalName(), "AriaVerifier", null,
                        "Failed to get DICOM service URL from configuration file " + CONFIG_FILE_NAME, e);
            }
        }
        Log.get().severe("ClientConfig.getServerBaseUrl: Unable to read configuration file " + CONFIG_FILE_NAME);
        return null;
    }


    /**
     * Get the flag indicating whether or not the upload capability should be shown.  If there is a problem,
     * return true.
     *  
     * @return True if all help, false if only anonymize help.
     */
    public boolean getShowUploadCapability() {
        if (config != null) {
            try {
                String text = XML.getValue(config, "/DicomClientConfig/ShowUploadHelp/text()");
                return text.equalsIgnoreCase("true") || text.equalsIgnoreCase("yes");
            }
            catch (UMROException e) {

            }
        }
        Log.get().severe("getShowUploadHelp: Unable to read configuration file " + CONFIG_FILE_NAME);
        return true;
    }


    /**
     * Get the template that controls how new patient IDs are generated for anonymization.
     *  
     * @return Template that controls how new patient IDs are generated for anonymization.
     */
    public String getAnonPatientIdTemplate() {
        if (config != null) {
            try {
                return XML.getValue(config, "/DicomClientConfig/AnonPatientIdTemplate/text()");
            }
            catch (UMROException e) {

            }
        }
        Log.get().severe("getAnonPatientIdTemplate: Unable to read configuration file " + CONFIG_FILE_NAME);
        return null;
    }


    /**
     * Get the values that should be replaced for aggressive patient anonymization.
     *  
     * @return List of values (in lower case) and their replacement values.  
     */
    public HashMap<String,String> getAggressiveAnonymization(AttributeList attributeList, DicomDictionary dictionary) {
        HashMap<String,String> replaceList = new HashMap<String, String>();
        getReservedWordList();
        if (config != null) {
            try {
                NodeList nodeList = XML.getMultipleNodes(config, "/DicomClientConfig/AggressiveAnonymization");
                for (int n = 0; n < nodeList.getLength(); n++) {
                    Node node = nodeList.item(n);
                    String replacement = XML.getAttributeValue(node, "replacement");
                    replacement = (replacement == null) ? "" : replacement;
                    String tagName = XML.getValue(node, "text()");
                    AttributeTag tag = dictionary.getTagFromName(tagName);
                    if (tag == null) {
                        throw new RuntimeException("Unknown DICOM attribute " + tagName + " in AggressiveAnonymization list.");
                    }
                    Attribute attribute = attributeList.get(tag);
                    if (attribute != null) {
                        for (String value : attribute.getOriginalStringValues()) {
                            String[] tokenList = value.toLowerCase().split("[^a-z0-9]");
                            for (String token : tokenList) {
                                if ((token.length() > 1) && (!reservedWordList.contains(token))) {
                                    replaceList.put(token, replacement);
                                    Log.get().info("Aggressive anonymization replace " + tagName + " value of " + token + " with " + replacement);
                                }
                            }
                        }
                    }
                }
            }
            catch (UMROException e) { Log.get().severe("UMROException getAggressiveAnonymization : " + e);}
            catch (DicomException e) { Log.get().severe("DicomException getAggressiveAnonymization : " + e);}
        }
        return replaceList;
    }

    private HashSet<String> reservedWordList = null;
    
    private HashSet<String> getReservedWordList() {
        if (reservedWordList == null) {
            reservedWordList = new HashSet<String>();
            if (config != null) {
                String text = null;
                try {
                    text = XML.getValue(config, "/DicomClientConfig/ReservedWordList/text()");
                    String[] list = text.toLowerCase().replace('\r', ' ').replace('\n', ' ').split(" ");
                    for (String word : list) {
                        reservedWordList.add(word);
                    }
                    DicomDictionary dictionary = new DicomDictionary();
                    Iterator i = dictionary.getTagIterator();
                    while (i.hasNext()) {
                        AttributeTag tag = (AttributeTag)i.next();
                        String fullName = dictionary.getFullNameFromTag(tag);
                        String[] wordList = fullName.toLowerCase().split(" ");
                        for (String word : wordList) {
                            reservedWordList.add(word);
                        }
                    }
                }
                catch (UMROException e) {
                }
            }
        }
        return reservedWordList;
    }


    /**
     * Get the template that controls how new patient IDs are generated for anonymization.
     *  
     * @return Template that controls how new patient IDs are generated for anonymization.
     */
    public String getRootGuid() {
        if (config != null) {
            try {
                return XML.getValue(config, "/DicomClientConfig/RootGuid/text()");
            }
            catch (UMROException e) {

            }
        }
        Log.get().severe("getRootGuid: Unable to read configuration file " + CONFIG_FILE_NAME);
        return null;
    }


    private boolean isTrue(String text) {
        return
        text.equalsIgnoreCase("true") || 
        text.equalsIgnoreCase("yes") || 
        text.equalsIgnoreCase("y") || 
        text.equalsIgnoreCase("1") || 
        text.equalsIgnoreCase("t");
    }


    private boolean getFlag(String path, String flagName) {
        if (config != null) {
            try {
                String text = XML.getValue(config, path).trim();
                boolean enabled = isTrue(text);
                return enabled;
            }
            catch (UMROException e) {
                Log.get().info(flagName + " : Unable to get flag: " + e);

            }
        }
        Log.get().info(flagName + " : Unable to read configuration file " + CONFIG_FILE_NAME);
        return false;
    }


    /**
     * Get flag indicating whether or not profiling is to be done.  If there is a problem
     * getting the flag, then assume false.
     *  
     * @return Flag indicating whether profiling should be done.
     */
    public boolean getProfileFlag() {
        return getFlag("/DicomClientConfig/Profile/text()", "getProfileFlag");
    }


    public boolean restrictXmlTagsToLength32() {
        return getFlag("/DicomClientConfig/RestrictXmlTagsToLength32/text()", "restrictXmlTagsToLength32");
    }


    public boolean replaceControlCharacters() {
        return getFlag("/DicomClientConfig/ReplaceControlCharacters/text()", "replaceControlCharacters");
    }


    /**
     * Only let user control certain types of attributes.  It does not make
     * sense to manually control values of UIDs, and sequence attributes do
     * not have values.
     * 
     * @param tag Tag to check.
     * 
     * @return
     */
    private boolean canControlAnonymizing(AttributeTag tag) {
        return (!ValueRepresentation.isSequenceVR(CustomDictionary.getInstance().getValueRepresentationFromTag(tag)));
    }


    /**
     * Get the list of attributes to anonymize and their values.  All UIDs are
     * anonymized by default.
     * 
     * @return
     */
    public AttributeList getAnonymizingReplacementList() {
        AttributeList attributeList = new AttributeList();
        try {
            NodeList nodeList = XML.getMultipleNodes(config, "/DicomClientConfig/AnonymizeDefaultList/*");
            for (int ad = 0; ad < nodeList.getLength(); ad++) {
                Node node = nodeList.item(ad);
                String tagText = XML.getValue(node, "@Name");
                if (tagText != null) {
                    AttributeTag tag = CustomDictionary.getInstance().getTagFromName(tagText);
                    if (tag != null) {
                        String value = XML.getValue(node, "text()");
                        value = (value == null) ? "" : value;
                        Attribute attribute = AttributeFactory.newAttribute(tag);
                        if (canControlAnonymizing(tag)) {
                            attribute.addValue(value);
                            attributeList.put(attribute);
                        }
                    }
                }
            }
        }
        catch (UMROException e) {
            Log.get().warning("Unable to parse list of default attributes to anonymize.  User will have to supply them manually.");
        }
        catch (DicomException e) {
            Log.get().warning(this.getClass().getName() + ".getAnonymizingReplacementList : Failed to construct DICOM Attribute: " + e);
        }
        return attributeList;
    }


    /**
     * Determine the trust store file to use and set it up.
     * First look at the javax.net.ssl.trustStore system property,
     * and if it is pointing to a readable file then use it.
     * If it is not set, then look through the javax.net.ssl.trustStore
     * list in the configuration file and use the first one that
     * points to a readable file.
     * 
     * @return The file to be used.
     */
    /*
    public File setupTrustStore() {
        NodeList nodeList = null;    
        try {
            nodeList = XML.getMultipleNodes(config, "/DicomClientConfig/javax.net.ssl.trustStore/text()");
        }
        catch (UMROException e) {
            Log.get().warning("Unable to parse list of javax.net.ssl.trustStore.  You will not be able to communicate with the DICOM service.  Details: " + e);
        }
        File file = null;
        try {
            file = Util.setupTrustStore(nodeList).getKeystoreFile();
        }
        catch (Exception e) {
            file = null;
        }
        if (file == null) {
            Log.get().warning("Unable to read trustStore file.  You will not be able to communicate with the DICOM service.");
        }
        else {
            System.setProperty("javax.net.ssl.trustStore", file.getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        }
        return file;
    }
     */


    /**
     * Determine which java key stores are available and return a
     * list of them.
     * 
     * @return List of available java key store files.
     */
    public ArrayList<File> getJavaKeyStoreList() {
        ArrayList<File> list = new ArrayList<File>();
        try {
            NodeList nodeList = XML.getMultipleNodes(config, "/DicomClientConfig/javax.net.ssl.trustStore/text()");
            for (int tsd = 0; tsd < nodeList.getLength(); tsd++) {
                Node node = nodeList.item(tsd);
                list.add(new File(node.getNodeValue()));
            }
        }
        catch (UMROException e) {
            Log.get().warning("Unable to parse list of javax.net.ssl.trustStore.  You will not be able to communicate with the DICOM service.");
        }
        return list;
    }


    public synchronized ArrayList<PrivateTag> getPrivateTagList() {
        if (privateTagList == null) {
            privateTagList = new ArrayList<PrivateTag>();
            try {
                NodeList nodeList = XML.getMultipleNodes(config, "/DicomClientConfig/PrivateTagList/*");
                for (int n = 0; n < nodeList.getLength(); n++) {
                    Node node = nodeList.item(n);
                    String group = XML.getValue(node, "@group");
                    if (group.contains(":")) {
                        int element = Integer.parseInt(XML.getValue(node, "@element").toUpperCase(), 16);
                        byte[] valueRepresentation = XML.getValue(node, "@vr").getBytes();
                        String name = node.getNodeName();
                        String fullName = XML.getValue(node, "@fullName");
                        String[] parts = group.split(":");
                        int firstGroup = Integer.parseInt(parts[0].toUpperCase(), 16);
                        int lastGroup = Integer.parseInt(parts[1].toUpperCase(), 16);
                        int incr = (parts.length < 2) ? 1 : Integer.parseInt(parts[2].toUpperCase(), 16);
                        for(int g = firstGroup; g <= lastGroup; g += incr) {
                            String gHex = String.format("%04x", g);
                            privateTagList.add(new PrivateTag(g, element, valueRepresentation, name+gHex, fullName + " " + gHex));
                        }
                    }
                    else {
                        privateTagList.add(new PrivateTag(node));
                    }
                }
            }
            catch (UMROException e) {
                Log.get().warning("Problem interpreting custom tags: " + e);
            }
            catch (DicomException e) {
                Log.get().warning("DICOM Problem interpreting custom tags: " + e);
            }
        }
        return privateTagList;
    }


    public static ClientConfig getInstance() {
        if (clientConfig == null) {
            clientConfig = new ClientConfig();
        }
        return clientConfig;
    }
}
