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
TwitterClientGUI is used to take user input and send to other processes 
 */

public class TwitterClientGUI
{
    PrintWriter outt;
    private int id;
    private HashMap<Integer,String> ips;
    public Site site;

    public TwitterClientGUI(int id, HashMap<Integer,String> ips, Site s)
    {
        this.id = id;
        this.ips = ips;
        this.site = s;
    }

    public void run() throws IOException 
    {
        //handles user interface and the branching to sections of the algo

        Scanner scanner = new Scanner(System.in);

        System.out.print("Welcome to Twitter!\n");
        System.out.print("Enter One of the follow options: \n");
        System.out.print("1 tweet \n");
        System.out.print("2 block user\n");
        System.out.print("3 unblock user\n");
        System.out.print("4 view timeline\n");
        System.out.print("5 view log\n");
        System.out.print("Crtl C to Exit \n");
        int entry = scanner.nextInt();

        String serverAddress = ips.get(id);
        int socketNumber = 9100 + id;
        Socket socket = new Socket("localhost", socketNumber);
        outt = new PrintWriter(socket.getOutputStream(), true);
        while(true) 
        {
            if (entry == 1) //tweet
            {
                System.out.print("My Tweet: \n");
                scanner.nextLine();
                String tweet = scanner.nextLine();
                Event event = propose(1, tweet, -1);
                socket = sender(socket, event, outt);   //send message processed here
                outt = new PrintWriter(socket.getOutputStream(), true);
            } 
            
            else if (entry == 2) //block
            {
                System.out.print("Block: \n");
                int block = scanner.nextInt();
                Event event = propose(2, null, block);
                socket = sender(socket, event, outt);   //send message processed here
            }
            else if (entry == 3) //unblock
            {
                System.out.print("Unblock: \n");
                int unblock = scanner.nextInt();
                Event event = propose(3, null, unblock);
                socket = sender(socket, event, outt);   //send message processed here
            }
            else if (entry == 4) //view
            {
                ArrayList<Tweet> twt = site.viewLog();
                for (Tweet t : twt){
                    System.out.println(t.msg + " at time:" + t.time);
                }
            }
            else if (entry == 5)
            {
                for(int i =0; i<site.log.size(); i++)
                {
                    Event event = site.log.get(i);
                    if(event.type == 1)
                    {
                        System.out.println("Index: " + i + " Site ID: " + event.id + " Tweet: " + event.twt.msg);
                    }
                    else if(event.type == 2)
                    {
                        System.out.println("Index: " + i + " Site ID: " + event.id + " Blocked: " + event.blockee);
                    }
                    else if(event.type == 3)
                    {
                        System.out.println("Index: " + i + " Site ID: " + event.id + " Unblocked: " + event.blockee);
                    }
                    else if (i<=site.index)
                    {
                        System.out.println("null");
                    }
                }
            }else
            {
                //if they input and invalid choice
                System.out.print("Invalid Entry! Try again! \n");
            }

            System.out.print("Enter One of the follow options: \n");
            System.out.print("1 tweet \n");
            System.out.print("2 block user\n");
            System.out.print("3 unblock user\n");
            System.out.print("4 view timeline\n");
            System.out.print("5 view log\n");
            System.out.print("Crtl C to Exit \n");
            entry = scanner.nextInt();
        }
    }

    public Event propose(int type, String tweet, int block)
    {
        //create and a propose event
        if(type == 1)
        {
            Tweet twt = new Tweet(id, tweet);
            Event event = new Event(type, id, twt);
            return event;
        }

        Event event = new Event(type, id, block);
        return event;
    }

    //send the message to the ips in the config file
    public Socket sender(Socket socket, Event event, PrintWriter out)
    {
        //open new socket based on machine numbers
        //must do comparision of ips to blocked data structure 
        //loop through machines to send messages
        //return to 'home' machine
        //use port 910# based on machine num 

        closeSocket(socket, out);

        //message creation
        Message msg = null;
        if(site.index > 0 && site.log.get(site.index-1).id == site.id){ //leader!!
            msg = new Message(site.id, site.index, 3, 0, event);
        }else{
            msg = site.propose(event);
        }
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = "";
        try {
            jsonString = mapper.writeValueAsString(msg);
        }catch (MismatchedInputException e){
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        jsonString.replace("\n", "");

        for(int key : ips.keySet())
        {
            
            String serverAddress = ips.get(key);
            int socketNumber = 9100 + key;
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