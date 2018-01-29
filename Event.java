package v1;
/*
v1.Event Object are log entries
 */

public class Event {
    public int type; //should be an enum later, 0 = null, 1 = tweet, 2 = block, 3 = unblock
    public int id;
    public int blockee;
    public Tweet twt;

    public Event(){}

    public Event(int type, int id, int blockee){
        this.type = type;
        this.id = id;
        this.blockee = blockee;
        twt = null;
    }

    public Event(int type, int id, Tweet x){
        this.type = type;
        this.id = id;
        twt = x;
    }
}
