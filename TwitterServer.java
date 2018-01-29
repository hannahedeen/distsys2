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
    }
    public void run()
    {
        try
        {
            int socketNumber = 9100 + id;
            ServerSocket listener = new ServerSocket(socketNumber);
            try
            {
                while (true)
                {
                    new Handler(listener.accept(), this.id, this.ips, this.site).start();
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
    public class Handler extends Thread
    {
        private BufferedReader in;
        private PrintWriter out;
        private Socket socket;
        private int id;
        private HashMap<Integer,String> ips;
        private Site site;

        public Handler(Socket socket, int id, HashMap<Integer,String> ips, Site site)
        {
            this.socket = socket;
            this.id = id;
            this.site = site;
            this.ips = ips;
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
                if (json.equals("")){
                    return;
                }
                //System.out.println("I received this json file:" + json);
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                Message msg = mapper.readValue(json, Message.class);

                //do message processing
                //System.out.println("Message type:" + msg.msgType);
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
                    site.recvCommit(msg);
                }
                else
                {
                    System.out.println("Incorrect Message Type");
                }

                //send to sites
                if(returnMsg != null)
                {
                    new TwitterClientSender(this.id, this.ips, this.site, returnMsg).run();
                }
            }

            catch (IOException e)
            {
                System.out.println(e);
            }
        }
    }
}