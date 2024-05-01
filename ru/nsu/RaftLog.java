package ru.nsu;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class RaftLog {
    private List<Entry> entries;

    public RaftLog(List<Entry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    public void insert(@NonNull List<Entry> entries, int index, int prevTerm) {
//        System.out.println("::: %s".formatted(entries));

        if (!isConsistent(index, prevTerm)) {
//            System.out.printf(" --- %s %s %s%n", entries, index, prevTerm);
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

    public void addEntry(Entry entry){
        entries.add(entry);
    }

    public boolean isConsistent(int index, int prevTerm) {
        return index == -1 && prevTerm == -1 ||
                index < entries.size() && entries.get(index).getTerm() == prevTerm;
    }

    public int getLastIndex() {
        return entries.size() - 1;
    }

    public int getLastTerm() {
        if (entries.isEmpty()) {
            return -1;
        }

        return entries.get(entries.size() - 1).getTerm();
    }

    public int getPrevTerm(int index) {
        if(index == 0) {
            return -1;
        }
        return entries.get(index-1).getTerm();
    }

    public Entry getEntry(int index) {
        return entries.get(index).copy();
    }
}
