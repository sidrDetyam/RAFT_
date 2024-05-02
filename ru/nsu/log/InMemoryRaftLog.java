package ru.nsu.log;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

public class InMemoryRaftLog implements RaftLog {
    private List<Entry> entries = new ArrayList<>();

    @Override
    public void insert(@NonNull List<Entry> entries, int index, int prevTerm) {
        if (!isConsistent(index, prevTerm)) {
            return;
        }

        List<Entry> tmpEntries = new ArrayList<>();
        for (int i = 0; i <= index; ++i) {
            Entry entry = this.entries.get(i).copy();
            tmpEntries.add(entry);
        }

        tmpEntries.addAll(entries);
        this.entries = tmpEntries;
    }

    @Override
    public void addEntry(Entry entry){
        entries.add(entry);
    }

    @Override
    public boolean isConsistent(int index, int prevTerm) {
        return index == -1 && prevTerm == -1 ||
                index < entries.size() && entries.get(index).getTerm() == prevTerm;
    }

    @Override
    public int getLastIndex() {
        return entries.size() - 1;
    }

    @Override
    public int getLastTerm() {
        if (entries.isEmpty()) {
            return -1;
        }

        return entries.get(entries.size() - 1).getTerm();
    }

    @Override
    public int getPrevTerm(int index) {
        if(index == 0) {
            return -1;
        }
        return entries.get(index-1).getTerm();
    }

    @Override
    public Entry getEntry(int index) {
        return entries.get(index).copy();
    }

    @Override
    public List<Entry> getEntries() {
        return entries.stream().map(Entry::copy).toList();
    }
}
