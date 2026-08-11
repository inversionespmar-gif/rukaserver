/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "motion/react";
import { 
  Play, 
  Copy, 
  Check, 
  FileVideo, 
  Cpu, 
  Layers, 
  ExternalLink, 
  Download, 
  RefreshCw, 
  AlertCircle, 
  Sparkles, 
  BookOpen, 
  Code2, 
  ShieldAlert, 
  Info, 
  Flame, 
  ChevronRight, 
  Video 
} from "lucide-react";
import Hls from "hls.js";
import { ConversionResult, TraceStep } from "./types";

export default function App() {
  // Input and settings state
  const [url, setUrl] = useState("");
  const [useAi, setUseAi] = useState(false);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ConversionResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  
  // UI states
  const [copiedText, setCopiedText] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"kotlin" | "java" | "android-xml">("kotlin");
  const [localTrace, setLocalTrace] = useState<TraceStep[]>([]);
  const [playerError, setPlayerError] = useState<string | null>(null);
  
  // Video player reference
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const hlsRef = useRef<Hls | null>(null);

  // List of preset URLs for quick testing
  const presets = [
    {
      label: "Vimeos.net Embed (Ejemplo)",
      url: "https://vimeos.net/embed-veurqn040997.html",
      useAi: true,
    },
    {
      label: "Demo Stream HLS (m3u8)",
      url: "https://test-streams.mux.dev/x36xhg/playlist.m3u8",
      useAi: false,
    },
    {
      label: "Demo Video Directo (mp4)",
      url: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
      useAi: false,
    }
  ];

  // Apply a preset
  const applyPreset = (presetUrl: string, presetAi: boolean) => {
    setUrl(presetUrl);
    setUseAi(presetAi);
  };

  // Run the conversion request
  const handleConvert = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!url.trim()) {
      setError("Por favor, ingresa un enlace válido.");
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);
    setPlayerError(null);
    
    // Set up initial optimistic trace steps for immediate UI feedback
    const initialTrace: TraceStep[] = [
      {
        step: "Iniciando",
        status: "pending",
        message: "Preparando entorno de conversión...",
        timestamp: new Date().toLocaleTimeString()
      }
    ];
    setLocalTrace(initialTrace);

    try {
      const response = await fetch("/api/convert", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ url, useAi })
      });

      const data = await response.json();

      if (data.success) {
        setResult(data);
        if (data.trace) {
          setLocalTrace(data.trace);
        }
      } else {
        setError(data.error || "No se pudo extraer ningún enlace de transmisión.");
        if (data.trace) {
          setLocalTrace(data.trace);
        }
      }
    } catch (err: any) {
      setError("Error de conexión con el servidor de conversión.");
      setLocalTrace(prev => [
        ...prev,
        {
          step: "Conexión",
          status: "failed",
          message: err.message || "Fallo al realizar la petición de conversión.",
          timestamp: new Date().toLocaleTimeString()
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  // Handle HLS and MP4 Player initialization when videoUrl updates
  useEffect(() => {
    if (!videoRef.current || !result?.url) return;

    setPlayerError(null);
    const videoElement = videoRef.current;
    const videoUrl = result.url;

    // Clean up previous Hls instance
    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }

    if (videoUrl.includes(".m3u8")) {
      if (Hls.isSupported()) {
        const hls = new Hls({
          maxMaxBufferLength: 10,
          enableWorker: true,
          lowLatencyMode: true,
        });
        hlsRef.current = hls;
        hls.loadSource(videoUrl);
        hls.attachMedia(videoElement);
        
        hls.on(Hls.Events.ERROR, (_event, data) => {
          if (data.fatal) {
            switch (data.type) {
              case Hls.ErrorTypes.NETWORK_ERROR:
                setPlayerError("Error de red al cargar el stream. Puede requerir cabeceras referer específicas.");
                hls.startLoad();
                break;
              case Hls.ErrorTypes.MEDIA_ERROR:
                setPlayerError("Error de códec/reproducción multimedia.");
                hls.recoverMediaError();
                break;
              default:
                setPlayerError("No se pudo iniciar la reproducción HLS.");
                break;
            }
          }
        });
      } else if (videoElement.canPlayType("application/vnd.apple.mpegurl")) {
        // Safari fallback native player
        videoElement.src = videoUrl;
      } else {
        setPlayerError("Tu navegador no soporta la reproducción de streams HLS (.m3u8).");
      }
    } else {
      // Standard MP4 video files
      videoElement.src = videoUrl;
    }

    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
    };
  }, [result?.url]);

  // Utility to copy text to clipboard
  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedText(id);
    setTimeout(() => setCopiedText(null), 2000);
  };

  // Generate M3U playlist file download
  const downloadM3U = () => {
    if (!result?.url) return;
    const m3uContent = `#EXTM3U\n#EXTINF:-1,${result.title || "Video Stream"}\n${result.url}`;
    const blob = new Blob([m3uContent], { type: "application/x-mpegurl" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${(result.title || "stream").replace(/[^a-z0-9]/gi, "_").toLowerCase()}.m3u`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  // ExoPlayer Configuration Snippets
  const getExoPlayerSnippet = () => {
    const refererHeader = result?.headers?.Referer || "https://vimeos.net/";
    const userAgentHeader = result?.headers?.["User-Agent"] || "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...";

    if (activeTab === "kotlin") {
      return `// Kotlin: Configuración completa para ExoPlayer (Jetpack Media3)
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

// 1. Configurar cabeceras de origen (Crucial para saltarse el bloqueo de referer)
const val VIDEO_URL = "${result?.url || "https://..."}"
const val REFERER_HEADER = "${refererHeader}"
const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36..."

val httpDataSourceFactory = DefaultHttpDataSource.Factory()
    .setUserAgent(USER_AGENT)
    .setDefaultRequestProperties(mapOf(
        "Referer" to REFERER_HEADER,
        "Origin" to "${refererHeader.endsWith("/") ? refererHeader.slice(0, -1) : refererHeader}"
    ))

// 2. Instanciar el Reproductor con el Factory de cabeceras
val player = ExoPlayer.Builder(context)
    .setMediaSourceFactory(DefaultMediaSourceFactory(context, httpDataSourceFactory))
    .build()

// 3. Crear item multimedia y reproducir
val mediaItem = MediaItem.fromUri(VIDEO_URL)
player.setMediaItem(mediaItem)
player.prepare()
player.playWhenReady = true`;
    } else if (activeTab === "java") {
      return `// Java: Configuración para ExoPlayer (Jetpack Media3)
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import java.util.HashMap;
import java.util.Map;

// 1. Configurar cabeceras anti-hotlinking
String videoUrl = "${result?.url || "https://..."}";
String referer = "${refererHeader}";

Map<String, String> headers = new HashMap<>();
headers.put("Referer", referer);
headers.put("Origin", "${refererHeader.endsWith("/") ? refererHeader.slice(0, -1) : refererHeader}");

DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36...")
    .setDefaultRequestProperties(headers);

// 2. Construir ExoPlayer cargando el factory
ExoPlayer player = new ExoPlayer.Builder(context)
    .setMediaSourceFactory(new DefaultMediaSourceFactory(context, httpDataSourceFactory))
    .build();

// 3. Preparar reproducción
MediaItem mediaItem = MediaItem.fromUri(videoUrl);
player.setMediaItem(mediaItem);
player.prepare();
player.setPlayWhenReady(true);`;
    } else {
      return `<!-- AndroidManifest.xml: Permisos necesarios para reproducir flujos de internet -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="your.package.name">

    <!-- 1. Habilitar permisos de Internet -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        <!-- 2. Habilitar tráfico HTTP plano si el m3u8 es http:// y no https:// -->
        android:usesCleartextTraffic="true"
        android:theme="@style/Theme.AppCompat">
        
        <activity android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>`;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 font-sans selection:bg-emerald-500/30 selection:text-emerald-400">
      {/* Visual background ambient glow */}
      <div className="absolute top-0 left-1/4 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute top-1/3 right-1/4 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

      {/* Outer Wrapper */}
      <div className="max-w-7xl mx-auto px-4 py-8 sm:px-6 lg:px-8 relative z-10">
        
        {/* Header section */}
        <header className="mb-10 text-center md:text-left flex flex-col md:flex-row md:items-center md:justify-between border-b border-slate-800/60 pb-6 gap-4">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 bg-emerald-500/10 border border-emerald-500/20 rounded-full text-xs text-emerald-400 font-mono mb-3">
              <Sparkles className="w-3.5 h-3.5 animate-pulse" />
              Soporte ExoPlayer, VLC & reproductores móviles
            </div>
            <h1 className="text-3xl sm:text-4xl font-display font-bold tracking-tight bg-gradient-to-r from-emerald-400 via-teal-300 to-indigo-400 bg-clip-text text-transparent">
              Convertidor de Enlaces Embed
            </h1>
            <p className="mt-1 text-sm text-slate-400 font-sans max-w-2xl">
              Extrae flujos directos <span className="text-slate-200 font-semibold">.m3u8 (HLS)</span> o <span className="text-slate-200 font-semibold">.mp4</span> de páginas de reproducción integradas (como <span className="text-indigo-400 font-mono">vimeos.net</span>) para usarlos en tus aplicaciones Android, iOS o Web.
            </p>
          </div>
          <div className="flex items-center gap-2 justify-center md:justify-end">
            <span className="text-xs text-slate-500 font-mono">v1.2.0 • Full-Stack AI</span>
          </div>
        </header>

        {/* Form and Presets layout */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* Left Column - Input, options and Trace Log */}
          <div className="lg:col-span-5 flex flex-col gap-6">
            <div id="converter-card" className="bg-slate-900/60 backdrop-blur-md border border-slate-800/80 rounded-2xl p-6 shadow-xl">
              <h2 className="text-lg font-display font-medium text-slate-200 mb-4 flex items-center gap-2">
                <Video className="w-5 h-5 text-emerald-400" />
                Configurar Conversión
              </h2>

              <form onSubmit={handleConvert} className="space-y-4">
                <div>
                  <label htmlFor="embed-url" className="block text-xs font-medium text-slate-400 mb-1.5 uppercase tracking-wider">
                    Enlace Embed de Video
                  </label>
                  <input
                    id="embed-url"
                    type="text"
                    placeholder="Escribe o pega el link aquí..."
                    value={url}
                    onChange={(e) => setUrl(e.target.value)}
                    className="w-full bg-slate-950/80 border border-slate-800 focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 rounded-xl px-4 py-3 text-sm text-slate-200 placeholder:text-slate-600 outline-none transition-all"
                  />
                </div>

                {/* AI Extractor Toggle */}
                <div className="bg-slate-950/40 border border-slate-800/60 rounded-xl p-3.5 flex items-start gap-3 transition-colors hover:bg-slate-950/60">
                  <input
                    id="ai-toggle"
                    type="checkbox"
                    checked={useAi}
                    onChange={(e) => setUseAi(e.target.checked)}
                    className="mt-1 accent-emerald-500 w-4 h-4 rounded border-slate-800 bg-slate-950 focus:ring-emerald-500 cursor-pointer"
                  />
                  <div className="flex-1 cursor-pointer" onClick={() => setUseAi(!useAi)}>
                    <label htmlFor="ai-toggle" className="text-xs font-semibold text-slate-300 flex items-center gap-1.5 cursor-pointer">
                      <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                      Análisis con Inteligencia Artificial (Gemini)
                    </label>
                    <p className="text-[11px] text-slate-500 mt-0.5 leading-relaxed">
                      Utiliza Gemini 3.5 Flash en el servidor para desofuscar reproductores complejos y extraer el stream cuando el escaneo básico no lo localiza.
                    </p>
                  </div>
                </div>

                <button
                  id="convert-btn"
                  type="submit"
                  disabled={loading}
                  className="w-full bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 active:scale-[0.98] disabled:from-slate-800 disabled:to-slate-800 text-slate-950 font-semibold rounded-xl py-3.5 transition-all flex items-center justify-center gap-2 cursor-pointer shadow-lg shadow-emerald-500/10 disabled:cursor-not-allowed disabled:text-slate-500"
                >
                  {loading ? (
                    <>
                      <RefreshCw className="w-4.5 h-4.5 animate-spin text-slate-950" />
                      <span>Analizando código fuente...</span>
                    </>
                  ) : (
                    <>
                      <Play className="w-4.5 h-4.5 fill-current" />
                      <span>Convertir Enlace</span>
                    </>
                  )}
                </button>
              </form>

              {/* Presets Grid */}
              <div className="mt-6 border-t border-slate-800/60 pt-5">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block mb-2.5">
                  Probar con Ejemplos Rápidos:
                </span>
                <div className="flex flex-col gap-2">
                  {presets.map((preset, index) => (
                    <button
                      key={index}
                      id={`preset-btn-${index}`}
                      onClick={() => applyPreset(preset.url, preset.useAi)}
                      className="text-left w-full text-xs bg-slate-950 hover:bg-slate-800/60 hover:text-emerald-400 px-3.5 py-2.5 rounded-lg border border-slate-800/40 hover:border-slate-700/60 transition-all flex items-center justify-between text-slate-400 font-medium group cursor-pointer"
                    >
                      <span className="truncate">{preset.label}</span>
                      <span className="text-[10px] bg-slate-900 border border-slate-800 px-1.5 py-0.5 rounded text-slate-500 group-hover:text-slate-300 font-mono transition-colors">
                        {preset.useAi ? "Usar IA" : "Regex"}
                      </span>
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Trace Step Logger Card */}
            <div id="trace-log-card" className="bg-slate-900/40 backdrop-blur-md border border-slate-800/50 rounded-2xl p-5">
              <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                <Cpu className="w-4 h-4 text-emerald-400" />
                Trace de Extracción en Servidor
              </h3>
              
              {localTrace.length === 0 ? (
                <div className="text-center py-6 text-slate-600 text-xs border border-dashed border-slate-800/40 rounded-xl">
                  Sin actividad de conversión reciente.
                </div>
              ) : (
                <div className="space-y-3.5 max-h-80 overflow-y-auto pr-1">
                  {localTrace.map((step, index) => (
                    <div key={index} className="flex items-start gap-2.5 text-xs">
                      {/* Timeline dot */}
                      <div className="mt-1">
                        {step.status === "success" && <div className="w-2 h-2 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.6)]" />}
                        {step.status === "failed" && <div className="w-2 h-2 rounded-full bg-red-500" />}
                        {step.status === "pending" && <div className="w-2 h-2 rounded-full bg-indigo-400 animate-ping" />}
                        {step.status === "info" && <div className="w-2 h-2 rounded-full bg-slate-500" />}
                      </div>

                      {/* Content */}
                      <div className="flex-1 bg-slate-950/40 p-2 rounded-lg border border-slate-800/40">
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-slate-300">{step.step}</span>
                          <span className="text-[10px] text-slate-600 font-mono">{step.timestamp}</span>
                        </div>
                        <p className="text-slate-400 mt-0.5 font-mono text-[11px] leading-relaxed break-all">
                          {step.message}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Right Column - Results, Video Player & ExoPlayer instructions */}
          <div className="lg:col-span-7 flex flex-col gap-6">
            
            {/* Global Error Banner */}
            {error && (
              <div id="error-banner" className="bg-red-500/10 border border-red-500/20 text-red-400 rounded-2xl p-4 flex items-start gap-3">
                <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
                <div>
                  <h4 className="font-semibold text-sm">No se pudo resolver el stream</h4>
                  <p className="text-xs mt-1 text-red-400/80 leading-relaxed">
                    {error} Asegúrate de que el enlace embed funciona en el navegador o prueba activando la **Extracción Avanzada con IA (Gemini)**.
                  </p>
                </div>
              </div>
            )}

            {/* Success and conversion result view */}
            <AnimatePresence mode="wait">
              {result && result.success ? (
                <motion.div
                  key="results-card"
                  initial={{ opacity: 0, y: 15 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -15 }}
                  transition={{ duration: 0.2 }}
                  className="space-y-6"
                >
                  {/* Master stream card */}
                  <div id="results-card" className="bg-slate-900/60 backdrop-blur-md border border-slate-800/80 rounded-2xl p-6 shadow-xl relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-500/5 rounded-full blur-2xl pointer-events-none" />
                    
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/60 pb-5">
                      <div className="flex items-start gap-3">
                        <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 shrink-0">
                          <FileVideo className="w-5 h-5" />
                        </div>
                        <div>
                          <span className="text-[10px] font-mono text-emerald-400 font-semibold tracking-wider uppercase bg-emerald-500/10 px-2 py-0.5 rounded-md border border-emerald-500/10">
                            {result.type?.toUpperCase()} Extracted
                          </span>
                          <h3 className="text-base font-display font-medium text-slate-200 mt-1 max-w-md truncate">
                            {result.title || "Flujo de transmisión obtenido"}
                          </h3>
                        </div>
                      </div>
                      
                      <div className="flex items-center gap-2">
                        <button
                          id="copy-stream-url"
                          onClick={() => copyToClipboard(result.url || "", "stream_url")}
                          className="px-3.5 py-2 bg-slate-950 hover:bg-slate-800 rounded-xl border border-slate-800 hover:border-slate-700 text-xs font-semibold text-slate-300 transition-all flex items-center gap-1.5 cursor-pointer"
                        >
                          {copiedText === "stream_url" ? (
                            <>
                              <Check className="w-3.5 h-3.5 text-emerald-400" />
                              <span className="text-emerald-400">¡Copiado!</span>
                            </>
                          ) : (
                            <>
                              <Copy className="w-3.5 h-3.5" />
                              <span>Copiar Enlace</span>
                            </>
                          )}
                        </button>
                        
                        <button
                          id="download-m3u"
                          onClick={downloadM3U}
                          className="px-3 py-2 bg-slate-950 hover:bg-slate-800 rounded-xl border border-slate-800 hover:border-slate-700 text-xs font-semibold text-slate-300 transition-all flex items-center gap-1.5 cursor-pointer"
                          title="Descargar archivo M3U para reproductores locales"
                        >
                          <Download className="w-3.5 h-3.5" />
                          <span>M3U</span>
                        </button>
                      </div>
                    </div>

                    {/* Extracted URL input display */}
                    <div className="mt-5">
                      <label className="block text-[11px] font-medium text-slate-500 uppercase tracking-wider mb-1.5 font-mono">
                        Enlace Directo de Transmisión (Pegar en ExoPlayer/VLC)
                      </label>
                      <div className="flex gap-2">
                        <input
                          type="text"
                          readOnly
                          value={result.url}
                          className="flex-1 bg-slate-950 font-mono text-xs text-emerald-400/90 border border-slate-800/80 rounded-xl px-3.5 py-3 select-all outline-none truncate"
                        />
                      </div>
                      
                      <div className="flex items-center gap-2.5 mt-3 text-xs text-slate-500 bg-slate-950/30 border border-slate-800/40 p-2.5 rounded-lg">
                        <Info className="w-4 h-4 text-indigo-400 shrink-0" />
                        <span>
                          Método de extracción: <strong className="text-slate-300 font-mono text-[11px]">{result.methodUsed === "gemini_ai" ? "Gemini AI" : result.methodUsed === "unpacker" ? "Desempaquetado JS" : "Escaneo Regex"}</strong>
                        </span>
                      </div>
                    </div>

                    {/* Headers needed */}
                    {result.headers && (
                      <div className="mt-4 border-t border-slate-800/40 pt-4">
                        <div className="flex items-center gap-1.5 mb-2">
                          <ShieldAlert className="w-4.5 h-4.5 text-amber-500" />
                          <span className="text-xs font-semibold text-slate-300">Cabeceras Requeridas para Reproducción</span>
                        </div>
                        <p className="text-[11px] text-slate-500 mb-2.5 leading-relaxed">
                          Muchos servidores de streaming bloquean conexiones si no envías las cabeceras originales. Asegúrate de configurarlas en tu ExoPlayer:
                        </p>
                        <div className="bg-slate-950 p-3 rounded-xl border border-slate-800 font-mono text-[10px] space-y-1 text-slate-400">
                          <div><span className="text-emerald-400">Referer:</span> {result.headers.Referer}</div>
                          <div className="truncate"><span className="text-emerald-400">User-Agent:</span> {result.headers["User-Agent"]}</div>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Built-in Stream Testing Player */}
                  <div id="video-player-card" className="bg-slate-900/60 backdrop-blur-md border border-slate-800/80 rounded-2xl p-6 shadow-xl">
                    <div className="flex items-center justify-between mb-4">
                      <h3 className="text-sm font-display font-medium text-slate-200 flex items-center gap-2">
                        <Play className="w-4 h-4 text-emerald-400" />
                        Probar Reproducción del Stream
                      </h3>
                      {result.type === "m3u8" && (
                        <span className="text-[10px] bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 px-2 py-0.5 rounded font-mono font-semibold uppercase">
                          Hls.js Activado
                        </span>
                      )}
                    </div>

                    <div className="bg-slate-950 rounded-xl overflow-hidden aspect-video border border-slate-800 relative flex items-center justify-center">
                      <video
                        id="web-player"
                        ref={videoRef}
                        controls
                        className="w-full h-full object-contain"
                        poster="https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=1200&auto=format&fit=crop"
                      />
                      
                      {playerError && (
                        <div className="absolute inset-0 bg-slate-950/90 backdrop-blur-sm flex flex-col items-center justify-center p-6 text-center">
                          <AlertCircle className="w-10 h-10 text-red-400 mb-3" />
                          <h4 className="text-sm font-semibold text-slate-200">Error en el reproductor web</h4>
                          <p className="text-xs text-slate-500 mt-1 max-w-sm">
                            {playerError}
                          </p>
                          <p className="text-[11px] text-slate-600 mt-3 leading-relaxed">
                            Nota: Los streams de servidores piratas suelen requerir cookies o cabeceras Referer nativas que los navegadores bloquean por CORS. No te preocupes, el enlace sigue siendo totalmente compatible en tu app móvil configurada con las cabeceras.
                          </p>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* ExoPlayer Configuration Integration Guide */}
                  <div id="developer-guide-card" className="bg-slate-900/60 backdrop-blur-md border border-slate-800/80 rounded-2xl p-6 shadow-xl">
                    <div className="flex items-center justify-between border-b border-slate-800/50 pb-4 mb-4">
                      <div className="flex items-center gap-2">
                        <Code2 className="w-5 h-5 text-indigo-400" />
                        <div>
                          <h3 className="text-sm font-display font-medium text-slate-200">Guía de Integración ExoPlayer</h3>
                          <p className="text-[11px] text-slate-500 mt-0.5">Añade soporte nativo en Android con Jetpack Media3</p>
                        </div>
                      </div>
                    </div>

                    {/* Code tabs selector */}
                    <div className="flex bg-slate-950 p-1 rounded-xl border border-slate-800/80 gap-1 mb-4">
                      <button
                        onClick={() => setActiveTab("kotlin")}
                        className={`flex-1 py-2 text-xs font-medium rounded-lg transition-all cursor-pointer ${activeTab === "kotlin" ? "bg-slate-800 text-emerald-400 font-semibold" : "text-slate-400 hover:text-slate-200"}`}
                      >
                        Kotlin
                      </button>
                      <button
                        onClick={() => setActiveTab("java")}
                        className={`flex-1 py-2 text-xs font-medium rounded-lg transition-all cursor-pointer ${activeTab === "java" ? "bg-slate-800 text-emerald-400 font-semibold" : "text-slate-400 hover:text-slate-200"}`}
                      >
                        Java
                      </button>
                      <button
                        onClick={() => setActiveTab("android-xml")}
                        className={`flex-1 py-2 text-xs font-medium rounded-lg transition-all cursor-pointer ${activeTab === "android-xml" ? "bg-slate-800 text-emerald-400 font-semibold" : "text-slate-400 hover:text-slate-200"}`}
                      >
                        AndroidManifest
                      </button>
                    </div>

                    {/* Code Box */}
                    <div className="relative">
                      <button
                        onClick={() => copyToClipboard(getExoPlayerSnippet(), "snippet_code")}
                        className="absolute top-3 right-3 p-1.5 bg-slate-900 hover:bg-slate-800 rounded-md border border-slate-800/80 hover:border-slate-700 text-slate-400 hover:text-slate-200 transition-colors cursor-pointer"
                        title="Copiar código fuente"
                      >
                        {copiedText === "snippet_code" ? (
                          <Check className="w-4 h-4 text-emerald-400" />
                        ) : (
                          <Copy className="w-4 h-4" />
                        )}
                      </button>
                      <pre className="bg-slate-950 text-[11px] font-mono text-slate-300 p-4 rounded-xl border border-slate-800 overflow-x-auto max-h-[380px] leading-relaxed select-text">
                        <code>{getExoPlayerSnippet()}</code>
                      </pre>
                    </div>

                    <div className="mt-4 bg-amber-500/5 border border-amber-500/10 rounded-xl p-3.5 flex items-start gap-2.5">
                      <ShieldAlert className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" />
                      <div>
                        <h4 className="text-xs font-semibold text-amber-400">Tip de Ingeniería (CORS / Referer)</h4>
                        <p className="text-[11px] text-slate-400 mt-1 leading-relaxed">
                          La mayoría de los servidores de video (como <span className="text-indigo-400 font-mono">vimeos.net</span>) protegen sus streams de la reproducción no autorizada. Si los cargas en un reproductor móvil básico, fallará con un error HTTP 403. Para solucionar esto, es <strong className="text-slate-200 font-semibold">imperativo</strong> inyectar la propiedad de cabecera <span className="text-emerald-400 font-mono">Referer</span> en las peticiones HTTP del reproductor, tal como muestra el código de arriba.
                        </p>
                      </div>
                    </div>
                  </div>
                </motion.div>
              ) : (
                <motion.div
                  key="intro-card"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="bg-slate-900/40 backdrop-blur-md border border-slate-800/40 rounded-2xl p-8 text-center flex flex-col items-center justify-center min-h-[350px] shadow-lg"
                >
                  <div className="w-14 h-14 bg-indigo-500/10 border border-indigo-500/20 rounded-2xl flex items-center justify-center text-indigo-400 mb-4 animate-pulse">
                    <FileVideo className="w-7 h-7" />
                  </div>
                  <h3 className="text-lg font-display font-medium text-slate-200">
                    Listo para convertir enlaces
                  </h3>
                  <p className="text-sm text-slate-500 mt-2 max-w-sm mx-auto leading-relaxed">
                    Pega un enlace de reproducción integrado arriba (por ejemplo, de <span className="text-indigo-400 font-mono font-semibold">vimeos.net</span> o reproductores de películas/streaming) y haz clic en "Convertir Enlace".
                  </p>
                  
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-w-lg w-full mt-8 border-t border-slate-800/40 pt-6">
                    <div className="text-left bg-slate-950/40 p-3.5 rounded-xl border border-slate-800/40">
                      <h4 className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                        <Layers className="w-3.5 h-3.5 text-emerald-400" />
                        Desempaquetador Integrado
                      </h4>
                      <p className="text-[11px] text-slate-500 mt-1 leading-relaxed">
                        Descomprime automáticamente scripts protegidos con Dean Edwards Packer u ofuscación de variables en base64 para hallar el archivo HLS oculto.
                      </p>
                    </div>
                    <div className="text-left bg-slate-950/40 p-3.5 rounded-xl border border-slate-800/40">
                      <h4 className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                        <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                        Análisis Guiado por IA
                      </h4>
                      <p className="text-[11px] text-slate-500 mt-1 leading-relaxed">
                        Si los algoritmos de escaneo fallan, Gemini analiza los scripts del reproductor simulando un entorno de ejecución para resolver las rutas del video.
                      </p>
                    </div>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
        
        {/* Footer info branding */}
        <footer className="mt-16 text-center border-t border-slate-800/40 pt-8 pb-12">
          <p className="text-xs text-slate-600">
            Desarrollado de forma profesional con soporte de reproducción HLS nativa en navegadores.
          </p>
          <p className="text-[10px] text-slate-700 mt-1.5 font-mono">
            Licencia Apache-2.0 • Google AI Studio
          </p>
        </footer>

      </div>
    </div>
  );
}
