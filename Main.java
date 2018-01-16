import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	private static final String ASTRO_LOCAL_IP = "192.168.2.5";

	private static Socket sock;
	private static DataInputStream inputStream;
	private static OutputStream outputStream;
	private static byte[] eventBuffr = new byte[72];
	private static int eventBufLen = 0;


	private static int srvMajor;
	private static int srvMinor;
	private static int clientMajor;
	private static int clientMinor;

	private static int framebufferHeight;
	private static int framebufferWidth;
	private static int bitsPerPixel;
	private static int depth;

	private static long delay = 300L;

	public static void main(String[] args)
	{


		HashMap<String,String> keys = new HashMap<String,String>();
		keys.put("1","0xE301");
		keys.put("2","0xE302");
		keys.put("3","0xE303");
		keys.put("4","0xE304");
		keys.put("5","0xE305");
		keys.put("6","0xE306");
		keys.put("7","0xE307");
		keys.put("8","0xE308");
		keys.put("9","0xE309");
		keys.put("0","0xE300");
		keys.put("STANDBY","0xE000");
		keys.put("BACK","0xE002");
		keys.put("CH_+","0xE006");
		keys.put("CH_-","0xE007");
		keys.put("RED","0xE200");
		keys.put("GREEN","0xE201");
		keys.put("YELLOW","0xE202");
		keys.put("BLUE","0xE203");
		keys.put("GUIDE","0xE00B");
		keys.put("INFO","0xE00E");
		keys.put("CHG_LNG","0xEF02");

		try
		{
		//String channel = "812";
		//for(int i=0; i<channel.length(); i++)
		//{
		//	String command = keys.get(channel.substring(i, i+1));
		//	System.out.println(i + ": " + command);
		//}

		//for(int i=0; i<args.length; i++)
		//{
		//	System.out.println(i + ": " + args[i]);
		//}
		//if(true){return;}

		boolean isStbActive = isStbActive();
		if(args[0].equals("state"))
		{
			System.out.print(isStbActive?"on":"off");
			return;
		}
		
		
		sock = new Socket(ASTRO_LOCAL_IP, 5900);
		inputStream = new DataInputStream(new BufferedInputStream(sock.getInputStream(), 4096));
		outputStream = sock.getOutputStream();

		readVersionMsg();
		writeVersionMsg();
		negotiateSecurity();
		authenticateNone();
		writeClientInit();
		//Thread.sleep(1000L);
		//readServerInit();


		switch(args[0])
		{
			case "on":
			if(!isStbActive)
			{
				sendKeys("0xE000");
				Thread.sleep(delay);
				sendKeys("0xE002");
			}
			break;

			case "off":
			if(isStbActive)
			{
				sendKeys("0xE000");
			}
			break;

			case "channel":
			String channel = args[1];

			switch(channel)
			{

			case "previous":
			sendKeys(keys.get("CH_-"));
			break;

			case "next":
			sendKeys(keys.get("CH_+"));
			break;


			default:
			for(int i=0; i<channel.length(); i++)
			{
				String command = keys.get(channel.substring(i, i+1));
				sendKeys(command);
				Thread.sleep(delay);
				//System.out.println(i + ": " + command);
			}
			}


			break;

			default:
			System.out.println("Unknown Command: " + args[0]);
		}
		//sendKeys("0xE308");
		//Thread.sleep(500L);
		//sendKeys("0xE303");
		//Thread.sleep(500L);
		//sendKeys("0xE301");
		//Thread.sleep(500L);

		System.out.println("Finish");

	  }
	  catch(Exception e)
	  {
		e.printStackTrace();
	  }
	}

	private static boolean isStbActive() throws IOException {
		URL obj = new URL("http://" + ASTRO_LOCAL_IP + ":49155/444D5376-3247-4D65-6469-60c5ad882b3bContentDirectory");
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Soapaction", "\"urn:schemas-upnp-org:service:ContentDirectory:2#X_NDS_GetDeviceMode\"");
		con.setRequestProperty("Content-Type", "text/xml;charset=\"utf-8\"");
		String POST_PARAMS = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?><s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:X_NDS_GetDeviceMode xmlns:u=\"urn:schemas-upnp-org:service:ContentDirectory:2\"/></s:Body></s:Envelope>";
		// For POST only - START
		con.setDoOutput(true);
		OutputStream os = con.getOutputStream();
		os.write(POST_PARAMS.getBytes());
		os.flush();
		os.close();
		// For POST only - END

		int responseCode = con.getResponseCode();
		//System.out.println("POST Response Code :: " + responseCode);

		if (responseCode == HttpURLConnection.HTTP_OK) { //success
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			Pattern pattern = Pattern.compile("<X_STBDeviceMode>([A-Za-z0-9]*)</X_STBDeviceMode>");
			Matcher matcher = pattern.matcher(response.toString());
			if (matcher.find()) {
				return matcher.group(1).equals("Active");
				//return matcher.group(1).equals("ActiveStandbyMode")
			}

			//System.out.println(response.toString());
		} else {
			System.out.println("POST request not worked");
		}

		return false;
	}

	private static void readVersionMsg() throws Exception
	{
		byte[] arrayOfByte = new byte[12];
		inputStream.readFully(arrayOfByte, 0, arrayOfByte.length);
		if ((arrayOfByte[0] != 82) || (arrayOfByte[1] != 70) || (arrayOfByte[2] != 66) || (arrayOfByte[3] != 32) || (arrayOfByte[4] < 48) || (arrayOfByte[4] > 57) || (arrayOfByte[5] < 48) || (arrayOfByte[5] > 57) || (arrayOfByte[6] < 48) || (arrayOfByte[6] > 57) || (arrayOfByte[7] != 46) || (arrayOfByte[8] < 48) || (arrayOfByte[8] > 57) || (arrayOfByte[9] < 48) || (arrayOfByte[9] > 57) || (arrayOfByte[10] < 48) || (arrayOfByte[10] > 57) || (arrayOfByte[11] != 10))
		{throw new Exception("Is it a RFB server?");}

		srvMajor = ((arrayOfByte[4] - 48) * 100 + (arrayOfByte[5] - 48) * 10 + (arrayOfByte[6] - 48));
		srvMinor = ((arrayOfByte[8] - 48) * 100 + (arrayOfByte[9] - 48) * 10 + (arrayOfByte[10] - 48));
		if (srvMajor < 3){throw new Exception("Not support protocol version 3");}
	}

	private static void writeVersionMsg() throws IOException
	{
		while(true)
		{
			try
			{
				clientMajor = 3;
				if ((srvMajor > 3) || (srvMinor >= 8))
				{
					clientMinor = 8;
					outputStream.write("RFB 003.008\n".getBytes());
					System.out.println("writeVersionMsg: VER_3_8");
					return;
				}
				if (srvMinor >= 7)
				{
					clientMinor = 7;
					outputStream.write("RFB 003.007\n".getBytes());
					System.out.println("writeVersionMsg: VER_3_7");
					continue;
				}
			}
			finally{ }
			clientMinor = 3;
			outputStream.write("RFB 003.003\n".getBytes());
			System.out.println("writeVersionMsg: VER_3_3");
		}
	}

	private static int negotiateSecurity() throws Exception
	{
		System.out.println("clientMinor:" + clientMinor);
		if (clientMinor >= 7){return selectionSecurityType();}
		return readSecurityType();
	}

	private static void authenticateNone() throws Exception
	{
		if (clientMinor >= 8)
		readSecurityResult("No authentication");
	}

	private static void readSecurityResult(String paramString) throws Exception
	{
		int i = inputStream.readInt();
		switch (i)
		{
			default:
				throw new Exception(paramString + ": unknown result " + i);
			case 0:
				System.out.println(paramString + ": success");
				return;
			case 1:
		}
		if (clientMinor >= 8) {readConnectionFailedReason();}
		throw new Exception(paramString + ": failed");
	}

	private static void readConnectionFailedReason() throws Exception
	{
		byte[] arrayOfByte = new byte[inputStream.readInt()];
		inputStream.readFully(arrayOfByte, 0, arrayOfByte.length);
		throw new Exception(new String(arrayOfByte));
	}

	private static int readSecurityType() throws Exception
	{
		int i = inputStream.readInt();
		switch (i)
		{
		default:
			throw new Exception("Unknown security from RFB server / Username required: " + i);
		case 1:
		case 2:
		}
		return i;
	}

	private static int selectionSecurityType() throws Exception
	{
		int m = inputStream.readUnsignedByte();
		if (m == 0)
		{
			readConnectionFailedReason();
			return 0;
		}

		byte[] arrayOfByte = new byte[m];
		inputStream.readFully(arrayOfByte, 0, arrayOfByte.length);
		//System.out.println("m:" + m + ", length: " + arrayOfByte.length);

		int k = 0;
		int i = 0;
		int j = 0;
		while (true)
		{
			System.out.println("k:" + k + ", " + "m:" + m);
			//j = k;
			if (i < m)
			{
				if ((arrayOfByte[i] == 1) || (arrayOfByte[i] == 2))
				{
					j = arrayOfByte[i];
				}
				//System.out.println("j:" + j);
			}
			else
			{
				//System.out.println("final j:" + j);
				if (j != 0)
				{
					break;
				}

				throw new Exception("Invalid security type");
			}
			i += 1;
		}
		outputStream.write(j);
		return j;
	}

	private static void writeClientInit() throws IOException
	{
		outputStream.write(new byte[] { 1 }[0]);
	}


	private static void readServerInit() throws Exception
	{
		boolean bool2 = true;
		framebufferWidth = inputStream.readUnsignedShort();
		framebufferHeight = inputStream.readUnsignedShort();
		bitsPerPixel = inputStream.readUnsignedByte();
		depth = inputStream.readUnsignedByte();


		System.out.println(framebufferWidth + "x" + framebufferHeight + ", bpp: "+ bitsPerPixel + ", depth: " + depth);
		//if (inputStream.readUnsignedByte() != 0)
		//{
		//bool1 = true;
		//bigEndian = bool1;
		//if (inputStream.readUnsignedByte() == 0)
		//break label195;
		//}
		//int i;
		//label195: for (boolean bool1 = bool2; ; bool1 = false)
		//{
		//trueColour = bool1;
		//redMax = inputStream.readUnsignedShort();
		//greenMax = inputStream.readUnsignedShort();
		//blueMax = inputStream.readUnsignedShort();
		//redShift = inputStream.readUnsignedByte();
		//greenShift = inputStream.readUnsignedByte();
		//blueShift = inputStream.readUnsignedByte();
		//arrayOfByte = new byte[3];
		//inputStream.readFully(arrayOfByte, 0, arrayOfByte.length);
		//i = inputStream.readInt();
		//if (i <= 1500)
		//break label200;
		//throw new Exception("nameLength too long");
		//bool1 = false;
		//break;
		//}

		//lbyte[] arrayOfByte = new byte[i];
		//inputStream.readFully(arrayOfByte, 0, arrayOfByte.length);
		//desktopName = new String(arrayOfByte);
	}

	private static boolean sendKeys(String paramString)
	{
		return pressKeys(new int[] { Integer.parseInt(paramString.replace("0x", ""), 16) }, Boolean.valueOf(true));
	}

	private static boolean pressKeys(int[] paramArrayOfInt, Boolean paramBoolean)
	{
		boolean bool = false;
		if (paramArrayOfInt != null)
		{
			eventBufLen = 0;
			int j = paramArrayOfInt.length;
			int i = 0;
			while (i < j)
			{
				writeKeyEvent(paramArrayOfInt[i], true);
				i += 1;
			}

			//if (paramBoolean.booleanValue())
			//{
			//	i = paramArrayOfInt.length - 1;
			//	while (i >= 0)
			//	{
			//		writeKeyEvent(paramArrayOfInt[i], false);
			//		i -= 1;
			//	}
			//}
		}

		try
		{
			System.out.println("once pressKeys: " +  Arrays.toString(eventBuffr) + eventBufLen);
			outputStream.write(eventBuffr, 0, eventBufLen);
			bool = true;
			eventBufLen = 0;
			return bool;
		}
		catch (Exception exception)
		{
			while (true)
			{
				exception.printStackTrace();
				bool = false;
			}
		}
	}

	private static void writeKeyEvent(int paramInt, boolean paramBoolean)
	{
		byte[] arrayOfByte = eventBuffr;
		int i = eventBufLen;
		eventBufLen = (i + 1);
		arrayOfByte[i] = 4;
		arrayOfByte = eventBuffr;
		int j = eventBufLen;
		eventBufLen = (j + 1);
		if (paramBoolean);
		for (i = 1; ; i = 0)
		{
			arrayOfByte[j] = ((byte)i);
			arrayOfByte = eventBuffr;
			i = eventBufLen;
			eventBufLen = (i + 1);
			arrayOfByte[i] = 0;
			arrayOfByte = eventBuffr;
			i = eventBufLen;
			eventBufLen = (i + 1);
			arrayOfByte[i] = 0;
			arrayOfByte = eventBuffr;
			i = eventBufLen;
			eventBufLen = (i + 1);
			arrayOfByte[i] = ((byte)(paramInt >> 24 & 0xFF));
			arrayOfByte = eventBuffr;
			i = eventBufLen;
			eventBufLen = (i + 1);
			arrayOfByte[i] = ((byte)(paramInt >> 16 & 0xFF));
			arrayOfByte = eventBuffr;
			i = eventBufLen;
			eventBufLen = (i + 1);
			arrayOfByte[i] = ((byte)(paramInt >> 8 & 0xFF));
			arrayOfByte = eventBuffr;
			i = eventBufLen;
			eventBufLen = (i + 1);
			arrayOfByte[i] = ((byte)(paramInt & 0xFF));
			return;
		}
	}

}
