package com.m0e_n00b.questimepinyin;
import java.util.*;
import java.util.concurrent.Executor;
public final class AsyncPinyinSessionTest {
    private static int checks;
    private static void yes(boolean value) { checks++; if (!value) throw new AssertionError("Actor contract " + checks); }
    private static final class Queue implements Executor {
        final ArrayDeque<Runnable> jobs = new ArrayDeque<>();
        public void execute(Runnable job) { jobs.add(job); }
        void drain() { while (!jobs.isEmpty()) jobs.remove().run(); }
    }
    public static void main(String[] args) {
        Queue worker = new Queue(), owner = new Queue();
        List<PinyinSession.Result> delivered = new ArrayList<>();
        AsyncPinyinSession actor = new AsyncPinyinSession(new FixedPinyinSession(), worker, owner, (result, had) -> delivered.add(result));
        actor.submit(s -> s.feed("nihao"));
        actor.submit(PinyinSession::space);
        yes(actor.isPending() && actor.getCandidates().isEmpty());
        worker.drain();
        yes(delivered.isEmpty());
        owner.drain();
        yes(delivered.size() == 2 && delivered.get(1).committedText.equals("你好"));
        yes(!actor.isPending() && actor.getComposition().isEmpty());
        actor.submit(s -> s.feed("ni"));
        worker.drain();
        actor.reset(); // Old query has completed, but its callback has not reached the owner.
        actor.submit(s -> s.feed("hao"));
        owner.drain(); worker.drain(); owner.drain();
        yes(actor.getComposition().equals("hao") && actor.getCandidates().get(0).equals("好"));
        yes(delivered.size() == 3);
        actor.reset();
        actor.submit(s -> s.feed("nihao"));
        actor.reset(); // Cancel before worker execution too.
        worker.drain(); owner.drain();
        yes(actor.getComposition().isEmpty() && !actor.isPending());
        actor.submit(s -> s.feed("nihao")); worker.drain(); owner.drain();
        actor.submit(s -> s.commitCandidate(4));
        actor.submit(PinyinSession::space);
        worker.drain(); owner.drain();
        yes(delivered.get(delivered.size()-2).committedText.equals("尼"));
        yes(delivered.get(delivered.size()-2).composition.equals("hao"));
        yes(delivered.get(delivered.size()-1).committedText.equals("好"));
        final boolean[] idle = {false};
        actor.submit(s -> s.feed("n"));
        actor.whenIdle(() -> idle[0] = true);
        worker.drain(); owner.drain();
        yes(idle[0] && actor.getComposition().equals("n"));
        System.out.println("AsyncPinyinSessionTest: " + checks + " ordering/cancellation cases passed");
    }
}
