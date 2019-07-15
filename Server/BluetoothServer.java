import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;

/**
* Class that implements an SPP Server which accepts single line of
* message from an SPP client and sends a single line of response to the client.
*/
public class BluetoothServer {

	
	//start server
    private void startServer() throws Exception{
    	
    	LocalDevice localDevice = LocalDevice.getLocalDevice();
        
        try {
            if (!localDevice.setDiscoverable(DiscoveryAgent.GIAC)) {
                System.out.println("Could not set discoverable to GIAC");
            }
            System.out.println("Set discoverable to GIAC");
        } catch (Throwable sd) {
        	System.out.println("Could not set discoverable to GIAC"+ sd.toString());
        }
  
        //Create a UUID for SPP
        UUID uuid = new UUID("1100", true);
        //Create the servicve url
        String connectionString = "btspp://localhost:" + uuid +";name=Sample SPP Server";
        
        //open server url
        StreamConnectionNotifier streamConnNotifier = (StreamConnectionNotifier)Connector.open( connectionString );
        
        //Wait for client connection
        System.out.println("\nServer Started. Waiting for clients to connect...");
        StreamConnection connection=streamConnNotifier.acceptAndOpen();
  
        RemoteDevice dev = RemoteDevice.getRemoteDevice(connection);
        System.out.println("Remote device address: "+dev.getBluetoothAddress());
        System.out.println("Remote device name: "+dev.getFriendlyName(true));
        
        //read string from spp client
        InputStream inStream=connection.openInputStream();
        BufferedReader bReader=new BufferedReader(new InputStreamReader(inStream));
        
        Robot robot = null;
        try 
        {
			robot = new Robot();
		}
        catch (AWTException e) {
			e.printStackTrace();
		}
        Point mousePointer = null;
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    	float screen_height = screenSize.height, screen_width = screenSize.width;
        int height = 1, width = 1;
        
        while(true)
        {
        	String lineRead=bReader.readLine();
        	System.out.println("recieved token: " + lineRead);
        	System.out.flush();
        	if(lineRead == null)
        		streamConnNotifier.close();
        	if(lineRead.contentEquals("$$TERMINATE$$")) break;
        	else if(lineRead.contentEquals("Scroll-Up"))
        	{
        		//System.out.println("Scroll-Up");
        		robot.mouseWheel(1);
        	}
        	else if(lineRead.contentEquals("Scroll-Down"))
        	{
//        		System.out.println("Scroll-Down");
        		robot.mouseWheel(-1);
        	}
        	else if(lineRead.contentEquals("Left-Click"))
        	{
//        		System.out.println("Left-Click");
        		
        		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        	    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        	}
        	else if(lineRead.contentEquals("Right-Click"))
        	{
//        		System.out.println("Right-Click");
        		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        	    robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        	}
        	else if(lineRead.contentEquals("Hello from Android")){
        		lineRead=bReader.readLine();
        		height = Integer.parseInt(lineRead);
        		System.out.println("Client Heigth: " + Integer.toString(height));
        		lineRead=bReader.readLine();
        		width = Integer.parseInt(lineRead);
        		System.out.println("Client Width: " + Integer.toString(width));
        	}
        	else
        	{
        		String[] cordinates = lineRead.split("&");
        		
        		float X = Integer.parseInt(cordinates[0])*screen_width/(width);   
        		float Y = Integer.parseInt(cordinates[1])*screen_height/(height); 
        		
//        		System.out.println("X = " + X + "\nY = " + Y);
        		//Single tap for left click
//        		if(X == 0 && Y == 0)
//        		{
//        			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//            	    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
//            	    continue;
//        		}
        		try
        		{
        			mousePointer = MouseInfo.getPointerInfo().getLocation();
					robot.mouseMove(mousePointer.x - (int)X, mousePointer.y - (int)Y);
        		}
        		catch(Exception e)
        		{
        			e.printStackTrace();
        		}
        	}
        }
        //send response to spp client
        //OutputStream outStream=connection.openOutputStream();
        //PrintWriter pWriter=new PrintWriter(new OutputStreamWriter(outStream));
        //pWriter.write("Response String from SPP Server\r\n");
        //pWriter.flush();
  
        //pWriter.close();
        
        streamConnNotifier.close();
  
    }
    
  
    public static void main(String[] args) throws IOException {
        
        //display local device address and name
        LocalDevice localDevice = LocalDevice.getLocalDevice();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    	int screen_height = screenSize.height, screen_width = screenSize.width;
        
        BluetoothServer sampleSPPServer=new BluetoothServer();
        boolean x = false;
        while(true)
        {
        	System.out.println("Address: "+localDevice.getBluetoothAddress());
            System.out.println("Name: "+localDevice.getFriendlyName());
            System.out.println("Screen Height: " + screen_height);
            System.out.println("Screen Width: " + screen_width);
            
        	try {
				sampleSPPServer.startServer();
			} 
        	catch (Exception e) {
				System.out.println("Error occured, Server Restarting...");
			}
        	if(x) break;
        }
        //System.out.println("Session Ended\n");
        
    }
}