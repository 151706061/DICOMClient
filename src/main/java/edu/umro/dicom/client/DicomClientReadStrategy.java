package edu.umro.dicom.client;

/*
 * Copyright 2013 Regents of the University of Michigan
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

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.ReadStrategy;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;


/*
 * Tags referenced application sorted by group and element.
 * (0x0002,0x0002)    name: MediaStorageSOPClassUID
 * (0x0008,0x0012)    name: InstanceCreationDate
 * (0x0008,0x0012)    name: InstanceCreationDate
 * (0x0008,0x0013)    name: InstanceCreationTime
 * (0x0008,0x0013)    name: InstanceCreationTime
 * (0x0008,0x0021)    name: SeriesDate
 * (0x0008,0x0022)    name: AcquisitionDate
 * (0x0008,0x0023)    name: ContentDate
 * (0x0008,0x0031)    name: SeriesTime
 * (0x0008,0x0032)    name: AcquisitionTime
 * (0x0008,0x0033)    name: ContentTime
 * (0x0008,0x0060)    name: Modality
 * (0x0008,0x103e)    name: SeriesDescription
 * (0x0010,0x0010)    name: PatientName
 * (0x0010,0x0020)    name: PatientID
 * (0x0020,0x000e)    name: SeriesInstanceUID
 * (0x3006,0x0008)    name: StructureSetDate
 * (0x3006,0x0009)    name: StructureSetTime
 * (0x300a,0x0006)    name: RTPlanDate
 * (0x300a,0x0007)    name: RTPlanTime
 */



public class DicomClientReadStrategy implements ReadStrategy {
    
    private static final long MAX_READ = 1024 *4;
    
    public static final DicomClientReadStrategy dicomClientReadStrategy = new DicomClientReadStrategy();

    @Override
    public boolean terminate(AttributeList attributeList, Attribute attribute, long bytesRead) {
        if (bytesRead >= MAX_READ)
            return true;
        Attribute sopClassUID = attributeList.get(TagFromName.SOPClassUID);
        AttributeTag tag = attribute.getTag();
        if (sopClassUID != null) {
            String classUID = sopClassUID.getSingleStringValueOrEmptyString();
            if (classUID.equals(SOPClass.RTStructureSetStorage) || classUID.equals(SOPClass.RTPlanStorage)) {
                if (tag.getGroup() > TagFromName.RTPlanTime.getGroup())
                    return true;
                if ((tag.getGroup() == TagFromName.RTPlanTime.getGroup()) && (tag.getElement() >= TagFromName.RTPlanTime.getElement()))
                    return true;
            }
            else {
                if (tag.getGroup() > TagFromName.SeriesInstanceUID.getGroup())
                    return true;
                if ((tag.getGroup() == TagFromName.SeriesInstanceUID.getGroup()) && (tag.getElement() >= TagFromName.SeriesInstanceUID.getElement()))
                    return true;
            }
        }
        return false;
    }
}
