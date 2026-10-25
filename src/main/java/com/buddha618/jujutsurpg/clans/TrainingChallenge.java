package com.buddha618.jujutsurpg.clans;

/** Single-use stage tickets. Server movement/action checks must also pass before advance. */
public final class TrainingChallenge {
    private final long sequence, deadline;
    private final int count;
    private long nonce, readyAt;
    private int step;
    public TrainingChallenge(long sequence,long nonce,long now,int count) {
        this.sequence=sequence;this.nonce=nonce;this.count=count;this.readyAt=now+60;this.deadline=now+3600;
    }
    public long sequence(){return sequence;} public long nonce(){return nonce;}
    public int step(){return step;} public int count(){return count;}
    public boolean expired(long now){return now>deadline;}
    public boolean accepts(long sequence,long nonce,long now) {
        return this.sequence==sequence && this.nonce==nonce && now>=readyAt && !expired(now) && step<count;
    }
    public boolean advance(long sequence,long nonce,long now,long nextNonce) {
        if(!accepts(sequence,nonce,now))return false;
        step++;this.nonce=nextNonce;readyAt=now+60;return true;
    }
    public boolean complete(){return step>=count;}
}
