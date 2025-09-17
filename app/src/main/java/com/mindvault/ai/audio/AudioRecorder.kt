package com.mindvault.ai.audio

import android.Manifest
import android.media.*
import androidx.annotation.RequiresPermission
import java.io.*

class AudioRecorder(
    private val sampleRate: Int = 16_000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private var recorder: AudioRecord? = null
    @Volatile private var recording = false
    private var worker: Thread? = null

    /**
     * Caller must ensure RECORD_AUDIO is granted (do this in Activity).
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(outputWavPath: String) {
        if (recording) return

        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        require(minBuf != AudioRecord.ERROR && minBuf != AudioRecord.ERROR_BAD_VALUE) {
            "Invalid AudioRecord buffer size"
        }

        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBuf * 2
        )
        require(rec.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord init failed" }

        recorder = rec
        rec.startRecording()
        recording = true

        worker = Thread {
            val pcmData = ByteArray(minBuf)
            var totalPcmBytes = 0L

            // Write placeholder header; fill sizes on stop
            RandomAccessFile(outputWavPath, "rw").use { raf ->
                // Reserve 44 bytes
                raf.setLength(0)
                raf.write(ByteArray(44))

                while (recording) {
                    val read = rec.read(pcmData, 0, pcmData.size)
                    if (read > 0) {
                        raf.write(pcmData, 0, read)
                        totalPcmBytes += read
                    }
                }

                // Fill in header now that we know sizes
                writeWavHeader(
                    raf = raf,
                    pcmDataSize = totalPcmBytes,
                    sampleRate = sampleRate,
                    channels = if (channelConfig == AudioFormat.CHANNEL_IN_STEREO) 2 else 1,
                    bitsPerSample = if (audioFormat == AudioFormat.ENCODING_PCM_8BIT) 8 else 16
                )
            }
        }.also { it.start() }
    }

    fun stop() {
        if (!recording) return
        recording = false
        worker?.join()
        worker = null
        recorder?.run {
            try { stop() } catch (_: Throwable) {}
            release()
        }
        recorder = null
    }

    private fun writeWavHeader(
        raf: RandomAccessFile,
        pcmDataSize: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        raf.seek(0)
        raf.writeBytes("RIFF")
        raf.writeIntLE((36 + pcmDataSize).toInt())
        raf.writeBytes("WAVE")
        raf.writeBytes("fmt ")
        raf.writeIntLE(16)                 // Subchunk1Size (PCM)
        raf.writeShortLE(1.toShort())      // AudioFormat (PCM)
        raf.writeShortLE(channels.toShort())
        raf.writeIntLE(sampleRate)
        raf.writeIntLE(byteRate)
        raf.writeShortLE(blockAlign)
        raf.writeShortLE(bitsPerSample.toShort())
        raf.writeBytes("data")
        raf.writeIntLE(pcmDataSize.toInt())
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        write(byteArrayOf(
            (value and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 24) and 0xFF).toByte()
        ))
    }

    private fun RandomAccessFile.writeShortLE(value: Short) {
        write(byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() ushr 8) and 0xFF).toByte()
        ))
    }
}
