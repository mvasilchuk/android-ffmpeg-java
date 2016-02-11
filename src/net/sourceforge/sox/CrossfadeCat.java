package net.sourceforge.sox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.ffmpeg.android.MediaDesc;

import android.util.Log;

/**
 * Concatenates two files together with a crossfade of user
 * defined length.
 *
 * It is a Java port of the scripts/crossfade_cat.sh script
 * in the sox source tree.
 *
 * Original script by Kester Clegg, with modifications by Chris
 * Bagwell.
 *
 * @author Abel Luck
 *
 */
// TODO make runnable?
public class CrossfadeCat {
	private final static String TAG = "SOX-XFADE";
	private	SoxController mController;
	private MediaDesc mFirstFile;
	private MediaDesc mSecondFile;
	private double mFadeLength;
	private MediaDesc mFinalMix;

	public CrossfadeCat(SoxController controller, MediaDesc firstFile, MediaDesc secondFile, double fadeLength, MediaDesc finalMix) {
		mController = controller;
		mFirstFile = firstFile;
		mSecondFile = secondFile;
		mFadeLength = fadeLength;
		mFinalMix = finalMix;
	}

	public boolean start() throws IOException, CloneNotSupportedException {
		// find length of first file
		double length = mController.getLength(mFirstFile.path);

		double trimLength = length - mFadeLength;

		// Obtain trimLength seconds of fade out position from the first File
		String trimmedOne = mController.trimAudio(mFirstFile.path, trimLength, mFadeLength, mFirstFile.audioVolume);
		if( trimmedOne == null )
			return abort();

		// We assume a fade out is needed (i.e., firstFile doesn't already fade out)
		MediaDesc fadedOne = new MediaDesc();
		fadedOne.path = mController.fadeAudio(trimmedOne, "t", 0, mFadeLength, mFadeLength);
		if( fadedOne.path == null )
			return abort();
		
		// Get crossfade section from the second file
		String trimmedTwo = mController.trimAudio(mSecondFile.path, 0, mFadeLength, mSecondFile.audioVolume);
		if( trimmedTwo == null )
			return abort();

		MediaDesc fadedTwo = new MediaDesc();
		fadedTwo.path = mController.fadeAudio(trimmedTwo, "t", mFadeLength, -1, -1);
		if( fadedTwo.path == null )
			return abort();

		// Mix crossfaded files together at full volume
		ArrayList<MediaDesc> files = new ArrayList<MediaDesc>();		
		files.add(fadedOne);
		files.add(fadedTwo);

		MediaDesc crossfaded = new MediaDesc();
		crossfaded.path = new File(mFirstFile.path).getCanonicalPath() + "-x-" + new File(mSecondFile.path).getName() +".wav";
		crossfaded = mController.combineMix(files, crossfaded);		
		if( crossfaded == null )
			return abort();
		crossfaded.audioVolume = mFirstFile.audioVolume;

		// Trim off crossfade sections from originals
		MediaDesc trimmedThree = mFirstFile.clone();
		trimmedThree.path = mController.trimAudio(mFirstFile.path, 0, trimLength, 1.0f);
		if( trimmedThree.path == null )
			return abort();
		
		MediaDesc trimmedFour = mSecondFile.clone();
		trimmedFour.path = mController.trimAudio(mSecondFile.path, mFadeLength, -1, 1.0f);
		if( trimmedFour.path == null )
			return abort();

		// Combine into final mix
		files.clear();
		
		files.add(trimmedThree);
		files.add(crossfaded);
		files.add(trimmedFour);
		mFinalMix = mController.combine(files, mFinalMix);
		return true;
	
	}

	
	private boolean abort() {
		return false;
	}


}
