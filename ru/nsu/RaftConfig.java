package ru.nsu;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RaftConfig {

    private int mCurrentTerm = 0;
    private int mVotedFor = 0;
    private final int mNumServers;
    private final int mTimeoutOverride;

    // @param new term. if new term is larger than current term it will
    // be synchronously written term to the config log. otherwise the
    // current term will remain the same.
    // @param server voted for in the current term (0 if none).
    public void setCurrentTerm(int term, int votedFor) {
        if (term > mCurrentTerm) {
            mCurrentTerm = term;
            mVotedFor = votedFor;
        }
    }

    // @return the current term
    public int getCurrentTerm() {
        return mCurrentTerm;
    }

    // @return the number of server
    public int getNumServers() {
        return mNumServers;
    }

    // @return the election timeout override (-1 if use default values)
    public int getTimeoutOverride() {
        return mTimeoutOverride;
    }

    public String toString() {
        return new String(
                "CURRENT_TERM =" +
                mCurrentTerm +
                ", VOTED_FOR" +
                "=" +
                mVotedFor);
    }
}

