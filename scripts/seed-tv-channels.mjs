import { createClient } from "@supabase/supabase-js";

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_KEY = process.env.SUPABASE_KEY;

if (!SUPABASE_URL || !SUPABASE_KEY) {
  console.error("SUPABASE_URL and SUPABASE_KEY required");
  process.exit(1);
}

const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

const raw = `
A&E HD [Entretenimiento] -> http://181.78.79.131:8000/play/a0mt
Adult Swim [Entretenimiento] -> http://181.78.79.131:8000/play/a0pk
BETHEL TV [Religion] -> http://181.78.79.131:8000/play/a0pn
CHV [Chile] -> http://181.78.79.131:8000/play/a0r0
CNN Espanol [Noticias] -> http://181.78.79.131:8000/play/a0p3
Canal 13 [Chile] -> http://181.78.79.131:8000/play/a0n4
Canal 24 Horas [Chile] -> http://181.78.79.131:8000/play/a0qr
Canal RCN [Colombia] -> http://181.78.79.131:8000/play/a0sc
Canal del Congreso [General] -> http://181.78.79.131:8000/play/a0se
Capital [Chile] -> http://181.78.79.131:8000/play/a0sj
Cinecanal [Peliculas] -> http://181.78.79.131:8000/play/a0pp
City TV [Colombia] -> http://181.78.79.131:8000/play/a0si
Comedy Central HD [Entretenimiento] -> http://181.78.79.131:8000/play/a0mu
De Pelicula [Peliculas] -> http://181.78.79.131:8000/play/a0qb
Discovery Science [Documentales] -> http://181.78.79.131:8000/play/a0pb
Discovery Turbo [Documentales] -> http://181.78.79.131:8000/play/a0p8
Disney Jr. [Entretenimiento] -> http://181.78.79.131:8000/play/a0p7
ESPN [Deportes] -> http://181.78.79.131:8000/play/a0s7
ESPN 2 HD [Deportes] -> http://181.78.79.131:8000/play/a0qj
ESPN 3 HD [Deportes] -> http://181.78.79.131:8000/play/a0s6
ESPN 4 HD [Deportes] -> http://181.78.79.131:8000/play/a0s9
ESPN 5 HD [Deportes] -> http://181.78.79.131:8000/play/a0s5
ESPN 6 HD [Deportes] -> http://181.78.79.131:8000/play/a0s3
ESPN 7 HD [Deportes] -> http://181.78.79.131:8000/play/a0s8
HBO [Peliculas] -> http://181.78.79.131:8000/play/a0qd
HBO Xtreme HD [Peliculas] -> http://181.78.79.131:8000/play/a0qy
HGTV [Documentales] -> http://181.78.79.131:8000/play/a0pf
HOLA TV HD [General] -> http://181.78.79.131:8000/play/a0pw
ID [General] -> http://181.78.79.131:8000/play/a0sl
La Red [Chile] -> http://181.78.79.131:8000/play/a0qq
Las Estrellas HD [General] -> http://181.78.79.131:8000/play/a0qu
MEGA HD [Chile] -> http://181.78.79.131:8000/play/a0qn
MTV HD [Entretenimiento] -> http://181.78.79.131:8000/play/a0nc
Nickelodeon HD [Entretenimiento] -> http://181.78.79.131:8000/play/a0qm
Panamericana TV [Latinoamerica] -> http://181.78.79.131:8000/play/a0so
Pasiones HD [Peliculas] -> http://181.78.79.131:8000/play/a0qh
RCN Novelas [Colombia] -> http://181.78.79.131:8000/play/a0sk
Space HD [Peliculas] -> http://181.78.79.131:8000/play/a0q2
TLC [Documentales] -> http://181.78.79.131:8000/play/a0t3
TNT Sports Premium SD [Deportes] -> http://181.78.79.131:8000/play/a0q9
TRO [Colombia] -> http://181.78.79.131:8000/play/a0sh
TV+ [Chile] -> http://181.78.79.131:8000/play/a0qo
Tlnovelas [Peliculas] -> http://181.78.79.131:8000/play/a0t6
Tooncast [Entretenimiento] -> http://181.78.79.131:8000/play/a0pe
UNIVERSAL CHANNEL HD [Peliculas] -> http://181.78.79.131:8000/play/a0mw
ViaX [Chile] -> http://181.78.79.131:8000/play/a0t4
Warner [Peliculas] -> http://181.78.79.131:8000/play/a0sz
Warner TV HD [Peliculas] -> http://181.78.79.131:8000/play/a0pz
Zoom [Chile] -> http://181.78.79.131:8000/play/a0sg
France 24 English [Noticias] -> https://static.france24.com/live/F24_EN_LO_HLS/live_web.m3u8
TVN [Chile] -> https://rudo.video/live/tvn
Canal 13 [Chile] -> https://rudo.video/live/c13
Mega [Chile] -> https://rudo.video/live/mega
AE [Entretenimiento] -> http://181.78.8.199:8000/play/a0al/index.m3u8
AE [Entretenimiento] -> http://181.78.8.199:8000/play/a0ct/index.m3u8
ANIMAL PLANET [Documentales] -> http://181.78.8.199:8000/play/a06w/index.m3u8
ANTV [General] -> http://181.78.8.199:8000/play/a0fo/index.m3u8
AXN [Peliculas] -> http://181.78.8.199:8000/play/a0am/index.m3u8
AXN [Peliculas] -> http://181.78.8.199:8000/play/a0cu/index.m3u8
Animal Planet HD AMAZONAS [Documentales] -> http://181.78.8.199:8000/play/a06w/index.m3u8
BABYTV ENVIADO [Entretenimiento] -> http://181.78.8.199:8000/play/a08d/index.m3u8
BANDAMAX [Musica] -> http://181.78.8.199:8000/play/a0g9/index.m3u8
BITME [General] -> http://181.78.8.199:8000/play/a0ga/index.m3u8
CABLE NOTICIAS [Colombia] -> http://181.78.8.199:8000/play/a0g3/index.m3u8
CABLE NOTICIAS [Colombia] -> http://181.78.8.199:8000/play/a0e8/index.m3u8
CANAL 1 NACIONAL SIN TRANS [Colombia] -> http://181.78.8.199:8000/play/a0cz/index.m3u8
CANAL 12 VALLEDUPAR [General] -> http://181.78.8.199:8000/play/a0e4/index.m3u8
CANAL 13 NACIONAL SIN TRANS [Chile] -> http://181.78.8.199:8000/play/a0d4/index.m3u8
CANAL 13 TRANS [Chile] -> http://181.78.8.199:8000/play/a0dj/index.m3u8
CANAL CAPITAL NACIONAL SIN TRANS [Chile] -> http://181.78.8.199:8000/play/a0d3/index.m3u8
CANAL CAPITAL TRANS [Chile] -> http://181.78.8.199:8000/play/a0dk/index.m3u8
CANAL CONGRE NACIONAL SIN TRANS [General] -> http://181.78.8.199:8000/play/a0d2/index.m3u8
CANAL CONGRESO TRANS [Colombia] -> http://181.78.8.199:8000/play/a0di/index.m3u8
CANAL DE LAS ESTRELLAS [General] -> http://181.78.8.199:8000/play/a0as/index.m3u8
CANAL TRO TRANS [Colombia] -> http://181.78.8.199:8000/play/a0dh/index.m3u8
CANAL VENEVICION LOCAL [General] -> http://181.78.8.199:8000/play/a091/index.m3u8
CANALES NACIONALES [General] -> http://181.78.8.199:8000/play/a0cy/index.m3u8
CANALESTRELLAS [General] -> http://181.78.8.199:8000/play/a0cx/index.m3u8
CANALINSTITUCIONAL NACIONAL SIN TRANS [General] -> http://181.78.8.199:8000/play/a0d0/index.m3u8
CARACOL [Colombia] -> http://181.78.8.199:8000/play/a0b0/index.m3u8
TRECE [Espana] -> https://play.cdn.enetres.net/091DB7AFBD77442B9BA2F141DCC182F5021/live.smil/playlist.m3u8
El Toro TV [Espana] -> https://streaming-1.eltorotv.com/lb0/eltorotv-streaming-web/index.m3u8
ETB 1 [Espana] -> https://multimedia.eitb.eus/live-content/etb1hd-hls/master.m3u8
ETB 2 [Espana] -> https://multimedia.eitb.eus/live-content/etb2hd-hls/master.m3u8
Negocios TV [Espana] -> https://streaming013.gestec-video.com/hls/negociostv.m3u8
RTVE Crimen [Espana] -> https://ztnr.rtve.es/ztnr/6924117.m3u8
RTVE Cuentame [Espana] -> https://ztnr.rtve.es/ztnr/6909843.m3u8
RTVE Somos Cine [Espana] -> https://ztnr.rtve.es/ztnr/6909845.m3u8
Canal Parlamento [Espana] -> https://congresodirecto.akamaized.net/hls/live/2037973/canalparlamento/master.m3u8
DW Espanol [Europa] -> https://dwamdstream104.akamaized.net/hls/live/2015530/dwstream104/index.m3u8
RTP 1 Portugal [Europa] -> https://streaming-live.rtp.pt/liverepeater/smil:rtp1HD.smil/playlist.m3u8
RTP 2 Portugal [Europa] -> https://streaming-live.rtp.pt/liverepeater/smil:rtp2HD.smil/playlist.m3u8
RTP Internacional [Europa] -> https://streaming-live.rtp.pt/liverepeater/smil:rtpint.smil/playlist.m3u8
Bloomberg Europe [Europa] -> https://www.bloomberg.com/media-manifest/streams/eu.m3u8
Tagesschau24 (Alemania) [Europa] -> https://tagesschau.akamaized.net/hls/live/2020115/tagesschau/tagesschau_1/master.m3u8
France 24 Espanol [Europa] -> https://static.france24.com/live/F24_ES_LO_HLS/live_web.m3u8
France 24 Frances [Europa] -> https://static.france24.com/live/F24_FR_LO_HLS/live_web.m3u8
France 24 Ingles [Europa] -> https://static.france24.com/live/F24_EN_LO_HLS/live_web.m3u8
EITB Basque [Europa] -> https://multimedia.eitb.eus/live-content/eitbbasque-hls/master.m3u8
TDTChannels ES (playlist) [Fuentes Online] -> https://www.tdtchannels.com/lists/tv.m3u
IPTV-org Mexico (playlist) [Fuentes Online] -> https://iptv-org.github.io/iptv/countries/mx.m3u
IPTV-org Chile (playlist) [Fuentes Online] -> https://iptv-org.github.io/iptv/countries/cl.m3u
`;

function parseChannels(text) {
  const lines = text.split("\n").map((l) => l.trim()).filter(Boolean);
  const rows = [];
  for (const line of lines) {
    const idx = line.lastIndexOf(" -> ");
    if (idx === -1) continue;
    const left = line.slice(0, idx).trim();
    const m3u8 = line.slice(idx + 4).trim();
    const m = left.match(/^(.*)\s+\[(.+)\]\s*$/);
    if (!m) {
      rows.push({ canal: left, pais: "unknown", m3u8, logo: "" });
      continue;
    }
    rows.push({ canal: m[1].trim(), pais: m[2].trim(), m3u8, logo: "" });
  }
  return rows;
}

const channels = parseChannels(raw);
console.log(`Parsed ${channels.length} channels`);

const { error } = await supabase.from("tv_channels").insert(channels);
if (error) {
  console.error("Insert error:", error);
  process.exit(1);
}
console.log("Inserted successfully");
