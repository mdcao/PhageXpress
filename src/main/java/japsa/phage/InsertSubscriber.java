package japsa.phage;

import japsa.seq.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Flow;

import static java.lang.Thread.currentThread;

/**
 * Created by minhduc on 23/04/17.
 */
public class InsertSubscriber implements Flow.Subscriber<Sequence> {
    private static final Logger LOG = LoggerFactory.getLogger(InsertSubscriber.class);
    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    }

    String name;

    private Flow.Subscription subscription;
    private int batchSize = 128;
    int actualInsert = 0;

    //temporary list
    Map<String, Sequence> currentBatch = new HashMap<String, Sequence>();
    SequenceCluster myCluster = null;

    public InsertSubscriber(String name, int batchSize ){
        super();
        LOG.trace(currentThread().getName() + "(" + name + ") Created" );
        this.name = name;
        this.batchSize = batchSize;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        LOG.trace(currentThread().getName() + "(" + name + ") subscribed");
        subscription.request(batchSize);
    }



    @Override
    public void onNext(Sequence seq) {
        //LOG.trace(currentThread().getName() + "(" + name + ") Got : " + seq.getName());
        currentBatch.put(seq.getName(), seq);
        actualInsert ++;

        subscription.request(batchSize);
        //process here
        if (currentBatch.size() >= batchSize){
            LOG.trace("Receive another batch");
            addNewBatch();
        }

    }

    private void addNewBatch(){
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

        System.out.println(new Date() + " " + actualInsert);
        for (int x = 0; x < groupList.size() && x < 40;x++){
            System.out.println(groupList.get(x).count + "\t" + groupList.get(x).getID() + "\t" +  groupList.get(x).repSequence.getName());
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
            LOG.trace("Receive final batch of " + currentBatch.size());
            addNewBatch();
        }
        LOG.trace(currentThread().getName() + "(" + name + ") Done");

    }


}
