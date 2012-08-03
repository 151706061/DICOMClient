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

import java.lang.NumberFormatException;

import java.util.Date;
import java.util.ArrayList;

import com.pixelmed.dicom.DicomDictionary;
import com.pixelmed.dicom.ValueRepresentation;
import com.pixelmed.dicom.AttributeTag;


/**
 * An extended DICOM Dictionary that allows the
 * inclusion of private tags.
 *  
 * @author Jim Irrer  irrer@umich.edu 
 *
 */
public class CustomDictionary extends DicomDictionary {

    private volatile static CustomDictionary instance = null;

    /** List of extensions provided by this dictionary. */
    private volatile static ArrayList<PrivateTag> extensions = null;


    /**
     * Format an integer as a hex number with leading zeroes
     * padded to the indicated length.
     *
     * @param i Integer to format.
     *
     * @param len Number of resulting digits.
     *
     * @return Formatted integer.
     */
    private String intToHex(int i, int len) {
        String text = Integer.toHexString(i);
        if (text.length() > len) {
            throw new NumberFormatException("Unable to fit integer value of '" + i +
                    "' into " + len + " hex digits.  Result is: " + text);
        }
        while (text.length() < len) {
            text = "0" + text;
        }
        return text;
    }


    /**
     * Override super's method to add extensions.
     */
    @SuppressWarnings("unchecked") protected void createTagList() {
        init();
        super.createTagList();
        for (int e = 0; e < extensions.size(); e++) {
            PrivateTag privateTag = extensions.get(e);
            tagList.add(privateTag.getAttributeTag());
        }
    }




    /**
     * Override super's method to add extensions.
     */
    @SuppressWarnings("unchecked") protected void createValueRepresentationsByTag() {
        init();
        super.createValueRepresentationsByTag();
        for (int e = 0; e < extensions.size(); e++) {
            PrivateTag privateTag = extensions.get(e);
            valueRepresentationsByTag.put(privateTag.getAttributeTag(), privateTag.getValueRepresentation());
        }
    }




    /**
     * Override super's method to add extensions.
     */
    @SuppressWarnings("unchecked") protected void createInformationEntityByTag() {
        init();
        super.createInformationEntityByTag();
        for (int e = 0; e < extensions.size(); e++) {
            PrivateTag privateTag = extensions.get(e);
            informationEntityByTag.put(privateTag.getAttributeTag(), privateTag.getInformationEntity());
        }
    }




    /**
     * Override super's method to add extensions.
     */
    @SuppressWarnings("unchecked") protected void createNameByTag() {
        init();
        super.createNameByTag();
        for (int e = 0; e < extensions.size(); e++) {
            PrivateTag privateTag = extensions.get(e);
            nameByTag.put(privateTag.getAttributeTag(), privateTag.getName());
        }
    }




    /**
     * Override super's method to add extensions.
     */
    @SuppressWarnings("unchecked") protected void createTagByName() {
        init();
        super.createTagByName();
        for (int e = 0; e < extensions.size(); e++) {
            PrivateTag privateTag = extensions.get(e);
            tagByName.put(privateTag.getName(), privateTag.getAttributeTag());
        }
    }




    /**
     * Override super's method to add extensions.
     */
    @SuppressWarnings("unchecked") protected void createFullNameByTag() {
        init();
        super.createFullNameByTag();
        for (int e = 0; e < extensions.size(); e++) {
            PrivateTag privateTag = extensions.get(e);
            fullNameByTag.put(privateTag.getName(), privateTag.getFullName());
        }
    }


    private synchronized void init() {
        // only do this once.
        if (extensions == null) {
            //extensions = new ArrayList<PrivateTag>();
            //generateCustomAttributes();
            Profile.profile();
            extensions = ClientConfig.getInstance().getPrivateTagList();
            Profile.profile();
        }
    }


    /**
     * Default constructor.
     */
    private CustomDictionary() {
        init();
    }


    public synchronized static CustomDictionary getInstance() {
        if (instance == null) {
            instance = new CustomDictionary();
        }
        return instance;
    }


    /**
     * Format an XML version of this dictionary.
     *
     * @param out Print it to here.
     */
    @Override
    public String toString() {

        StringBuffer text = new StringBuffer("");
        Date now = new Date();

        text.append("<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n");

        text.append("<CustomDictionary>\n");
        text.append("<CustomDictionaryGenerationTimeAndDate>" +
                now +
        "</CustomDictionaryGenerationTimeAndDate>\n");
        text.append("<CustomDictionaryVersionTimeStamp>" +
                now.getTime() +
        "</CustomDictionaryVersionTimeStamp>\n");
        for (Object oTag : tagList) {
            AttributeTag tag = (AttributeTag)(oTag);
            // out.write(getNameFromTag(tag) + " : " + tag);
            // String tagName = getNameFromTag(tag);
            String subText = "<" + getNameFromTag(tag);

            subText +=
                " element='" + intToHex(tag.getElement(), 4) + "'" +
                " group='" + intToHex(tag.getGroup(), 4) + "'" +
                " vr='" + ValueRepresentation.getAsString(getValueRepresentationFromTag(tag)) + "'" +
                "></" + getNameFromTag(tag) + ">\n";
            text.append(subText);
        }
        text.append("</CustomDictionary>\n");
        return text.toString().replaceAll("\n", System.getProperty("line.separator"));
    }


    public static void main(String[] args) {
        CustomDictionary cd = new CustomDictionary();
        System.out.println("varian tag: " + cd.getNameFromTag(new AttributeTag(0x3249, 0x0010)));
    }

}

