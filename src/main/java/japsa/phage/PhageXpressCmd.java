package japsa.phage;

import japsa.seq.Sequence;
import japsa.util.CommandLine;
import japsa.util.deploy.Deployable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SubmissionPublisher;

/**
 * Created by minhduc on 22/04/17.
 */
public class PhageXpressCmd extends CommandLine {
    private static final Logger LOG = LoggerFactory.getLogger(PhageXpressCmd.class);
    public PhageXpressCmd(){


        super();
        Deployable annotation = getClass().getAnnotation(Deployable.class);
        setUsage(annotation.scriptName() + " [options]");
        setDesc(annotation.scriptDesc());

        addString("input", "-", "Name of the input file, - for standard input", true);
        addString("format", "fasta", "Format of the input: SAM/BAM or FASTA/FASTQ");
        addString("plasmid", null, "Name of a sample plasmid file in FASTA format", true);
        addString("output", "out.fasta", "Name of the output file, - for standard input");

        addString("bwaExe", "bwa", "Path to BWA mem.");

        addBoolean("pure", false, "Use this option to get rid of flanking regions on both ends.");
        addStdHelp();
    }

    public static void main(String[] args) throws IOException {

        /*********************** Setting up script ****************************/
        CommandLine cmdLine = new PhageXpressCmd();
        args = cmdLine.stdParseLine(args);
        /**********************************************************************/

        String 	input = cmdLine.getStringVal("input"),
                format = cmdLine.getStringVal("format"),
                plasmid = cmdLine.getStringVal("plasmid"),
                bwaExe = cmdLine.getStringVal("bwaExe"),
                output = cmdLine.getStringVal("output");
        boolean pure = cmdLine.getBooleanVal("pure");
        try {
            //VectorSequenceExtraction vectorSequenceExtraction = new VectorSequenceExtraction(plasmid, "bwa",pure, 1658, 2735);
            VectorSequenceExtraction vectorSequenceExtraction = new VectorSequenceExtraction(plasmid, "bwa",pure, 1711, 2624);
            vectorSequenceExtraction.extractInsertSequence(input,0, format, 4,output);
            //Allocate: 2 CPUs for bwa, 2CPUS for bwa

            ExecutorService executor = Executors.newFixedThreadPool(8);
            //Create Publisher to submit insertSequence
            SubmissionPublisher<Sequence> insertPublisher = new SubmissionPublisher<>(executor,256 );




            executor.shutdown();
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

}