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

@Deployable(
        scriptName = "phageXpress.sh",
        scriptDesc = "Run phageXpress pipeline"
)
public class PhageXpressCmd extends CommandLine {
    static {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
        //System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    }
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

        //addString("bwaExe", "bwa", "Path to BWA mem.");
        addInt("batchSize", 512, "Batch size");
        addDouble("threshold", 0.90, "Threshold identity");
        addString("prefix", null, "prefix");
        addBoolean("pure", false, "Use this option to get rid of flanking regions on both ends.");
        addInt("thread", 8, "Number of threads to use, not including those for bwa");
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
          //      bwaExe = cmdLine.getStringVal("bwaExe"),
                output = cmdLine.getStringVal("output");
        String prefix = cmdLine.getStringVal("prefix");
        if (prefix == null)
            prefix = "tmp_" + System.currentTimeMillis();

        double thresholdOption = cmdLine.getDoubleVal("threshold");
        int 	batchSize = cmdLine.getIntVal("batchSize");
        int 	thread = cmdLine.getIntVal("thread");
        SequenceCluster.ratio = thresholdOption;
        SequenceCluster.prefix = prefix;


        //System.out.println(Logger.ROOT_LOGGER_NAME);

        //boolean pure = cmdLine.getBooleanVal("pure");
        try {
            ExecutorService executor = Executors.newFixedThreadPool(thread);
            SequenceCluster.executor = executor;
            SequenceCluster.total_thread = thread;

            //Create Publisher to submit insertSequence
            SubmissionPublisher<Sequence> insertPublisher = new SubmissionPublisher<>(executor,batchSize*2 );
            InsertSubscriber subcriber = new InsertSubscriber("subscriber",  batchSize, output);
            insertPublisher.subscribe(subcriber);


            //Create a subcriber

            //VectorSequenceExtraction vectorSequenceExtraction = new VectorSequenceExtraction(plasmid, "bwa",pure, 1658, 2735);
            //The actual insert (the variation part) is around 1820-2520, and I take around 120 bp + 100 flank
            //So around 450 bp was added
            //The minimum is 650
            VectorSequenceExtraction vectorSequenceExtraction = new VectorSequenceExtraction(plasmid, insertPublisher, 1711, 2624,100, 800);
            vectorSequenceExtraction.extractInsertSequence(input,0, format, 4, output);
            insertPublisher.close();
            //Allocate: 2 CPUs for bwa, 2CPUS for bwa

            executor.shutdown();
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

}