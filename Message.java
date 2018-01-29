package v1;

/*
v1.Message object is passed between processes
*/

public class Message {
    public int siteId;
    public int entryIndex;
    public int msgType;    // 0 = undefined, 1 = propose, 2 = promise, 3 = accept, 4 = ack, 5 = commit
    public int accNum;
    public Event accVal;

    public Message(){}

    public Message(int siteId, int entryIndex, int msgType, int accNum, Event accVal){
        this.siteId = siteId;
        this.entryIndex = entryIndex;
        this.msgType = msgType;
        this.accNum = accNum;
        this.accVal = accVal;
    }
}
