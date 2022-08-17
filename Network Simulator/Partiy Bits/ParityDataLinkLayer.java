// =============================================================================
// IMPORTS

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
// =============================================================================


// =============================================================================
/**
 * @file   DumbDataLinkLayer.java
 * @author Scott F. Kaplan (sfkaplan@cs.amherst.edu)
 * @date   August 2018, original September 2004
 *
 * A data link layer that uses start/stop tags and byte packing to frame the
 * data, and that performs no error management.
 */
public class ParityDataLinkLayer extends DataLinkLayer {
// =============================================================================



    // =========================================================================
    /**
     * Embed a raw sequence of bytes into a framed sequence.
     *
     * @param  data The raw sequence of bytes to be framed.
     * @return A complete frame.
     */
    protected byte[] createFrame (byte[] data) {

	Queue<Byte> framingData = new LinkedList<Byte>();
	
		int i = 0;
		// Add each byte of original data.
		while(i < data.length) {
			// Begin with the start tag.
			framingData.add(startTag);
			int counter = 0;
			Queue<Byte> toAdd = new LinkedList<Byte>();
			while(i < data.length && counter < frameSize){
				// If the current data byte is itself a metadata tag, then precede
				// it with an escape tag.
				byte currentByte = data[i];
				if ((currentByte == startTag) ||
				(currentByte == stopTag) ||
				(currentByte == escapeTag)) {

				toAdd.add(escapeTag);

				}

				// Add the data byte itself.
				toAdd.add(currentByte);
				i++;
				counter++;
			}
			framingData.addAll(toAdd);
			//Add the parity byte
			byte parityByte = generateParity(toAdd);
			framingData.add(parityByte);
			//Clear the list
			toAdd = new LinkedList<Byte>();
			

			// End with a stop tag.
			framingData.add(stopTag);
		}


		// Convert to the desired byte array.
		byte[] framedData = new byte[framingData.size()];
		Iterator<Byte>  k = framingData.iterator();
		int             j = 0;
		while (k.hasNext()) {
			framedData[j++] = k.next();
		}

		return framedData;
	
    } // createFrame ()
    // =========================================================================


    
    // =========================================================================
    /**
     * Determine whether the received, buffered data constitutes a complete
     * frame.  If so, then remove the framing metadata and return the original
     * data.  Note that any data preceding an escaped start tag is assumed to be
     * part of a damaged frame, and is thus discarded.
     *
     * @return If the buffer contains a complete frame, the extracted, original
     * data; <code>null</code> otherwise.
     */
    protected byte[] processFrame () {

	// Search for a start tag.  Discard anything prior to it.
	boolean        startTagFound = false;
	Iterator<Byte>             i = byteBuffer.iterator();
	while (!startTagFound && i.hasNext()) {
	    byte current = i.next();
	    if (current != startTag) {
		i.remove();
	    } else {
		startTagFound = true;
	    }
	}

	// If there is no start tag, then there is no frame.
	if (!startTagFound) {
	    return null;
	}
	
	// Try to extract data while waiting for an unescaped stop tag.
	Queue<Byte> extractedBytes = new LinkedList<Byte>();
	int numBytes = 0;
	boolean       stopTagFound = false;
	while (!stopTagFound && i.hasNext()) {

	    // Grab the next byte.  If it is...
	    //   (a) An escape tag: Skip over it and grab what follows as
	    //                      literal data.
	    //   (b) A stop tag:    Remove all processed bytes from the buffer and
	    //                      end extraction.
	    //   (c) A start tag:   All that precedes is damaged, so remove it
	    //                      from the buffer and restart extraction.
	    //   (d) Otherwise:     Take it as literal data.
	    byte current = i.next();
	    if (current == escapeTag) {
		if (i.hasNext()) {
		    current = i.next();
			numBytes++;
		    extractedBytes.add(current);
		} else {
		    // An escape was the last byte available, so this is not a
		    // complete frame.
		    return null;
		}
	    } else if (current == stopTag) {
		cleanBufferUpTo(i);
		stopTagFound = true;
	    } else if (current == startTag) {
		cleanBufferUpTo(i);
		extractedBytes = new LinkedList<Byte>();
	    } else {
			numBytes++;
			//Extract bytes
			extractedBytes.add(current);
	    }

	}

	// If there is no stop tag, then the frame is incomplete.
	if (!stopTagFound) {
	    return null;
	}
	//Check if more than 9 bytes were found 
	if(numBytes > frameSize+1){
		printError("There were more than 8 bytes found in the frame");
		return null;
	}
	
	//Have to get everything in front of the parity byte to check if corrupted
	Queue<Byte> temp = new LinkedList<Byte>();
	for(int counter = 0; counter < numBytes-1; counter++){
			byte b = extractedBytes.remove();
			temp.add(b);
		}
	numBytes = 0;
	byte parityR = extractedBytes.remove();
	//Genertate parity of extracted data
	byte parityG = generateParity( new LinkedList<Byte>(temp));
	//See if data was corrupted
	if(parityR != parityG){
		printError("Parity bits do not match");
		printError(temp);
		return null;
	}
	

	// Convert to the desired byte array.
	if (debug) {
	    System.out.println("DumbDataLinkLayer.processFrame(): Got whole frame!");
	}
	byte[] extractedData = new byte[temp.size()];
	int                j = 0;
	i = temp.iterator();
	while (i.hasNext()) {
	    extractedData[j] = i.next();
	    if (debug) {
		System.out.printf("DumbDataLinkLayer.processFrame():\tbyte[%d] = %c\n",
				  j,
				  extractedData[j]);
	    }
	    j += 1;
	}

	return extractedData;

    } // processFrame ()
    // ===============================================================



    // ===============================================================
    private void cleanBufferUpTo (Iterator<Byte> end) {

	Iterator<Byte> i = byteBuffer.iterator();
	while (i.hasNext() && i != end) {
	    i.next();
	    i.remove();
	}

    }
	
	//Given a queue of bytes, return parity
	private byte generateParity(Queue<Byte> myList){
		int ones = 0;
		while(myList.peek() != null){
			byte cur = myList.remove();
			ones += Integer.bitCount(cur & 0xff);
		}
		byte parity = (byte)(ones % 2);
		return parity;
	
	}
	//Given a queue of bytes and a parity, return True if no error is detected
	private boolean checkParity(Queue<Byte> myList, byte parity){
		byte generatedParity = generateParity(myList);
		return generatedParity == parity;
	}
	private void printError(Queue<Byte> myList){
		System.out.println("Error detected");
		while(myList.peek() != null){
			byte b = myList.remove();
			char c = (char) (b & 0xFF);
			System.out.print(c + " ");
		}
	}
	private void printError(ArrayList<Byte> myList){
		System.out.println("Error detected");
		for(int i = 0; i < myList.size();i++){
			byte b = myList.get(i);
			char c = (char) (b & 0xFF);
			System.out.print(c + " ");
		}
	}
	private void printError(byte [] bytes){
		System.out.println("Error detected");
		for(int i = 0; i < bytes.length; i++){
			byte b = bytes[i];
			char c = (char) (b & 0xFF);
			System.out.print(c + " ");
		}
	}
	private void printError(String a){
		System.out.println(a);
	}
	
	
	
    // ===============================================================



    // ===============================================================
    // DATA MEMBERS
    // ===============================================================



    // ===============================================================
    // The start tag, stop tag, and the escape tag.
    private final byte startTag  = (byte)'{';
    private final byte stopTag   = (byte)'}';
    private final byte escapeTag = (byte)'\\';
	private final int frameSize = 8;
    // ===============================================================



// ===================================================================
} // class DumbDataLinkLayer
