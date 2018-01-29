package v1;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.HashMap;

/*
TwitterClientSender is used to send messages (accept, promise, ack) to other processes
 */

public class TwitterClientSender
{
    PrintWriter outt;
    private int id;
    private HashMap<Integer,String> ips;
    public Site site;
    private int count;
    private Message msg;

    public TwitterClientSender(int id, HashMap<Integer,String> ips, Site s, Message msg)
    {
        this.id = id;
        this.ips = ips;
        this.site = s;
        this.msg = msg;
    }

    public void run() throws IOException 
    {
        //handles user interface and the branching to sections of the algo

        String serverAddress = ips.get(id);
        int socketNumber = 9100 + id;
        Socket socket = new Socket("localhost", socketNumber);
        outt = new PrintWriter(socket.getOutputStream(), true);
        socket = sender(socket, outt);   //send message processed here
        closeSocket(socket, outt);
        
    }

    //send the message to the ips in the config file
    public Socket sender(Socket socket, PrintWriter out)
    {
        //open new socket based on machine numbers
        //must do comparision of ips to blocked data structure 
        //loop through machines to send messages
        //return to 'home' machine
        //use port 910# based on the count of disposable clients
        closeSocket(socket, out);

        //message creation
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = "";
        try {
            jsonString = mapper.writeValueAsString(msg);
        }catch (MismatchedInputException e){

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        jsonString.replace("\n", "");

        //message sending
        //send to specific site (aka a promise or ack message)
        if(msg.msgType == 2 || msg.msgType == 4)
        {
            String serverAddress = ips.get(msg.siteId);
            int socketNumber = 9100 + msg.siteId;
            try
            {
                socket = new Socket(InetAddress.getByName(serverAddress), socketNumber);
                out = new PrintWriter(socket.getOutputStream(), true);
                out.append(jsonString);
                closeSocket(socket, out);
            }
            catch (IOException e) 
            {
                System.out.println(e);
            }
        }
        else //send to all sites (aka accept or commit message)
        {
            for(int key : ips.keySet())
            {
                
                String serverAddress = ips.get(key);
                int socketNumber = 9100 + key;
                try
                {
                    socket = new Socket(InetAddress.getByName(serverAddress), socketNumber);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    out.append(jsonString);
                    //System.out.println("Connected to process " + key + " at " + serverAddress);
                    closeSocket(socket, out);
                }
                catch (IOException e) 
                {
                    System.out.println(e);
                }              
            }
        }

        String serverAddress = ips.get(id);
        int socketNumber = 9100 + id;
        Socket newSocket = new Socket();
        try
        {
            newSocket = new Socket("localhost", socketNumber);
        }
        catch (IOException e) 
        {
            System.out.println(e);
        }
        return newSocket;
    }

    public void closeSocket(Socket socket, PrintWriter out)
    {
        //closes all streams out and socket
        try
        {
            out.close();
            socket.close();
        }
        catch (IOException e) 
        {
            System.out.println(e);
        }        
    } 
}