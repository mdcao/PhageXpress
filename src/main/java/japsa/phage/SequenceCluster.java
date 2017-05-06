package japsa.phage;

import japsa.bio.np.ErrorCorrection;
import japsa.seq.Sequence;
import japsa.seq.SequenceOutputStream;
import japsa.seq.SequenceReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * Created by minhduc on 25/04/17.
 */
public class SequenceCluster extends HashMap<String, SequenceCluster.ReadGroup> {
    private static final Logger LOG = LoggerFactory.getLogger(SequenceCluster.class);


    static int COUNT = 0;
    static double ratio = 0.9;
    static String prefix = "_" + new Date().getTime();

    public SequenceCluster(){
        super();
    }

    //Cluster a brand new set
    public static SequenceCluster cluster(SequenceCluster oldCluster, Map<String, Sequence> seqs){
        LOG.info("Starting clustering with " + (oldCluster ==null?0:oldCluster.size()) + " clusters and " + seqs.size() + " sequences");
        SequenceCluster clusters = new SequenceCluster();
        try {
            //If the existing cluster has some sequences, concatenate that to the list of seqs
            if (oldCluster  != null) {
                for (ReadGroup group : oldCluster.values()) {
                    Sequence seq = group.representative();
                    seq.setName("GGG_" + group.ID + "#" + seq.getName());
                    seqs.put(seq.getName(), seq);
                }
            }

            //implementation using cdhit
            SequenceOutputStream sos = SequenceOutputStream.makeOutputStream(prefix + "_cdhit.fas");

            for (Sequence seq:seqs.values()){
                seq.writeFasta(sos);
            }
            sos.close();
            ProcessBuilder pb = new ProcessBuilder("cd-hit-est",
                    "-i", prefix + "_cdhit.fas",
                    "-o", prefix + "_cdhit.cluster",
                    "-c", ""+ratio,
                    "-n", "6",
                    "-T", "2",
                    "-g", "1",
                    "-aL", "0.9",
                    "-aS", "0.9",
                    "-r","0",
                    "-d", "0"
            );

            Process process  = pb.redirectError(ProcessBuilder.Redirect.to(new File("/dev/null"))).start();
            process.waitFor();

            BufferedReader reader = SequenceReader.openFile(prefix + "_cdhit.cluster");
            String line = "";

            ReadGroup readGroup = null;

            ArrayList<Sequence> seqList = new ArrayList<Sequence>();

            String groupID = "";
            int groupCount = 0;
            while ( (line = reader.readLine())!= null){
                if (line.startsWith(">")){
                    //need to save the existing group
                    if (readGroup != null){
                        if (groupID.length() == 0) {
                            COUNT ++;
                            groupID = COUNT + "_";
                        }

                        readGroup.ID = groupID;

                        Sequence repSequence;
                        if (seqList.size() == 1)
                                repSequence = seqList.get(0);
                        else{
                            LOG.info("Run concenssus on " + seqList.size() + " sequences");
                            repSequence = ErrorCorrection.consensusSequence(seqList,prefix,"poa");
                        }
                        seqList.clear();

                        repSequence.setName(groupID);
                        repSequence.setDesc(readGroup.count + " reads");
                        readGroup.repSequence = repSequence;

                        clusters.put(groupID, readGroup);
                    }
                    //start a new one
                    readGroup = new ReadGroup();
                    groupID = "";
                    continue;
                }

                String [] toks = line.split("\\s");

                String name = toks[2].substring(1,toks[2].length() - 3);
                String actualName = name;

                try {
                    if (name.startsWith("GGG_")) {
                        actualName = name.substring(name.indexOf("#") + 1);
                        String oldGroup = name.substring(4, name.indexOf("#"));
                        groupID += oldGroup;
                        //System.out.println("======= " + name + "  " + actualName + " " + groupID + " " + name.substring(4, name.indexOf("#")));

                        readGroup.count = readGroup.count + oldCluster.get(oldGroup).count;

                    } else
                        readGroup.count = readGroup.count + 1;
                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println(groupID);
                    System.exit(0);
                }

                //if (toks[3].equals("*")) {
                //   readGroup.repSequence = seqs.get(name);
                //  readGroup.repSequence.setName(actualName);
                //}

                seqList.add(seqs.get(name));
            }//while

            if (readGroup != null){
                if (groupID.length() == 0) {
                    COUNT ++;
                    groupID = COUNT + "_";
                }
                readGroup.ID = groupID;

                Sequence repSequence = (seqList.size() == 1)?
                        seqList.get(0)
                        :
                        ErrorCorrection.consensusSequence(seqList,prefix,"poa");
                seqList.clear();

                repSequence.setName(groupID);
                readGroup.repSequence = repSequence;


                clusters.put(groupID, readGroup);
            }
            /////
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }        catch (InterruptedException e) {
            e.printStackTrace();
        }

        LOG.info("Ending clustering with " + (clusters.size()) + " clusters ");
        return clusters;
    }

    public static class ReadGroup {
        String ID = "";
        Sequence repSequence = null;
        int count = 0;

        public ReadGroup() {
        }

        public ReadGroup(Sequence seq) {
            super();
            repSequence = seq;
            count = 1;
        }

        public void setID(String id) {
            this.ID = id;
        }

        public String getID() {
            return ID;
        }

        public Sequence representative() {
            return repSequence;
        }
    }

}
