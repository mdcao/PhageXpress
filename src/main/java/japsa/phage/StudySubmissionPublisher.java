package japsa.phage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

import static java.lang.Thread.currentThread;

/**
 * Created by minhduc on 21/04/17.
 */
public class StudySubmissionPublisher {
    public static void  main(String [] args){
        ExecutorService executor = Executors.newFixedThreadPool(8);
        //Create Publisher
        SubmissionPublisher<String> publisher = new SubmissionPublisher<>(executor,16 );

        //Register Subscriber

        //publisher.subscribe(subscriberA);


        TransformProcessor<String, String> times2 =
                new TransformProcessor<String, String>(executor,s -> ""+(Integer.parseInt(s) * 2), "times2");

        TransformProcessor<String, String> plus2 =
                new TransformProcessor<String, String>(executor,s -> ""+(Integer.parseInt(s) + 2), "plus2");

        TransformProcessor<String, String> div2 =
                new TransformProcessor<String, String>(executor,s -> ""+(Integer.parseInt(s) / 2), "div2");

        StudySubscriber<String> subscriberA = new StudySubscriber<>("sA");

        publisher.subscribe(times2);
        times2.subscribe(plus2);
        plus2.subscribe(div2);
        div2.subscribe(subscriberA);


        //Publish items
        System.out.println("Publishing Items...");
        String[] items = {"1", "3", "5", "7", "9", "11","13"};

        List<Integer> s = new ArrayList<Integer>();

        for (int i = 0; i < 20; i++)
            s.add(i *2 + 1);

        s.stream().map(a -> ""+ a).forEach(i ->
        {
            System.out.println(currentThread().getName() + ": Submitting  " +  i);
            publisher.submit(i);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        publisher.close();
        executor.shutdown();

    }


    static class StudySubscriber<T> implements Flow.Subscriber<T> {
        String name;
        public StudySubscriber(String name){
            super();
            this.name = name;
            System.out.println(currentThread().getName() + "(" + name + ") Created" );
        }
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            System.out.println(currentThread().getName() + "(" + name + ") subcribed");
            subscription.request(1); //a value of  Long.MAX_VALUE may be considered as effectively unbounded
        }

        @Override
        public void onNext(T item) {
            System.out.println(currentThread().getName() + "(" + name + ") Got : " + item);
            //try {
            ///   Thread.sleep(500);
            //} catch (InterruptedException e) {
            //   e.printStackTrace();
            //}
            subscription.request(1); //a value of  Long.MAX_VALUE may be considered as effectively unbounded
        }

        @Override
        public void onError(Throwable t) {
            t.printStackTrace();
        }

        @Override
        public void onComplete() {
            System.out.println(currentThread().getName() + "(" + name + ") Done");
        }
    }

    static class TransformProcessor<S,T> extends SubmissionPublisher<T>
            implements Flow.Processor<S,T> {
        String name;
        final Function<? super S, ? extends T> function;
        Flow.Subscription subscription;

        TransformProcessor(Executor executor, Function<? super S, ? extends T> function, String name) {
            super(executor, 16);
            this.function = function;
            this.name = name;
        }
        public void onSubscribe(Flow.Subscription subscription) {
            System.out.println(currentThread().getName() + "(" + name + ") subcribed");
            (this.subscription = subscription).request(1);
        }
        public void onNext(S item) {
            subscription.request(1);
            T value = function.apply(item);
            System.out.println(currentThread().getName() + "(" + name + ") received " + item + " and transformed to " + value);
            submit(function.apply(item));
        }
        public void onError(Throwable ex) { closeExceptionally(ex); }
        public void onComplete() { close(); }
    }
}
