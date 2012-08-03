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

import org.w3c.dom.Node;

import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.ValueRepresentation;

import edu.umro.util.UMROException;
import edu.umro.util.XML;

/**
 * Container for all of the attributes to define a private DICOM tag.
 * 
 * @author Jim Irrer  irrer@umich.edu
 */

public class PrivateTag {

    /** Value representation. */
    private byte valueRepresentation[] = null;

    /** Name. */
    private String name = null;

    /** Full name. */
    private String fullName = null;

    /** Attribute tag. */
    private AttributeTag attributeTag = null;

    // ----------------------------------------------------------------

    /**
     * Set up all values associated with this tag.
     *
     * @param gr Group id.
     *
     * @param el Element.
     *
     * @param vr Value representation.
     *
     * @param n Name.
     *
     * @param fn Full name.
     *
     * @param ie Information entity.
     */
    public PrivateTag(int group, int element, byte [] vr, String n, String fn) {
        valueRepresentation = vr;
        name = n;
        fullName = fn;
        attributeTag = new AttributeTag(group, element);
    }


    /**
     * Construct an attribute tag from a DOM node.  Example XML text:<p>
     * 
     * {@code <CurveLabel50da element='2500' group='50da' vr='LO' fullName='Curve Label 50da'></CurveLabel50da> }
     * 
     * @param node Node containing info.
     * 
     * @throws UMROException
     * @throws DicomException
     */
    public PrivateTag(Node node) throws UMROException, DicomException {
        name = node.getNodeName();
        valueRepresentation = XML.getValue(node, "@vr").getBytes();
        String tagText = "0x" + XML.getValue(node, "@group") + ",0x" + XML.getValue(node, "@element");
        attributeTag = new AttributeTag(tagText);
        fullName = XML.getValue(node, "@fullName");
    }

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
     * Convert this to XML.
     * 
     * return String containing XML.
     */
    public String toXml() {
        // out.write(getNameFromTag(tag) + " : " + tag);
        // String tagName = getNameFromTag(tag);
        return
        "<" + name +
        " element='" + intToHex(attributeTag.getElement(), 4) + "'" +
        " group='" + intToHex(attributeTag.getGroup(), 4) + "'" +
        " vr='" + ValueRepresentation.getAsString(valueRepresentation) + "'" +
        " fullName='" + XML.escapeSpecialChars(fullName) + "'" +
        "></" + name + ">";
    }


    // ----------------------------------------------------------------

    /**
     * Get the value representation.
     *
     * @return The value representation.
     */
    public byte[] getValueRepresentation() {
        return valueRepresentation;
    }
    // ----------------------------------------------------------------

    /**
     * Get the name.
     *
     * @return The name.
     */
    public String getName() {
        return name;
    }

    // ----------------------------------------------------------------

    /**
     * Get the full name.
     *
     * @return The full name.
     */
    public String getFullName() {
        return fullName;
    }

    // ----------------------------------------------------------------

    /**
     * Get the attribute tag.
     *
     * @return The attribute tag.
     */
    public AttributeTag getAttributeTag() {
        return attributeTag;
    }

    // ----------------------------------------------------------------

    /**
     * Get the information entity.
     *
     * @return The information entity.
     */
    public AttributeTag getInformationEntity() {
        return attributeTag;
    }

    // ----------------------------------------------------------------
}
