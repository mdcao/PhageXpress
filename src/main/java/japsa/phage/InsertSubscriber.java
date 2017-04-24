package japsa.phage;

import japsa.seq.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;

import static java.lang.Thread.currentThread;

/**
 * Created by minhduc on 23/04/17.
 */
public class InsertSubscriber implements Flow.Subscriber<Sequence> {
    private static final Logger LOG = LoggerFactory.getLogger(InsertSubscriber.class);

    String name;
    private Flow.Subscription subscription;

    public InsertSubscriber(String name){
        super();
        LOG.trace(currentThread().getName() + "(" + name + ") Created" );
        this.name = name;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        LOG.trace(currentThread().getName() + "(" + name + ") subcribed");
        subscription.request(1);
    }

    @Override
    public void onNext(Sequence seq) {
        LOG.trace(currentThread().getName() + "(" + name + ") Got : " + seq.getName());
        subscription.request(1); //a value of  Long.MAX_VALUE may be considered as effectively unbounded
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @Override
    public void onComplete() {
        LOG.trace(currentThread().getName() + "(" + name + ") Done");
    }
}
