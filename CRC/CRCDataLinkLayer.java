// =============================================================================
// IMPORTS

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
// =============================================================================


// =============================================================================

public class CRCDataLinkLayer extends DataLinkLayer {
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
			while(i < data.length && counter < 8){
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
			//Pad 8 or less data bytes with one byte that is zero
			byte b = 0;
			toAdd.add(b);
			//Generate remainder
			byte crc = crc8(toAdd);
			//Add remainder to framed data
			framingData.add(crc);
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
	//count the number of bytes being extracted
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
			extractedBytes.add(current);
	    }

	}

	// If there is no stop tag, then the frame is incomplete.
	if (!stopTagFound) {
	    return null;
	}
	//If we find more than 9 bytes in the frame there was an error
	if(numBytes > 9){
		printError("There were more than 8 bytes found in the frame");
		printError(extractedBytes);
		return null;
	}
	//Generate remainder from extracted bytes
	byte crcByteGenerated = crc8(new LinkedList(extractedBytes));
	//Remove the last byte to transmit message successfully 
	Queue<Byte> myList = new LinkedList<Byte>();
	for(int counter = 0; counter < numBytes-1; counter++){
			byte b = extractedBytes.remove();
			myList.add(b);
		}
	//If remainder is not zero we have an error
	if(crcByteGenerated != 0){
		printError("Remainder does not equal zero");
		printError(myList);
		return null;
	}
	
	numBytes = 0;
	
	// Convert myList to the desired byte array.
	if (debug) {
	    System.out.println("DumbDataLinkLayer.processFrame(): Got whole frame!");
	}
	byte[] extractedData = new byte[myList.size()];
	int                j = 0;
	i = myList.iterator();
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
	//Caclulate CRC checksum for bytes in queue
	public static byte crc8(Queue<Byte> q){
		//Initally crc is 0 
		int crc = (int)0x00;
		//Loop through the bytes
		while(q.peek() != null){
			//Grab current byte
			int cur = q.remove();
			//Loop through all 8 bits of byte
			for(int j = 0; j < 8; j++){
				int temp = cur & 0b10000000;
				temp = temp >> 7;
				crc = (crc << 1) | temp;
				cur = cur<<1;
				if((crc >> 8) == 1){
					crc = crc ^ 0x1D5;
				}
			}
		}
		return (byte)(crc);
	}
	private void printError(String a){
		System.out.println(a);
	}
	private void printError(Queue<Byte> myList){
		System.out.println("Error detected");
		while(myList.peek() != null){
			byte b = myList.remove();
			char c = (char) (b & 0xFF);
			System.out.print(c + " ");
		}
		System.out.println();
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
    // ===============================================================



// ===================================================================
} // class DumbDataLinkLayer
// ===================================================================
