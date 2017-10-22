# socket-live

Java library for TCP/IP socket communication. 

Consists of three main components (which later result into three running threads):

  * SocketConnection.java: main thread, run by the user of the library, which makes sure the connection is always open
  * SocketRead.java: read thread which continuously attempts to read incoming messages if any
  * SocketWrite.java: write thread which writes any messages in write queue to socket

## Installation

Package can be installed via maven by adding the following to your pom.xml:

    <dependency>
        <groupId>si.trina</groupId>
        <artifactId>socket-live</artifactId>
        <version>coming-soon</version>
    </dependency>
    
## How to use

**Initialize the socket connection**

    /*
        args: 
            ** name of socket connection
            ** IP of socket endpoint
            ** port of socket endpoint
    */
    SocketConnection example = new SocketConnection("m3scan-channel1", "10.0.0.2", 5040);    
    new Thread(example).start();

**Add listeners to socket connection**

    public class MyListener implements SocketListener {
        @Override
        public void processSocketEvent(String name, byte[] data) {
           short aShort = example.byteArrayToUInt16(new byte[] {data[0],reply[data[1]]});
           byte[] stringByteArray = Arrays.copyOfRange(data,data[2],data[20]);
					      String aString = example.byteArrayToString(stringByteArray);
           ...
        }
    }
    
    MyListener listener = new MyListener();
    example.addListener(listener);
    
Note that read thread expects an integer (4 bytes) with byte length of message before every message.

**Send data to socket**

    ByteArrayOutputStream os = new ByteArrayOutputStream();

    os.write(example.intToByteArray(42)); // for example: datagram length in bytes

    os.write(example.uint16ToByteArray((short)1)); // 1, 2, 3
    os.write(example.stringToByteArray("test", 40));

    example.sendToServer(os.toByteArray());
    

### Optional parameters of SocketConnection instance

**charsetName**

String indicating which charset to use when encoding/decoding strings.

**reconnectInterval**

Long indicating how often should connection attempt to be restored in case of target endpoind disconnecting.

**checkConnectionInterval**

Long indicating how often should the connection status be checked. Checking the connection status will result in attempting to reconnect if disconnected.

**readEnabled**

In case only writing to socket is required, this boolean can be set to false.

**writeEnabled**

In case only reading from socket is required, this boolean can be set to false.
