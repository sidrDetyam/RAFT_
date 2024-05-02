package ru.nsu.log;

import java.util.List;

import lombok.NonNull;

public interface RaftLog {

    void insert(@NonNull List<Entry> entries, int index, int prevTerm);

    void addEntry(Entry entry);

    boolean isConsistent(int index, int prevTerm);

    int getLastIndex();

    int getLastTerm();

    int getPrevTerm(int index);

    Entry getEntry(int index);

    List<Entry> getEntries();
}
