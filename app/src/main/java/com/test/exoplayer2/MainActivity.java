package com.test.exoplayer2;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import java.io.File;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    public static final String AES_ALGORITHM = "AES";
    public static final String AES_TRANSFORMATION = "AES/CTR/NoPadding";

    private static final String ENCRYPTED_FILE_NAME = "encrypted.mp4";

    private Cipher mCipher;
    private SecretKeySpec mSecretKeySpec;
    private IvParameterSpec mIvParameterSpec;

    private Button playButton;
    private Button encryptButton;


    private File mEncryptedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playButton = findViewById(R.id.playButton);
        encryptButton = findViewById(R.id.encryptButton);

        mEncryptedFile = new File(getFilesDir(), ENCRYPTED_FILE_NAME);

        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[16];
        byte[] iv = new byte[16];
        secureRandom.nextBytes(key);
        secureRandom.nextBytes(iv);

        mSecretKeySpec = new SecretKeySpec(key, AES_ALGORITHM);
        mIvParameterSpec = new IvParameterSpec(iv);

        try {
            mCipher = Cipher.getInstance(AES_TRANSFORMATION);
            mCipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, mIvParameterSpec);
        } catch (Exception e) {
            e.printStackTrace();
        }

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playVideo();
            }
        });

        encryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                encryptVideo();
            }
        });

    }

    private boolean hasFile() {
        return mEncryptedFile != null
                && mEncryptedFile.exists()
                && mEncryptedFile.length() > 0;
    }

    public void encryptVideo() {
//        if (hasFile()) {
//            Log.d(getClass().getCanonicalName(), "encrypted file found, no need to recreate");
//            return;
//        }
        try {
            Cipher encryptionCipher = Cipher.getInstance(AES_TRANSFORMATION);
            encryptionCipher.init(Cipher.ENCRYPT_MODE, mSecretKeySpec, mIvParameterSpec);
            // TODO:
            // you need to encrypt a video somehow with the same key and iv...  you can do that yourself and update
            // the ciphers, key and iv used in this demo, or to see it from top to bottom,
            // supply a url to a remote unencrypted file - this method will download and encrypt it
            // this first argument needs to be that url, not null or empty...
            String url = "https://ia801304.us.archive.org/34/items/PlaylistWilson/01%20-%20%20.mp3";
            new DownloadAndEncryptFileTask(url, mEncryptedFile, encryptionCipher).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playVideo() {
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();
        RenderersFactory renderersFactory = new DefaultRenderersFactory(this);
        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, loadControl);
        DataSource.Factory dataSourceFactory =
                new EncryptedFileDataSourceFactory(mCipher, mSecretKeySpec, mIvParameterSpec, bandwidthMeter);
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        try {
            Uri uri = Uri.fromFile(mEncryptedFile);
            MediaSource videoSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
            player.prepare(videoSource);
            player.setPlayWhenReady(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
