package v1;

import java.util.ArrayList;
import com.fasterxml.jackson.databind.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;


public class Site {
    public int[][] blockTable;      //[Dictionary [blocker][person being blocked]
    public ArrayList<Tweet> tweets; //Tweets list
    public int id;                  //site id
    public ArrayList<Event> log;    //site Log
    public int index;               //last entry of log
    public int majority;            //size of majority (half plus 1)
    public ArrayList<Integer> maxPrepare;
    public ArrayList<Integer> accNum;
    public ArrayList<Event> accVal;
    public ArrayList<Integer> lastProposed;         //What was the last proposal sent
    public ArrayList<Event> hold;                   //Temporary holder for events while propose/promise executes
    public ArrayList<ArrayList<accInfo>> promised;  //list of promise responses
    public ArrayList<ArrayList<accInfo>> acked;     //list of ack responses

    public Site(){}

    public Site(int numprocesses, int id){      //what happens when a new site initializes
        blockTable = new int[numprocesses][numprocesses];
        tweets = new ArrayList<Tweet>();
        this.id = id;
        log = new ArrayList<Event>();
        index = 0;
        maxPrepare = new ArrayList<Integer>();
        accNum = new ArrayList<Integer>();
        accVal = new ArrayList<Event>();
        lastProposed = new ArrayList<Integer>();
        hold = new ArrayList<Event>();
        promised = new ArrayList<ArrayList<accInfo>>();
        acked = new ArrayList<ArrayList<accInfo>>();
        majority = 2;
        initialize();
    }

    public void initialize(){
        for (int i = 0; i<100; i++){
            ArrayList<accInfo> acc = new ArrayList<accInfo>();
            ArrayList<accInfo> acc2 = new ArrayList<accInfo>();
            Event e = new Event();
            Event f = new Event();
            Event g = new Event();
            this.promised.add(acc);
            this.acked.add(acc2);
            this.log.add(e);
            this.hold.add(f);
            this.maxPrepare.add(0);
            this.accNum.add(-1);
            this.lastProposed.add(0);
            this.accVal.add(g);
        }
    }

    /*
    Proposer Functions
     */
    public Message propose(Event temp){ //accNum is the proposal number (n)
        //add special case if leader
        System.out.println("Sending out propose!");
        hold.set(index, temp);
        int accNum = lastProposed.get(index)+1;
        lastProposed.set(index, accNum);
        int newNum = 5*accNum + this.id;
        Message msg = new Message(this.id, this.index, 1, newNum, null);
        try {
            stableStorage(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }

    public Message promise(Message propose, int accNum, Event accVal){
        System.out.println("Sending out promise!");
        Message msg = new Message(propose.siteId, propose.entryIndex, 2, accNum, accVal);
        try {
            stableStorage(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }

    public Message accept(Message promise){
        int max = Integer.MIN_VALUE;
        int indexofmax = 0;
        for (int i = 0; i<majority; i++){
            if(promised.get(index).get(i).accNum > max){
                max = promised.get(index).get(i).accNum;
                indexofmax = i;
            }
        }
        if (max == -1){ //all empty
            System.out.println("Sending out accept!");
            Message msg = new Message(this.id, promise.entryIndex, 3, lastProposed.get(index)*5+this.id, this.hold.get(index));
            try {
                stableStorage(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return msg;
        }else{
            System.out.println("Sorry! Your proposal for index " + promise.entryIndex + " failed!");
            try {
                stableStorage(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public Message ack(Message accept){
        System.out.println("Sending out ack!");
        Message msg = new Message(accept.siteId, accept.entryIndex, 4, accept.accNum, accept.accVal);
        try {
            stableStorage(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }

    public Message commit(Message ack){
        System.out.println("Sending out commit!");
        Message msg = new Message(ack.siteId, ack.entryIndex, 5, ack.accNum, ack.accVal);
        try {
            stableStorage(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }


    /*
    Accepter Functions
     */
    public Message recvPropose(Message msg) {
        System.out.println("Received Propose Message!");
        //respond with promise if necessary
        int index = msg.entryIndex;
        if (msg.accNum > this.maxPrepare.get(index)) {
            this.maxPrepare.set(msg.entryIndex, msg.accNum);
            return promise(msg, this.accNum.get(index), this.accVal.get(index));
        }
        return null;
    }

    public Message recvPromise(Message msg) {
        System.out.println("Received Promise Message!");
        int index = msg.entryIndex;
        this.promised.get(index).add(new accInfo(msg.accNum, msg.accVal));
        Message m2 = null;
        if (this.promised.get(index).size() == majority){ //a majority
            System.out.println("I've received a majority of promise...processing!");
            m2 = accept(msg);
        }
        return m2;
    }

    public Message recvAccept(Message msg) {
        System.out.println("Received Accept Message!");
        int index = msg.entryIndex;
        Message m2 = null;
        if(msg.accNum >= this.maxPrepare.get(index)){
            this.maxPrepare.set(index, msg.accNum);
            this.accNum.set(index, msg.accNum);
            this.accVal.set(index, msg.accVal);
            m2 = ack(msg);
        }
        return m2;
    }

    public Message recvAck(Message msg) {
        System.out.println("Received Ack Message!");
        int index = msg.entryIndex;
        this.acked.get(index).add(new accInfo(msg.accNum, msg.accVal));
        Message m2 = null;
        if(this.acked.get(index).size() == majority){
            System.out.println("I've received a majority of acks...processing!");
            m2 = commit(msg);
        }
        return m2;
    }

    public void recvCommit(Message msg) {
        this.index++;
        System.out.println("Received Commit Message!");
        log.set(msg.entryIndex, msg.accVal);

        if(msg.accVal.type == 1)
        {
            tweets.add(msg.accVal.twt);
        }
        else if(msg.accVal.type == 2)
        {
            blockTable[msg.accVal.id][msg.accVal.blockee] = 1;
        }
        else if(msg.accVal.type == 3)
        {
            blockTable[msg.accVal.id][msg.accVal.blockee] = 0;
        }
        try {
            stableStorage(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Tweet> viewLog(){
        ArrayList<Tweet> twtList = new ArrayList<>();
        for (Tweet e : this.tweets){
            if (blockTable[e.id][this.id] == 0 && !e.equals(""))
                twtList.add(e);
        }
        Collections.sort(twtList, new sortByUTC());
        return twtList;
    }

    /*
    Recovery Functions
     */
    public static void stableStorage(Site s) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        File f = new File("site.json");
        mapper.writeValue(f, s);
    }

    public void rebuild()
    {
        for(Event event : log)
        {
            if(event.type == 1)
            {
                tweets.add(event.twt);
            }
            else if(event.type == 2)
            {
                blockTable[event.id][event.blockee] = 1;
            }
            else if(event.type == 3)
            {
                blockTable[event.id][event.blockee] = 0;
            }
            else
            {
                System.out.println("Invalid event type!");
            }
        }
    }

    /*
    chronological timeline
     */
    class sortByUTC implements Comparator<Tweet>
    {
        public int compare(Tweet a, Tweet b)
        {
            return (int) (a.time - b.time);
        }
    }

}
