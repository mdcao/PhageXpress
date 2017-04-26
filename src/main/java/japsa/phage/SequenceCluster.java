package japsa.phage;

import japsa.seq.Sequence;
import japsa.seq.SequenceOutputStream;
import japsa.seq.SequenceReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * Created by minhduc on 25/04/17.
 */
public class SequenceCluster extends HashMap<String, SequenceCluster.ReadGroup> {
    static int COUNT = 0;
    static double ratio = 0.82;
    static String prefix = "_" + new Date().getTime();

    public SequenceCluster(){
        super();
    }

    //Cluster a brand new set
    public static SequenceCluster cluster(SequenceCluster oldCluster, Map<String, Sequence> seqs){
        SequenceCluster clusters = new SequenceCluster();
        try {
            if (oldCluster  != null) {
                for (ReadGroup group : oldCluster.values()) {
                    Sequence seq = group.representative();
                    seq.setName("GGG_" + group.ID + "#" + seq.getName());
                    seqs.put(seq.getName(), seq);
                }
            }

            //implementation using cdhit

            SequenceOutputStream sos = SequenceOutputStream.makeOutputStream(prefix + ".fas");

            for (Sequence seq:seqs.values()){
                seq.writeFasta(sos);
            }
            sos.close();
            ProcessBuilder pb = new ProcessBuilder("cd-hit-est",
                    "-i", prefix + ".fas",
                    "-o", prefix + "_cluster",
                    "-c", "0.8",
                    "-n", "6",
                    "-T", "2",
//                    "-g", "1",
                    "-aL", "0.9",
                    "-aS", "0.9",
                    "-r","0",
                    "-d", "0"
            );

            Process process  = pb.redirectError(ProcessBuilder.Redirect.to(new File("/dev/null"))).start();
            process.waitFor();

            BufferedReader reader = SequenceReader.openFile(prefix + "_cluster.clstr");
            String line = "";

            ReadGroup readGroup = null;

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

                if (toks[3].equals("*")) {
                    readGroup.repSequence = seqs.get(name);
                    readGroup.repSequence.setName(actualName);
                }
            }

            if (readGroup != null){
                if (groupID.length() == 0) {
                    COUNT ++;
                    groupID = COUNT + "_";
                }
                readGroup.ID = groupID;
                clusters.put(groupID, readGroup);
            }
            /////
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }        catch (InterruptedException e) {
            e.printStackTrace();
        }

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
