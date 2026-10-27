package com.m0e_n00b.questpinyinprobe;
import java.util.*;
import java.util.concurrent.Executor;
/** Serial worker actor. Accepted editor results are delivered in order on the owner thread. */
public final class AsyncPinyinSession {
    public interface Action { PinyinSession.Result run(PinyinSession session); }
    public interface Listener { void accept(PinyinSession.Result result, boolean hadComposition); }
    private final PinyinSession model;
    private final Executor worker, owner;
    private final Listener listener;
    private volatile long epoch;
    private int pending;
    private String composition = "", error;
    private List<String> candidates = Collections.emptyList();
    private List<PinyinCandidate> details = Collections.emptyList();
    public AsyncPinyinSession(PinyinSession model, Executor worker, Executor owner, Listener listener) {
        this.model = model; this.worker = worker; this.owner = owner; this.listener = listener;
    }
    public String getComposition() { return composition; }
    public List<String> getCandidates() { return pending == 0 ? candidates : Collections.emptyList(); }
    public String getCandidateId(int index) { return details.get(index).id; }
    public String getError() { return error; }
    public boolean isPending() { return pending > 0; }
    public void submit(Action action) {
        final long token = epoch;
        pending++;
        worker.execute(() -> {
            if (token != epoch) return;
            boolean had = !model.getComposition().isEmpty();
            PinyinSession.Result result = action.run(model);
            owner.execute(() -> {
                if (token != epoch) return;
                pending--;
                composition = result.composition;
                candidates = result.candidates;
                details = result.candidateDetails;
                error = result.error;
                listener.accept(result, had);
            });
        });
    }
    public void whenIdle(Runnable callback) {
        final long token = epoch;
        worker.execute(() -> owner.execute(() -> {
            if (token != epoch) return;
            if (pending == 0) callback.run(); else whenIdle(callback);
        }));
    }
    public void reset() {
        final long token = ++epoch;
        pending = 0; composition = ""; candidates = Collections.emptyList(); details = Collections.emptyList(); error = null;
        worker.execute(() -> { if (token == epoch) model.reset(); });
    }
    public void close() { reset(); worker.execute(model::close); }
}
