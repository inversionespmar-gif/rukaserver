/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import express from "express";
import path from "path";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI, Type } from "@google/genai";
import dotenv from "dotenv";

// Load environment variables
dotenv.config();

// Initialize Express app
const app = express();
const PORT = 3000;

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Types for conversion trace
interface TraceStep {
  step: string;
  status: "pending" | "success" | "failed" | "info";
  message: string;
  timestamp: string;
}

// Function to add a step to the trace
function addTrace(trace: TraceStep[], step: string, status: "pending" | "success" | "failed" | "info", message: string) {
  trace.push({
    step,
    status,
    message,
    timestamp: new Date().toLocaleTimeString(),
  });
}

// Helper to resolve relative URLs
function makeAbsolute(url: string, baseUrl: string): string {
  try {
    return new URL(url, baseUrl).href;
  } catch {
    return url;
  }
}

// Unpacker for Dean Edwards Packed JS
function unpackDeanEdwards(packed: string): string {
  const pattern = /eval\s*\(\s*function\s*\(\s*p\s*,\s*a\s*,\s*c\s*,\s*k\s*,\s*e\s*,\s*[rd]\s*\)\s*\{(?:[^{}]*|\{[^{}]*\})*\}\s*\(\s*(['"`].*?['"`]|[^,]+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*('[^']*'|"[^"]*"|`[^`]*`|.+?)\s*\.split\s*\(\s*['"`]\|['"`]\s*\)/i;
  
  const match = packed.match(pattern);
  if (!match) return "";

  try {
    let p = match[1];
    if ((p.startsWith("'") && p.endsWith("'")) || (p.startsWith('"') && p.endsWith('"')) || (p.startsWith('`') && p.endsWith('`'))) {
      p = p.slice(1, -1);
    }
    
    p = p.replace(/\\'/g, "'").replace(/\\"/g, '"');
    
    const a = parseInt(match[2], 10);
    const c = parseInt(match[3], 10);
    const kStr = match[4];
    
    let k: string[] = [];
    if ((kStr.startsWith("'") && kStr.endsWith("'")) || (kStr.startsWith('"') && kStr.endsWith('"')) || (kStr.startsWith('`') && kStr.endsWith('`'))) {
      k = kStr.slice(1, -1).split('|');
    } else {
      k = kStr.split('|');
    }
    
    const e = (c: number): string => {
      return (c < a ? "" : e(Math.floor(c / a))) + (c % a > 35 ? String.fromCharCode(c % a + 29) : (c % a).toString(36));
    };
    
    const d: Record<string, string> = {};
    for (let i = 0; i < c; i++) {
      d[e(i)] = k[i] || e(i);
    }
    
    const unpacked = p.replace(/\b\w+\b/g, (token) => {
      return d[token] !== undefined ? d[token] : token;
    });
    
    return unpacked;
  } catch (err) {
    console.error("Error unpacking packed JS:", err);
    return "";
  }
}

// Extract base64 strings that look like URLs
function extractBase64Urls(text: string): string[] {
  const urls: string[] = [];
  const base64Regex = /['"`]([A-Za-z0-9+/]{16,}=*)['"`]/g;
  let match;
  while ((match = base64Regex.exec(text)) !== null) {
    try {
      const decoded = Buffer.from(match[1], "base64").toString("utf-8");
      if (/^https?:\/\/[^\s"'`<>]+/i.test(decoded)) {
        urls.push(decoded);
      }
    } catch {
      // Not a valid base64 or URL
    }
  }
  return urls;
}

// Primary API conversion route
app.post("/api/convert", async (req, res) => {
  const { url, useAi } = req.body;
  const trace: TraceStep[] = [];
  const allFoundUrls: string[] = [];
  let videoUrl: string | undefined;
  let videoType: "m3u8" | "mp4" | "unknown" = "unknown";
  let title: string | undefined;
  let methodUsed: "regex" | "unpacker" | "gemini_ai" | "fallback" = "fallback";

  if (!url || typeof url !== "string") {
    addTrace(trace, "Validación", "failed", "El enlace provisto no es válido.");
    res.status(400).json({ success: false, error: "URL inválida", trace });
    return;
  }

  // Ensure protocol is present
  let targetUrl = url.trim();
  if (!/^https?:\/\//i.test(targetUrl)) {
    targetUrl = "https://" + targetUrl;
    addTrace(trace, "Formateo", "info", `Protocolo ausente. Ajustado a: ${targetUrl}`);
  } else {
    addTrace(trace, "Formateo", "success", `Enlace recibido correctamente: ${targetUrl}`);
  }

  try {
    // 1. Fetch HTML from URL
    addTrace(trace, "Petición HTTP", "pending", `Obteniendo contenido HTML de ${targetUrl}...`);
    
    const origin = new URL(targetUrl).origin;
    const fetchResponse = await fetch(targetUrl, {
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Referer": origin,
        "Origin": origin
      }
    });

    if (!fetchResponse.ok) {
      throw new Error(`Código de estado HTTP: ${fetchResponse.status} ${fetchResponse.statusText}`);
    }

    const html = await fetchResponse.text();
    addTrace(trace, "Petición HTTP", "success", `HTML obtenido con éxito (${Math.round(html.length / 1024)} KB)`);

    // Extract Title if possible
    const titleMatch = html.match(/<title>(.*?)<\/title>/i);
    if (titleMatch && titleMatch[1]) {
      title = titleMatch[1].trim();
      addTrace(trace, "Metadatos", "info", `Título del video/página encontrado: "${title}"`);
    }

    // Helper to scan text for video streams
    const scanForUrls = (text: string, sourceLabel: string) => {
      // Regular Expressions for .m3u8 and .mp4
      const m3u8Regex = /(https?:\/\/[^\s"'`<>]+?\.(?:m3u8)(?:\?[^\s"'`<>]*)?)/gi;
      const mp4Regex = /(https?:\/\/[^\s"'`<>]+?\.(?:mp4)(?:\?[^\s"'`<>]*)?)/gi;
      
      // Relative or protocol-relative streams
      const relativeRegex = /["'](\/\/[^\s"'`<>]+?\.(?:m3u8|mp4)(?:\?[^\s"'`<>]*)?)["']/gi;
      const localPathRegex = /["'](\/[^\s"'`<>]+\.(?:m3u8|mp4)(?:\?[^\s"'`<>]*)?)["']/gi;

      let foundCount = 0;

      // Extract absolute m3u8s
      let match;
      while ((match = m3u8Regex.exec(text)) !== null) {
        const decodedUrl = match[1].replace(/\\/g, ""); // Clean escaping backslashes
        const absUrl = makeAbsolute(decodedUrl, targetUrl);
        if (!allFoundUrls.includes(absUrl)) {
          allFoundUrls.push(absUrl);
          foundCount++;
        }
      }

      // Extract absolute mp4s
      while ((match = mp4Regex.exec(text)) !== null) {
        const decodedUrl = match[1].replace(/\\/g, "");
        const absUrl = makeAbsolute(decodedUrl, targetUrl);
        if (!allFoundUrls.includes(absUrl)) {
          allFoundUrls.push(absUrl);
          foundCount++;
        }
      }

      // Extract protocol-relative (e.g. //site.com/video.m3u8)
      while ((match = relativeRegex.exec(text)) !== null) {
        const relativeUrl = match[1];
        const protocol = new URL(targetUrl).protocol;
        const absUrl = makeAbsolute(protocol + relativeUrl, targetUrl);
        if (!allFoundUrls.includes(absUrl)) {
          allFoundUrls.push(absUrl);
          foundCount++;
        }
      }

      // Extract relative path streams (e.g. /streams/video.m3u8)
      while ((match = localPathRegex.exec(text)) !== null) {
        const absUrl = makeAbsolute(match[1], targetUrl);
        if (!allFoundUrls.includes(absUrl)) {
          allFoundUrls.push(absUrl);
          foundCount++;
        }
      }

      if (foundCount > 0) {
        addTrace(trace, "Escaneo", "success", `Se encontraron ${foundCount} enlace(s) de stream en [${sourceLabel}]`);
      }
    };

    // 2. Perform standard regex scan on raw HTML
    addTrace(trace, "Escaneo Estándar", "pending", "Buscando enlaces de video directamente en el HTML...");
    scanForUrls(html, "HTML Raw");

    // Replace common escapes like \u002F or \u003A and scan again
    const cleanedHtml = html.replace(/\\u([0-9a-fA-F]{4})/g, (_, grp) => {
      return String.fromCharCode(parseInt(grp, 16));
    });
    if (cleanedHtml !== html) {
      addTrace(trace, "Desofuscación Unicode", "info", "Se detectaron caracteres Unicode escapados. Analizando versión decodificada...");
      scanForUrls(cleanedHtml, "HTML Decodificado Unicode");
    }

    // Scan for Base64 URLs
    const base64Urls = extractBase64Urls(html);
    if (base64Urls.length > 0) {
      addTrace(trace, "Desofuscación Base64", "success", `Se descifraron ${base64Urls.length} enlace(s) codificados en Base64.`);
      for (const bUrl of base64Urls) {
        const absUrl = makeAbsolute(bUrl, targetUrl);
        if (!allFoundUrls.includes(absUrl)) {
          allFoundUrls.push(absUrl);
        }
      }
    }

    // 3. Scan for Packed JS (Dean Edwards)
    addTrace(trace, "Descompresión JS", "pending", "Buscando bloques de código JavaScript compactados (Dean Edwards packer)...");
    const evalBlocks = html.match(/eval\s*\(\s*function\s*\(.*?\.split\s*\(\s*['"`]\|['"`]\s*\)\s*\)\s*\)/gi);
    if (evalBlocks) {
      addTrace(trace, "Descompresión JS", "info", `Se encontraron ${evalBlocks.length} bloque(s) compactados.`);
      evalBlocks.forEach((block, idx) => {
        const unpacked = unpackDeanEdwards(block);
        if (unpacked) {
          addTrace(trace, "Descompresión JS", "success", `Bloque #${idx + 1} desempaquetado correctamente.`);
          scanForUrls(unpacked, `JS Descomprimido #${idx + 1}`);
        }
      });
    } else {
      addTrace(trace, "Descompresión JS", "success", "No se detectaron scripts compactados estándar.");
    }

    // 4. Fallback to Gemini AI-powered extraction if requested or if standard regex found nothing
    const shouldRunAi = useAi || allFoundUrls.length === 0;
    const hasApiKey = !!process.env.GEMINI_API_KEY;

    if (shouldRunAi) {
      if (!hasApiKey) {
        addTrace(
          trace, 
          "Extracción por IA", 
          "info", 
          "La extracción con IA fue solicitada o sugerida, pero GEMINI_API_KEY no está configurada. Continuando con la búsqueda estándar."
        );
      } else {
        addTrace(
          trace, 
          "Extracción por IA", 
          "pending", 
          useAi 
            ? "Invocando Inteligencia Artificial (Gemini 3.5 Flash) para análisis profundo..."
            : "No se hallaron streams con métodos rápidos. Inicializando extracción asistida por IA..."
        );

        try {
          const ai = new GoogleGenAI({
            apiKey: process.env.GEMINI_API_KEY,
            httpOptions: {
              headers: {
                "User-Agent": "aistudio-build",
              }
            }
          });

          // Trim HTML to essential script tags, sources, embeds, titles to fit context and optimize
          const scripts: string[] = [];
          const scriptRegex = /<script\b[^>]*>([\s\S]*?)<\/script>/gi;
          let scriptMatch;
          while ((scriptMatch = scriptRegex.exec(html)) !== null && scripts.length < 15) {
            if (scriptMatch[1].trim().length > 30) {
              scripts.push(scriptMatch[0].trim().slice(0, 5000)); // limit length per script
            }
          }

          const embedsAndVideo: string[] = [];
          const embedRegex = /<(video|source|iframe|embed)\b[^>]*>/gi;
          let embedMatch;
          while ((embedMatch = embedRegex.exec(html)) !== null && embedsAndVideo.length < 20) {
            embedsAndVideo.push(embedMatch[0]);
          }

          const trimmedContext = `
URL del Embed: ${targetUrl}
Título de la Página: ${title || "Desconocido"}

--- ESTRUCTURA DE ELEMENTOS MULTIMEDIA Y EMBEDS ---
${embedsAndVideo.join("\n")}

--- SCRIPTS DE CONFIGURACIÓN DEL REPRODUCTOR (MUESTRA) ---
${scripts.join("\n\n").slice(0, 15000)}
          `;

          const prompt = `
Tu rol es extraer el enlace de transmisión de video directo (.m3u8 para HLS o .mp4 para reproducción directa) de la página web embed provista.
Analiza el código HTML/JS provisto. El enlace puede estar dentro de un objeto JSON, asignado a una variable de configuración de reproductores (como JWPlayer, Clappr, Video.js, Plyr, o variables tipo "file", "sources", "hls", "url"), o codificado.

IMPORTANTE:
- Resuelve cualquier ruta relativa usando el URL de origen: ${targetUrl}.
- Devuelve la respuesta en formato JSON estructurado con el siguiente esquema exacto:
{
  "success": boolean,
  "url": string (o null si no se encuentra),
  "type": "m3u8" | "mp4" | "unknown",
  "explanation": string (explicación de dónde o cómo lo encontraste),
  "otherUrls": string[] (otros enlaces de transmisión detectados)
}
`;

          const response = await ai.models.generateContent({
            model: "gemini-3.5-flash",
            contents: trimmedContext,
            config: {
              systemInstruction: "Eres un experto en desofuscación de páginas de transmisión de video y extracción de enlaces directos (HLS m3u8 o MP4).",
              responseMimeType: "application/json",
              responseSchema: {
                type: Type.OBJECT,
                properties: {
                  success: { type: Type.BOOLEAN },
                  url: { type: Type.STRING },
                  type: { type: Type.STRING },
                  explanation: { type: Type.STRING },
                  otherUrls: {
                    type: Type.ARRAY,
                    items: { type: Type.STRING }
                  }
                },
                required: ["success", "url", "type", "explanation"]
              }
            }
          });

          if (response.text) {
            const aiResult = JSON.parse(response.text.trim());
            if (aiResult.success && aiResult.url) {
              const absAiUrl = makeAbsolute(aiResult.url, targetUrl);
              addTrace(trace, "Extracción por IA", "success", `¡IA localizó el stream! Encontrado por: ${aiResult.explanation}`);
              
              if (!allFoundUrls.includes(absAiUrl)) {
                allFoundUrls.unshift(absAiUrl); // AI found url takes precedence
              }
              
              if (aiResult.otherUrls && Array.isArray(aiResult.otherUrls)) {
                for (const other of aiResult.otherUrls) {
                  const absOther = makeAbsolute(other, targetUrl);
                  if (!allFoundUrls.includes(absOther)) {
                    allFoundUrls.push(absOther);
                  }
                }
              }
              methodUsed = "gemini_ai";
            } else {
              addTrace(trace, "Extracción por IA", "info", `La IA analizó el código pero no logró determinar un enlace de reproducción válido.`);
            }
          }
        } catch (aiErr: any) {
          addTrace(trace, "Extracción por IA", "failed", `Ocurrió un error en la API de Gemini: ${aiErr.message || aiErr}`);
          console.error("Gemini conversion error:", aiErr);
        }
      }
    }

    // 5. Select the primary/best video URL from found list
    if (allFoundUrls.length > 0) {
      // Prioritize m3u8 over mp4 for HLS streaming, unless the user requested mp4 specifically.
      // But actually, vimeos.net or general players usually run on HLS.
      const m3u8Stream = allFoundUrls.find(u => u.includes(".m3u8"));
      const mp4Stream = allFoundUrls.find(u => u.includes(".mp4"));
      
      videoUrl = m3u8Stream || mp4Stream || allFoundUrls[0];
      
      if (videoUrl.includes(".m3u8")) {
        videoType = "m3u8";
      } else if (videoUrl.includes(".mp4")) {
        videoType = "mp4";
      }

      if (methodUsed !== "gemini_ai") {
        methodUsed = html.includes("eval(function") ? "unpacker" : "regex";
      }

      addTrace(trace, "Finalización", "success", `¡Conversión exitosa! Enlace de reproducción establecido: ${videoUrl}`);

      res.json({
        success: true,
        url: videoUrl,
        type: videoType,
        title: title || new URL(targetUrl).hostname,
        methodUsed,
        allFoundUrls,
        trace,
        headers: {
          "Referer": origin,
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
      });
    } else {
      addTrace(trace, "Finalización", "failed", "No se detectó ningún flujo de video m3u8 o mp4 en los recursos analizados.");
      res.json({
        success: false,
        error: "No se encontraron enlaces de video directos. Intente usar la extracción con IA.",
        trace
      });
    }

  } catch (err: any) {
    addTrace(trace, "Petición HTTP", "failed", `Error al conectar o descargar la URL: ${err.message || err}`);
    res.status(500).json({
      success: false,
      error: `Error al acceder al sitio web: ${err.message || err}`,
      trace
    });
  }
});

// Configure Vite or Static files serving
async function setupViteOrStatic() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on port ${PORT}`);
  });
}

setupViteOrStatic();
