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

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.example.exoplayer.databinding.ActivityPlayerBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.util.Util

/**
 * オーディオまたはビデオストリームを再生するためのフルスクリーンアクティビティです。
 */
class PlayerActivity : AppCompatActivity() {
    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    /**
     * playerは、メモリ、CPU、ネットワーク接続、ハードウェア コーデックなどの多くのリソースを占有する可能性がある。
     * アプリがリソースを使用していないとき（バックグラウンドで実行されているときなど）に、
     * これらのリソースを解放して他のアプリが使用できるようにする。
     * そのためにplayerのlifecycleをactivityのlifecycleと紐づける
     */
    private var player: ExoPlayer? = null

    /**
     * 以下の３つの変数は中断された位置から再生を再開するために使う。
     * playWhenReady -> 再生/一時停止の状態
     * playbackPosition -> 再生位置
     * currentItem -> mediaItemのindex、どのmediaItemかということだと思う。ちょっとあってるか分からない。
     * https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/Timeline.html をちゃんと見とく
     */
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        /**
         * os7以上でマルチウィンドウのサポートが入っている。
         * 分割のウィンドウモードがアクティブになるように、onStartでplayerを初期化する必要がある。
         */
        if (Util.SDK_INT >= Build.VERSION_CODES.N) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        /**
         * os7以下のAndroidではアプリがリソースを取得するまでできるだけ長く待機する必要がある。
         * そのためonResumeまで待ってから初期化する。
         */
        if (Util.SDK_INT < Build.VERSION_CODES.N || player == null) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        /**
         * APIレベル24以下ではonStopが呼び出される保証がないため、onPauseでできるだけ早くreleaseする必要がある
         */
        if (Util.SDK_INT < Build.VERSION_CODES.N) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        /**
         * APIレベル24以上では、onStopが呼び出されることが保証される
         * pauseではアクティビティが引き続き表示されるため、onStopまで待ってからrelease
         */
        if (Util.SDK_INT >= Build.VERSION_CODES.N) {
            releasePlayer()
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                binding.playerView.player = exoPlayer

                //mediaItemは色々なタイプ(mp4,mp3...)がある。再生したいコンテンツ。
                val mediaItem = MediaItem.fromUri(getString(R.string.media_url_mp3))
                exoPlayer.setMediaItem(mediaItem)
            }
    }

    private fun releasePlayer() {
        player?.run {
            playbackPosition = currentPosition
            //無印exoのcurrentWindowIndexからのreplace
            currentItem = currentMediaItemIndex
            this@PlayerActivity.playWhenReady = this.playWhenReady
            release()
        }
        player = null
    }

    private fun hideSystemUi() {
        if (Util.SDK_INT >= Build.VERSION_CODES.R) {
            binding.playerView.windowInsetsController?.hide(
                WindowInsets.Type.statusBars() or
                        WindowInsets.Type.navigationBars()
            )
            binding.playerView.windowInsetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            binding.playerView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        }
    }
}