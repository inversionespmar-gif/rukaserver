import path from "path";
import express from "express";
import fs from "fs";

export function registerUpdateRoutes(app, { config }) {
  // The public server may terminate TLS in a reverse proxy.
  app.set("trust proxy", 1);
  const updateDir = path.join(process.cwd(), "public", "update");
  if (!fs.existsSync(updateDir)) {
    fs.mkdirSync(updateDir, { recursive: true });
  }

  // Serve APK download file statically
  app.use("/update", express.static(updateDir));

  // OTA Version check endpoint for Android App
  app.get(["/api/version", "/version"], (req, res) => {
    const host = req.headers.host || `localhost:${config?.port || 3000}`;
    const forwardedProtocol = String(req.headers["x-forwarded-proto"] || "").split(",")[0].trim();
    const protocol = forwardedProtocol || req.protocol || "http";
    const configuredBase = process.env.RUKATV_PUBLIC_BASE_URL || config?.publicBaseUrl;
    const defaultApkUrl = configuredBase
      ? `${String(configuredBase).replace(/\/$/, "")}/update/app-release.apk`
      : `${protocol}://${host}/update/app-release.apk`;

    const versionFile = path.join(process.cwd(), "version.json");
    let versionData = {
      versionCode: 2,
      versionName: "1.1.0",
      apkUrl: defaultApkUrl,
      releaseNotes: "• Sincronización instantánea de controles Play/Pausa.\n• Contador de tiempo en vivo y barra de progreso fluida.\n• Captura de pantalla nativa en la galería.\n• Menú de subtítulos CC y calidad 4K HD.\n• Modo Picture-in-Picture (PiP).",
      forceUpdate: false
    };

    if (fs.existsSync(versionFile)) {
      try {
        const customData = JSON.parse(fs.readFileSync(versionFile, "utf-8"));
        versionData = { ...versionData, ...customData };
        if (!customData.apkUrl) {
          versionData.apkUrl = defaultApkUrl;
        }
      } catch (e) {
        console.error("Error reading version.json:", e);
      }
    }

    res.json(versionData);
  });
}
