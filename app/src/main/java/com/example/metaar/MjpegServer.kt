package com.example.metaar

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.CopyOnWriteArrayList

class MjpegServer(port: Int = PORT) : NanoHTTPD(port) {

    private val sinks = CopyOnWriteArrayList<PipedOutputStream>()

    fun pushFrame(jpeg: ByteArray) {
        if (sinks.isEmpty()) return
        val header = "--frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${jpeg.size}\r\n\r\n"
        val footer = "\r\n"
        val it = sinks.iterator()
        while (it.hasNext()) {
            val sink = it.next()
            try {
                sink.write(header.toByteArray(Charsets.US_ASCII))
                sink.write(jpeg)
                sink.write(footer.toByteArray(Charsets.US_ASCII))
                sink.flush()
            } catch (_: IOException) {
                sinks.remove(sink)
                Log.d(TAG, "Client disconnected, ${sinks.size} remaining")
            }
        }
    }

    override fun serve(session: IHTTPSession): Response = when (session.uri) {
        "/stream" -> serveStream()
        else -> serveIndex()
    }

    private fun serveStream(): Response {
        val sink = PipedOutputStream()
        val source = PipedInputStream(sink, PIPE_BUFFER_BYTES)
        sinks.add(sink)
        Log.d(TAG, "New stream client, total: ${sinks.size}")
        return newChunkedResponse(
            Response.Status.OK,
            "multipart/x-mixed-replace; boundary=frame",
            source
        ).also {
            it.addHeader("Cache-Control", "no-cache, no-store")
            it.addHeader("Access-Control-Allow-Origin", "*")
        }
    }

    private fun serveIndex(): Response =
        newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", INDEX_HTML)

    companion object {
        const val PORT = 8080
        private const val TAG = "MjpegServer"
        private const val PIPE_BUFFER_BYTES = 128 * 1024

        private val INDEX_HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1"/>
              <title>Meta AR Live</title>
              <style>
                *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                  background: #0a0a0a; color: #e0e0e0;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                  min-height: 100dvh;
                  display: flex; flex-direction: column;
                  align-items: center; justify-content: center;
                  gap: 16px; padding: 16px;
                }
                h1 { font-size: 18px; font-weight: 500; letter-spacing: 0.05em; opacity: 0.8; }
                #wrap {
                  position: relative; width: 100%; max-width: 720px;
                  aspect-ratio: 16/9; background: #111;
                  border-radius: 12px; overflow: hidden; border: 1px solid #2a2a2a;
                }
                #feed { width: 100%; height: 100%; object-fit: contain; display: block; }
                #overlay {
                  position: absolute; inset: 0;
                  display: flex; align-items: center; justify-content: center;
                  background: rgba(0,0,0,.7); font-size: 14px; opacity: .8;
                }
                #overlay.hidden { display: none; }
                footer { font-size: 13px; opacity: .4; }
              </style>
            </head>
            <body>
              <h1>Meta AR — Live Camera</h1>
              <div id="wrap">
                <img id="feed" src="/stream" alt="glasses camera feed"/>
                <div id="overlay">Connecting…</div>
              </div>
              <footer id="status">Waiting for stream</footer>
              <script>
                const feed = document.getElementById('feed');
                const overlay = document.getElementById('overlay');
                const status = document.getElementById('status');
                feed.onload = () => {
                  overlay.classList.add('hidden');
                  status.textContent = 'Streaming live';
                };
                feed.onerror = () => {
                  overlay.classList.remove('hidden');
                  overlay.textContent = 'Reconnecting…';
                  status.textContent = 'Stream lost — retrying…';
                  setTimeout(() => { feed.src = '/stream?' + Date.now(); }, 2000);
                };
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
