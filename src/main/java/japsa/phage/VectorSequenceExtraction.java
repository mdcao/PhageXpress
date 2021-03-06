/*
 * Copyright (c) 2017  Minh Duc Cao (minhduc.cao@gmail.com).
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the names of the institutions nor the names of the contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package japsa.phage;

/**
 * Created by minhduc on 22/04/17.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.SubmissionPublisher;


import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import japsa.seq.SequenceOutputStream;
import japsa.util.HTSUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VectorSequenceExtraction {

    private static final Logger LOG = LoggerFactory.getLogger(VectorSequenceExtraction.class);

    SubmissionPublisher<Sequence> insertPublisher;

    int minimum;
    int flanking;
    //Sequence plasmid;
    String plasmidFile; //plasmid fasta/q file that are already indexed by bwa
    int s5, e5,
            s3, e3;
    String bwaExe = "bwa";
    Process bwaProcess = null;

    public VectorSequenceExtraction(String seqFile, SubmissionPublisher<Sequence> publisher, int e5, int s3, int flank, int minimum) throws IOException{
        plasmidFile=seqFile;
        this.flanking = flanking;
        this.minimum = minimum;
        this.e5=e5;
        this.s5=this.e5 - this.flanking;
        this.s3=s3;
        this.e3=this.s3 + this.flanking;
        this.insertPublisher = publisher;

        SamReaderFactory.setDefaultValidationStringency(ValidationStringency.SILENT);
    }


    private SamReader getSamStream(String inFile, String format, int bwaThread) throws IOException, InterruptedException{
        SamReader reader = null;

        if (format.toLowerCase().endsWith("am")){//bam or sam
            if ("-".equals(inFile))
                reader = SamReaderFactory.makeDefault().open(SamInputResource.of(System.in));
            else
                reader = SamReaderFactory.makeDefault().open(new File(inFile));
        }else{
            LOG.info("Starting bwa  at " + new Date());

            ProcessBuilder pb = null;
            if ("-".equals(inFile)){
                pb = new ProcessBuilder(bwaExe,
                        "mem",
                        "-t",
                        "" + bwaThread,
                        "-k11",
                        "-W20",
                        "-r10",
                        "-A1",
                        "-B1",
                        "-O1",
                        "-E1",
                        "-L0",
                        "-a",
                        "-Y",
                        "-K",
                        "20000",
                        plasmidFile,
                        "-"
                ).redirectInput(ProcessBuilder.Redirect.INHERIT);
            }else{
                pb = new ProcessBuilder(bwaExe,
                        "mem",
                        "-t",
                        "" + bwaThread,
                        "-k11",
                        "-W20",
                        "-r10",
                        "-A1",
                        "-B1",
                        "-O1",
                        "-E1",
                        "-L0",
                        "-a",
                        "-Y",
                        "-K",
                        "20000",
                        plasmidFile,
                        inFile
                );
            }
            bwaProcess  = pb.redirectError(ProcessBuilder.Redirect.to(new File("/dev/null"))).start();
            reader = SamReaderFactory.makeDefault().open(SamInputResource.of(bwaProcess.getInputStream()));
        }
        return reader;
    }

    public void extractInsertSequence(String inFile, int qual, String format, int bwaThread, String output) throws IOException, InterruptedException{
        int [] refP5 = {s5,e5};
        int [] refP3 = {s3,e3};

        SamReader reader = getSamStream(inFile, format, bwaThread);
        SAMRecordIterator iter = reader.iterator();

        String currentReadName = "";

        SequenceOutputStream outFile = SequenceOutputStream.makeOutputStream(output);
        SAMRecord currentRecord= null;

        boolean extracted = false;
        boolean firstDirection = false;
        int startInsertPosition = 0,  endInsertPosition = 0;
        String sequenceStr = "";
        while (iter.hasNext()) {
            currentRecord = iter.next();

            if (currentRecord.getReadUnmappedFlag())
                continue;
            if (currentRecord.getMappingQuality() <= qual)
                continue;
            //count++;

            String currentRecordName = currentRecord.getReadName();

            if (!currentReadName.equals(currentRecordName)) {
                //start of a new record
                extracted = false;
                startInsertPosition = endInsertPosition = 0;
                currentReadName = currentRecordName;
                firstDirection = currentRecord.getReadNegativeStrandFlag();
                sequenceStr = currentRecord.getReadString();

            }else if (extracted){
                continue;//I have extracted from this read
            }else if (firstDirection != currentRecord.getReadNegativeStrandFlag()) {
                LOG.warn("Read " + currentReadName + " not match first direction " + firstDirection);
                continue;
            }

            //Neg
            //if (currentRecord.getReadNegativeStrandFlag()) {
            //Check left
            if (currentRecord.getAlignmentStart() <= s5 && currentRecord.getAlignmentEnd() >= e5) {
                int[] pos = HTSUtilities.positionsInRead(currentRecord, refP5);
                //if(pos[0] > s5*0.8 && pos[0] < s5*1.2){
                if (pos[0] > 0) {
                    startInsertPosition = pos[0];
                }
            }

            //Check right
            if (currentRecord.getAlignmentStart() <= s3 && currentRecord.getAlignmentEnd() >= e3) {
                int[] pos = HTSUtilities.positionsInRead(currentRecord, refP3);
                //if(pos[0] > s5*0.8 && pos[0] < s5*1.2){
                if (pos[1] > 0) {
                    endInsertPosition = pos[1];
                }
            }

            //Check if both startNeg and endNeg were anchored
            if (endInsertPosition > 0 && startInsertPosition > 0) {
                if (endInsertPosition < startInsertPosition) {
                    LOG.warn("Read " + currentReadName + ": find end (" + endInsertPosition + ") < start (" + startInsertPosition + ") of " + firstDirection);
                    continue;
                }

                if (endInsertPosition > sequenceStr.length()) {
                    LOG.warn("Read " + currentReadName + ": find end (" + endInsertPosition + ") > length (" + sequenceStr.length() + ")");
                    continue;
                }

                extracted = true;
                //LOG.info("Found insert of length " + (endInsertPosition - startInsertPosition));
                if (endInsertPosition - startInsertPosition > minimum && endInsertPosition - startInsertPosition < minimum + 120) {
                    String readSub = sequenceStr.substring(startInsertPosition, endInsertPosition);
                    Sequence rs = new Sequence(Alphabet.DNA16(), readSub, currentReadName);
                    //rs.setName(rs.getName() + "_start=" + startInsertPosition + ";end=" + endInsertPosition + ";direction=" + firstDirection);
                    rs.writeFasta(outFile);
                    //process(rs);
                    insertPublisher.submit(rs);
                }
            }
        }
        iter.close();
        outFile.close();
        reader.close();
        if (bwaProcess != null){
            bwaProcess.waitFor();
        }
    }

}
