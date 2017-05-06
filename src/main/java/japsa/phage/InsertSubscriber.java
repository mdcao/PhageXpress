package japsa.phage;

import japsa.seq.Sequence;
import japsa.seq.SequenceOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Flow;

import static java.lang.Thread.currentThread;

/**
 * Created by minhduc on 23/04/17.
 */
public class InsertSubscriber implements Flow.Subscriber<Sequence> {
    private static final Logger LOG = LoggerFactory.getLogger(InsertSubscriber.class);

    String name;

    private Flow.Subscription subscription;
    private int batchSize = 128;
    int actualInsert = 0;

    //temporary list
    Map<String, Sequence> currentBatch = new HashMap<String, Sequence>();
    SequenceCluster myCluster = null;

    SequenceOutputStream outputStream;

    public InsertSubscriber(String name, int batchSize, String output ) throws IOException {
        super();
        LOG.info(currentThread().getName() + "(" + name + ") Created" );
        this.name = name;
        this.batchSize = batchSize;
        outputStream = SequenceOutputStream.makeOutputStream("cluster_" + output);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        LOG.info(currentThread().getName() + "(" + name + ") subscribed");
        subscription.request(batchSize);
    }

    @Override
    public void onNext(Sequence seq) {
        //LOG.info(currentThread().getName() + "(" + name + ") Got : " + seq.getName());
        currentBatch.put(seq.getName(), seq);
        actualInsert ++;
        //process here
        if (currentBatch.size() >= batchSize){
            subscription.request(batchSize);
            LOG.info("Receive another batch");
            try {
                addNewBatch();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addNewBatch() throws IOException {
        myCluster = myCluster = SequenceCluster.cluster(myCluster, currentBatch);
        ArrayList<SequenceCluster.ReadGroup> groupList = new ArrayList<SequenceCluster.ReadGroup>();
        for (SequenceCluster.ReadGroup g: myCluster.values())
            groupList.add(g);

        Collections.sort(groupList, new Comparator<SequenceCluster.ReadGroup>() {
            @Override
            public int compare(SequenceCluster.ReadGroup o1, SequenceCluster.ReadGroup o2) {
                return o2.count - o1.count;
            }
        });

        //System.out.println(new Date() + " " + actualInsert);
        outputStream.print("##" + new Date() + " " + actualInsert);
        outputStream.println();
        for (int x = 0; x < groupList.size() && x < 40;x++){
            groupList.get(x).repSequence.writeFasta(outputStream);
            //System.out.println(groupList.get(x).count + "\t" + groupList.get(x).getID() + "\t" +  groupList.get(x).repSequence.getName());

        }
        //reset count
        currentBatch.clear();

    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @Override
    public void onComplete() {
        if (currentBatch.size() > 0){
            LOG.info("Receive final batch of " + currentBatch.size());
            try {
                addNewBatch();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        LOG.info(currentThread().getName() + "(" + name + ") Done");

    }


}
