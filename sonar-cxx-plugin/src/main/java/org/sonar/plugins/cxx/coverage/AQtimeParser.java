/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010 Neticoa SAS France
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.cxx.coverage;import java.io.File;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.utils.StaxParser;
import org.sonar.plugins.cxx.utils.CxxUtils;
;

/**
 * {@inheritDoc}
 */
public class AQtimeParser implements CoverageParser {

    /**
     * {@inheritDoc}
     */
    public void parseReport(File xmlFile, final Map<String, CoverageMeasuresBuilder> coverageData) throws XMLStreamException {
        CxxUtils.LOG.info("Parsing report '{}'", xmlFile);

        StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {
            /**
             * {@inheritDoc}
             */
            public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
                rootCursor.advance();
                collectPackageMeasures(rootCursor.childElementCursor("RESULTS").childElementCursor("DATA"), coverageData);
            }

        });
        parser.parse(xmlFile);
    }


    private void collectPackageMeasures(SMInputCursor dataCrusor, Map<String, CoverageMeasuresBuilder> coverageData) throws XMLStreamException {
        SMInputCursor row = dataCrusor.descendantElementCursor("ROW");
        while (row.getNext() != null){
            collectFileMeasures(row, coverageData);
        }

    }


    private void collectFileMeasures(SMInputCursor row, Map<String, CoverageMeasuresBuilder> coverageData) throws XMLStreamException {

        String fileName = "Empty";
        SMInputCursor fields = row.childElementCursor("FIELD");

        while (fields.getNext() != null){
            if (fields.getAttrValue("ID").equals("0")){
                fileName = fields.collectDescendantText();
                break;
            }
        }

        CoverageMeasuresBuilder builder = coverageData.get(fileName);
        if (builder == null) {
            builder = CoverageMeasuresBuilder.create();
            coverageData.put(fileName, builder);
        }

        SMInputCursor lineRows = row.childElementCursor("CHILDREN").childElementCursor("DATA");

        while(lineRows.getNext() != null){
            SMInputCursor lineFields = lineRows.childElementCursor("FIELD");
            int lineId = 0 ;
            long noHits = 0;
            int blocks = 0;
            int blockHits = 0;
            while (lineFields.getNext() != null){
                int lineFieldsID = Integer.parseInt(lineFields.getAttrValue("ID"));
                switch (lineFieldsID) {
                    case 0:
                        lineId = Integer.parseInt(lineFields.collectDescendantText());
                        break;
                    case 1:
                        noHits = Long.parseLong(lineFields.collectDescendantText());
                        if(noHits > Integer.MAX_VALUE){
                            CxxUtils.LOG.warn("Truncating the actual number of hits ({}) to the maximum number supported by Sonar ({})",
                                    noHits, Integer.MAX_VALUE);
                            noHits = Integer.MAX_VALUE;
                        }
                        break;
                    case 2:
                        blocks = Integer.parseInt(lineFields.collectDescendantText());
                        break;
                    case 3:
                        //image file name is ignored
                        break;
                    case 4:
                        blockHits = Integer.parseInt(lineFields.collectDescendantText());
                        break;
                    default:
                        CxxUtils.LOG.warn("Unknown Line Field ID: {} with text: {}",
                                lineFieldsID, lineFields.collectDescendantText());
                        break;
                }

            }

            builder.setHits(lineId, (int)noHits);
            if (blocks > 1){
                builder.setConditions(lineId, blocks, blockHits);
            }

            //Sanity check
            if (blocks < blockHits){
                CxxUtils.LOG.warn("Block hits ({}) is bigger then the amount of blocks ({})!", blockHits, blocks);
            }
        }
    }
}
