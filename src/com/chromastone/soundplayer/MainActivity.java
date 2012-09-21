/*
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.chromastone.soundplayer;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity 
	implements 
	MediaPlayer.OnErrorListener, 
	MediaPlayer.OnCompletionListener,
	MediaPlayer.OnInfoListener
{
	private enum PlayerState
	{
		Stopped,
		Playing,
		Paused
	}
	
	private static final int REQUEST_CODE_PICK_FILE = 1;
	
	private Uri _currentUri;
	private boolean _autoPlay;
	private PlayerState _state = PlayerState.Stopped;
	private MediaPlayer _mediaPlayer;
	private Handler _handler = new Handler();
	
	private SeekBar _playbackSeekBar;  
	
	private void killMediaPlayer() {
        if (_mediaPlayer != null) {
            try {
            	_mediaPlayer.release();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
	
	private void setFilename(String name) {
		TextView filenameLabel = (TextView) findViewById(R.id.filename_label);
		
		filenameLabel.setText(name);
	}
	
	private void setFilename(Uri uri) {
		setFilename(uri.getLastPathSegment());
	}
	
	private void showError(String error) {
		TextView actionLabel = (TextView) findViewById(R.id.action_label);
		
		actionLabel.setText("Error:");
		setFilename(error);
	}
	
	private int getProgressPercentage(long currentDuration, long totalDuration){
        Double percentage = (double) 0;
 
        double currentSeconds = currentDuration / 1000d;
        double totalSeconds = totalDuration / 1000d;
 
        // calculating percentage
        percentage = (currentSeconds/totalSeconds)*100;
 
        // return percentage
        return percentage.intValue();
    }
	
	private void updateProgressUI() {
		long totalDuration = _mediaPlayer.getDuration();
        long currentPosition = _mediaPlayer.getCurrentPosition();
        
        // HACK: for some reason current position can go backwards at the end on some files
        if (currentPosition < _previousPostion && currentPosition > 0)
        	return;
        
        int progress = getProgressPercentage(currentPosition, totalDuration);
        
        _playbackSeekBar.setProgress(progress);
        Log.d(getLocalClassName(), "Progress: " + progress + " (" + currentPosition + "/" + totalDuration + ")");
        
        _previousPostion = currentPosition;
	}
	
	private void updateProgress() {
		_handler.postDelayed(_updateProgressTask, 100);
	}
	
	private void updateProgressNow() {
		updateProgressUI();
	}
	
	private long _previousPostion = 0;
	
	private Runnable _updateProgressTask = new Runnable() {
		public void run() {
			updateProgressUI();

            if (_mediaPlayer.isPlaying())
            	updateProgress();
		}
	};
	
	private void initMediaPlayer() {
		_mediaPlayer = new MediaPlayer();
    	_mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    	_mediaPlayer.setOnErrorListener(this);
    	_mediaPlayer.setOnCompletionListener(this);
    	_mediaPlayer.setOnInfoListener(this);
        _playbackSeekBar.setProgress(0);
        _playbackSeekBar.setMax(100);
	}
    
    private void beginPlayback(Uri uri) {
    	if (uri == null) {
    		Log.w(getLocalClassName(), "Cannot play null Uri");
    		return;
    	}
    	
    	Log.d(getLocalClassName(), "Begining playback of: " + uri.toString());
        
        //togglePlayButton();
        //killMediaPlayer();
        
        
        if (_mediaPlayer == null)
        	initMediaPlayer();

        _previousPostion = 0;
		_mediaPlayer.reset();
        
		try {
			_mediaPlayer.setDataSource(getApplicationContext(), uri);
			_mediaPlayer.prepare();
		} 
		catch (IllegalArgumentException ex) {
			showError(ex.getMessage());
			ex.printStackTrace();
			return;
		} 
		catch (SecurityException ex) {
			showError(ex.getMessage());
			ex.printStackTrace();
			return;
		} 
		catch (IllegalStateException ex) {
			showError(ex.getMessage());
			ex.printStackTrace();
			return;
		} 
		catch (IOException ex) {
			showError(ex.getMessage());
			ex.printStackTrace();
			return;
		}

        setFilename(uri);
		resumePlayback();
    }
	
	private void togglePlayButton() {
        Button playButton = (Button) findViewById(R.id.play_button);
		
        if (_state == PlayerState.Playing)
        	playButton.setText(R.string.pause_button);
        else
        	playButton.setText(R.string.play_button);
	}
    
    private void pausePlayback() {
    	if (_state != PlayerState.Playing) return;
    	
    	_mediaPlayer.pause();    	
    	_handler.removeCallbacks(_updateProgressTask);
    	_state = PlayerState.Paused;
    	
    	togglePlayButton();
    }
    
    private void resumePlayback() {
    	if (_state == PlayerState.Playing) return;

    	_mediaPlayer.start();
    	_state = PlayerState.Playing;
    	
    	togglePlayButton();
    	updateProgress();
    }
    
    private void stopPlayback() {
    	if (_state == PlayerState.Stopped) return;
    	
    	_mediaPlayer.stop();
    	_handler.removeCallbacks(_updateProgressTask);
    	_state = PlayerState.Stopped;
    	
    	togglePlayButton();
    }
    
    public void playClick(View view) {    	
    	switch (_state) {
    	case Paused:
    		resumePlayback();
    		break;
    	case Playing:
    		pausePlayback();
    		break;
    	default:
    		beginPlayback(_currentUri);
    		break;
    	}
    }
    
    public void exitApplication(View view) {
    	stopPlayback();
        killMediaPlayer();
        finish();
    }
	
	public void showFileSelection(View view) {
		Intent fileExploreIntent = new Intent(
			ua.com.vassiliev.androidfilebrowser.FileBrowserActivity.INTENT_ACTION_SELECT_FILE,
			null,
			this,
			ua.com.vassiliev.androidfilebrowser.FileBrowserActivity.class
		);

		fileExploreIntent.putExtra(
			ua.com.vassiliev.androidfilebrowser.FileBrowserActivity.startDirectoryParameter, 
			Environment.getExternalStorageDirectory().getPath());
		
		startActivityForResult(fileExploreIntent, REQUEST_CODE_PICK_FILE);
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(getLocalClassName(), "Created");
        
        setContentView(R.layout.activity_main);
        
        _playbackSeekBar = (SeekBar) findViewById(R.id.playback_seekbar);
        
        Intent intent = getIntent();
        Uri uri = intent.getData();
        
        if (uri == null)
        	uri = Uri.parse("android.resource://com.chromastone.soundplayer/raw/default_sound");
        
        if (intent.getType() != null && intent.getType().indexOf("audio/") == -1)
        	return;
        
        if (uri != _currentUri) {
        	_autoPlay = uri.getScheme().equals("file");
        	_currentUri = uri;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public void onResume() {
    	Log.d(getLocalClassName(), "Resumed");
        super.onResume();
        
        if (_autoPlay) {
        	_autoPlay = false;
        	beginPlayback(_currentUri);
        }
    }
    
    @Override
    protected void onStop() {
    	Log.d(getLocalClassName(), "Stopped");
        super.onStop();
        
        pausePlayback();
    }
    
    @Override
    protected void onDestroy() {
    	Log.d(getLocalClassName(), "Destroyed");
        super.onDestroy();
        
        stopPlayback();
        killMediaPlayer();
    }

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
		Log.e(getLocalClassName(), "Error occurred: " + what + " (" + extra + ")");
		
		stopPlayback();
        killMediaPlayer();
		showError("Error occurred");
		
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		Log.d(getLocalClassName(), "Completed");
		
		// HACK: for some reason we don't reach 100% on some files automatically
		mediaPlayer.seekTo(mediaPlayer.getDuration());
		updateProgressNow();
		
		stopPlayback();
	}

	@Override
	public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
		Log.w(getLocalClassName(), "Info occurred: " + what + " (" + extra + ")");
		return false;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(getLocalClassName(), "Activity result: " + requestCode + ", " + resultCode);
		
		if (requestCode == REQUEST_CODE_PICK_FILE) {
        	if (resultCode == RESULT_OK) {
        		String newFile = data.getStringExtra(
        			ua.com.vassiliev.androidfilebrowser.FileBrowserActivity.returnFileParameter);
        		
        		_currentUri = Uri.fromFile(new File(newFile));
        		beginPlayback(_currentUri);	        	
        	}
        }
	}
}
