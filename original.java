package v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/*
TwitterServer is used to receive messages from over processes
 */

public class TwitterServer {

    private int id;
    private HashMap<Integer,String> ips;
    public Site site;
    private int count;

    public TwitterServer(int id, HashMap<Integer,String> ips, Site s)
    {
        this.id = id;
        this.ips = ips;
        this.site = s;
        this.count = 0;
    }

    public void run()
    {
        try
        {
            int socketNumber = 9100 + id;
            ServerSocket listener = new ServerSocket(socketNumber); 
           
            while (true) 
            {
                this.count++;
                
                new Handler(listener.accept(), this.id, this.ips, this.site, this.count).start();
                new EchoThread(this.id, this.count, this.site, this.ips).start();
            }        
        }
        catch (IOException e) 
        {
            System.out.println(e);
        }
    }

    public class EchoThread extends Thread 
    {
        private int id;
        private HashMap<Integer,String> ips;
        private Site site;
        private int count;

        public EchoThread(int id, int count, Site site, HashMap<Integer,String> ips) {
            this.id = id;
            this.site = site;
            this.ips = ips;
            this.count = count;
        }

        public void run() 
        {
            int socketNum = 9110 + this.id + this.count;
            ServerSocket listener = null;
            try {
                listener = new ServerSocket(socketNum);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try
            {
                try 
                {
                    while (true) 
                    {
                        new Handler(listener.accept(),this.id, this.ips, this.site, this.count).start();
                    }
                } 
                finally 
                {
                    listener.close();
                }
            }
            catch (IOException e) 
            {
                System.out.println(e);
            }
        }
    }
    public class Handler extends Thread
    {
        private BufferedReader in;
        private PrintWriter out;
        private Socket socket;
        private int id;
        private HashMap<Integer,String> ips;
        private Site site;
        private int count;
        
        public Handler(Socket socket, int id, HashMap<Integer,String> ips, Site site, int count) 
        {
            this.socket = socket;
            this.id = id; 
            this.site = site;
            this.ips = ips;
            this.count = count;
        }

        public void run() 
        {
            try 
            {
                String clientMsg;
                String json = "";
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
                while((clientMsg = in.readLine()) != null)
                {
                    writer.append(clientMsg);
                    json += clientMsg;
                }
                writer.close();
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                Message msg = mapper.readValue(json, Message.class);

                //do message processing
                Message returnMsg = null;
                if(msg.msgType == 1)
                {
                    returnMsg = site.recvPropose(msg);
                }
                else if(msg.msgType == 2)
                {
                    returnMsg = site.recvPromise(msg);
                }
                else if(msg.msgType == 3)
                {
                    returnMsg = site.recvAccept(msg);
                }
                else if(msg.msgType == 4)
                {
                    returnMsg = site.recvAck(msg);
                }
                else if(msg.msgType == 5)
                {
                    returnMsg = site.recvCommit(msg);
                }
                else
                {
                    System.out.println("Incorrect Message Type");
                }

                //send to sites
                if(returnMsg != null)
                {
                    new TwitterClientSender(this.id, this.ips, this.site, this.count, returnMsg).run();
                }

            }
            catch (IOException e) 
            {
                System.out.println(e);
            }
        }
    }
}

