// =============================================================================
// IMPORTS

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
// =============================================================================



// =============================================================================
/**
 * @file   RandomNetworkLayer.java
 * @author Scott F. Kaplan (sfkaplan@cs.amherst.edu)
 * @date   April 2022
 *
 * A network layer that perform routing via random link selection.
 */
public class RandomNetworkLayer extends NetworkLayer {
// =============================================================================



    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================



    // =========================================================================
    /**
     * Default constructor.  Set up the random number generator.
     */
    public RandomNetworkLayer () {

	random = new Random();

    } // RandomNetworkLayer ()
    // =========================================================================

    

    // =========================================================================
    /**
     * Create a single packet containing the given data, with header that marks
     * the source and destination hosts.
     *
     * @param destination The address to which this packet is sent.
     * @param data        The data to send.
     * @return the sequence of bytes that comprises the packet.
     */
    protected byte[] createPacket (int destination, byte[] data) {
		//if we are trying to make a packet that is too large return null
		if(data.length > MAX_PACKET_SIZE){
			return null;
		}
		//allocate space in new byte array to include header
		//header: length, destination, source
		byte[] packet = new byte[data.length + bytesPerHeader];
		byte[] length = intToBytes(packet.length);
		byte[] destinationInt = intToBytes(destination);
		byte[] source = intToBytes(address);
		
		//populate packet header
		for(int i = 0; i < 12; i++){
			if(i < 4){
				packet[i] = length[i];
			}
			else if(i >= 4 && i < 8){
				packet[i] = destinationInt[i-4];
			}
			else{
				packet[i] = source[i-8];
			}
		}
		//populate data for delviery
		for(int i = 0; i < data.length; i++){
			packet[i+12] = data[i];
		}
		//return complete packet
		return packet;

	// COMPLETE ME
	
    } // createPacket ()
    // =========================================================================



    // =========================================================================
    /**
     * Randomly choose the link through which to send a packet given its
     * destination.
     *
     * @param destination The address to which this packet is being sent.
     */
    protected DataLinkLayer route (int destination) {
		//if we are connected to destination host return its dataLinkLayer
		if(dataLinkLayers.containsKey(destination)){
			return dataLinkLayers.get(destination);
		}
		//else send it to a random link
		ArrayList<DataLinkLayer> myLinks = new ArrayList(dataLinkLayers.values());
		Random rand = new Random();
		int listSize = myLinks.size();
		int randomIndex = random.nextInt(listSize);
		return myLinks.get(randomIndex);
	
	// COMPLETE ME
	
    } // route ()
    // =========================================================================



    // =========================================================================
    /**
     * Examine a buffer to see if it's data can be extracted as a packet; if so,
     * do it, and return the packet whole.
     *
     * @param buffer The receive-buffer to be examined.
     * @return the packet extracted packet if a whole one is present in the
     *         buffer; <code>null</code> otherwise.
     */
    protected byte[] extractPacket (Queue<Byte> buffer) {
		//if at least one packet could be in the buffer
		if(buffer.size() < 13){
			return null;
		}
		//make a copy of buffer and get the length of packet
		Queue<Byte> bufferCopy = new LinkedList<>(buffer);
		byte[] lengthArr = new byte[4];
		for(int i = 0; i < 4; i++){
			byte b = bufferCopy.remove();
			lengthArr[i] = b;
		}
		int length = bytesToInt(lengthArr);
		//check if all the bytes have arrived 
		if(length > buffer.size()){
			return null;
		}
		
		//make the array to return
		byte [] toReturn = new byte[length];
		//copy over the bytes from the buffer and return
		for(int i = 0 ; i < length; i++){
			byte b = buffer.remove();
			toReturn[i] = b;
		}
		return toReturn;
	// COMPLETE ME
		
	
    } // extractPacket ()
    // =========================================================================



    // =========================================================================
    /**
     * Given a received packet, process it.  If the destination for the packet
     * is this host, then deliver its data to the client layer.  If the
     * destination is another host, route and send the packet.
     *
     * @param packet The received packet to process.
     * @see   createPacket
     */
    protected void processPacket (byte[] packet) {

		byte [] destinationAddr = new byte[4];
		for(int i = 0; i < destinationAddr.length; i++){
			destinationAddr[i] = packet[i+4];
		}
		//see if the current address is the delivery address
		byte [] currAddr = intToBytes(address);
		boolean shouldDeliver = bytesAreEqual(currAddr, destinationAddr);
		//if we want to deliver to this host
		if(shouldDeliver){
			//extract data from packet
			byte [] toDeliver = new byte[packet.length-12];
			for(int i = 0;  i < packet.length-12; i++){
				toDeliver[i] = packet[i+12];
			}
			//deliver data to client, if it was a valid packet
			if(toDeliver.length <= MAX_PACKET_SIZE)
				client.receive(toDeliver);
		}
		else{
			// Choose the data link layer through which to route.
			//destination not important in this case
			DataLinkLayer dataLink = route(0);
			//reroute packet to chosen host
			dataLink.send(packet);
		}
	
    } // processPacket ()
    // =========================================================================
    	
		//checks if two byte[] are equal
		public static boolean bytesAreEqual(byte[] a, byte[] b){
		if(a.length != b.length){
			return false;
		}
		for(int i = 0; i < a.length; i++){
			if(a[i] != b[i])
				return false;
		}
		return true;
	}


    // =========================================================================
    // INSTANCE DATA MEMBERS

    /** The random source for selecting routes. */
    private Random random;
    // =========================================================================



    // =========================================================================
    // CLASS DATA MEMBERS

    /** The offset into the header for the length. */
    public static final int     lengthOffset      = 0;

    /** The offset into the header for the source address. */
    public static final int     sourceOffset      = lengthOffset + Integer.BYTES;

    /** The offset into the header for the destination address. */
    public static final int     destinationOffset = sourceOffset + Integer.BYTES;

    /** How many total bytes per header. */
    public static final int     bytesPerHeader    = destinationOffset + Integer.BYTES;

    /** Whether to emit debugging information. */
    public static final boolean debug             = false;
   // =========================================================================


    
// =============================================================================
} // class RandomNetworkLayer
// =============================================================================
