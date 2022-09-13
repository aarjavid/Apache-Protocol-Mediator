/*
Proxy Server
The first module of th project listens on a user-specified socket for DNS lookup requests and return a preferred IP address.  
The second module recieves HTTP & FTP requests from client and sends and acts as a proxy between the browser and the server. It serves the clients with the data from the serevers and close the connection.
*/
import java.net.InetAddress;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetSocketAddress; 
import java.net.UnknownHostException;
/*--------------- end imports --------------*/

class Apache {

  private byte []     HOST ;         
  private int         PORT ;      
  
  private InetAddress PREFERRED; 
  private byte[] URL;

public static void main(String [] a)
{ 
  Apache apache = new Apache(Integer.parseInt(a[0]));
  apache.run(2);
}

Apache(int port) 
{
  PORT = port;
}

int parse(byte [] buffer) throws IOException
{
  int space = 0, slash = 0, ind = 0, index1 = 0, begin = 0, end = 0;
// return -1 for bad request, -2 if its not HTTP/1.1, 0 if good
  if(!(buffer[0]==(byte)'G' && buffer[1]==(byte)'E' && buffer[2]==(byte)'T' && buffer[3]== (byte)' '))
  {
    return -1;
  }
  
	 if(buffer[4] == (byte)'f' || buffer[4] == 'F' && buffer[5] == (byte)'t' || buffer[5] == (byte)'T' && buffer[6] == (byte)'p' || buffer[6] == (byte)'P' && buffer[7] == (byte)':' && buffer[8] == (byte)'/' && buffer[9] == (byte)'/')
	 {
       //get url from everything after this till space
			for(int i = 14; i < buffer.length; i ++)
			{
				if(buffer[i] == (byte)'/')
				{
					if (ind == 0)
					{
						slash = i;
						ind = ind + 1;
					}
					else 
					{
						continue;
					}
				}
				else
				{
					if(buffer[i] == (byte)' ')
					{
						space = i ;
						break;
					}
					continue;
				}
			}
			
			URL = new byte[space - (slash)];
			for(int i = (slash); i < space; i++)
			{
				URL[i-(slash)] = buffer[i];
			}
		  int count=0;
			for(int i = space; i < buffer.length; i++)
			{
				if(buffer[i] == (byte)'\r' || buffer[i] == (byte)'\n') 
				{
					if (index1 == 0)
					{
						begin = i+1;
						index1 = index1 + 1;
            count++;
					}
					else
					{
            count++;
            if(count==3)
            {
						  end = i;
              break;
            }
					}
				}
			}
			HOST = new byte [end - (begin + 7)];
			// for loop to find HOST 
			for(int i = begin + 7; i < end; i++)
			{
				HOST[i-begin-7] = buffer[i];
			}
          return 1;
	 }
	 else 
	 {
     int count=0;
			for(int i = 4; i < buffer.length; i++)
			{
				if (buffer[i] == (byte)'/')
				{
          count++;
          if(count==3){
					    slash = i;
              count++;}
         }
					if(buffer[i] == (byte)' ')
          {
            space = i;
            break;
          }
				
			} 
      
			URL = new byte[space-slash];

			// loop find the url until the first space is found
			for(int i = slash; i < space; i++)
			{
				URL[i-slash] = buffer[i];
			}
      count=0;
			for(int i = space; i < buffer.length; i++)
			{
				if(buffer[i] == (byte)'\r' || buffer[i] == (byte)'\n') 
				{
					if (ind == 0)
					{
						begin = i+1;
						ind = ind + 1;
            count++;
					}
					else
					{
            count++;
            if(count==3)
            {
						  end = i;
              break;
            }
					}
				}
			}
			HOST = new byte [end - (begin + 7)];
			// for loop to find HOST 
			for(int i = begin + 7; i < end; i++)
			{
				HOST[i-begin-7] = buffer[i];
			}
           return 2;
	 }  
 //set HOST by going through buffer (ex. GET www.berkeley.edu ) and pulling just www....
}

// Note: dns() must set PREFERRED

/*
  we thought that the best way to find the preferred IP would be to choose 
  which one of them takes the shortest time to make the connection and close it. 
*/
int dns(int X)  
{
      long timeEnd;
    	long totalTime = 0;
    	long shortestTime = Long.MAX_VALUE;
      long timeStart;
      int port;
      if(X==1)
        port = 21;
      else
        port = 80;
      /*
        The selection logic is that the IP that we can connect to the fastest would be this system's preferred IP.
        We do this by calculating the time that elapsed before and after connecting and saving the one with the shortest time.
        The preferred IP will change dependent on where this Apache is hosted on.
      */
    		try {
    		InetAddress [] addresses = InetAddress.getAllByName(byteToString(HOST, 1, HOST.length));
    		for (int i = 0; i < addresses.length; i++) {
     
    			String ip = addresses[i].getHostAddress();
     
    			try{
                 Socket socket = new Socket();
                 timeStart = System.currentTimeMillis();
                 //port 80 might be bad port to use
    			       socket.connect(new InetSocketAddress(ip, port));
    			       timeEnd = System.currentTimeMillis();
    			       totalTime = timeEnd - timeStart;
    			       socket.close();
                
                 if (totalTime < shortestTime) {
    				      shortestTime = totalTime;
    				      PREFERRED = addresses[i];
    			       }     
                              			
    			}
    			catch (IOException e) {
   
            continue;
    			}
    		}
    	}
    	catch (UnknownHostException e) {
        return -1;
    	}
     if (shortestTime == Long.MAX_VALUE){
       return -1;
       }
       
  //we take the HOST, do math to find best IP address (study how InetAddress.GetAllByName(String) works and decide best IP)
  //return -1 if Error
  return 0; 
}

int http_fetch(Socket c)
{

  try
  {
  Socket ServerSoc = new Socket();
  String ServerIp = PREFERRED.getHostAddress();
 
  ServerSoc.connect(new InetSocketAddress (ServerIp, 80)); 
  byte[] buff = new byte[65535];               
  int totalBytes=0;
  
  OutputStream opt = ServerSoc.getOutputStream();
  InputStream inp = ServerSoc.getInputStream(); 
  
  byte[] get = {'G', 'E','T',' '};
  byte[] protocol = {' ', 'H','T','T','P','/','1','.','1','\r','\n','H','O','S','T',':',' '};
  byte[] headers = {'\r','\n','\r','\n'};
  byte[] requestHTTP = new byte[get.length+URL.length+protocol.length+HOST.length+headers.length];
  int index=0;
  for(byte a: get)
  {
    requestHTTP[index]=a;
    index++;
  }
  for(byte b: URL)
  {
    requestHTTP[index]=b;
    index++;
  }
  for(byte f: protocol)
  {
    requestHTTP[index]=f;
    index++;
  }
  for(byte d: HOST)
  {
    requestHTTP[index]=d;
    index++;
  }
  for(byte e: headers)
  {
    requestHTTP[index]=e;
    index++;
  }
 opt.write(requestHTTP);
  //receive each buffer, immediately write it over to socket c
  int bytes;
  bytes = inp.read(buff);

  if(bytes<0) 
  	{
  	inp.close();
  	opt.close();
  	ServerSoc.close();
  	return -1;
	}
  if(!(buff[9]==(byte)'2' || buff[9]==(byte)'3'))
  	{
  	inp.close();
  	opt.close();
  	ServerSoc.close();
  	return -1;
	}
  OutputStream optC = c.getOutputStream();
  optC.write(buff, 0, bytes);
  totalBytes+=bytes;
  //System.out.println(byteToString(buff,1,buff.length));
  do
  {
    bytes = inp.read(buff);
    if(bytes>0)
    {
      totalBytes+=bytes;
      optC.write(buff,0,bytes);
      //System.out.println(byteToString(buff,1,buff.length));
      emptyBuf(buff);
      //now write buff to s1
    }
  }while(bytes>0);
  optC.close();
  inp.close();
  opt.close();
  ServerSoc.close();
  requestHTTP = null;
  buff = null;
  return totalBytes;
  } 
  catch (IOException e) {
  //System.out.println("####################Exception in http :" + e.getMessage());
  return -1;
  }
}


String getServerDataIP(byte[] response) {

	int start = 0;
	int comma_cnt = 0;
	int octate_length = 0;
	String Ip = "";
	char[] temp = new char[10];
	for(int i=0;i<response.length;i++) {
		
		if(response[i] == (byte)'(') { start = 1; continue;}
		if(start == 1) {
			if(response[i] == (byte)',') {
			for(int j=0 ; j < octate_length;j++) {
				Ip += temp[j];
			}
			if(comma_cnt == 3) break;	
			Ip += '.';
			for (int k=0;k<temp.length;k++) {temp[k]='\0';}
			comma_cnt += 1;
			octate_length = 0;
			}
			else{
			temp[octate_length] = (char)response[i];
			octate_length += 1;
			}
		}
	}
	return Ip;
}

int power(int base, int pow) {
int val = 1;
for (int i =0;i<pow;i++) {val = val*base ;}
return val;

}

int getServerDataPort(byte[] response) {

	int start = 0;
	int comma_cnt = 0;
	int octate_length = 0;
	int port = 0;
	int p1 = 0;
	int p2 = 0;
	int[] temp = new int[10];
	int pos = 0;
	
	for(int i=0;i<response.length;i++) {
		
		if(response[i] == (byte)'(') { start = 1; continue;}
		if(start == 1) {
			if(response[i] == (byte)',' || response[i] == (byte)')') { 
			comma_cnt += 1;
			if(comma_cnt < 5) continue;
			pos = octate_length - 1;
			for(int j=0; j < octate_length;j++,pos--) {
			port  += temp[j]* power(10,pos);		
			}
			if(comma_cnt == 5)  p1 = port;	
			if(comma_cnt == 6)  p2 = port;	
			port = 0;
			for (int k=0;k<temp.length;k++) {temp[k]='0';}
			octate_length = 0;
			if(response[i] == (byte)')') { break; }
			continue;
			}
			if(comma_cnt>3){
			temp[octate_length] = (int)response[i] - 48 ;
			octate_length += 1;
			}
		}
	}
	port = p1*256 + p2;
	return port;
}

void  emptyBuf(byte[] buf) {
	for(int i=0;i<buf.length;i++) {buf[i] = '\0';}
}


int  ftp_fetch(Socket c) 
{

byte[] response = new byte[65535];             
int bytesRead = 0;

try{
  Socket ServerCmndSoc = new Socket(); // peer, connection to HOST
  String ServerCmndIp = PREFERRED.getHostAddress();
  ServerCmndSoc.connect(new InetSocketAddress (ServerCmndIp, 21)); //control connection
 
  
  byte[] user = {'U','S','E','R',' ','a','n','o','n','y','m','o','u','s','\r','\n'};
  byte[] pw   = {'P','A','S','S',' ','u','s','e','r','i','d','@','d','o','m','a','i','n','.','c','o','m','\r','\n'}; //change later to actual email 
  byte[] pasv = {'P','A','S','V','\r','\n'};
  byte[] quit = {'q','u','i','t','\r','\n'};
  byte[] RespOK = { 'H', 'T', 'T', 'P', '/', '1', '.', '1', ' ', '2', '0', '0', ' ', 'O', 'K','\r','\n','\r','\n'};
  
  int port;
 
  OutputStream opt = ServerCmndSoc.getOutputStream();
  InputStream inp = ServerCmndSoc.getInputStream(); 
  
  
  inp.read(response);
  if(!(response[0] == (byte)'2' &&  response[1]==(byte)'2' && response[2]==(byte)'0')) {
  	opt.write(quit);
       //System.out.println("220 response code not recieved in start in the response buffer:- " + byteToString(response,1,response.length));
 	 inp.close();
 	 opt.close();
 	 ServerCmndSoc.close();
       return -1;
       }
  emptyBuf(response);
  
  opt.write(user); 
 int first_buf = 1;
 int resp_read_bytes = 0;
 do
 {
 resp_read_bytes = inp.read(response);
 if(first_buf == 1) {
 	if(!((response[0] == (byte)'2' &&  response[1]==(byte)'3' && response[2]==(byte)'0') || (response[0] == (byte)'3' &&  response[1]==(byte)'3' && response[2]==(byte)'1'))  )
 	{
	//System.out.println("230 response code not recieved in USER in the response buffer:- " + byteToString(response,1,response.length));
  	opt.write(quit);
 	 inp.close();
 	 opt.close();
 	 ServerCmndSoc.close();
	return -1;
	}
 first_buf=0;
 }
 emptyBuf(response); 
 }while(inp.available() > 0);
 
  opt.write(pw); 
  inp.read(response);
 if(!(response[0] == (byte)'2' &&  response[1]==(byte)'3' && response[2]==(byte)'0'))
 	{
	//System.out.println("230 response code not recieved in PASS in the response buffer:- " + byteToString(response,1,response.length));
  	opt.write(quit);
 	 inp.close();
 	 opt.close();
 	 ServerCmndSoc.close();
	 return -1;
	}
  emptyBuf(response);
  
  opt.write(pasv); 
  inp.read(response);

 while(true) {
 if(!(response[0] == (byte)'2' &&  response[1]==(byte)'2' && response[2]==(byte)'7'))  {
 	if(!(response[0] == (byte)'2' &&  response[1]==(byte)'3' && response[2]==(byte)'0'))  
		{
		//System.out.println("227 response code not recieved in PASV in the response buffer:- " + byteToString(response,1,response.length));
		opt.write(quit);
 	 	inp.close();
 	 	opt.close();
 	 	ServerCmndSoc.close();
		return -1;}
	else {
	emptyBuf(response);
	inp.read(response);
	}
  }
  else break;
  }
  /*Entering Passive Mode (a1,a2,a3,a4,p1,p2)
  where a1.a2.a3.a4 is the IP address and p1*256+p2 is the port number.*/

  String ServerDataIp = getServerDataIP(response);
  int ServerDataPort = getServerDataPort(response); 
  Socket ServerDataSoc = new Socket(ServerDataIp , ServerDataPort);
  emptyBuf(response);
  InputStream Dinp = ServerDataSoc.getInputStream(); 
  
    byte[] getR = { 'R','E','T','R',' '};
    byte[] newL = {'\r','\n'};
    byte[] retr = new byte[getR.length+URL.length+newL.length];
  int index=0;
  for(byte a: getR)
  {
    retr[index]=a;
    index++;
  }
  for(byte a: URL)
  {
    retr[index]=a;
    index++;
  }
  for(byte a: newL)
  {
    retr[index]=a;
    index++;
  }

  opt.write(retr);
  inp.read(response);

 if(!((response[0] == (byte)'1' &&  response[1]==(byte)'5' && response[2]==(byte)'0') || (response[0] == (byte)'1' &&  response[1]==(byte)'2' && response[2]==(byte)'5'))  )
 	{
	//System.out.println("230 response code not recieved in PASS in the response buffer:- " + byteToString(response,1,response.length));
  	opt.write(quit);
 	 inp.close();
 	 opt.close();
 	 ServerCmndSoc.close();
  	Dinp.close();
  	ServerDataSoc.close();
	return -1;
	}
  emptyBuf(response);
 OutputStream ClientOpt = c.getOutputStream();
 int currentBytes=0;
  first_buf = 1;
 while(true)
 {
 currentBytes = Dinp.read(response);
 if(currentBytes<=0) break;
 if(first_buf == 1) {
 	ClientOpt.write(RespOK);
	first_buf=0;
	}
 bytesRead+=currentBytes;
 ClientOpt.write(response,0,currentBytes);
 emptyBuf(response); 
 }
  opt.write(quit);
  inp.close();
  opt.close();
  ServerCmndSoc.close();
  Dinp.close();
  ClientOpt.close();
  ServerDataSoc.close();
  }
catch (IOException e) {
  
  //System.out.println("Exception in ftp########################################################### " + e.getMessage());
  return -1;
  }
  response = null;
  return bytesRead;

}

String byteToString(byte[] b, int i, int j)
{
  char[] temp = new char[j-(i-1)];
  for(int k=0; k<temp.length; k++, i++)
  {
      temp[k] = (char)b[i-1];
  }
  return String.valueOf(temp);
}

int run(int X)  
{
  ServerSocket s0 = null; // this is the listening socket
  Socket       s1 = null; //  this is the accept-ed socket i.e. client
  byte []      b0 = new byte[65535] ;  // general purpose buffer 
  try{
  s0 = new ServerSocket(PORT,999999999); //This creates an implicit bind to the port, hence bind() is avoided.
  System.out.println("Apache listening on socket " + PORT);
  int count = 1;
  
  byte[] RespBAD = {'H', 'T', 'T', 'P', '/', '1', '.', '1', ' ', '4', '0', '0', ' ', 'B', 'a', 'd', ' ', 'R', 'e', 'q', 'u', 'e', 's', 't', '\r','\n','\r','\n', '<', 'h', 't', 'm', 'l', '>', '<', 'h', 'e', 'a', 'd', '>', '<', 'h', '1', '>', 'E', 'r', 'r', 'o', 'r', ' ', '4', '0', '0', '.', ' ', 'B', 'a', 'd', ' ', 'R', 'e', 'q', 'u', 'e', 's', 't', '<', '/', 'h', '1', '>', '<', '/', 'h', 'e', 'a', 'd', '>', '<', 'b', 'o', 'd', 'y', '>', 'E', 'r', 'r', 'o', 'r', ' ', '4', '0', '0', '.', ' ', 'B', 'a', 'd', ' ', 'R', 'e', 'q', 'u', 'e', 's', 't', '<', '/', 'b', 'o', 'd', 'y', '>', '<', '/', 'h', 't', 'm', 'l', '>'};
  
  while ( true ) 
  {        
    InputStream inputStream = null;
    OutputStream outputStream = null;
    // Listen for client request. When received, we return the IP address
    s1 = s0.accept();
    System.out.println("("+count+") Incoming client connection from ["+s1.getInetAddress().getHostAddress()+":"+s1.getPort()+"] to me ["+s0.getInetAddress().getLocalHost().getHostAddress()+":"+s0.getLocalPort()+"]");
    //s1.InputStream.read(b0); // say req is "GET http://site.com/dir1/dir2/file2.html"
    count++;
    inputStream = s1.getInputStream();
    outputStream = s1.getOutputStream();
  
    inputStream.read(b0);
    int tryParse = parse(b0); 
    int dnsResponse=0;
    //if the request is good, we can find preferred
    if(tryParse!=-1) {
      if(tryParse==1){
        dnsResponse = dns(1); }
       else{
         dnsResponse = dns(2); }
    }

    /* Part 2
    */
    int nbytes=0;
    if(tryParse!=-1 && dnsResponse!=-1)
    {
      if(tryParse==1){
        nbytes = ftp_fetch(s1);
       }
      else{
        nbytes = http_fetch(s1);
        }
    
      if(nbytes == -1){
        System.out.println("    REQ: "+byteToString(HOST, 1, HOST.length) + byteToString(URL, 1, URL.length)+" / RESP: ERROR");
      	outputStream.write(RespBAD);
	}
      else
          {System.out.println("    REQ: "+ byteToString(HOST, 1, HOST.length) + byteToString(URL, 1, URL.length) + " ("+nbytes+" bytes transferred)");}
    }
    else
    {
      //says to just close connection and log error if any errors
      System.out.println("    ERROR: Invalid Request");
      outputStream.write(RespBAD);
    }
    
    inputStream.close();
    outputStream.close();
    s1.close();
    }

    
  } 
  catch (IOException e) {
    return -1;
  }
 
}

} 
