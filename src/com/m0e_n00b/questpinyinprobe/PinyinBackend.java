package com.m0e_n00b.questpinyinprobe;
import java.util.List;
/** Called only on the serial worker, including initialization and close. */
public interface PinyinBackend {
    List<PinyinCandidate> evaluate(String raw);
    default void close() { }
}
