package com.test.exoplayer2

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.upstream.crypto.AesCipherDataSink
import com.google.android.exoplayer2.upstream.crypto.AesCipherDataSource
import com.google.android.exoplayer2.util.Util
import java.security.MessageDigest

/**
 * cache with encryption while listening to media
 */
class ExoPlayerComponentsActivity : AppCompatActivity() {

    lateinit var cache: SimpleCache

    val simpleExoPlayer: SimpleExoPlayer by lazy {
        val bandwidthMeter = DefaultBandwidthMeter()
        val trackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(trackSelectionFactory)
        ExoPlayerFactory.newSimpleInstance(this, trackSelector)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.exoplayer_components)

        val playButton = findViewById<Button>(R.id.playButton)

        cache = SimpleCache(cacheDir(), NoOpCacheEvictor())

        val url = "https://ia801304.us.archive.org/34/items/PlaylistWilson/01%20-%20%20.mp3"
        playButton.setOnClickListener {
            prepareMediaSource(url)
        }
    }

    fun prepareMediaSource(url: String) {
        simpleExoPlayer.prepare(createMediaSource(url))
        simpleExoPlayer.playWhenReady = true
    }

    private fun createMediaSource(url: String) = ExtractorMediaSource(Uri.parse(url), cacheDataSourceFactory(), DefaultExtractorsFactory(), null, null)

    private fun cacheDataSourceFactory() = object : DataSource.Factory {
        override fun createDataSource(): DataSource {
            val secret = generateSecret("password")
            val aesCipherDataSource = AesCipherDataSource(secret, FileDataSource())
            val scratch = ByteArray(3897)
            val aesCipherDataSink = AesCipherDataSink(
                    secret, CacheDataSink(cache, Long.MAX_VALUE), scratch)

            return CacheDataSource(
                    cache,
                    okHttpDataSourceFactory().createDataSource(),
                    aesCipherDataSource,
                    aesCipherDataSink,
                    CacheDataSource.FLAG_BLOCK_ON_CACHE, null)

        }
    }

    private fun okHttpDataSourceFactory() = DefaultHttpDataSourceFactory(Util.getUserAgent(applicationContext, packageName), null)

    private fun cacheDir() = this.externalCacheDir

    private fun generateSecret(key: String): ByteArray {
        val keyBytes = ByteArray(16)
        val md = MessageDigest.getInstance("SHA-256")
        md.update(key.toByteArray())
        System.arraycopy(md.digest(), 0, keyBytes, 0, keyBytes.size)
        return keyBytes
    }
}