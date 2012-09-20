package com.chromastone.soundplayer;

import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity 
	implements 
	MediaPlayer.OnPreparedListener, 
	MediaPlayer.OnErrorListener, 
	MediaPlayer.OnCompletionListener,
	MediaPlayer.OnInfoListener,
	AudioManager.OnAudioFocusChangeListener
{
	
	private Uri _currentUri;
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
	
	private void togglePlayButton() {
        Button playButton = (Button) findViewById(R.id.play_button);
		
        playButton.setEnabled(!playButton.isEnabled());
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
	
	private void updateProgress() {
		_handler.postDelayed(_updateProgressTask, 100);
	}
	
	private long _previousPostion = 0;
	
	private Runnable _updateProgressTask = new Runnable() {
		public void run() {
			long totalDuration = _mediaPlayer.getDuration();
            long currentPosition = _mediaPlayer.getCurrentPosition();
            
            // HACK: for some reason current position can go backwards at the end on some files
            if (currentPosition < _previousPostion && currentPosition > 0)
            	return;
            
            int progress = getProgressPercentage(currentPosition, totalDuration);
            
            _playbackSeekBar.setProgress(progress);
            Log.d(getLocalClassName(), "Progress: " + progress + " (" + currentPosition + "/" + totalDuration + ")");
            
            _previousPostion = currentPosition;

            if (_mediaPlayer.isPlaying())
            	updateProgress();
		}
	};
    
    private void beginPlayback(Uri uri) {
    	if (uri == null) {
    		Log.w(getLocalClassName(), "Cannot play null Uri");
    		return;
    	}
    	
    	Log.d(getLocalClassName(), "Begining playback of: " + uri.toString());
        
        setFilename(uri);
        togglePlayButton();
        killMediaPlayer();
        
        _previousPostion = 0;
        _playbackSeekBar.setProgress(0);
        _playbackSeekBar.setMax(100);
        
    	_mediaPlayer = new MediaPlayer();
    	_mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    	//_mediaPlayer.setOnPreparedListener(this);
    	//_mediaPlayer.setOnErrorListener(this);
    	_mediaPlayer.setOnCompletionListener(this);
    	//_mediaPlayer.setOnInfoListener(this);
        
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

		//_mediaPlayer.prepareAsync();
		_mediaPlayer.start();
		
        updateProgress();
    }
    
    public void playAudio(View view) {
    	if (_mediaPlayer == null || !_mediaPlayer.isPlaying())
    		beginPlayback(_currentUri);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(getLocalClassName(), "Created");
        
        setContentView(R.layout.activity_main);
        
        _playbackSeekBar = (SeekBar) findViewById(R.id.playback_seekbar);
        
        // Ensure volume keys effect app volume
        //setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        Intent intent = getIntent();
        Uri uri = intent.getData();
        
        if (uri == null)
        	uri = Uri.parse("android.resource://com.chromastone.soundplayer/raw/default_sound");
        
        if (intent.getType() != null && intent.getType().indexOf("audio/") == -1)
        	return;
        
        _currentUri = uri;
        
        //beginPlayback(uri);
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
        
        if (_currentUri != null && _currentUri.getScheme().equals("file"))
        	beginPlayback(_currentUri);
    }
    
    @Override
    protected void onStop() {
    	Log.d(getLocalClassName(), "Stopped");
        super.onStop();
        
        if (_mediaPlayer != null)
        	_mediaPlayer.pause();
        
        _handler.removeCallbacks(_updateProgressTask);
    }
    
    @Override
    protected void onDestroy() {
    	Log.d(getLocalClassName(), "Destroyed");
        super.onDestroy();
        
        killMediaPlayer();
    }

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		Log.d(getLocalClassName(), "Prepared");
		
		//AudioManager manager = (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE);
		
		//int result = manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
		
		//if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			Log.d(getLocalClassName(), "Starting");
			mediaPlayer.start();
		//}
	}

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
		Log.e(getLocalClassName(), "Error occurred: " + what + " (" + extra + ")");
		
		mediaPlayer.release();
		mediaPlayer = null;
		
		showError("Problem loading file");
		
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		Log.d(getLocalClassName(), "Completed");
		togglePlayButton();
		
		// HACK: for some reason we don't reach 100% on some files automatically
		mediaPlayer.seekTo(mediaPlayer.getDuration());
		updateProgress();
	}

	@Override
	public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
		Log.w(getLocalClassName(), "Info occurred: " + what + " (" + extra + ")");
		return false;
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		Log.d(getLocalClassName(), "Audio Focus Changed: " + focusChange);
		
		AudioManager manager = (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE);
		
		if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
			if (_mediaPlayer != null)
				_mediaPlayer.pause();
        }
		else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            if (_mediaPlayer != null)
            	_mediaPlayer.start();
        }
        else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
        	manager.abandonAudioFocus(this);
        	
        	if (_mediaPlayer != null)
        		_mediaPlayer.stop();
        }
	}
}
