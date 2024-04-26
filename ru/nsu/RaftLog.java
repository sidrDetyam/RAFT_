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

        if (isInconsistent(index, prevTerm)) {
//            System.out.printf(" --- %s %s %s%n", entries, index, prevTerm);
            return;
        }

        List<Entry> tmpEntries = new ArrayList<>();
        for (int i = 0; i < index; i++) {
            Entry entry = this.entries.get(i);
            tmpEntries.add(entry);
        }

        tmpEntries.addAll(entries);
        this.entries = tmpEntries;
    }

    public boolean isInconsistent(int index, int prevTerm) {
        return entries.size() < index || !this.entries.isEmpty() && entries.get(entries.size() - 1).getTerm() != prevTerm;
    }

    public int getLastIndex() {
        return entries.size() - 1;
    }

    public int getLastTerm() {
        if (entries.isEmpty()) {
            return -1;
        }

        Entry entry = entries.get(entries.size() - 1);
        if (entry != null) {
            return entry.getTerm();
        }

        return -1;
    }

    public Entry getEntry(int index) {
        if (index > -1 && index < entries.size()) {
            Entry e = entries.get(index);
            return new Entry(e.getAction(), e.getTerm());
        }

        return null;
    }
}
