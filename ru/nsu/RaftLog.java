package ru.nsu;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import lombok.NonNull;

public class RaftLog {
    private List<Entry> mEntries = new ArrayList<>();

    // @param entries to append (in order of 0 to append.length-1). must
    // be non-null.
    // @param index of log entry before entries to append (-1 if
    // inserting at index 0)
    // @param term of log entry before entries to append (ignored if
    // prevIndex is -1)
    // @return highest index in log after entries have been appended, if
    // the entry at prevIndex is not from prevTerm or if the log does
    // not have an entry at prevIndex, the append request will fail, and
    // the method will return -1.
    public boolean insert(@NonNull List<Entry> entries, int index, int prevTerm) {

        if (mEntries.size() < index || mEntries.isEmpty() || mEntries.get(mEntries.size()-1).getTerm() != prevTerm) {
            System.out.println(
                    "RaftLog: " +
                            "index and term mismatch, could not insert new log entries.");
            return false;
        }

        List<Entry> tmpEntries = new ArrayList<>();
        for (int i = 0; i < index; i++) {
            Entry entry = mEntries.get(i);
            tmpEntries.add(entry);
        }

        tmpEntries.addAll(entries);
        mEntries = tmpEntries;

        return true;
    }

    // @return index of last entry in log
    public int getLastIndex() {
        return (mEntries.size() - 1);
    }

    // @return term of last entry in log
    public int getLastTerm() {
        if (mEntries.isEmpty()) {
            return -1;
        }

        Entry entry = mEntries.get(mEntries.size()-1);
        if (entry != null) {
            return entry.getTerm();
        }
        return -1;
    }

    // @return entry at passed-in index, null if none
    public Entry getEntry(int index) {
        if ((index > -1) && (index < mEntries.size())) {
            Entry e = mEntries.get(index);
            return new Entry(e.getAction(), e.getTerm());
        }

        return null;
    }

    public String toString() {
        String toReturn = "{";
        for (Entry e : mEntries) {
            toReturn += " (" + e + ") ";
        }
        toReturn += "}";
        return toReturn;
    }
}
