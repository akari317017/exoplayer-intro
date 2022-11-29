/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
* limitations under the License.
 */
package com.example.exoplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.exoplayer.databinding.ActivityPlayerBinding

/**
 * A fullscreen activity to play audio or video streams.
 */
class PlayerActivity : AppCompatActivity() {
    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    private var player: ExoPlayer? = null

    //isPlayingが変更されたときに通知を受けたい場合は、onIsPlayingChangedをlistenすることができます。
    private val playbackStateListener = playbackStateListener()

    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    /**
     * playListはplayerにaddMediaItemでMediaItemを追加していくと作成できる。
     * これによってシームレス(途切れない)再生が可能となる。
     * バッファリング(データを取り込んでおく処理?)はバックグラウンドで処理される。
     * そのため次のmediaItemに映った時にbufferingSpinnerが出ない
     */
    private fun initializePlayer() {
        val trackSelector = DefaultTrackSelector(this).apply {
            //標準画質以下のトラックのみを選択するようにtrackSelectorに指示
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }
        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()
            .also { exoPlayer ->
                binding.videoView.player = exoPlayer
                val dashUrl = getString(R.string.media_url_dash)
                //https://exoplayer.dev/media-items.html < mediaItemBuilderのことはここでみてね
                val mediaItem = MediaItem.Builder()
                    .setUri(dashUrl)
                    /**
                     * HLS (MimeTypes.APPLICATION_M3U8) および SmoothStreaming (MimeTypes.APPLICATION_SS) は、
                     * 一般的に使用されているその他のアダプティブ ストリーミング フォーマットです。
                     * どちらも ExoPlayer でサポートされています
                     */
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build()

                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(currentItem, playbackPosition)
                exoPlayer.addListener(playbackStateListener)
                exoPlayer.prepare()
            }
    }

    private fun releasePlayer() {
        player?.let { exo ->
            playbackPosition = exo.currentPosition
            currentItem = exo.currentMediaItemIndex
            playWhenReady = exo.playWhenReady
            exo.removeListener(playbackStateListener)
            exo.release()
        }
        player = null
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.videoView).let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun playbackStateListener() = object : Player.Listener {
        /**
         * ExoPlayer.STATE_IDLE : プレーヤーはインスタンス化されましたが、まだ準備されていません。
         *
         * ExoPlayer.STATE_BUFFERING : 十分なデータがバッファリングされていないため、プレーヤーは現在の位置から再生できません。
         *     バッファリング中を知りたい？時とか？
         *
         * ExoPlayer.STATE_READY : プレーヤーが現在の位置からすぐに再生できる状態です。
         *     つまり、プレーヤーの playWhenReady プロパティが true の場合、メディア再生が自動的に開始されます。
         *     falseの場合、プレーヤーは一時停止されます。
         *
         * ExoPlayer.STATE_ENDED : プレーヤーがメディアの再生を終了しました。
         */
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "Idle"
                ExoPlayer.STATE_BUFFERING -> "Buffering"
                ExoPlayer.STATE_READY -> "Ready"
                ExoPlayer.STATE_ENDED -> "Ended"
                else -> "unKnownState"
            }
            Log.d(TAG, "changed state to $stateString")
        }
    }

    companion object {
        private const val TAG = "PlayerActivity"
    }
}